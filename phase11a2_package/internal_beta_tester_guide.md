# GymFlow AI Beta Tester Guide

Welcome to the internal beta of GymFlow AI! This document outlines how to get started, set up your device, and test the core flows.

## 1. Installation & Setup
1. **Download APK**: You will receive an `app-debug.apk` file.
2. **Install**: Tap to install. Your Android device may show a "Play Protect" or "Unknown Source" warning because this is an internal beta build that has not been verified by Google Play. Select "Install Anyway".
3. **Android Version**: Ensure you are running Android 7.0 (Nougat) or newer (API 24+).

## 2. Testing AI Features (Optional)
AI features are completely optional. The app will work without an API key, but to test AI food parsing and workout summaries, you must configure a key.
1. Go to the Settings screen (gear icon or menu).
2. Enter a **Gemini API Key** (you can generate one for free from Google AI Studio).
3. **IMPORTANT**: This API key is saved locally in encrypted storage. This is a strictly development-only approach for this internal beta. *Never share your API key*.
4. You can clear the key at any time from the settings screen.

## 3. What to Test First
Please go through the following flows:

### Workouts
1. **A/B/C/D Workouts**: Tap "Start Workout". Complete a few sets. Mark some sets as completed and skip others. Test the rest timer that appears. 
2. **Resume Workout**: Force-close the app mid-workout, reopen it, and tap the active workout banner to ensure your progress was saved.
3. **Smart Coaching**: Try "Readiness Check-In". Enter low energy or high joint pain to see if the app recommends a "Light" or "Rest-Walk" modification.
4. **AI Summary**: Go to the History tab, tap a completed workout, and tap the "Generate AI Summary" button to get a breakdown of your session.

### Nutrition
1. **Manual Entry**: Go to Nutrition, tap "Add Food", and manually enter macros for a meal.
2. **Saved Foods**: Create a new food and save it to your library. Try adding it to your log again.
3. **Copy Yesterday**: In the daily diary, use the "Copy Yesterday" button to clone your previous day's meals.
4. **AI Parser**: Tap the "AI Parse" button and type or paste a text description of your meal (e.g., "I ate 2 eggs and a slice of toast"). Review the AI's estimation and add it to your log.

### Body Metrics & Analytics
1. **Body Metrics**: Add your body weight and body fat percentage. Edit the entry for today and confirm it updates rather than duplicating.
2. **Analytics**: Go to the Dashboard and verify your weekly volume, completed workouts, and macro averages are displayed correctly based on the data you entered.

## 4. Known Limitations
* **Local API Key Storage**: The API key input method is an internal beta mechanism. The final release will not require you to provide your own key.
* **No Cloud Sync**: All data stays on your phone. Deleting the app deletes your data.
* **Meal Editing**: You cannot directly edit a logged meal yet. If you make a mistake, delete the meal log and re-add it.
* **AI Chat**: There is no open-ended chatbot. The AI is restricted to structured parsers and summaries.

## 5. Reporting Bugs
Please use the `bug_report_template.md` format to report any issues you find. Pay special attention to crashes, data that doesn't save, or UI glitches.
