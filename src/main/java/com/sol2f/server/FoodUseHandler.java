package com.sol2f.server;

import com.sol2f.config.Sol2FConfig;
import com.sol2f.network.payload.*;
import com.sol2f.util.FunctionCaculator;
import com.sol2f.SpiceOfLifeFabricFlavor;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.text.Text;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.util.Identifier;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.ArrayList;

public class FoodUseHandler {

    // 为初始化器保留注册；使用mixin时不需要运行时注册
    public static void register() {
    // 注册占位符（服务器事件处理程序位于ServerEventHandlers中）
    }

    public static boolean isFoodItem(ItemStack stack) {// 二次判断物品是否为食物
        return stack != null && stack.contains(DataComponentTypes.FOOD);
    }

    public static boolean isFoodItemType(Item item) {// 获取ALLFoods时使用
        return item != null && item.getComponents().contains(DataComponentTypes.FOOD);
    }

    public static void onFoodEaten(ServerPlayerEntity serverPlayer, ItemStack stack) {
        if (!isFoodItem(stack)) return;
        if (serverPlayer == null) {
            SpiceOfLifeFabricFlavor.LOGGER.error("sol2f: onFoodEaten called with null player");
            return;
        }

    String id = Registries.ITEM.getId(stack.getItem()).toString();

        // 在玩家上使用同步块以避免竞争条件，其中两个事件
        // 都在写入之前读取NBT，因此都发送首次食用消息
        synchronized (serverPlayer) {
            // developerMode的入口日志：记录尝试
            try {
                if (AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig().features.developerMode) {
                    SpiceOfLifeFabricFlavor.LOGGER.info("sol2f-log: eat attempt player={} food={}", serverPlayer.getName().getString(), id);
                    java.util.Map<String, String> _f = new java.util.LinkedHashMap<>();
                    _f.put("event", "eat_attempt");
                    _f.put("player", serverPlayer.getName().getString());
                    _f.put("food", id);
                    SpiceOfLifeFabricFlavor.writeStructuredDevLog(_f);
                }
            } catch (Throwable t) {
                // 忽略日志记录失败
            }
            // 在锁内重新读取权威NBT
            Set<String> current = getEatenFoods(serverPlayer);
            if (AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig().features.developerMode) {
                SpiceOfLifeFabricFlavor.LOGGER.info("sol2f-log: before change player={} eaten={}", serverPlayer.getName().getString(), current);
                java.util.Map<String, String> _b = new java.util.LinkedHashMap<>();
                _b.put("event", "before_change");
                _b.put("player", serverPlayer.getName().getString());
                _b.put("eaten", current.toString());
                SpiceOfLifeFabricFlavor.writeStructuredDevLog(_b);
            }
            if (!current.contains(id)) {
                // 更新并持久化，存到NBT里
                current.add(id);
                saveEatenFoods(serverPlayer, current);

                // 重新读取权威持久化集合以确保写入成功
                Set<String> authoritative = getEatenFoods(serverPlayer);
                if (!authoritative.contains(id)) {
                    SpiceOfLifeFabricFlavor.LOGGER.warn("sol2f: after save, authoritative eaten set does not contain {} for player {}", id, serverPlayer.getName().getString());
                } else {
                    // 使用权威更新集应用生命值修饰符
                    double prevMax = serverPlayer.getMaxHealth();
                    applyHealthModifier(serverPlayer, authoritative);
                    double newMax = serverPlayer.getMaxHealth();
                    syncToClient(serverPlayer, authoritative);

                    // 基于NBT状态在消息栏发送一次消息
                    Text displayName = stack.getName();
                    serverPlayer.sendMessage(Text.translatable("sol2f.msg.first_eaten", displayName), false);
                    SpiceOfLifeFabricFlavor.LOGGER.info("sol2f: Player {} first ate {}", serverPlayer.getName().getString(), id);
                    if (AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig().features.developerMode) {
                        SpiceOfLifeFabricFlavor.LOGGER.info("sol2f-log: eat success player={} food={} eaten={} prevMax={} newMax={}", serverPlayer.getName().getString(), id, authoritative, prevMax, newMax);
                        java.util.Map<String, String> _s = new java.util.LinkedHashMap<>();
                        _s.put("event", "eat_success");
                        _s.put("player", serverPlayer.getName().getString());
                        _s.put("food", id);
                        _s.put("eaten", authoritative.toString());
                        _s.put("prevMaxHealth", Double.toString(prevMax));
                        _s.put("newMaxHealth", Double.toString(newMax));
                        SpiceOfLifeFabricFlavor.writeStructuredDevLog(_s);
                    }
                }
            }
                if (AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig().features.developerMode) {
                    SpiceOfLifeFabricFlavor.LOGGER.info("sol2f-log: eat ignored (already eaten) player={} food={} eaten={}", serverPlayer.getName().getString(), id, current);
                    java.util.Map<String, String> _i = new java.util.LinkedHashMap<>();
                    _i.put("event", "eat_ignored");
                    _i.put("player", serverPlayer.getName().getString());
                    _i.put("food", id);
                    _i.put("eaten", current.toString());
                    SpiceOfLifeFabricFlavor.writeStructuredDevLog(_i);
                }
        }
    }

