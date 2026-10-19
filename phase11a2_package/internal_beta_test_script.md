# Internal Beta Test Script

Please go through this checklist to systematically verify the application.

- `[ ]` **1. First Launch**
  - `[ ]` Install cleanly on the device.
  - `[ ]` Verify the app opens directly to the Dashboard without crashing.

- `[ ]` **2. AI Resilience & Settings**
  - `[ ]` Verify AI features (e.g., Food Parsing) either prompt for an API key or fail gracefully when no key is provided.
  - `[ ]` Go to Settings, enter a Gemini API Key.
  - `[ ]` Go back to Settings and verify the key is masked/encrypted. Clear the key and re-add it.

- `[ ]` **3. Workouts & Smart Coaching**
  - `[ ]` Tap "Start A Workout" from Dashboard.
  - `[ ]` Perform the Readiness Check-In (set energy to 3, soreness to 1, no joint pain). 
  - `[ ]` Verify the app recommends "Normal" load.
  - `[ ]` Perform the Readiness Check-In again, but set knee sensitivity to "High" and energy to "1".
  - `[ ]` Verify the app recommends "Rest or Walk" or "Light".
  - `[ ]` Proceed to a workout.
  - `[ ]` Complete a warm-up set. Verify rest timer triggers.
  - `[ ]` Skip a working set. Verify it highlights as skipped.
  - `[ ]` Finish the workout. Verify the app navigates out properly.

- `[ ]` **4. Nutrition Flows**
  - `[ ]` Go to Nutrition tab.
  - `[ ]` Tap "Add Food" -> Enter "Chicken Breast", Protein: 30g. Save.
  - `[ ]` Verify the entry appears in today's diary.
  - `[ ]` Tap "AI Parse Food". Type: "I ate a banana and a scoop of whey protein".
  - `[ ]` Wait for the AI Review Screen. Accept the candidates. Verify they appear in the diary with an "AI" badge.
  - `[ ]` Switch to tomorrow's date. Tap "Copy Yesterday". Verify yesterday's meals copy over cleanly.

- `[ ]` **5. Body Metrics**
  - `[ ]` Go to Body Metrics.
  - `[ ]` Enter Weight: 80kg, Body Fat: 15%. Save.
  - `[ ]` Re-enter Weight: 79kg for the *same* date. Verify it updates the existing entry instead of duplicating.

- `[ ]` **6. Analytics**
  - `[ ]` Go to Analytics tab.
  - `[ ]` Verify the cards for "Total Weekly Volume", "Workouts Completed", and "Average Macros" reflect the data you just inputted in the steps above.

- `[ ]` **7. Data Persistence**
  - `[ ]` Force-close the app from the Android recent apps screen.
  - `[ ]` Reopen the app. Verify your workout history, nutrition logs, and body metrics are completely preserved.
