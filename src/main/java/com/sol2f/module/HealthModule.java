package com.sol2f.module;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import com.sol2f.SpiceOfLifeFabricFlavor;
import com.sol2f.config.Sol2FConfig;
import com.sol2f.network.NetWorkHandler;
import com.sol2f.network.payload.S2CHealthPayload;

import me.shedaniel.autoconfig.AutoConfig;

public class HealthModule {
    private static final Identifier HEALTH_MODIFIER_ID = Identifier.of("sol2f", "health_bonus");

    public static void onFoodEaten(ServerPlayerEntity serverPlayer, ItemStack stack) {
        Sol2FConfig config = AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig();
        if (!CommonTools.isFoodItem(stack))
            return;

        // 黑名单检查
        String itemId = Registries.ITEM.getId(stack.getItem()).toString();
        if (config.health.BlackList.contains(itemId)) {
            if (config.dev.DeveloperMode) {
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
            if (config.dev.DeveloperMode) {
                SpiceOfLifeFabricFlavor.LOGGER.info("sol2f.HealthUseHandler.onFoodEaten | eat attempt player={} food={}", serverPlayer.getName().getString(), id);
            }
            Set<String> current = getEatenFoods(serverPlayer);
            if (config.dev.DeveloperMode) {
                SpiceOfLifeFabricFlavor.LOGGER.info("sol2f.HealthUseHandler.onFoodEaten | before change player={} eaten={}", serverPlayer.getName().getString(), current);
            }
            if (!current.contains(id)) { // 食物未发现
                current.add(id);
                saveEatenFoods(serverPlayer, current);

                // 重新读取权威持久化集合以确保写入成功
                Set<String> authoritative = getEatenFoods(serverPlayer);
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
                    if (config.dev.DeveloperMode) {
                        SpiceOfLifeFabricFlavor.LOGGER.info("sol2f.HealthUseHandler.onFoodEaten | eat success player={} food={} eaten={} prevMax={} newMax={}", serverPlayer.getName().getString(), id, authoritative, prevMax, newMax);
                    }
                }
            }
            if (config.dev.DeveloperMode) {
                SpiceOfLifeFabricFlavor.LOGGER.info("sol2f.HealthUseHandler.onFoodEaten | eat ignored (already eaten) player={} food={} eaten={}", serverPlayer.getName().getString(), id, current);
            }
        }
    }

    public static void applyHealthModifier(ServerPlayerEntity player) {
        applyHealthModifier(player, getEatenFoods(player));
    }

    public static void applyHealthModifier(ServerPlayerEntity player, Set<String> eatenFoods) {
        Sol2FConfig config = AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig();
    
        if (config.dev.DeveloperMode) {
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
            if (config.dev.DeveloperMode)
                SpiceOfLifeFabricFlavor.LOGGER.info("sol2f.HealthUseHandler.applyHealthModifier | Removed existing health modifier for player {}", player.getName().getString());

            // 计算生命值增益
            // 这里计算默认增益
            int unique = eatenFoods.size();// 将healthyGain解释为每个独特食物（新食用）项目增加的生命值（以生命值单位）
            double perHp = config.health.healthGain;
            double BaseBonus = unique * perHp; // 来自独特食物的总生命值奖励

            // 计算函数
            String Formula = config.health.Expression;
            double Result = CommonTools.evaluate(Formula, Map.of("uniqueFoods", (double) unique));

            double HealthBonus = BaseBonus + Result;// 计算总生命值奖励

            // 判断总增益是否超过最大生命值
            double HealthyMaximum = config.health.maxHealth;
            double PureBonus = Math.min(HealthBonus, HealthyMaximum);

            // 应用生命修饰符
            EntityAttributeModifier mod = new EntityAttributeModifier(HEALTH_MODIFIER_ID, PureBonus , EntityAttributeModifier.Operation.ADD_VALUE);
            attr.addPersistentModifier(mod);
            if (config.dev.DeveloperMode) {// 输出日志
                SpiceOfLifeFabricFlavor.LOGGER.info("sol2f.HealthUseHandler.applyHealthModifier | Added health modifier: {} for player {} (unique={}, perHp={}, bonus={})", mod, player.getName().getString(), unique, perHp, HealthBonus);
                SpiceOfLifeFabricFlavor.LOGGER.info("sol2f.HealthUseHandler.applyHealthModifier | Complete. Total bonus: {}, Base bonus: {}, Unique foods: {}", HealthBonus, BaseBonus, unique);
            }
            ServerPlayNetworking.send(player, new S2CHealthPayload((int) PureBonus));// 发送纯增益值给客户端（仅用于GUI显示）

            // 恢复生命逻辑
            if (config.health.healthToMaxOnIncrease) { // 恢复最大生命
                player.setHealth(player.getMaxHealth());
                SpiceOfLifeFabricFlavor.LOGGER.info("sol2f.HealthUseHandler.applyHealthModifier | Player {} healed to max health: {}", player.getName().getString(), player.getMaxHealth());
            } else if (config.health.healthIncreaseOnIncrease > 0) { // 恢复指定生命
                float currentHealth = player.getHealth();
                float increase = config.health.healthIncreaseOnIncrease;
                float newHealth = Math.min(currentHealth + increase, (float) player.getMaxHealth());
                player.setHealth(newHealth);
                if (config.dev.DeveloperMode){
                    SpiceOfLifeFabricFlavor.LOGGER.info("sol2f.HealthUseHandler.applyHealthModifier | Player {} healed to {}", player.getName().getString(), newHealth);
                }
            } else {}
        } catch (Exception e) {
            SpiceOfLifeFabricFlavor.LOGGER.error("sol2f.HealthUseHandler.applyHealthModifier | Failed to apply health modifier for player {}", player.getName().getString(), e);
        }
    }

