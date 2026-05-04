package com.sol2f.module;

import net.minecraft.server.network.ServerPlayerEntity;
import me.shedaniel.autoconfig.AutoConfig;

import com.sol2f.config.Sol2FConfig;
import com.sol2f.SpiceOfLifeFabricFlavor;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.player.PlayerEntity;

import java.util.HashSet;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;

public class FoodModule {

    Sol2FConfig config = AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig();
    private static final String KEY_RECENT_SHORT = "Sol2F_RecentShort";
    private static final String KEY_RECENT_LONG = "Sol2F_RecentLong";
    private static final String KEY_CURRENT_STREAK = "Sol2F_CurrentStreak";

    private static final String STREAK_KEY_ID = "ItemId";
    private static final String STREAK_KEY_COUNT = "Count";

    public static void onFoodEaten(ServerPlayerEntity serverPlayer, ItemStack stack) {
        Sol2FConfig config = AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig();

        if (!CommonTools.isFoodItem(stack)) {
            if (config.dev.DeveloperMode) {
                SpiceOfLifeFabricFlavor.LOGGER.info("FoodModule.onFoodEaten | Item is not food. Player: {}, Item: {}", 
                    serverPlayer.getName().toString(), stack.getItem().toString());
            }
            return;
        }

        String foodItemId = stack.getItem().toString();

        if (config.food.Blacklist.contains(foodItemId)) {
            if (config.dev.DeveloperMode) {
                SpiceOfLifeFabricFlavor.LOGGER.info("FoodModule.onFoodEaten | Food {} is in BlackList", foodItemId);
            }
            return;
        }

        if (config.dev.DeveloperMode) {
            SpiceOfLifeFabricFlavor.LOGGER.info("FoodModule.onFoodEaten | Player {} ate food {}", 
                serverPlayer.getName().getString(), foodItemId);
        }

        UpdatePlayerFoodData(serverPlayer, foodItemId);
    }

    private static void UpdatePlayerFoodData(ServerPlayerEntity player, String newFoodId) {
        Sol2FConfig config = AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig();
        try {
            NbtCompound persistent = CommonTools.readPersistentCompound(player);

            // 更新短期记录
            NbtList shortList = GetOrCreateList(persistent, KEY_RECENT_SHORT);
            UpdateQueueList(shortList, newFoodId, config.food.RecentShortListSize);
            persistent.put(KEY_RECENT_SHORT, shortList);

            // 更新长期记录
            NbtList longList = GetOrCreateList(persistent, KEY_RECENT_LONG);
            UpdateQueueList(longList, newFoodId, config.food.RecentLongListSize);
            persistent.put(KEY_RECENT_LONG, longList);

            if (!GetCurrentStreak(player).ItemId.equals(newFoodId)) {
                if (config.dev.DeveloperMode) {
                    SpiceOfLifeFabricFlavor.LOGGER.info("FoodModule.onFoodEaten | Current streak updated: {} x {}", GetCurrentStreak(player).ItemId, GetCurrentStreak(player).Count);
                }
            }
            // 更新当前累计食用记录
            NbtCompound streakData = UpdateStreakLogic(persistent, newFoodId);
            persistent.put(KEY_CURRENT_STREAK, streakData);
            // 如果读取的先前的数据和现在处理的数据不同则记录并记为需要更新食物组件的物品

            CommonTools.writePersistentCompound(player, persistent);

        } catch (Exception e) {
            SpiceOfLifeFabricFlavor.LOGGER.error("sol2f.FoodModule.onFoodEaten | Failed to update food data for player {}", 
                player.getName().getString(), e);
        }
    }

    private static NbtList GetOrCreateList(NbtCompound compound, String key) {
        if (compound.contains(key, 9)) { // 9 = List
            return compound.getList(key, 8); // 8 = String
        }
        return new NbtList();
    }

    private static void UpdateQueueList(NbtList list, String newItem, int limit) {
        if (limit <= 0) {
            return;
        }
        list.add(NbtString.of(newItem));
        if (list.size() > limit) {
            String deletedItem = list.getString(0);
            list.remove(0); // 删除最旧的项目（如果列表大小超过限制）
        }
    }

