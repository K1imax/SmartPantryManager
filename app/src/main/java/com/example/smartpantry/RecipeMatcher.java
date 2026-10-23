package com.example.smartpantry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The strict-matching rule: a recipe is suggested ONLY if every ingredient
 * it needs is in the pantry in at least the required quantity. otherwise it will show nothing
 */
public class RecipeMatcher {

    /** Returns only the recipes the user can make right now. */
    public static List<Recipe> getSuggestions(List<Recipe> allRecipes, List<PantryItem> pantry) {
        Map<String, Double> totals = buildPantryTotals(pantry);
        List<Recipe> result = new ArrayList<>();
        for (Recipe recipe : allRecipes) {
            if (canMake(recipe, totals)) {
                result.add(recipe);
            }
        }
        return result;
    }

    /** True only if EVERY ingredient is available in enough quantity. */
    public static boolean canMake(Recipe recipe, Map<String, Double> pantryTotals) {
        for (RecipeIngredient ing : recipe.getIngredients()) {
            String key = makeKey(ing.getName(), ing.getUnit());
            double needed = toBase(ing.getUnit(), ing.getQuantity());
            Double have = pantryTotals.get(key);
            if (have == null || have + 0.0001 < needed) {
                return false; // one missing or short ingredient rules the recipe out
            }
        }
        return true;
    }

    /** Adds up the pantry per ingredient, so two "egg" entries count together. */
    private static Map<String, Double> buildPantryTotals(List<PantryItem> pantry) {
        Map<String, Double> totals = new HashMap<>();
        for (PantryItem item : pantry) {
            String key = makeKey(item.getName(), item.getUnit());
            double qty = toBase(item.getUnit(), item.getQuantity());
            Double current = totals.get(key);
            totals.put(key, (current == null ? 0 : current) + qty);
        }
        return totals;
    }

    private static String makeKey(String name, String unit) {
        return normalizeName(name) + "|" + family(unit);
    }

    /** Lowercase, trim, and make the last word singular: "Tomatoes" -> "tomato". */
    public static String normalizeName(String raw) {
        String s = raw.toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
        int space = s.lastIndexOf(' ');
        String head = (space == -1) ? "" : s.substring(0, space + 1);
        String last = (space == -1) ? s : s.substring(space + 1);
        return head + singular(last);
    }

    private static String singular(String w) {
        if (w.length() <= 3) return w;
        if (w.endsWith("ies")) return w.substring(0, w.length() - 3) + "y";   // berries -> berry
        if (w.endsWith("oes")) return w.substring(0, w.length() - 2);          // tomatoes -> tomato
        if (w.endsWith("ches") || w.endsWith("shes") || w.endsWith("xes")) {
            return w.substring(0, w.length() - 2);                              // sandwiches -> sandwich
        }
        if (w.endsWith("ss") || w.endsWith("us")) return w;                    // hummus, glass
        if (w.endsWith("s")) return w.substring(0, w.length() - 1);            // eggs -> egg
        return w;
    }

    /** Converts to a base unit: pieces stay as pieces; everything else becomes ml/g (1 g ~ 1 ml). */
    private static double toBase(String unit, double qty) {
        switch (unit.toLowerCase(Locale.ROOT)) {
            case "kg":   return qty * 1000;
            case "l":    return qty * 1000;
            case "tbsp": return qty * 15;
            case "tsp":  return qty * 5;
            default:     return qty; // g, ml, pcs
        }
    }

    /** Units only compare within a family: counted items vs measured amounts. */
    private static String family(String unit) {
        return unit.equalsIgnoreCase("pcs") ? "count" : "measure";
    }

    /** Shows 3.0 as "3" and 2.5 as "2.5". */
    public static String formatQty(double q) {
        return (q == Math.floor(q)) ? String.valueOf((long) q) : String.valueOf(q);
    }
}