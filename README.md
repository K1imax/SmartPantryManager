# Smart Pantry Manager

A Java Android app that reduces food waste by suggesting recipes based **strictly** on the ingredients the user already has at home.

## Features
- Add, edit and delete pantry items (name, quantity, unit, optional expiry date)
- 16 pre-loaded recipes, seeded on first run
- Suggested Recipes screen using a strict-matching rule: a recipe appears only if every ingredient is in the pantry in at least the required quantity
- Matching handles plurals ("tomatoes" matches "tomato") and unit conversion (kg to g, l to ml, tbsp, tsp)
- Recipe detail screen with ingredients and method
- Settings screen with an expiring-soon highlight toggle
- Friendly message when no recipes match

## Database choice: SQLite
I chose SQLite (SQLiteOpenHelper) because it runs locally on the device, needs no server or internet connection, supports full CRUD, and keeps data after the app is closed and reopened.

Tables: `pantry_items`, `recipes`, `recipe_ingredients`.

## Setup and run
1. Clone the repository: `git clone https://github.com/K1imax/SmartPantryManager`
2. Open the project in Android Studio.
3. Wait for the Gradle sync to finish.
4. Create or select an emulator (or connect a phone) and click **Run**.
5. No API keys or extra setup are needed. The app does not use maps or location.

## Author
Tshwane , Student number 402413178, Mobile App Development 700