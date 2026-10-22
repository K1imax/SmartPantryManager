package com.example.smartpantry;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles all SQLite work: creating tables, seeding recipes on first run,
 * and CRUD operations for pantry items.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "smart_pantry.db";
    private static final int DB_VERSION = 1;

    // Table and column names
    public static final String T_PANTRY = "pantry_items";
    public static final String T_RECIPES = "recipes";
    public static final String T_RECIPE_ING = "recipe_ingredients";

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true); // so deleting a recipe removes its ingredients
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + T_PANTRY + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "name TEXT NOT NULL, "
                + "quantity REAL NOT NULL, "
                + "unit TEXT NOT NULL, "
                + "expiry TEXT)");

        db.execSQL("CREATE TABLE " + T_RECIPES + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "name TEXT NOT NULL, "
                + "steps TEXT NOT NULL)");

        db.execSQL("CREATE TABLE " + T_RECIPE_ING + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "recipe_id INTEGER NOT NULL, "
                + "name TEXT NOT NULL, "
                + "quantity REAL NOT NULL, "
                + "unit TEXT NOT NULL, "
                + "FOREIGN KEY(recipe_id) REFERENCES " + T_RECIPES + "(id) ON DELETE CASCADE)");

        seedRecipes(db); // runs only once, when the database file is first created
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + T_RECIPE_ING);
        db.execSQL("DROP TABLE IF EXISTS " + T_RECIPES);
        db.execSQL("DROP TABLE IF EXISTS " + T_PANTRY);
        onCreate(db);
    }

    // ======================= PANTRY CRUD =======================

    /** CREATE */
    public long addPantryItem(PantryItem item) {
        SQLiteDatabase db = getWritableDatabase();
        return db.insert(T_PANTRY, null, toValues(item));
    }

    /** READ (all) */
    public List<PantryItem> getAllPantryItems() {
        List<PantryItem> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(T_PANTRY, null, null, null, null, null, "name COLLATE NOCASE ASC");
        while (c.moveToNext()) {
            list.add(fromCursor(c));
        }
        c.close();
        return list;
    }

    /** READ (one) */
    public PantryItem getPantryItem(long id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(T_PANTRY, null, "id = ?", new String[]{String.valueOf(id)},
                null, null, null);
        PantryItem item = null;
        if (c.moveToFirst()) {
            item = fromCursor(c);
        }
        c.close();
        return item;
    }

    /** UPDATE */
    public int updatePantryItem(PantryItem item) {
        SQLiteDatabase db = getWritableDatabase();
        return db.update(T_PANTRY, toValues(item), "id = ?",
                new String[]{String.valueOf(item.getId())});
    }

    /** DELETE */
    public int deletePantryItem(long id) {
        SQLiteDatabase db = getWritableDatabase();
        return db.delete(T_PANTRY, "id = ?", new String[]{String.valueOf(id)});
    }

    private ContentValues toValues(PantryItem item) {
        ContentValues v = new ContentValues();
        v.put("name", item.getName());
        v.put("quantity", item.getQuantity());
        v.put("unit", item.getUnit());
        v.put("expiry", item.getExpiryDate());
        return v;
    }

    private PantryItem fromCursor(Cursor c) {
        return new PantryItem(
                c.getLong(c.getColumnIndexOrThrow("id")),
                c.getString(c.getColumnIndexOrThrow("name")),
                c.getDouble(c.getColumnIndexOrThrow("quantity")),
                c.getString(c.getColumnIndexOrThrow("unit")),
                c.getString(c.getColumnIndexOrThrow("expiry")));
    }

    // ======================= RECIPE READS =======================

    /** Returns every recipe with its ingredient list already loaded. */
    public List<Recipe> getAllRecipes() {
        List<Recipe> recipes = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(T_RECIPES, null, null, null, null, null, "name ASC");
        while (c.moveToNext()) {
            Recipe r = new Recipe(
                    c.getLong(c.getColumnIndexOrThrow("id")),
                    c.getString(c.getColumnIndexOrThrow("name")),
                    c.getString(c.getColumnIndexOrThrow("steps")));
            loadIngredients(db, r);
            recipes.add(r);
        }
        c.close();
        return recipes;
    }

    public Recipe getRecipe(long id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(T_RECIPES, null, "id = ?", new String[]{String.valueOf(id)},
                null, null, null);
        Recipe r = null;
        if (c.moveToFirst()) {
            r = new Recipe(
                    c.getLong(c.getColumnIndexOrThrow("id")),
                    c.getString(c.getColumnIndexOrThrow("name")),
                    c.getString(c.getColumnIndexOrThrow("steps")));
            loadIngredients(db, r);
        }
        c.close();
        return r;
    }

    private void loadIngredients(SQLiteDatabase db, Recipe recipe) {
        Cursor c = db.query(T_RECIPE_ING, null, "recipe_id = ?",
                new String[]{String.valueOf(recipe.getId())}, null, null, null);
        while (c.moveToNext()) {
            recipe.getIngredients().add(new RecipeIngredient(
                    c.getString(c.getColumnIndexOrThrow("name")),
                    c.getDouble(c.getColumnIndexOrThrow("quantity")),
                    c.getString(c.getColumnIndexOrThrow("unit"))));
        }
        c.close();
    }

    // ======================= SEED DATA =======================

    /**
     * Adds one recipe and its ingredients.
     * Each ingredient string is "name|quantity|unit".
     */
    private void addRecipe(SQLiteDatabase db, String name, String steps, String... ingredients) {
        ContentValues rv = new ContentValues();
        rv.put("name", name);
        rv.put("steps", steps);
        long recipeId = db.insert(T_RECIPES, null, rv);

        for (String ing : ingredients) {
            String[] p = ing.split("\\|");
            ContentValues iv = new ContentValues();
            iv.put("recipe_id", recipeId);
            iv.put("name", p[0]);
            iv.put("quantity", Double.parseDouble(p[1]));
            iv.put("unit", p[2]);
            db.insert(T_RECIPE_ING, null, iv);
        }
    }

    private void seedRecipes(SQLiteDatabase db) {
        addRecipe(db, "Scrambled Eggs",
                "1. Crack the eggs into a bowl and whisk with the salt.\n2. Melt the butter in a pan on medium heat.\n3. Pour in the eggs and stir gently until just set.\n4. Serve immediately.",
                "egg|3|pcs", "butter|10|g", "salt|1|tsp");

        addRecipe(db, "Cheese Omelette",
                "1. Dice the onion and grate the cheese.\n2. Whisk the eggs and pour into a hot oiled pan.\n3. Add onion and cheese on one half.\n4. Fold over and cook until golden.",
                "egg|3|pcs", "cheese|50|g", "onion|1|pcs", "oil|1|tbsp");

        addRecipe(db, "Tomato Pasta",
                "1. Boil the pasta until tender and drain.\n2. Fry the chopped onion and garlic in olive oil.\n3. Add chopped tomatoes and simmer 10 minutes.\n4. Toss the pasta through the sauce.",
                "pasta|200|g", "tomato|3|pcs", "onion|1|pcs", "garlic|2|pcs", "oil|2|tbsp");

        addRecipe(db, "Simple Pancakes",
                "1. Mix flour, sugar, milk and eggs into a smooth batter.\n2. Heat a lightly oiled pan.\n3. Pour in a ladle of batter and cook until bubbles form.\n4. Flip and cook until golden.",
                "flour|200|g", "milk|300|ml", "egg|2|pcs", "sugar|1|tbsp");

        addRecipe(db, "Egg Fried Rice",
                "1. Cook the rice and let it cool.\n2. Scramble the eggs in a hot pan and set aside.\n3. Stir-fry the diced carrot and onion.\n4. Add rice, eggs and soy sauce and toss until hot.",
                "rice|200|g", "egg|2|pcs", "carrot|1|pcs", "onion|1|pcs", "soy sauce|2|tbsp");

        addRecipe(db, "Cheese Toast",
                "1. Butter the bread slices.\n2. Top with sliced cheese.\n3. Grill or toast until the cheese melts and bubbles.",
                "bread|2|pcs", "cheese|40|g", "butter|10|g");

        addRecipe(db, "Quick Guacamole",
                "1. Mash the avocados in a bowl.\n2. Dice the tomato and onion and stir in.\n3. Squeeze in the lime juice and mix well.\n4. Season and serve.",
                "avocado|2|pcs", "tomato|1|pcs", "onion|1|pcs", "lime|1|pcs");

        addRecipe(db, "Mashed Potatoes",
                "1. Peel and chop the potatoes.\n2. Boil until soft, then drain.\n3. Mash with butter and warm milk until smooth.",
                "potato|4|pcs", "butter|30|g", "milk|100|ml");

        addRecipe(db, "Vegetable Soup",
                "1. Chop the carrots, potatoes and onion.\n2. Add to a pot with the stock.\n3. Simmer for 25 minutes until tender.\n4. Blend or serve chunky.",
                "carrot|2|pcs", "potato|2|pcs", "onion|1|pcs", "vegetable stock|500|ml");

        addRecipe(db, "French Toast",
                "1. Whisk eggs, milk and sugar in a shallow dish.\n2. Dip each bread slice on both sides.\n3. Fry in a buttered pan until golden on each side.",
                "bread|4|pcs", "egg|2|pcs", "milk|100|ml", "sugar|1|tbsp", "butter|10|g");

        addRecipe(db, "Tuna Salad",
                "1. Drain the tuna and flake into a bowl.\n2. Dice the tomatoes and onion and add.\n3. Dress with olive oil and mix.",
                "tuna|1|pcs", "tomato|2|pcs", "onion|1|pcs", "oil|1|tbsp");

        addRecipe(db, "Banana Smoothie",
                "1. Peel the bananas and break into a blender.\n2. Add the milk and honey.\n3. Blend until smooth and serve cold.",
                "banana|2|pcs", "milk|250|ml", "honey|1|tbsp");

        addRecipe(db, "Chicken Rice Bowl",
                "1. Cook the rice.\n2. Slice the chicken and fry in oil with chopped onion until cooked through.\n3. Serve the chicken over the rice.",
                "chicken breast|300|g", "rice|200|g", "onion|1|pcs", "oil|1|tbsp");

        addRecipe(db, "Garlic Butter Noodles",
                "1. Boil the noodles and drain.\n2. Melt the butter and fry the crushed garlic for 1 minute.\n3. Toss the noodles through the garlic butter.",
                "noodle|200|g", "garlic|3|pcs", "butter|20|g");

        addRecipe(db, "Loaded Baked Potatoes",
                "1. Pierce the potatoes and bake at 200C for 50 minutes.\n2. Split open and add butter and salt.\n3. Top with grated cheese and return to the oven for 5 minutes.",
                "potato|4|pcs", "butter|20|g", "salt|1|tsp", "cheese|50|g");

        addRecipe(db, "Bean and Tomato Stew",
                "1. Fry the chopped onion and garlic.\n2. Add chopped tomatoes and cook down for 10 minutes.\n3. Stir in the beans and simmer for 15 minutes.",
                "bean|400|g", "tomato|2|pcs", "onion|1|pcs", "garlic|2|pcs");
    }
}