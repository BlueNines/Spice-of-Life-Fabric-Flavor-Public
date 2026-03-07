package com.sol2f.handler;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import com.sol2f.SpiceOfLifeFabricFlavor;
import com.sol2f.config.Sol2FConfig;
import com.sol2f.network.NetWorkHandler;
import com.sol2f.network.payload.S2CHealthPayload;
import com.sol2f.util.FunctionCaculator;
import com.sol2f.data.PlayerFoodData;

import me.shedaniel.autoconfig.AutoConfig;

public class HealthHandler {
    private static final Identifier HEALTH_MODIFIER_ID = Identifier.of("sol2f", "health_bonus");

    public static void clearEatenFoods(ServerPlayerEntity player) {
        Set<String> empty = new HashSet<>();
        PlayerFoodData.saveEatenFoods(player, empty);
        Set<String> eaten = PlayerFoodData.getEatenFoods(player);
        applyHealthModifier(player, eaten);
        NetWorkHandler.SyncConsumedFoodToClient(player, eaten);
    }

    public static void addEatenFood(ServerPlayerEntity player, ItemStack food) {
        Set<String> eatenFoods = PlayerFoodData.getEatenFoods(player);
        String foodId = Registries.ITEM.getId(food.getItem()).toString();

        if (eatenFoods.add(foodId)) {
            PlayerFoodData.saveEatenFoods(player, eatenFoods);
            applyHealthModifier(player, eatenFoods);
            NetWorkHandler.SyncConsumedFoodToClient(player, eatenFoods);
        }
    }

    public static void onFoodEaten(ServerPlayerEntity serverPlayer, ItemStack stack) {
        Sol2FConfig Config = AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig();
        if (!isFoodItem(stack))
            return;

        // 黑名单检查 - 如果物品在黑名单中，直接返回不处理
        String itemId = Registries.ITEM.getId(stack.getItem()).toString();
        if (Config.health.blacklist.contains(itemId)) {
            if (Config.health.developerMode) {
                SpiceOfLifeFabricFlavor.LOGGER.info("sol2f.HealthUseHandler.onFoodEaten | Item {} is blacklisted, ignoring", itemId);
            }
            return;
        }
        if (serverPlayer == null) {
            SpiceOfLifeFabricFlavor.LOGGER.error("sol2f: onFoodEaten called with null player");
            return;
        }

        String id = Registries.ITEM.getId(stack.getItem()).toString();

        // 在玩家上使用同步块以避免竞争条件，其中两个事件都在写入之前读取NBT，因此都发送首次食用消息
        synchronized (serverPlayer) {
            if (Config.health.developerMode) {
                SpiceOfLifeFabricFlavor.LOGGER.info("sol2f.HealthUseHandler.onFoodEaten | eat attempt player={} food={}", serverPlayer.getName().getString(), id);
            }
            Set<String> current = PlayerFoodData.getEatenFoods(serverPlayer);
            if (Config.health.developerMode) {
                SpiceOfLifeFabricFlavor.LOGGER.info("sol2f.HealthUseHandler.onFoodEaten | before change player={} eaten={}", serverPlayer.getName().getString(), current);
            }
            if (!current.contains(id)) { // 食物未发现
                current.add(id);
                PlayerFoodData.saveEatenFoods(serverPlayer, current);

                // 重新读取权威持久化集合以确保写入成功
                Set<String> authoritative = PlayerFoodData.getEatenFoods(serverPlayer);
                if (!authoritative.contains(id)) {
                    SpiceOfLifeFabricFlavor.LOGGER.warn("sol2f.HealthUseHandler | after save, authoritative eaten set does not contain {} for player {}", id, serverPlayer.getName().getString());
                } else {
                    // 使用权威更新集应用生命值修饰符
                    double prevMax = serverPlayer.getMaxHealth();
                    applyHealthModifier(serverPlayer, authoritative);
                    double newMax = serverPlayer.getMaxHealth();
                    NetWorkHandler.SyncConsumedFoodToClient(serverPlayer, authoritative);

                    // 基于NBT状态在消息栏发送一次消息
                    Text displayName = stack.getName();
                    serverPlayer.sendMessage(Text.translatable("sol2f.msg.first_eaten", displayName), false);
                    SpiceOfLifeFabricFlavor.LOGGER.info("sol2f.HealthUseHandler | Player {} first ate {}", serverPlayer.getName().getString(), id);
                    if (Config.health.developerMode) {
                        SpiceOfLifeFabricFlavor.LOGGER.info("sol2f.HealthUseHandler.onFoodEaten | eat success player={} food={} eaten={} prevMax={} newMax={}", serverPlayer.getName().getString(), id, authoritative, prevMax, newMax);
                    }
                }
            }
            if (Config.health.developerMode) {
                SpiceOfLifeFabricFlavor.LOGGER.info("sol2f.HealthUseHandler.onFoodEaten | eat ignored (already eaten) player={} food={} eaten={}", serverPlayer.getName().getString(), id, current);
            }
        }
    }


