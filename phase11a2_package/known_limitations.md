# GymFlow AI: Internal Beta Known Limitations

Welcome to the internal beta! Please review the following known limitations before testing.

## AI Configuration & Security
*   **Local API Key Storage:** The current version securely encrypts and stores a Gemini API Key locally on your device (`EncryptedSharedPreferences`). This is strictly a **development/internal beta** setup. The final production release will utilize Firebase AI Logic or a secure backend proxy.
*   **Data Restrictions:** The AI only receives summaries of your prompts (e.g., "Generated weekly review"), not your raw sensitive data or exact API key.

## Features Excluded from this Beta
*   **Cloud Sync:** All data is currently stored locally in a Room Database. There is no cross-device sync or cloud backup available yet.
*   **Barcode Scanning:** Adding food items via barcode scanning is planned but not included in this build.
*   **Open-Ended AI Chat:** The AI is strictly integrated into structured features (Food Parsing, Workout Summaries, Progression Explanations, Weekly Reviews). A freeform "Coach Chatbot" is not implemented.

## UX Limitations
*   **Editing Meal Logs:** Direct editing of a logged meal in your daily diary is not yet supported. If you make a mistake, the current supported workflow is to **delete** the meal log and **re-add** it.
*   **Charts & Graphs:** The Analytics dashboard relies on basic summary cards. Advanced visual line/bar graphs will be introduced in a future update.
*   **Release Signing & Build:** Debug APKs are provided for testing. A fully signed release build may trigger Play Protect warnings until it is uploaded via Google Play Console. Note: `assembleRelease` lint checks are temporarily suppressed (`abortOnError = false`) to bypass an internal AGP lint crash for the beta. This will be remediated prior to production.
