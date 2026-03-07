package com.sol2f.handler;

import net.minecraft.server.network.ServerPlayerEntity;
import me.shedaniel.autoconfig.AutoConfig;

import java.util.Map;
import java.util.HashMap;

import com.sol2f.config.Sol2FConfig;
import com.sol2f.util.FunctionCaculator;
import com.sol2f.SpiceOfLifeFabricFlavor;

public class HungerHandler {
    public static void PlayerNaturalHungerHandler(ServerPlayerEntity player) {
        Sol2FConfig Config = AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig();

        try {
            Map<String, Double> vars = GetAdvancedPlayerStateVariables(player);
            int FoodLevel = vars.get("FoodLevel").intValue();
            String Formula = Config.hunger.NaturalHungerExpression;
            double Result = FunctionCaculator.evaluate(Formula, vars);

            if (FoodLevel <= Config.hunger.MinFoodLevel) {
                if (Config.health.developerMode) {
                    SpiceOfLifeFabricFlavor.LOGGER.info("sol2f.HungerHandler.PlayerNaturalHungerHandler | Skip applying hunger change: Current food level ({}) is already at or below minimum ({}).", FoodLevel, Config.hunger.MinFoodLevel);
                }
                return;
            }
            int NewFoodLevel = Math.max(FoodLevel - (int) Math.round(Result), Config.hunger.MinFoodLevel);
            player.getHungerManager().setFoodLevel(NewFoodLevel);

            if (Config.health.developerMode) {
                SpiceOfLifeFabricFlavor.LOGGER.info("sol2f.HungerHandler.PlayerNaturalHungerHandler | Hunger updated. Result:{}, OldFoodLevel:{}, NewFoodLevel:{}", Result, FoodLevel, NewFoodLevel);
            }
        } catch (Exception e) {
            SpiceOfLifeFabricFlavor.LOGGER.error("sol2f.HungerHandler.PlayerNaturalHungerHandler | An error occurred while calculating hunger: {}", e.getMessage());
        }
    }

    public static void PlayerSleepHungerHandler(ServerPlayerEntity player, int SleepDuration) {
        Sol2FConfig Config = AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig();
        if (Config.hunger.EnableSleepHunger) {
            try {
                int FoodLevel = GetAdvancedPlayerStateVariables(player).get("FoodLevel").intValue();
                String Formula = Config.hunger.SleepHungerExpression;
                Map<String, Double> vars = GetAdvancedPlayerStateVariables(player);
                vars.put("SleepDuration", (double) SleepDuration);
                double Result = FunctionCaculator.evaluate(Formula, vars);

                if (FoodLevel <= Config.hunger.MinFoodLevel) {
                    if (Config.health.developerMode) {
                        SpiceOfLifeFabricFlavor.LOGGER.info("sol2f.HungerHandler.PlayerSleepHungerHandler | Skip applying hunger change: Current food level ({}) is already at or below minimum ({}).", FoodLevel, Config.hunger.MinFoodLevel);
                    }
                    return;
                }
                int NewFoodLevel = Math.max(FoodLevel - (int) Math.round(Result), Config.hunger.MinFoodLevel);
                player.getHungerManager().setFoodLevel(NewFoodLevel);

                if (Config.health.developerMode) {
                    SpiceOfLifeFabricFlavor.LOGGER.info("sol2f.HungerHandler.PlayerSleepHungerHandler | Hunger updated. Result:{}, OldFoodLevel:{}, NewFoodLevel:{}", Result, FoodLevel, NewFoodLevel);
                }
            } catch (Exception e) {
                SpiceOfLifeFabricFlavor.LOGGER.error("sol2f.HungerHandler.PlayerSleepHungerHandler | An error occurred while apply hunger: {}", e.getMessage());
            }
        }
    }