    public static void applyHealthModifier(ServerPlayerEntity player) {
        applyHealthModifier(player, PlayerFoodData.getEatenFoods(player));
    }

    public static void applyHealthModifier(ServerPlayerEntity player, Set<String> eatenFoods) {
        Sol2FConfig Config = AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig();
    
        if (Config.health.developerMode) {
            SpiceOfLifeFabricFlavor.LOGGER.info("sol2f.HealthUseHandler.applyHealthModifier | Applying health modifier for player {}", player.getName().getString());
        }
        try {
            EntityAttributeInstance attr = player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
            if (attr == null) {
                SpiceOfLifeFabricFlavor.LOGGER.error("sol2f.HealthUseHandler.applyHealthModifier | Health attribute instance is null for player {}", player.getName().getString());
                return;
            }

            // ---计算及判断生命值部分---
            attr.removeModifier(HEALTH_MODIFIER_ID);// 1\ 移除已有的生命值修饰符
            SpiceOfLifeFabricFlavor.LOGGER.info("sol2f.HealthUseHandler.applyHealthModifier | Removed existing health modifier for player {}",
                    player.getName().getString());

            // 计算生命值增益
            // 这里计算默认增益
            int unique = eatenFoods.size();// 将healthyGain解释为每个独特食物（新食用）项目增加的生命值（以生命值单位）
            double perHp = Config.health.healthGain;
            double BaseBonus = unique * perHp; // 来自独特食物的总生命值奖励

            // 计算函数
            String Formula = Config.health.Expression;
            double Result = FunctionCaculator.evaluate(Formula, Map.of(
                    "uniqueFoods", (double) unique));

            double HealthBonus = BaseBonus + Result;// 计算总生命值奖励

            // 判断总增益是否超过最大生命值
            double HealthyMaximum = Config.health.maxHealth;
            double PureBonus = Math.min(HealthBonus, HealthyMaximum);

            // 应用生命修饰符
            EntityAttributeModifier mod = new EntityAttributeModifier(HEALTH_MODIFIER_ID, PureBonus , EntityAttributeModifier.Operation.ADD_VALUE);
            attr.addPersistentModifier(mod);
            SpiceOfLifeFabricFlavor.LOGGER.info("sol2f.HealthUseHandler.applyHealthModifier | Added health modifier: {} for player {} (unique={}, perHp={}, bonus={})", mod, player.getName().getString(), unique, perHp, HealthBonus);
            if (Config.health.developerMode) {// 输出日志
                SpiceOfLifeFabricFlavor.LOGGER.info("sol2f.HealthUseHandler.applyHealthModifier | Complete. Total bonus: {}, Base bonus: {}, Unique foods: {}", HealthBonus, BaseBonus, unique);
            }
            ServerPlayNetworking.send(player, new S2CHealthPayload((int) PureBonus));// 发送纯增益值给客户端（仅用于GUI显示）

            // 恢复生命逻辑
            if (Config.health.healthToMaxOnIncrease) { // 恢复最大生命
                player.setHealth(player.getMaxHealth());
                SpiceOfLifeFabricFlavor.LOGGER.info("sol2f.HealthUseHandler.applyHealthModifier | Player {} healed to max health: {}", player.getName().getString(),
                        player.getMaxHealth());
            } else if (Config.health.healthIncreaseOnIncrease > 0) { // 恢复指定生命
                float currentHealth = player.getHealth();
                float increase = Config.health.healthIncreaseOnIncrease;
                float newHealth = Math.min(currentHealth + increase, (float) player.getMaxHealth());
                player.setHealth(newHealth);
                SpiceOfLifeFabricFlavor.LOGGER.info("sol2f.HealthUseHandler.applyHealthModifier | Player {} healed to {}", player.getName().getString(), newHealth);
            } else {}
        } catch (Exception e) {
            SpiceOfLifeFabricFlavor.LOGGER.error("sol2f.HealthUseHandler.applyHealthModifier | Failed to apply health modifier for player {}", player.getName().getString(), e);
        }
    }