    private static final Identifier HEALTH_MODIFIER_ID = Identifier.of("sol2f", "health_bonus");
    private static final String CONSUMED_KEY = "consumed_foods";
    private static final String DATA_VERSION = "data_version";
    private static final int CURRENT_VERSION = 1;

    private static NbtCompound readPersistentCompound(ServerPlayerEntity player) {
        if (player instanceof com.sol2f.util.IEntityDataSaver saver) {
            return saver.getPersistentData();
        }

        SpiceOfLifeFabricFlavor.LOGGER.error("Failed to read persistent compound");
        return new NbtCompound();
    }

    private static void writePersistentCompound(ServerPlayerEntity player, NbtCompound data) {
        if (player instanceof com.sol2f.util.IEntityDataSaver saver) {
            NbtCompound persistent = saver.getPersistentData();
            for (String key : data.getKeys()) {
                persistent.put(key, data.get(key));
            }
            return;
        }
        SpiceOfLifeFabricFlavor.LOGGER.error("Failed to write persistent compound. Player is not IEntityDataSaver!");
    }

    // 这里获取所有食物集合
    public static final Set<String> ALLFoods = Registries.ITEM.stream()
        .filter(item -> isFoodItemType(item))
        .map(item -> Registries.ITEM.getId(item).toString())
        .collect(Collectors.toSet());

    public static Set<String> getEatenFoods(ServerPlayerEntity player) {
        try {
            NbtCompound persistent = readPersistentCompound(player);
            NbtList consumed = persistent.contains(CONSUMED_KEY, 9) ? persistent.getList(CONSUMED_KEY, 8) : new NbtList();
            Set<String> set = new HashSet<>();
            for (int i = 0; i < consumed.size(); i++) set.add(consumed.getString(i));
            return set;
        } catch (Exception e) {
                SpiceOfLifeFabricFlavor.LOGGER.error("sol2f: failed to get eaten foods", e);
            return new HashSet<>();
        }
    }

    private static void saveEatenFoods(ServerPlayerEntity player, Set<String> eatenFoods) {
        try {
            NbtCompound persistent = readPersistentCompound(player);
            SpiceOfLifeFabricFlavor.LOGGER.info("sol2f: before save for player {} persistent contains: {}", player.getName().getString(), persistent.contains(CONSUMED_KEY, 9) ? persistent.getList(CONSUMED_KEY, 8) : "<none>");
            NbtList newList = new NbtList();
            for (String food : eatenFoods) {
                newList.add(NbtString.of(food));
            }
            persistent.put(CONSUMED_KEY, newList);
            persistent.putInt(DATA_VERSION, CURRENT_VERSION);
            writePersistentCompound(player, persistent);
            // 读回并记录权威持久化内容
            try {
                NbtCompound after = readPersistentCompound(player);
                SpiceOfLifeFabricFlavor.LOGGER.info("sol2f: after save for player {} persistent contains: {}", player.getName().getString(), after.contains(CONSUMED_KEY, 9) ? after.getList(CONSUMED_KEY, 8) : "<none>");
            } catch (Throwable t) {
                SpiceOfLifeFabricFlavor.LOGGER.warn("sol2f: failed to read back persistent compound after save for player {}", player.getName().getString());
            }
        } catch (Exception e) {
            SpiceOfLifeFabricFlavor.LOGGER.error("sol2f: failed to save eaten foods", e);
        }
    }

    public static void addEatenFood(ServerPlayerEntity player, ItemStack food) {
        Set<String> eatenFoods = getEatenFoods(player);
        String foodId = Registries.ITEM.getId(food.getItem()).toString();

        if (eatenFoods.add(foodId)) {
            saveEatenFoods(player, eatenFoods);
            applyHealthModifier(player, eatenFoods);
            syncToClient(player, eatenFoods);
        }
    }

    public static void syncToClient(ServerPlayerEntity player, Set<String> eatenFoods) {
        try {
            ServerPlayNetworking.send(player, new S2CFoodListPayload(new ArrayList<>(eatenFoods)));
            SpiceOfLifeFabricFlavor.LOGGER.info("Synced food list to client: {}", eatenFoods);
        } catch (Exception e) {
            SpiceOfLifeFabricFlavor.LOGGER.error("Failed to sync to client", e);
        }
    }

