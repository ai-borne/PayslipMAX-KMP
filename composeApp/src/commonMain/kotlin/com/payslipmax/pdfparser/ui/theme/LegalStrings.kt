package com.payslipmax.pdfparser.ui.theme

object LegalStrings {
    const val settingsHelpFaqContent = """Q: What is the primary purpose of PayslipMax?
A: PayslipMax is engineered specifically to empower PCDA(O) Pune defence officers with deep financial, salary, and tax insights—including month-on-month trend analytics, DSOP projections, and allowances breakdowns—that are otherwise not easily available through raw, fragmented monthly PDF statements.

Q: How is my payslip decrypted and parsed?
A: All decryption and parsing run 100% offline on your device using local sandboxed PDF libraries. Your PDF password is never uploaded to any server, transmitted over the internet, or stored in plaintext.

Q: Where is my personal and financial data stored?
A: Your data is saved in a secure, local Room database inside the app's private sandbox directory on your device. Zero cloud uploads are performed.

Q: How does the AI Insights feature work?
A: The AI Insights feature uses an on-device Local Gemma AI model that runs 100% offline directly on your device. Your salary figures and payslip data never leave your phone.

Q: How does Secure Backup & Sync work?
A: Backups are fully encrypted client-side using AES-256 with a key derived from your password. Only you can decrypt the backup, ensuring cross-platform sync (iOS & Android) remains 100% private.

Q: What happens if I use "Reset App & Clear Data"?
A: It permanently wipes your local database statements, cached PDF documents, saved custom corrections, passcode PIN, and local encryption keys. This action is instantaneous and cannot be undone.

Q: Is this application officially associated with the PCDA or Indian Army?
A: No. PayslipMax is an independent financial analytics tool created by AI-Borne. It is not affiliated with, endorsed by, or connected to the PCDA (O) Pune, the Ministry of Defence, or the Indian Army.

Q: Does the app collect any telemetry or usage data?
A: We collect anonymous telemetry regarding the Gemma AI model installation status (downloading progress, success, and failure) to troubleshoot and improve installation reliability. No personal or financial data is ever collected. You can disable telemetry at any time in Settings."""

    const val settingsHelpPrivacyContent = """PayslipMax is committed to protecting your privacy through a strict 100% offline-first architecture:
1. Empowering PCDA(O) Officers: We provide rich financial, tax, and salary intelligence without compromising confidentiality.
2. Zero Server Uploads: We do not collect, upload, monitor, or monetize your personal or financial data on external servers or cloud databases.
3. 100% On-Device Processing: PDF decryption, parsing, ledger calculation, and insights generation run exclusively locally on your device.
4. Secure Backup Encryption: If you configure Secure Backup & Sync, your archive is encrypted on-device via AES-256 before saving. We cannot decrypt or view your backup.
5. Full Data Ownership: You retain complete ownership of your data and can permanently erase all local records instantly using "Reset App & Clear Data".
6. Anonymous Telemetry: Optional, non-PII diagnostic events regarding Gemma model installation can be toggled on/off under Settings."""

    const val settingsHelpAiContent = """To preserve military-grade confidentiality, the app enforces the following AI Privacy Promise:
1. 100% On-Device Local Processing: AI financial insights, rule calculations, and projections run entirely offline on your phone using local algorithms and the sideloaded on-device Gemma AI model.
2. Zero Network Data Transmission: Your payslip information, salary details, and personal identifiers are never transmitted across the network or shared with external AI cloud APIs.
3. Complete Confidentiality: Your financial data remains strictly under your control within your device's secured application sandbox."""

    const val settingsHelpDisclaimerContent = """This analytical tool is for reference and productivity purposes only. It is not an official app of the PCDA, Ministry of Defence, or the Indian Army. It does not replace professional advice from chartered accountants or official audit statements."""

    const val settingsTermsOfUse = "Terms of Use"
    const val termsOfUseUrl = "https://www.apple.com/legal/internet-services/itunes/dev/stdeula/"
    const val privacyPolicyUrl = "https://ai-borne.in/support"
}