    private static NbtCompound UpdateStreakLogic(NbtCompound persistent, String newFoodId) {
        NbtCompound streak;

        if (persistent.contains(KEY_CURRENT_STREAK, 10)) { // 10 = Compound
            streak = persistent.getCompound(KEY_CURRENT_STREAK);
        } else {
            streak = new NbtCompound();
            streak.putString(STREAK_KEY_ID, "");
            streak.putInt(STREAK_KEY_COUNT, 0);
        }

        String lastFoodId = streak.getString(STREAK_KEY_ID);
        int currentCount = streak.getInt(STREAK_KEY_COUNT);

        if (newFoodId.equals(lastFoodId)) {
            streak.putInt(STREAK_KEY_COUNT, currentCount + 1);
        } else {
            streak.putString(STREAK_KEY_ID, newFoodId);
            streak.putInt(STREAK_KEY_COUNT, 1);
        }

        return streak;
    }

    public static Set<String> GetRecentShortFoods(ServerPlayerEntity player) {
        return GetListAsSet(player, KEY_RECENT_SHORT);
    }

    public static Set<String> GetRecentLongFoods(ServerPlayerEntity player) {
        return GetListAsSet(player, KEY_RECENT_LONG);
    }

    public static StreakInfo GetCurrentStreak(ServerPlayerEntity player) {
        try {
            NbtCompound persistent = CommonTools.readPersistentCompound(player);
            if (!persistent.contains(KEY_CURRENT_STREAK, 10)) {
                return new StreakInfo("", 0);
            }
            NbtCompound streak = persistent.getCompound(KEY_CURRENT_STREAK);
            return new StreakInfo(
                streak.getString(STREAK_KEY_ID), 
                streak.getInt(STREAK_KEY_COUNT)
            );
        } catch (Exception e) {
            SpiceOfLifeFabricFlavor.LOGGER.error("sol2f.FoodModule.getCurrentStreak | Failed to get streak", e);
            return new StreakInfo("", 0);
        }
    }

    private static Set<String> GetListAsSet(ServerPlayerEntity player, String key) {
        try {
            NbtCompound persistent = CommonTools.readPersistentCompound(player);
            NbtList list = persistent.contains(key, 9) ? persistent.getList(key, 8) : new NbtList();
            Set<String> set = new HashSet<>();
            for (int i = 0; i < list.size(); i++) {
                set.add(list.getString(i));
            }
            return set;
        } catch (Exception e) {
            SpiceOfLifeFabricFlavor.LOGGER.error("sol2f.FoodModule.getListAsSet | Failed to get list {}", key, e);
            return new HashSet<>();
        }
    }
    public static double FoodCalculate(ServerPlayerEntity player, ItemStack item, FoodComponent foodComponent) {
        Sol2FConfig config = AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig();

        Set<String> recentShortFoods = GetRecentShortFoods(player);
        Set<String> recentLongFoods = GetRecentLongFoods(player);
        StreakInfo streakInfo = GetCurrentStreak(player);

        int shortCount = recentShortFoods.stream().filter(food -> food.equals(item.getItem().toString())).mapToInt(String::length).sum();
        int longCount = recentLongFoods.stream().filter(food -> food.equals(item.getItem().toString())).mapToInt(String::length).sum();
        int streakCount = streakInfo.ItemId.equals(item.getItem().toString()) ? streakInfo.Count : 0;

        int nutrition = foodComponent.nutrition();

        Map<String, Double> variables = new HashMap<String, Double>(4);
        variables.put("shortCount", (double) shortCount);
        variables.put("longCount", (double) longCount);
        variables.put("streakCount", (double) streakCount);
        variables.put("nutrition", (double) nutrition);

        double result = CommonTools.evaluate(config.food.NutritionExpression, variables);

        return result;
    }

    public static class StreakInfo {
        public final String ItemId;
        public final int Count;

        public StreakInfo(String itemId, int count) {
            ItemId = itemId;
            Count = count;
        }
    }
}