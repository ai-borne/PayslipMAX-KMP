package com.payslipmax.pdfparser.ui.theme

object LegalStrings {
    const val settingsHelpFaqContent = """Q: How is my payslip decrypted and parsed?
A: All decryption and parsing run entirely offline on your device using local sandboxed PDF libraries. Your PDF password is never uploaded to any server or stored in plaintext.

Q: Where is my personal and financial data stored?
A: Your data is saved in a secure, local Room database inside the app's private sandbox directory on your device.

Q: How does the AI Insights feature work?
A: You can choose between two AI options in Settings:
1. Local Gemma AI: Runs 100% offline on your device. Your data never leaves your device.
2. Cloud Gemini AI: Anonymizes all data by stripping out your Name, CDA A/C No, PAN, and other PII. Only numeric financial rows are processed to generate insights.

Q: How does Secure Backup & Sync work?
A: Backups are fully encrypted client-side using AES-256 with a key derived from your password. Only you can decrypt the backup, ensuring cross-platform sync (iOS & Android) remains private.

Q: What happens if I use "Reset App & Clear Data"?
A: It permanently wipes your local database statements, cached PDF documents, saved custom corrections, passcode PIN, and local encryption keys. This cannot be undone.

Q: Is this application officially associated with the PCDA or Indian Army?
A: No. PayslipMax is an independent financial analytics tool. It is not affiliated with, endorsed by, or connected to the PCDA (O) Pune, the Ministry of Defence, or the Indian Army."""

    const val settingsHelpPrivacyContent = """PayslipMax is committed to protecting your privacy through an offline-first architecture:
1. No Personal Data Collection: We do not collect, store, or monitor your personal or financial data on external servers.
2. Local Sandboxed Processing: PDF decryption, parsing, and ledger storage are done entirely on-device.
3. Secure Backup Encryption: If you configure Secure Backup & Sync, your archive is encrypted on-device via AES-256 before upload. We cannot decrypt or view your backup.
4. Data Control: You have full control over your data and can permanently wipe everything instantly using the "Reset App & Clear Data" option."""

    const val settingsHelpAiContent = """To preserve strict confidentiality, the app enforces the following AI Privacy Promise:
1. Complete PII Stripping: Before sending any payslip data to the Gemini API, the app automatically redacts your Name, CDA Account Number, PAN, and other identifiers.
2. Numeric-Only Processing: Only anonymous, numeric financial fields (Basic Pay, allowances, deductions) are processed to generate tax and savings insights.
3. On-Device Option: You can toggle "Use Local Gemma AI Model" to process insights 100% offline on-device without any network transmission."""

    const val settingsHelpDisclaimerContent = """This analytical tool is for reference purposes only. It is not an official app of the PCDA, Ministry of Defence, or the Indian Army. It does not replace professional advice from chartered accountants or official audit statements."""
}