    public static Set<String> getAllFoods() {
        try {
            Sol2FConfig config = AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig();
            Set<String> foods = Registries.ITEM.stream()
                    .filter(item -> CommonTools.isFoodItem(item))
                    .map(item -> Registries.ITEM.getId(item).toString())
                    .filter(itemId -> !config.health.BlackList.contains(itemId))
                    .collect(Collectors.toSet());
            
            if (config.dev.DeveloperMode) {
                SpiceOfLifeFabricFlavor.LOGGER.info("sol2f.HealthUseHandler.getAllFoods | Get ALLFoods: {} items, blacklist contains {} items", 
                    foods.size(), config.health.BlackList.size());
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
                functionBonus = CommonTools.evaluate(formula, Map.of("uniqueFoods", (double) totalFoods));
            }
            
            double totalBonus = baseBonus + functionBonus;

            // 与配置的最大增益值比较
            double maxBonusAllowed = config.health.maxHealth;
            double finalBonus = Math.min(totalBonus, maxBonusAllowed);
            
            if (config.dev.DeveloperMode) {
                SpiceOfLifeFabricFlavor.LOGGER.info("sol2f.HealthUseHandler.calculateTheoreticalMaxHealthBonus | Theoretical max bonus calculation - foods:{}, baseBonus:{}, funcBonus:{}, totalBonus:{}, maxAllowed:{}, final:{}", totalFoods, baseBonus, functionBonus, totalBonus, maxBonusAllowed, finalBonus
                );
            }
            
            return (int) finalBonus;
        } catch (Exception e) {
            SpiceOfLifeFabricFlavor.LOGGER.error("sol2f.HealthUseHandler.calculateTheoreticalMaxHealthBonus | Failed to calculate theoretical max bonus", e);
            return config.health.maxHealth; // 返回配置中的最大增益值作为fallback
        }
    }

    // 数据部分
    public static void cleanEatenFoods(ServerPlayerEntity player) {
        Set<String> empty = new HashSet<>();
        saveEatenFoods(player, empty);
        Set<String> eaten = getEatenFoods(player);
        applyHealthModifier(player, eaten);
        NetWorkHandler.SyncConsumedFoodToClient(player, eaten);
    }

    public static void addEatenFood(ServerPlayerEntity player, ItemStack food) {
        Set<String> eatenFoods = getEatenFoods(player);
        String foodId = Registries.ITEM.getId(food.getItem()).toString();

        if (eatenFoods.add(foodId)) {
            saveEatenFoods(player, eatenFoods);
            applyHealthModifier(player, eatenFoods);
            NetWorkHandler.SyncConsumedFoodToClient(player, eatenFoods);
        }
    }

    private static final String CONSUMED_KEY = "consumed_foods";
    private static final String DATA_VERSION = "data_version";
    private static final int CURRENT_VERSION = 1;

    public static Set<String> getEatenFoods(ServerPlayerEntity player) {
        try {
            NbtCompound persistent = CommonTools.readPersistentCompound(player);
            NbtList consumed = persistent.contains(CONSUMED_KEY, 9) ? persistent.getList(CONSUMED_KEY, 8)
                    : new NbtList();
            Set<String> set = new HashSet<>();
            for (int i = 0; i < consumed.size(); i++)
                set.add(consumed.getString(i));
            return set;
        } catch (Exception e) {
            SpiceOfLifeFabricFlavor.LOGGER.error("sol2f.HealthModule.getEatenFoods | failed to get eaten foods", e);
            return new HashSet<>();
        }
    }

    public static void saveEatenFoods(ServerPlayerEntity player, Set<String> eatenFoods) {
        try {
            NbtCompound persistent = CommonTools.readPersistentCompound(player);
            SpiceOfLifeFabricFlavor.LOGGER.info("sol2f.HealthModule.saveEatenFoods | before save for player {} persistent contains: {}",
                    player.getName().getString(),
                    persistent.contains(CONSUMED_KEY, 9) ? persistent.getList(CONSUMED_KEY, 8) : "<none>");
            NbtList newList = new NbtList();
            for (String food : eatenFoods) {
                newList.add(NbtString.of(food));
            }
            persistent.put(CONSUMED_KEY, newList);
            persistent.putInt(DATA_VERSION, CURRENT_VERSION);
            CommonTools.writePersistentCompound(player, persistent);
            // 读回并记录权威持久化内容
            try {
                NbtCompound after = CommonTools.readPersistentCompound(player);
                SpiceOfLifeFabricFlavor.LOGGER.info("sol2f.HealthModule.saveEatenFoods | after save for player {} persistent contains: {}",
                        player.getName().getString(),
                        after.contains(CONSUMED_KEY, 9) ? after.getList(CONSUMED_KEY, 8) : "<none>");
            } catch (Throwable t) {
                SpiceOfLifeFabricFlavor.LOGGER.warn(
                        "sol2f.HealthModule.saveEatenFoods | failed to read back persistent compound after save for player {}",
                        player.getName().getString());
            }
        } catch (Exception e) {
            SpiceOfLifeFabricFlavor.LOGGER.error("sol2f.HealthModule.saveEatenFoods | failed to save eaten foods", e);
        }
    }
}
