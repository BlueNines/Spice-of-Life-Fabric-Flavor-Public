package com.food_advancement.data;

import java.util.HashSet;
import java.util.Set;

public class ClientFoodData {

    private static Set<String> eatenFoods = new HashSet<>();

    public static void setEatenFoods(Set<String> foodsFromServer) {
        eatenFoods = foodsFromServer;
    }

    public static boolean hasEaten(String foodId) {
        return eatenFoods.contains(foodId);
    }

    public static void clear() {
        eatenFoods.clear();
    }
}