    public static Map<String, Double> GetAdvancedPlayerStateVariables(ServerPlayerEntity player) {
        Sol2FConfig Config = AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig();

        Map<String, Double> vars = new HashMap<>();

        if (player == null || player.getWorld() == null) {
            SpiceOfLifeFabricFlavor.LOGGER.warn("sol2f.HungerHandler.GetAdvancedPlayerStateVariables | Failed to get advanced player state (Player or world is null)");
            return ADVANCED_DEFAULT_SAFE_VARS;
        }

        boolean isTouchingWater = player.isTouchingWater(); // 是否接触水
        vars.put("isTouchingWater", isTouchingWater ? 1.0 : 0.0);

        boolean isBeingRainedOn = player.isTouchingWaterOrRain() && !player.isTouchingWater(); // 是否淋雨([接触水或雨] 且 非[接触水])
        vars.put("isBeingRainedOn", isBeingRainedOn ? 1.0 : 0.0);

        boolean isTouchingLava = player.isTouchingWaterOrRain(); // 是否淋雨或接触水
        vars.put("isTouchingLava", isTouchingLava ? 1.0 : 0.0);

        boolean isWet = player.isWet(); // 是否潮湿(淋雨或接触水或接触气泡柱)
        vars.put("isWet", isWet ? 1.0 : 0.0);

        boolean isSprinting = player.isSprinting(); // 是否疾跑
        vars.put("isSprinting", isSprinting ? 1.0 : 0.0);

        boolean isSneaking = player.isSneaking(); // 是否潜行
        vars.put("isSneaking", isSneaking ? 1.0 : 0.0);

        boolean isSwimming = player.isSwimming(); // 是否游泳
        vars.put("isSwimming", isSwimming ? 1.0 : 0.0);

        boolean isCrawling = player.isCrawling(); // 是否爬行 (不在水中)
        vars.put("isCrawling", isCrawling ? 1.0 : 0.0);

        boolean BrightnessAtEyes = player.getBrightnessAtEyes() > 0.5; // 玩家眼睛位置坐标的光照强度
        vars.put("BrightnessAtEyes", BrightnessAtEyes ? 1.0 : 0.0);

        int FoodLevel = player.getHungerManager().getFoodLevel(); // 玩家饥饿值
        float Saturation = player.getHungerManager().getSaturationLevel(); // 玩家饱食度
        float Exhaustion = player.getHungerManager().getExhaustion(); // 玩家 exhaustion
        vars.put("FoodLevel", (double) FoodLevel);
        vars.put("Saturation", (double) Saturation);
        vars.put("Exhaustion", (double) Exhaustion);

        if (Config.health.developerMode) {
            SpiceOfLifeFabricFlavor.LOGGER.info("sol2f.HungerHandler.GetAdvancedPlayerStateVariables | Get advanced player state variables successfully. isTouchingWater: {}, isBeingRainedOn: {}, isTouchingLava: {}, isWet: {}, isSprinting: {}, isSneaking: {}, isSwimming: {}, isCrawling: {}, BrightnessAtEyes: {}, FoodLevel: {}, Saturation: {}, Exhaustion: {}", isTouchingWater, isBeingRainedOn, isTouchingLava, isWet, isSprinting, isSneaking, isSwimming, isCrawling, BrightnessAtEyes, FoodLevel, Saturation, Exhaustion);
        }
        return vars;
    }

    public static Map<String, Double> GetPlayerStateVariables(ServerPlayerEntity player) {
        Sol2FConfig Config = AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig();

        Map<String, Double> vars = new HashMap<>();

        if (player == null || player.getWorld() == null) {
            SpiceOfLifeFabricFlavor.LOGGER.warn("sol2f.HungerHandler.GetPlayerStateVariables | Failed to get player state (Player or world is null)");
            return DEFAULT_SAFE_VARS;
        }

        int FoodLevel = player.getHungerManager().getFoodLevel(); // 玩家饥饿值
        float Saturation = player.getHungerManager().getSaturationLevel(); // 玩家饱食度
        float Exhaustion = player.getHungerManager().getExhaustion(); // 玩家 exhaustion
        vars.put("FoodLevel", (double) FoodLevel);
        vars.put("Saturation", (double) Saturation);
        vars.put("Exhaustion", (double) Exhaustion);

        if (Config.health.developerMode) {
            SpiceOfLifeFabricFlavor.LOGGER.info("sol2f.HungerHandler.GetPlayerStateVariables | Get player state variables successfully. FoodLevel: {}, Saturation: {}, Exhaustion: {}", FoodLevel, Saturation, Exhaustion);
        }

        return vars;
    }

    private static final Map<String, Double> ADVANCED_DEFAULT_SAFE_VARS = new HashMap<>();
    static {
        ADVANCED_DEFAULT_SAFE_VARS.put("isTouchingWater", 0.0);
        ADVANCED_DEFAULT_SAFE_VARS.put("isBeingRainedOn", 0.0);
        ADVANCED_DEFAULT_SAFE_VARS.put("isTouchingLava", 0.0);
        ADVANCED_DEFAULT_SAFE_VARS.put("isWet", 0.0);
        ADVANCED_DEFAULT_SAFE_VARS.put("isSprinting", 0.0);
        ADVANCED_DEFAULT_SAFE_VARS.put("isSneaking", 0.0);
        ADVANCED_DEFAULT_SAFE_VARS.put("isSwimming", 0.0);
        ADVANCED_DEFAULT_SAFE_VARS.put("isCrawling", 0.0);
        ADVANCED_DEFAULT_SAFE_VARS.put("BrightnessAtEyes", 0.0);
    }

    private static final Map<String, Double> DEFAULT_SAFE_VARS = new HashMap<>();
    static {
        DEFAULT_SAFE_VARS.put("FoodLevel", 0.0);
        DEFAULT_SAFE_VARS.put("Saturation", 0.0);
        DEFAULT_SAFE_VARS.put("Exhaustion", 0.0);
    }
}