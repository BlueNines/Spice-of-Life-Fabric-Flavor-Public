package com.sol2f.server;

import com.sol2f.config.Sol2FConfig;
import me.shedaniel.autoconfig.AutoConfig;
import com.sol2f.SpiceOfLifeFabricFlavor;
import net.minecraft.text.Text;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;

public class FoodUseHandler {

    // 为初始化器保留注册；使用mixin时不需要运行时注册
    public static void register() {
    // 注册占位符（服务器事件处理程序位于ServerEventHandlers中）
    }

    public static void onFoodEaten(ServerPlayerEntity serverPlayer, ItemStack stack) {
        if (stack == null || stack.getItem().getFoodComponent() == null) return;

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
            // 在锁内重新读取权威NBT，你这NBT保真吗？
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

                // 重新读取权威持久化集合以确保写入成功，虽然但是网页后端的写入+应用逻辑就是这样的，mod里就这样吧
                Set<String> authoritative = getEatenFoods(serverPlayer);
                if (!authoritative.contains(id)) {
                    SpiceOfLifeFabricFlavor.LOGGER.warn("sol2f: after save, authoritative eaten set does not contain {} for player {}", id, serverPlayer.getName().getString());
                } else {
                    // 使用权威更新集应用生命值修饰符，直接应用新上限而不是加值
                    double prevMax = serverPlayer.getMaxHealth();
                    applyHealthModifier(serverPlayer, authoritative);
                    double newMax = serverPlayer.getMaxHealth();
                    syncToClient(serverPlayer, authoritative);

                    // 基于NBT状态在消息栏发送一次消息
                    serverPlayer.sendMessage(Text.translatable("sol2f.msg.first_eaten", Text.literal(id)), false);
                    SpiceOfLifeFabricFlavor.LOGGER.info("sol2f: Player {} first ate {}", serverPlayer.getName().getString(), id);

                    // 写日志：记录变更后详情，
                    try {
                        if (AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig().features.developerMode) {
                            SpiceOfLifeFabricFlavor.LOGGER.info("sol2f-log: after eat player={} food={} prevMax={} newMax={} eaten={}", serverPlayer.getName().getString(), id, prevMax, newMax, authoritative);
                            java.util.Map<String, String> fields = new java.util.LinkedHashMap<>();
                            fields.put("event", "after_eat");
                            fields.put("player", serverPlayer.getName().getString());
                            fields.put("food", id);
                            fields.put("prevMax", Double.toString(prevMax));
                            fields.put("newMax", Double.toString(newMax));
                            fields.put("eaten", authoritative.toString());
                            fields.put("unique", Integer.toString(authoritative.size()));
                            fields.put("perHp", Double.toString(AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig().features.healthyGain));
                            fields.put("bonus", Double.toString(Math.min(AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig().healthy.defaultHealthy + authoritative.size() * AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig().features.healthyGain, AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig().healthy.maxHealthy) - AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig().healthy.defaultHealthy));
                            fields.put("modifier_uuid", HEALTH_MODIFIER_ID.toString());
                            SpiceOfLifeFabricFlavor.writeStructuredDevLog(fields);
                        }
                    } catch (Throwable t) {
                        // 忽略日志记录失败。反正先给债欠着，火还没出现想什么烧眉毛，大不了改成失败写入“unfiled”
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

    public static void initializePlayer(ServerPlayerEntity player) {
    double defaultHealthy = AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig().healthy.defaultHealthy;
        player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(defaultHealthy);
        Set<String> eaten = getEatenFoods(player);
        applyHealthModifier(player, eaten);
        syncToClient(player, eaten);
    }

    private static final UUID HEALTH_MODIFIER_ID = UUID.fromString("a6b4d2a1-9f23-41a2-a6c6-b56db969f6de");
    private static final String PERSISTENT_ROOT = "sol2f";
    private static final String CONSUMED_KEY = "consumed_foods";
    private static final String LEGACY_KEY = "sol2f:consumed_foods";
    private static final String DATA_VERSION = "data_version";
    private static final int CURRENT_VERSION = 1;

    // 反射助手：如果可用，尝试调用IEntityDataSaver.getPersistentData()
    private static java.lang.reflect.Method persistentGetterMethod = null;
    private static boolean persistentReflectionInitialized = false;

    private static void ensurePersistentReflection() {
        if (persistentReflectionInitialized) return;
        persistentReflectionInitialized = true;
            try {
                // 尝试旧包然后是我们自己的包
                Class<?> saver = null;
                try {
                    saver = Class.forName("com.food_advancement.util.IEntityDataSaver");
                } catch (Throwable t) {
                    // 忽略
                }
                if (saver == null) {
                    saver = Class.forName("com.sol2f.util.IEntityDataSaver");
                }
                persistentGetterMethod = saver.getMethod("getPersistentData");
        } catch (Throwable t) {
            // 无操作：将回退到read/writeNbt
                persistentGetterMethod = null;
        }
    }

    private static NbtCompound readPersistentCompound(ServerPlayerEntity player) {
        ensurePersistentReflection();
        if (persistentGetterMethod != null) {
            try {
                Object res = persistentGetterMethod.invoke(player);
                if (res instanceof NbtCompound) {
                    return (NbtCompound) res;
                }
            } catch (Throwable t) {
                SpiceOfLifeFabricFlavor.LOGGER.debug("Persistent reflection failed, falling back", t);
            }
        }

        // 回退：读取完整的玩家NBT并获取或创建我们的根复合体
        try {
            NbtCompound nbt = new NbtCompound();
            player.writeNbt(nbt);
            if (nbt.contains(PERSISTENT_ROOT, 10)) {
                return nbt.getCompound(PERSISTENT_ROOT);
            }

            // 如果根部存在旧密钥，则迁移到我们的根复合体中
            if (nbt.contains(LEGACY_KEY, 9)) {
                NbtList legacy = nbt.getList(LEGACY_KEY, 8);
                NbtCompound root = new NbtCompound();
                root.put(CONSUMED_KEY, legacy);
                root.putInt(DATA_VERSION, CURRENT_VERSION);
                // 写回
                nbt.put(PERSISTENT_ROOT, root);
                player.readNbt(nbt);
                return root;
            }

            // 创建空根并写入
            NbtCompound root = new NbtCompound();
            root.putInt(DATA_VERSION, CURRENT_VERSION);
            nbt.put(PERSISTENT_ROOT, root);
            player.readNbt(nbt);
            return root;
        } catch (Throwable t) {
            SpiceOfLifeFabricFlavor.LOGGER.error("Failed to read persistent compound", t);
            return new NbtCompound();
        }
    }

    private static void writePersistentCompound(ServerPlayerEntity player, NbtCompound data) {
        ensurePersistentReflection();
        if (persistentGetterMethod != null) {
            try {
                Object res = persistentGetterMethod.invoke(player);
                if (res instanceof NbtCompound) {
                    NbtCompound target = (NbtCompound) res;
                    // 将数据中的键复制到目标中
                    for (String key : data.getKeys()) {
                        target.put(key, data.get(key));
                    }
                    SpiceOfLifeFabricFlavor.LOGGER.debug("sol2f: attempted reflection write to persistent data for player {}", player.getName().getString());
                    // 通过再次调用getter来验证
                    try {
                        Object verify = persistentGetterMethod.invoke(player);
                        if (verify instanceof NbtCompound) {
                            NbtCompound v = (NbtCompound) verify;
                            if (v.contains(CONSUMED_KEY, 9)) {
                                SpiceOfLifeFabricFlavor.LOGGER.info("sol2f: persistent reflection write verified for player {}: {}", player.getName().getString(), v.getList(CONSUMED_KEY, 8));
                                java.util.Map<String, String> _r = new java.util.LinkedHashMap<>();
                                _r.put("event", "persistent_reflection_write_verified");
                                _r.put("player", player.getName().getString());
                                _r.put("consumed", v.getList(CONSUMED_KEY, 8).toString());
                                SpiceOfLifeFabricFlavor.writeStructuredDevLog(_r);
                            } else {
                                SpiceOfLifeFabricFlavor.LOGGER.warn("sol2f: persistent reflection write did not find {} for player {}", CONSUMED_KEY, player.getName().getString());
                            }
                        }
                    } catch (Throwable t) {
                        SpiceOfLifeFabricFlavor.LOGGER.debug("sol2f: verification after reflection write failed", t);
                    }
                    // 不返回；也执行回退写入以确保在不同实现间持久化
                    SpiceOfLifeFabricFlavor.LOGGER.debug("sol2f: falling back to NBT write after reflection attempt for player {}", player.getName().getString());
                }
            } catch (Throwable t) {
                SpiceOfLifeFabricFlavor.LOGGER.debug("Persistent reflection write failed, falling back", t);
            }
        }

        // 回退：放入玩家主NBT中的PERSISTENT_ROOT下
        try {
            NbtCompound nbt = new NbtCompound();
            player.writeNbt(nbt);
            nbt.put(PERSISTENT_ROOT, data);
            player.readNbt(nbt);
            // 通过重新读取玩家的NBT来验证
            try {
                NbtCompound check = new NbtCompound();
                player.writeNbt(check);
                if (check.contains(PERSISTENT_ROOT, 10)) {
                    NbtCompound root = check.getCompound(PERSISTENT_ROOT);
                    if (root.contains(CONSUMED_KEY, 9)) {
                        SpiceOfLifeFabricFlavor.LOGGER.info("sol2f: fallback write verified for player {}: {}", player.getName().getString(), root.getList(CONSUMED_KEY, 8));
                        java.util.Map<String, String> _rf = new java.util.LinkedHashMap<>();
                        _rf.put("event", "fallback_write_verified");
                        _rf.put("player", player.getName().getString());
                        _rf.put("consumed", root.getList(CONSUMED_KEY, 8).toString());
                        SpiceOfLifeFabricFlavor.writeStructuredDevLog(_rf);
                    } else {
                        SpiceOfLifeFabricFlavor.LOGGER.warn("sol2f: fallback write did not find {} in player {}'s persistent root", CONSUMED_KEY, player.getName().getString());
                    }
                } else {
                    SpiceOfLifeFabricFlavor.LOGGER.warn("sol2f: fallback write did not produce persistent root for player {}", player.getName().getString());
                }
            } catch (Throwable t) {
                SpiceOfLifeFabricFlavor.LOGGER.debug("sol2f: verification after fallback write failed", t);
            }
        } catch (Throwable t) {
            SpiceOfLifeFabricFlavor.LOGGER.error("Failed to write persistent compound", t);
        }
    }

    private static Set<String> getEatenFoods(ServerPlayerEntity player) {
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
            com.sol2f.network.S2CFoodListSync.sendTo(player, new java.util.ArrayList<>(eatenFoods));
            SpiceOfLifeFabricFlavor.LOGGER.info("Synced food list to client: {}", eatenFoods);
        } catch (Exception e) {
            SpiceOfLifeFabricFlavor.LOGGER.error("Failed to sync to client", e);
        }
    }

    public static void clearEatenFoods(ServerPlayerEntity player) {
        Set<String> empty = new HashSet<>();
        saveEatenFoods(player, empty);
        applyHealthModifier(player, empty);
        syncToClient(player, empty);
    }

    public static void applyHealthModifier(ServerPlayerEntity player) {
        applyHealthModifier(player, getEatenFoods(player));
    }

    public static void applyHealthModifier(ServerPlayerEntity player, Set<String> eatenFoods) {
        try {
            EntityAttributeInstance attr = player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
            if (attr == null) {
                SpiceOfLifeFabricFlavor.LOGGER.error("Health attribute instance is null for player {}", player.getName().getString());
                return;
            }

            double defaultBase = AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig().healthy.defaultHealthy;
            attr.setBaseValue(defaultBase);
            SpiceOfLifeFabricFlavor.LOGGER.info("Set base health to {} for player {}", defaultBase, player.getName().getString());

            attr.removeModifier(HEALTH_MODIFIER_ID);
            SpiceOfLifeFabricFlavor.LOGGER.info("Removed existing health modifier for player {}", player.getName().getString());

            int unique = eatenFoods.size();
            // 将healthyGain解释为每个独特食物项目增加的生命值（以生命值单位）
            double perHp = AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig().features.healthyGain;
            double healthBonus = unique * perHp; // 来自独特食物的总生命值奖励

            double maxHealthy = AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig().healthy.maxHealthy;
            double newMaxHealth = Math.min(defaultBase + healthBonus, maxHealthy);

            if (newMaxHealth > defaultBase) {
                EntityAttributeModifier mod = new EntityAttributeModifier(HEALTH_MODIFIER_ID, "sol2f Health Bonus", newMaxHealth - defaultBase, EntityAttributeModifier.Operation.ADDITION);
                attr.addPersistentModifier(mod);
                SpiceOfLifeFabricFlavor.LOGGER.info("Added health modifier: {} for player {} (unique={}, perHp={}, bonus={})", mod, player.getName().getString(), unique, perHp, healthBonus);
            } else {
                SpiceOfLifeFabricFlavor.LOGGER.info("No health bonus applied. Unique foods: {}, perHp: {}, computed bonus: {}, Max health: {}", unique, perHp, healthBonus, maxHealthy);
            }
            // 恢复生命逻辑
            if (AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig().features.healToMaxOnIncrease) { // 恢复最大生命
                player.setHealth(player.getMaxHealth());
            } else if (AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig().features.healthIncrease > 0) { // 恢复指定生命
                float currentHealth = player.getHealth();
                float increase = AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig().features.healthIncrease;
                float newHealth = Math.min(currentHealth + increase, (float)player.getMaxHealth());
                player.setHealth(newHealth);
            } else {}
            SpiceOfLifeFabricFlavor.LOGGER.info("Player {} healed to max health: {}", player.getName().getString(), player.getMaxHealth());
        } catch (Exception e) {
            SpiceOfLifeFabricFlavor.LOGGER.error("Failed to apply health modifier for player {}", player.getName().getString(), e);
        }
    }
}