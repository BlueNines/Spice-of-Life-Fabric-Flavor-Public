package com.sol2f.module;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.FoodComponent;
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
import com.sol2f.network.NetworkChannels;
import com.sol2f.sync.FoodValue;
import com.sol2f.sync.PlayerDataSyncService;

import me.shedaniel.autoconfig.AutoConfig;

public final class HealthModule {
    private static final UUID HEALTH_MODIFIER_ID = UUID.fromString("d78153c1-68a3-48c0-88d7-74c495008c47");
    private static final String CONSUMED_KEY = "consumed_foods";
    private static final String FOOD_VALUES_KEY = "consumed_food_values";
    private static final String FOOD_ID_KEY = "id";
    private static final String HUNGER_POINTS_KEY = "hunger_points";
    private static final String SATURATION_MODIFIER_KEY = "saturation_modifier";
    private static final String DATA_VERSION = "data_version";
    private static final String DATABASE_GENERATION_KEY = "database_generation";
    private static final String DATABASE_DIRTY_KEY = "database_dirty";
    private static final int CURRENT_VERSION = 3;

    /**
     * 工具类不允许实例化。
     */
    private HealthModule() {
    }

    /**
     * 处理玩家完成食用后的首次发现、属性刷新和异步持久化。
     */
    public static void onFoodEaten(ServerPlayerEntity serverPlayer, ItemStack stack) {
        if (serverPlayer == null) {
            SpiceOfLifeFabricFlavor.LOGGER.error("sol2f: onFoodEaten called with null player");
            return;
        }
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
        String id = Registries.ITEM.getId(stack.getItem()).toString();

        // 在玩家上使用同步块以避免竞争条件，其中两个事件都在写入之前读取NBT，因此都发送首次食用消息
        synchronized (serverPlayer) {
            if (config.dev.DeveloperMode) {
                SpiceOfLifeFabricFlavor.LOGGER.info("sol2f.HealthUseHandler.onFoodEaten | eat attempt player={} food={}", serverPlayer.getName().getString(), id);
            }
            Map<String, FoodValue> current = getEatenFoodValues(serverPlayer);
            if (config.dev.DeveloperMode) {
                SpiceOfLifeFabricFlavor.LOGGER.info("sol2f.HealthUseHandler.onFoodEaten | before change player={} eaten={}", serverPlayer.getName().getString(), current);
            }
            if (!current.containsKey(id)) { // 食物未发现
                if (current.size() >= NetworkChannels.MAX_FOOD_ENTRIES) {
                    SpiceOfLifeFabricFlavor.LOGGER.warn(
                            "Ignored new food {} for {} because the bounded food list is full",
                            id, serverPlayer.getUuid());
                    return;
                }
                FoodValue foodValue = readFoodValue(stack);
                current.put(id, foodValue);
                saveLocalFoodValues(serverPlayer, current, true);
                PlayerDataSyncService.recordFood(serverPlayer, foodValue, current);

                // 当前服务端线程中的集合是在线会话权威状态
                Map<String, FoodValue> authoritative = new HashMap<>(current);
                if (!authoritative.containsKey(id)) {
                    SpiceOfLifeFabricFlavor.LOGGER.warn("sol2f.HealthUseHandler | after save, authoritative eaten set does not contain {} for player {}", id, serverPlayer.getName().getString());
                } else {
                    // 使用权威更新集应用生命值修饰符
                    double prevMax = serverPlayer.getMaxHealth();
                    applyHealthModifier(serverPlayer, authoritative);
                    healAfterFoodDiscovery(serverPlayer);
                    double newMax = serverPlayer.getMaxHealth();
                    NetWorkHandler.syncConsumedFoodToClient(serverPlayer, authoritative.keySet());

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
        applyHealthModifier(player, getEatenFoodValues(player));
    }

    /**
     * 按给定食物集合重新应用生命值属性，不触发治疗。
     */
    public static void applyHealthModifier(ServerPlayerEntity player, Set<String> eatenFoods) {
        applyHealthModifier(player, resolveFoodValues(eatenFoods));
    }

    /**
     * 按给定食物数值快照重新应用生命值属性，不触发治疗。
     */
    public static void applyHealthModifier(ServerPlayerEntity player, Map<String, FoodValue> eatenFoods) {
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
            Set<String> effectiveFoods = new HashSet<>(eatenFoods.keySet());
            effectiveFoods.retainAll(getAllFoods());
            int unique = effectiveFoods.size();// 只统计本服存在且配置允许的食物
            double totalHunger = 0.0D;
            double totalSaturation = 0.0D;
            for (String foodId : effectiveFoods) {
                FoodValue food = eatenFoods.get(foodId);
                if (food != null && food.known()) {
                    totalHunger += food.hungerPoints();
                    totalSaturation += food.saturationPoints();
                }
            }
            double perHp = config.health.healthGain;
            double BaseBonus = unique * perHp; // 来自独特食物的总生命值奖励

            // 计算函数
            String Formula = config.health.Expression;
            double Result = evaluateHealthFormula(Formula, unique, totalHunger, totalSaturation);

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
            NetWorkHandler.syncHealthBonusToClient(player, PureBonus);// 发送纯增益值给客户端（仅用于GUI显示）

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

    /**
     * 扫描本服注册表并返回通过黑白名单过滤的标准食物 ID。
     */
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

    /**
     * 计算当前本服全部有效食物可提供的理论最大生命增益。
     */
    public static double calculateTheoreticalMaxHealthBonus() {
        Sol2FConfig config = AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig();
        try {
            Map<String, FoodValue> allFoodValues = getAllFoodValues();
            int totalFoods = allFoodValues.size();
            double totalHunger = allFoodValues.values().stream().mapToInt(FoodValue::hungerPoints).sum();
            double totalSaturation = allFoodValues.values().stream().mapToDouble(FoodValue::saturationPoints).sum();

            double perHp = config.health.healthGain;
            double baseBonus = totalFoods * perHp;
                        
            String formula = config.health.Expression;
            double functionBonus = 0;
            if (!"0".equals(formula)) {
                functionBonus = evaluateHealthFormula(formula, totalFoods, totalHunger, totalSaturation);
            }
            
            double totalBonus = baseBonus + functionBonus;

            // 与配置的最大增益值比较
            double maxBonusAllowed = config.health.maxHealth;
            // 白名单不为空时，额外受白名单本身条目数限制
            if (!config.health.WhiteList.isEmpty()) {
                Map<String, FoodValue> whitelistFoods = new HashMap<>(allFoodValues);
                whitelistFoods.keySet().retainAll(config.health.WhiteList);
                int whitelistCount = whitelistFoods.size();
                double whitelistHunger = whitelistFoods.values().stream().mapToInt(FoodValue::hungerPoints).sum();
                double whitelistSaturation = whitelistFoods.values().stream().mapToDouble(FoodValue::saturationPoints).sum();
                double whitelistExpr = "0".equals(formula) ? 0
                        : evaluateHealthFormula(formula, whitelistCount, whitelistHunger, whitelistSaturation);
                double whitelistCap = whitelistCount * perHp + whitelistExpr;
                maxBonusAllowed = Math.min(maxBonusAllowed, whitelistCap);
            }
            double finalBonus = Math.min(totalBonus, maxBonusAllowed);
            
            if (config.dev.DeveloperMode) {
                SpiceOfLifeFabricFlavor.LOGGER.info("sol2f.HealthUseHandler.calculateTheoreticalMaxHealthBonus | Theoretical max bonus calculation - foods:{}, baseBonus:{}, funcBonus:{}, totalBonus:{}, maxAllowed:{}, final:{}", totalFoods, baseBonus, functionBonus, totalBonus, maxBonusAllowed, finalBonus
                );
            }
            
            return finalBonus;
        } catch (Exception e) {
            SpiceOfLifeFabricFlavor.LOGGER.error("sol2f.HealthUseHandler.calculateTheoreticalMaxHealthBonus | Failed to calculate theoretical max bonus", e);
            return config.health.maxHealth; // 返回配置中的最大增益值作为fallback
        }
    }

    // 数据部分
    /**
     * 清空玩家本地食物记录并同步客户端属性。
     */
    public static void cleanEatenFoods(ServerPlayerEntity player) {
        Map<String, FoodValue> empty = new HashMap<>();
        saveLocalFoodValues(player, empty, true);
        applyHealthModifier(player, empty);
        NetWorkHandler.syncConsumedFoodToClient(player, empty.keySet());
    }

    /**
     * 兼容旧调用方，为玩家添加一项经过上限保护的食物记录。
     */
    public static void addEatenFood(ServerPlayerEntity player, ItemStack food) {
        Map<String, FoodValue> eatenFoods = getEatenFoodValues(player);
        String foodId = Registries.ITEM.getId(food.getItem()).toString();

        if (!Util.isFoodItem(food) || eatenFoods.size() >= NetworkChannels.MAX_FOOD_ENTRIES) {
            return;
        }
        if (!eatenFoods.containsKey(foodId)) {
            FoodValue foodValue = readFoodValue(food);
            eatenFoods.put(foodId, foodValue);
            saveLocalFoodValues(player, eatenFoods, true);
            PlayerDataSyncService.recordFood(player, foodValue, eatenFoods);
            applyHealthModifier(player, eatenFoods);
            NetWorkHandler.syncConsumedFoodToClient(player, eatenFoods.keySet());
        }
    }

    /**
     * 读取在线会话食物快照；未启用数据库时回退到本地 NBT。
     */
    public static Set<String> getEatenFoods(ServerPlayerEntity player) {
        return getEatenFoodValues(player).keySet();
    }

    /**
     * 读取在线会话食物数值快照；未启用数据库时回退到本地 NBT。
     */
    public static Map<String, FoodValue> getEatenFoodValues(ServerPlayerEntity player) {
        Map<String, FoodValue> synced = PlayerDataSyncService.getFoodValuesSnapshot(player.getUuid());
        if (synced != null) {
            return synced;
        }
        return readLocalFoodValues(player);
    }

    /**
     * 直接读取玩家本地 NBT，不访问在线同步会话。
     */
    public static Set<String> readLocalEatenFoods(ServerPlayerEntity player) {
        return readLocalFoodValues(player).keySet();
    }

    /**
     * 直接读取玩家本地 NBT 中的食物数值，旧版 ID 列表会从当前注册表补全。
     */
    public static Map<String, FoodValue> readLocalFoodValues(ServerPlayerEntity player) {
        try {
            NbtCompound persistent = Util.readPersistentCompound(player);
            NbtList consumed = persistent.contains(CONSUMED_KEY, 9) ? persistent.getList(CONSUMED_KEY, 8)
                    : new NbtList();
            Map<String, FoodValue> values = new HashMap<>();
            int limit = Math.min(consumed.size(), NetworkChannels.MAX_FOOD_ENTRIES);
            for (int i = 0; i < limit; i++) {
                String food = consumed.getString(i);
                if (!food.isBlank() && food.length() <= NetworkChannels.MAX_FOOD_ID_LENGTH) {
                    values.put(food, FoodValue.unknown(food));
                }
            }
            NbtList storedValues = persistent.contains(FOOD_VALUES_KEY, 9)
                    ? persistent.getList(FOOD_VALUES_KEY, 10)
                    : new NbtList();
            int valueLimit = Math.min(storedValues.size(), NetworkChannels.MAX_FOOD_ENTRIES);
            for (int i = 0; i < valueLimit; i++) {
                NbtCompound value = storedValues.getCompound(i);
                String foodId = value.getString(FOOD_ID_KEY);
                if (values.containsKey(foodId) && value.contains(HUNGER_POINTS_KEY, 3)
                        && value.contains(SATURATION_MODIFIER_KEY, 6)) {
                    values.put(foodId, new FoodValue(
                            foodId,
                            value.getInt(HUNGER_POINTS_KEY),
                            value.getDouble(SATURATION_MODIFIER_KEY)));
                }
            }
            for (Map.Entry<String, FoodValue> entry : values.entrySet()) {
                if (!entry.getValue().known()) {
                    entry.setValue(resolveFoodValue(entry.getKey()));
                }
            }
            return values;
        } catch (Exception e) {
            SpiceOfLifeFabricFlavor.LOGGER.error("sol2f.HealthModule.getEatenFoods | failed to get eaten foods", e);
            return new HashMap<>();
        }
    }

    /**
     * 将食物集合保存到本地玩家 NBT，并记录是否等待数据库确认。
     */
    public static void saveLocalEatenFoods(ServerPlayerEntity player, Set<String> eatenFoods, boolean dirty) {
        saveLocalFoodValues(player, resolveFoodValues(eatenFoods), dirty);
    }

    /**
     * 将食物 ID 和首次发现数值保存到本地玩家 NBT。
     */
    public static void saveLocalFoodValues(ServerPlayerEntity player, Map<String, FoodValue> eatenFoods, boolean dirty) {
        try {
            NbtCompound persistent = Util.readPersistentCompound(player);
            NbtList newList = new NbtList();
            NbtList valueList = new NbtList();
            int written = 0;
            for (FoodValue food : eatenFoods.values()) {
                if (written >= NetworkChannels.MAX_FOOD_ENTRIES) {
                    break;
                }
                if (food == null || food.foodId().length() > NetworkChannels.MAX_FOOD_ID_LENGTH) {
                    continue;
                }
                newList.add(NbtString.of(food.foodId()));
                if (food.known()) {
                    NbtCompound value = new NbtCompound();
                    value.putString(FOOD_ID_KEY, food.foodId());
                    value.putInt(HUNGER_POINTS_KEY, food.hungerPoints());
                    value.putDouble(SATURATION_MODIFIER_KEY, food.saturationModifier());
                    valueList.add(value);
                }
                written++;
            }
            persistent.put(CONSUMED_KEY, newList);
            persistent.put(FOOD_VALUES_KEY, valueList);
            persistent.putInt(DATA_VERSION, CURRENT_VERSION);
            persistent.putBoolean(DATABASE_DIRTY_KEY, dirty);
            Util.writePersistentCompound(player, persistent);
        } catch (Exception e) {
            SpiceOfLifeFabricFlavor.LOGGER.error("sol2f.HealthModule.saveLocalEatenFoods | failed to save eaten foods", e);
        }
    }

    /**
     * 从物品的标准 FoodComponent 创建持久化食物数值。
     */
    public static FoodValue readFoodValue(ItemStack stack) {
        String foodId = Registries.ITEM.getId(stack.getItem()).toString();
        FoodComponent component = stack.getItem().getFoodComponent();
        if (component == null) {
            return FoodValue.unknown(foodId);
        }
        return new FoodValue(foodId, component.getHunger(), component.getSaturationModifier());
    }

    /**
     * 将食物 ID 集合解析成当前注册表中的数值快照。
     */
    public static Map<String, FoodValue> resolveFoodValues(Set<String> foodIds) {
        Map<String, FoodValue> values = new HashMap<>();
        for (String foodId : foodIds) {
            if (values.size() >= NetworkChannels.MAX_FOOD_ENTRIES) {
                break;
            }
            if (foodId != null && !foodId.isBlank() && foodId.length() <= NetworkChannels.MAX_FOOD_ID_LENGTH) {
                values.put(foodId, resolveFoodValue(foodId));
            }
        }
        return values;
    }

    /**
     * 读取当前注册表中一个食物 ID 的标准 FoodComponent 数值。
     */
    public static FoodValue resolveFoodValue(String foodId) {
        try {
            Identifier identifier = new Identifier(foodId);
            if (!Registries.ITEM.containsId(identifier)) {
                return FoodValue.unknown(foodId);
            }
            FoodComponent component = Registries.ITEM.get(identifier).getFoodComponent();
            return component == null
                    ? FoodValue.unknown(foodId)
                    : new FoodValue(foodId, component.getHunger(), component.getSaturationModifier());
        } catch (RuntimeException exception) {
            return FoodValue.unknown(foodId);
        }
    }

    /**
     * 返回当前本服全部有效食物及其标准 FoodComponent 数值。
     */
    private static Map<String, FoodValue> getAllFoodValues() {
        return resolveFoodValues(getAllFoods());
    }

    /**
     * 使用统一变量计算生命值公式。
     */
    static double evaluateHealthFormula(String formula, int uniqueFoods, double totalHunger,
            double totalSaturation) {
        return Util.evaluate(formula, Map.of(
                "uniqueFoods", (double) uniqueFoods,
                "totalHunger", totalHunger,
                "totalSaturation", totalSaturation));
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
