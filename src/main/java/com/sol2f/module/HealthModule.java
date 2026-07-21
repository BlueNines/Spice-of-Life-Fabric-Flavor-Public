package com.sol2f.module;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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

import com.sol2f.SpiceOfLifeFabricFlavor;
import com.sol2f.config.Sol2FConfig;
import com.sol2f.network.NetWorkHandler;
import com.sol2f.network.NetworkChannels;
import com.sol2f.sync.PlayerDataSyncService;

import me.shedaniel.autoconfig.AutoConfig;

public final class HealthModule {
    private static final UUID HEALTH_MODIFIER_ID = UUID.fromString("d78153c1-68a3-48c0-88d7-74c495008c47");
    private static final String CONSUMED_KEY = "consumed_foods";
    private static final String DATA_VERSION = "data_version";
    private static final String DATABASE_GENERATION_KEY = "database_generation";
    private static final String DATABASE_DIRTY_KEY = "database_dirty";
    private static final int CURRENT_VERSION = 2;

    private HealthModule() {
    }

    public static void onFoodEaten(ServerPlayerEntity serverPlayer, ItemStack stack) {
        Sol2FConfig config = AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig();
        if (!Util.isFoodItem(stack))
            return;

        // 黑名单检查
        String itemId = Registries.ITEM.getId(stack.getItem()).toString();
        if (config.health.BlackList.contains(itemId)) {
            if (config.dev.DeveloperMode) {
                SpiceOfLifeFabricFlavor.LOGGER.info("sol2f.HealthUseHandler.onFoodEaten | Item {} is blacklisted, ignoring", itemId);
            }
            return;
        }

        // 白名单检查
        if (!config.health.WhiteList.isEmpty() && !config.health.WhiteList.contains(itemId)) {
            if (config.dev.DeveloperMode) {
                SpiceOfLifeFabricFlavor.LOGGER.info("sol2f.HealthUseHandler.onFoodEaten | Item {} is not in whitelist, ignoring", itemId);
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
                saveLocalEatenFoods(serverPlayer, current, true);
                PlayerDataSyncService.recordFood(serverPlayer, id, current);

                // 当前服务端线程中的集合是在线会话权威状态
                Set<String> authoritative = new HashSet<>(current);
                if (!authoritative.contains(id)) {
                    SpiceOfLifeFabricFlavor.LOGGER.warn("sol2f.HealthUseHandler | after save, authoritative eaten set does not contain {} for player {}", id, serverPlayer.getName().getString());
                } else {
                    // 使用权威更新集应用生命值修饰符
                    double prevMax = serverPlayer.getMaxHealth();
                    applyHealthModifier(serverPlayer, authoritative);
                    healAfterFoodDiscovery(serverPlayer);
                    double newMax = serverPlayer.getMaxHealth();
                    NetWorkHandler.syncConsumedFoodToClient(serverPlayer, authoritative);

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

    /**
     * 按玩家当前进度重新应用生命值属性，不触发治疗。
     */
    public static void applyHealthModifier(ServerPlayerEntity player) {
        applyHealthModifier(player, getEatenFoods(player));
    }

    /**
     * 按给定食物集合重新应用生命值属性，不触发治疗。
     */
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
            Set<String> effectiveFoods = new HashSet<>(eatenFoods);
            effectiveFoods.retainAll(getAllFoods());
            int unique = effectiveFoods.size();// 只统计本服存在且配置允许的食物
            double perHp = config.health.healthGain;
            double BaseBonus = unique * perHp; // 来自独特食物的总生命值奖励

            // 计算函数
            String Formula = config.health.Expression;
            double Result = Util.evaluate(Formula, Map.of("uniqueFoods", (double) unique));

            double HealthBonus = BaseBonus + Result;// 计算总生命值奖励

            // 判断总增益是否超过最大生命值
            double HealthyMaximum = config.health.maxHealth;
            double PureBonus = Math.min(HealthBonus, HealthyMaximum);

            // 应用生命修饰符
            EntityAttributeModifier mod = new EntityAttributeModifier(HEALTH_MODIFIER_ID, "sol2f health bonus", PureBonus,
                    EntityAttributeModifier.Operation.ADDITION);
            attr.addPersistentModifier(mod);
            if (config.dev.DeveloperMode) {// 输出日志
                SpiceOfLifeFabricFlavor.LOGGER.info("sol2f.HealthUseHandler.applyHealthModifier | Added health modifier: {} for player {} (unique={}, perHp={}, bonus={})", mod, player.getName().getString(), unique, perHp, HealthBonus);
                SpiceOfLifeFabricFlavor.LOGGER.info("sol2f.HealthUseHandler.applyHealthModifier | Complete. Total bonus: {}, Base bonus: {}, Unique foods: {}", HealthBonus, BaseBonus, unique);
            }
            NetWorkHandler.syncHealthBonusToClient(player, (int) PureBonus);// 发送纯增益值给客户端（仅用于GUI显示）

        } catch (Exception e) {
            SpiceOfLifeFabricFlavor.LOGGER.error("sol2f.HealthUseHandler.applyHealthModifier | Failed to apply health modifier for player {}", player.getName().getString(), e);
        }
    }

    /**
     * 只在确认首次发现食物后执行配置中的治疗效果。
     */
    private static void healAfterFoodDiscovery(ServerPlayerEntity player) {
        Sol2FConfig config = AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig();
        if (config.health.healthToMaxOnIncrease) {
            player.setHealth(player.getMaxHealth());
            return;
        }
        if (config.health.healthIncreaseOnIncrease <= 0) {
            return;
        }

        float newHealth = Math.min(player.getHealth() + config.health.healthIncreaseOnIncrease, player.getMaxHealth());
        player.setHealth(newHealth);
    }

    public static Set<String> getAllFoods() {
        try {
            Sol2FConfig config = AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig();
            Set<String> foods = Registries.ITEM.stream()
                    .filter(item -> Util.isFoodItem(item))
                    .map(item -> Registries.ITEM.getId(item).toString())
                    .filter(itemId -> !config.health.BlackList.contains(itemId))
                    .filter(itemId -> config.health.WhiteList.isEmpty() || config.health.WhiteList.contains(itemId))
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
                functionBonus = Util.evaluate(formula, Map.of("uniqueFoods", (double) totalFoods));
            }
            
            double totalBonus = baseBonus + functionBonus;

            // 与配置的最大增益值比较
            double maxBonusAllowed = config.health.maxHealth;
            // 白名单不为空时，额外受白名单本身条目数限制
            if (!config.health.WhiteList.isEmpty()) {
                double whitelistExpr = "0".equals(formula) ? 0 : Util.evaluate(formula, Map.of("uniqueFoods", (double) config.health.WhiteList.size()));
                double whitelistCap = config.health.WhiteList.size() * perHp + whitelistExpr;
                maxBonusAllowed = Math.min(maxBonusAllowed, whitelistCap);
            }
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
        saveLocalEatenFoods(player, empty, true);
        applyHealthModifier(player, empty);
        NetWorkHandler.syncConsumedFoodToClient(player, empty);
    }

    public static void addEatenFood(ServerPlayerEntity player, ItemStack food) {
        Set<String> eatenFoods = getEatenFoods(player);
        String foodId = Registries.ITEM.getId(food.getItem()).toString();

        if (eatenFoods.add(foodId)) {
            saveLocalEatenFoods(player, eatenFoods, true);
            PlayerDataSyncService.recordFood(player, foodId, eatenFoods);
            applyHealthModifier(player, eatenFoods);
            NetWorkHandler.syncConsumedFoodToClient(player, eatenFoods);
        }
    }

    /**
     * 读取在线会话食物快照；未启用数据库时回退到本地 NBT。
     */
    public static Set<String> getEatenFoods(ServerPlayerEntity player) {
        Set<String> synced = PlayerDataSyncService.getFoodsSnapshot(player.getUuid());
        if (synced != null) {
            return synced;
        }
        return readLocalEatenFoods(player);
    }

    /**
     * 直接读取玩家本地 NBT，不访问在线同步会话。
     */
    public static Set<String> readLocalEatenFoods(ServerPlayerEntity player) {
        try {
            NbtCompound persistent = Util.readPersistentCompound(player);
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

    /**
     * 将食物集合保存到本地玩家 NBT，并记录是否等待数据库确认。
     */
    public static void saveLocalEatenFoods(ServerPlayerEntity player, Set<String> eatenFoods, boolean dirty) {
        try {
            NbtCompound persistent = Util.readPersistentCompound(player);
            NbtList newList = new NbtList();
            int written = 0;
            for (String food : eatenFoods) {
                if (written >= NetworkChannels.MAX_FOOD_ENTRIES) {
                    break;
                }
                newList.add(NbtString.of(food));
                written++;
            }
            persistent.put(CONSUMED_KEY, newList);
            persistent.putInt(DATA_VERSION, CURRENT_VERSION);
            persistent.putBoolean(DATABASE_DIRTY_KEY, dirty);
            Util.writePersistentCompound(player, persistent);
        } catch (Exception e) {
            SpiceOfLifeFabricFlavor.LOGGER.error("sol2f.HealthModule.saveLocalEatenFoods | failed to save eaten foods", e);
        }
    }

    /**
     * 获取本地 NBT 最近确认的数据库 generation，旧数据返回 -1。
     */
    public static long getLocalDatabaseGeneration(ServerPlayerEntity player) {
        NbtCompound persistent = Util.readPersistentCompound(player);
        return persistent.contains(DATABASE_GENERATION_KEY, 4) ? persistent.getLong(DATABASE_GENERATION_KEY) : -1L;
    }

    /**
     * 判断本地 NBT 是否存在尚未确认写入数据库的数据。
     */
    public static boolean isLocalDatabaseDirty(ServerPlayerEntity player) {
        return Util.readPersistentCompound(player).getBoolean(DATABASE_DIRTY_KEY);
    }

    /**
     * 写入本地 NBT 对应的数据库 generation 和确认状态。
     */
    public static void markLocalDatabaseState(ServerPlayerEntity player, long generation, boolean dirty) {
        NbtCompound persistent = Util.readPersistentCompound(player);
        persistent.putLong(DATABASE_GENERATION_KEY, generation);
        persistent.putBoolean(DATABASE_DIRTY_KEY, dirty);
        Util.writePersistentCompound(player, persistent);
    }
}
