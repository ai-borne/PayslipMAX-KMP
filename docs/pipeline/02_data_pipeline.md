# 02. Data Pipeline & Schema Design

This document describes the validation, extraction, normalization, and local database storage layers of PayslipMax.

---

## 1. Upload Layer

### PDF Upload
The user selects a document from local device storage. The system obtains a file descriptor or stream to read the binary data.

### Validation
To prevent invalid files or security exploits:
* **MIME Verification**: The system checks the file header magic bytes (must match `%PDF-`).
* **Size Boundary**: The file size must be less than 5 MB.
* **Integrity Check**: The system opens the PDF catalog structure. If it is corrupted or unreadable, it throws a validation error.

### Duplicate Detection
To prevent importing the same payslip twice:
* **Hash Validation**: A SHA-256 hash of the raw PDF bytes is computed and checked against the database.
* **Month/Year Metadata Check**: During parsing, the month and year are extracted (e.g. "Aug 2024"). The system blocks import if a record with the same month and year already exists in the ledger.

### File Storage
- **Sandbox Directory**: On Android, files are copied to the app's `files/payslips/` internal directory. On iOS, they are stored in the `Documents/payslips/` container.
- **Encryption**: Files are encrypted using AES-256 (via Jetpack Security / EncryptedFile on Android, or FileProtectionType.complete on iOS).

---

## 2. Parsing Layer

### PDF Extraction
- Payslip PDFs from PCDA(O) are vector-based documents containing clear string character segments.
- The system uses native text extraction engines (PDFBox on Android, PDFKit on iOS) to extract raw character tokens with absolute positions `(x, y)`.

### OCR Fallback
- If text extraction yields zero characters (e.g., scanned printout or custom format):
  - **Fallback**: The client renders the PDF pages to high-resolution bitmaps in the background.
  - **OCR Processing**: Runs local on-device OCR (ML Kit Text Recognition on Android, Vision Framework / VNRecognizeTextRequest on iOS) to extract the text.

### Field Normalization & Component Mapping
The raw extracted text is passed through the component mapper:
1. **Redaction**: Regex rules strip out Personal Identifiable Information (PII) like Army Number, Name, Bank Account Number, and PAN.
2. **Parsing Rules**: Standard regex anchors extract pay components.
3. **Normalization**: Number formats are normalized (removing commas, currency symbols like `₹`, and standardizing decimal delimiters).

### Dynamic Token Parsing (DynamicSpatialParser)
To capture custom, unmapped, and arrears entries (such as `ARR-DA`, `ARR-TPTADA`, and recovery items like `ETKT`), the system employs a two-pass token reconstruction process:
1. **First-Pass Filtering**: The parser processes the text line-by-line, filtering out lines containing only blocklisted headers (e.g. `Description`, `Amount`, `Earnings`, `Deductions`) or sentence-style filler words.
2. **Second-Pass Joining**: The remaining lines are joined with space separators into a single-line string. This reconstructs key-value token segments (such as `ARR-DA 9870`) even when the underlying PDF extractor yields key labels and their numeric values on separate lines.
3. **Regex Pattern Extraction**: A regex extraction pattern isolates word and number groupings from the reconstructed single-line string, mapping them into the dynamic `rawEarnings` and `rawDeductions` maps.

---

## 3. Storage Layer & Schemas

### SQLite DB Schemas

#### Entity: `ledger_records`
Holds the historical ledger data for chronological metrics:
```sql
CREATE TABLE IF NOT EXISTS ledger_records (
    id TEXT PRIMARY KEY NOT NULL,
    year INTEGER NOT NULL,
    monthNum INTEGER NOT NULL,
    grossPay REAL NOT NULL,
    netPay REAL NOT NULL,
    dsopSubscription REAL NOT NULL,
    incomeTax REAL NOT NULL,
    hash TEXT UNIQUE NOT NULL
);
```

#### Entity: `financial_insights`
Stores the generated AI Insights to prevent redundant LLM invocations:
```sql
CREATE TABLE IF NOT EXISTS financial_insights (
    month_key TEXT PRIMARY KEY NOT NULL, -- Format: "YYYY-MM"
    payload TEXT NOT NULL,               -- Cached JSON string matching AiInsightReport
    timestamp INTEGER NOT NULL           -- Epoch millisecond cache write time
);
```

### Parsed JSON Payload Schema (Example)
The structured output from the Parsing Layer matches the following schema:

```json
{
  "earnings": {
    "basicPay": 82600.0,
    "dearnessAllowance": 41300.0,
    "militaryServicePay": 15500.0,
    "transportAllowance": 7200.0,
    "transportAllowanceDa": 3600.0,
    "grossPay": 150200.0
  },
  "deductions": {
    "dsopSubscription": 45000.0,
    "incomeTax": 18000.0,
    "agif": 5000.0
  },
  "summary": {
    "grossPay": 150200.0,
    "netRemittance": 82200.0
  },
  "taxAndSavings": {
    "grossSalaryYtd": 751000.0,
    "taxDeductedYtd": 90000.0,
    "dsopFund": {
      "closingBalance": 340500.0
    }
  }
}
```