    public static void clearEatenFoods(ServerPlayerEntity player) {
        Set<String> empty = new HashSet<>();
        saveEatenFoods(player, empty);
        Set<String> eaten = getEatenFoods(player);// 确保数据一致性，方便测试发现错误
        applyHealthModifier(player, eaten);
        syncToClient(player, eaten);
    }

    public static void applyHealthModifier(ServerPlayerEntity player) {
        applyHealthModifier(player, getEatenFoods(player));
    }

    public static void applyHealthModifier(ServerPlayerEntity player, Set<String> eatenFoods) {// 和生命值有关的操作
        if (AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig().features.developerMode) {
            player.sendMessage(Text.literal("run apply"), false);
        }
        try {
            EntityAttributeInstance attr = player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
            if (attr == null) {// 玩家不存在最大生命值属性，不应该发生，但是防止空指针异常，人人有责
                SpiceOfLifeFabricFlavor.LOGGER.error("Health attribute instance is null for player {}", player.getName().getString());
                return;
            }

            // ---计算及判断生命值部分---
            attr.removeModifier(HEALTH_MODIFIER_ID);// 1\ 移除已有的生命值修饰符
            SpiceOfLifeFabricFlavor.LOGGER.info("Removed existing health modifier for player {}", player.getName().getString());

            double CurrentHealth = attr.getValue();// 2\ 获取当前最大生命值（不含修饰符，包含别的mod的修改）

            // 3\ 计算生命值增益
            // 3.1\ 这里计算默认增益
            int unique = eatenFoods.size();// 将healthyGain解释为每个独特食物（新食用）项目增加的生命值（以生命值单位）
            double perHp = AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig().features.healthyGain;
            double BaseBonus = unique * perHp; // 来自独特食物的总生命值奖励

            // 3.2\ 这里计算频率增益
            int FrequencyCount;// 达到的频率奖励次数
            if (AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig().features.Increasefrequency > 0) {// 当频率增益频率大于0时（功能启用时）执行，不判断将除以0导致报错
                FrequencyCount = unique / AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig().features.Increasefrequency;
            } else {
                FrequencyCount = 0;
            }
            double FrequencyBonus = FrequencyCount * AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig().features.frequencyGain; // 来自频率奖励的总生命值奖励

            // 3.3\ 计算函数
            String formula = AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig().features.Expression;
            double result = FunctionCaculator.evaluate(formula, Map.of(
                "uniqueFoods", (double) unique,
                "currentHealth", CurrentHealth
            ));

            double HealthBonus = BaseBonus + FrequencyBonus + result;// 3.4\ 计算总生命值奖励

            // 4\ 这里判断总增益是否超过最大生命值
            double HealthyMaximum = AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig().healthy.maxHealthy;
            double newMaxHealth = Math.min(CurrentHealth + HealthBonus, HealthyMaximum);

            // 这里应用生命修饰符
            EntityAttributeModifier mod = new EntityAttributeModifier(HEALTH_MODIFIER_ID, HealthBonus, EntityAttributeModifier.Operation.ADD_VALUE);
            attr.addPersistentModifier(mod);
            SpiceOfLifeFabricFlavor.LOGGER.info("Added health modifier: {} for player {} (unique={}, perHp={}, bonus={})", mod, player.getName().getString(), unique, perHp, HealthBonus);
            if (AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig().features.developerMode) {// 输出日志
                player.sendMessage(Text.literal("apply: " + HealthBonus + ", " + FrequencyBonus + ", " + BaseBonus + ", " + FrequencyCount + ", " + unique), false);// 总生值奖励、频率奖励、基础奖励、频率计数、独特食物计数
            }

            ServerPlayNetworking.send(player, new S2CHealthMaxPayload((int) newMaxHealth));// 发送生命值数据（仅用于GUI显示）

            // 这里是恢复生命逻辑
            if (AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig().features.healthToMaxOnIncrease) { // 恢复最大生命
                player.setHealth(player.getMaxHealth());
                SpiceOfLifeFabricFlavor.LOGGER.info("Player {} healed to max health: {}", player.getName().getString(), player.getMaxHealth());
            } else if (AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig().features.healthIncreaseOnIncrease > 0) { // 恢复指定生命 
                float currentHealth = player.getHealth();
                float increase = AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig().features.healthIncreaseOnIncrease;
                float newHealth = Math.min(currentHealth + increase, (float)player.getMaxHealth());
                player.setHealth(newHealth);
                SpiceOfLifeFabricFlavor.LOGGER.info("Player {} healed to max health: {}", player.getName().getString(), player.getMaxHealth());
            } else {}
        } catch (Exception e) {
            player.sendMessage(Text.literal("[apply error]"), false);
            SpiceOfLifeFabricFlavor.LOGGER.error("Failed to apply health modifier for player {}", player.getName().getString(), e);
        }
    }
}