    public static boolean isFoodItem(ItemStack stack) {
        return stack != null && stack.contains(DataComponentTypes.FOOD);
    }
    public static boolean isFoodItemType(Item item) {
        return item != null && item.getComponents().contains(DataComponentTypes.FOOD);
    }
    public static Set<String> getAllFoods() {
        try {
            Sol2FConfig config = AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig();
            Set<String> foods = Registries.ITEM.stream()
                    .filter(item -> isFoodItemType(item))
                    .map(item -> Registries.ITEM.getId(item).toString())
                    .filter(itemId -> !config.health.blacklist.contains(itemId))
                    .collect(Collectors.toSet());
            
            if (config.health.developerMode) {
                SpiceOfLifeFabricFlavor.LOGGER.info("sol2f.HealthUseHandler.getAllFoods | Get ALLFoods: {} items, blacklist contains {} items", 
                    foods.size(), config.health.blacklist.size());
            }
            
            return foods;
        } catch (Exception e) {
            SpiceOfLifeFabricFlavor.LOGGER.error("sol2f.HealthUseHandler.getAllFoods | Failed to get ALLFoods", e);
            return new HashSet<>();
        }
    }

    public static int calculateTheoreticalMaxHealthBonus() {
        Sol2FConfig config = AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig();
        try {
            int totalFoods = getAllFoods().size();

            double perHp = config.health.healthGain;
            double baseBonus = totalFoods * perHp;
                        
            String formula = config.health.Expression;
            double functionBonus = 0;
            if (!"0".equals(formula)) {
                functionBonus = FunctionCaculator.evaluate(formula, Map.of("uniqueFoods", (double) totalFoods));
            }
            
            double totalBonus = baseBonus + functionBonus;

            // 与配置的最大增益值比较
            double maxBonusAllowed = config.health.maxHealth;
            double finalBonus = Math.min(totalBonus, maxBonusAllowed);
            
            if (config.health.developerMode) {
                SpiceOfLifeFabricFlavor.LOGGER.info("sol2f.HealthUseHandler.calculateTheoreticalMaxHealthBonus | Theoretical max bonus calculation - foods:{}, baseBonus:{}, funcBonus:{}, totalBonus:{}, maxAllowed:{}, final:{}", totalFoods, baseBonus, functionBonus, totalBonus, maxBonusAllowed, finalBonus
                );
            }
            
            return (int) finalBonus;
        } catch (Exception e) {
            SpiceOfLifeFabricFlavor.LOGGER.error("sol2f.HealthUseHandler.calculateTheoreticalMaxHealthBonus | Failed to calculate theoretical max bonus", e);
            return config.health.maxHealth; // 返回配置中的最大增益值作为fallback
        }
    }

}
