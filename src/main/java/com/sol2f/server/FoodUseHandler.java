package com.sol2f.server;

import com.sol2f.config.ModConfig;
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

    // keep register for initializer; no runtime registration needed when using mixin
    public static void register() {
    // registration placeholder (server event handlers live in ServerEventHandlers)
    }

    public static void onFoodEaten(ServerPlayerEntity serverPlayer, ItemStack stack) {
        if (stack == null || stack.getItem().getFoodComponent() == null) return;

    String id = Registries.ITEM.getId(stack.getItem()).toString();

        // Use synchronized block on the player to avoid race conditions where two events
        // both read NBT before either writes it and thus both send the first-eaten message.
        synchronized (serverPlayer) {
            // Entry log for developerMode: record attempt
            try {
                if (ModConfig.getInstance().developerMode) {
                    SpiceOfLifeFabricFlavor.LOGGER.info("sol2f-log: eat attempt player={} food={}", serverPlayer.getName().getString(), id);
                }
            } catch (Throwable t) {
                // ignore logging failures
            }
            // re-read authoritative NBT inside the lock
            Set<String> current = getEatenFoods(serverPlayer);
            if (ModConfig.getInstance().developerMode) {
                SpiceOfLifeFabricFlavor.LOGGER.info("sol2f-log: before change player={} eaten={}", serverPlayer.getName().getString(), current);
            }
            if (!current.contains(id)) {
                // update and persist
                current.add(id);
                saveEatenFoods(serverPlayer, current);

                // re-read authoritative persistent set to ensure write succeeded
                Set<String> authoritative = getEatenFoods(serverPlayer);
                if (!authoritative.contains(id)) {
                    SpiceOfLifeFabricFlavor.LOGGER.warn("sol2f: after save, authoritative eaten set does not contain {} for player {}", id, serverPlayer.getName().getString());
                } else {
                    // apply health modifier using authoritative updated set
                    double prevMax = serverPlayer.getMaxHealth();
                    applyHealthModifier(serverPlayer, authoritative);
                    double newMax = serverPlayer.getMaxHealth();
                    syncToClient(serverPlayer, authoritative);

                    // send message once based on authoritative NBT state
                    serverPlayer.sendMessage(Text.translatable("sol2f.msg.first_eaten", Text.literal(id)), false);
                    SpiceOfLifeFabricFlavor.LOGGER.info("sol2f: Player {} first ate {}", serverPlayer.getName().getString(), id);

                    // Developer log: record post-change details
                    try {
                        if (ModConfig.getInstance().developerMode) {
                            SpiceOfLifeFabricFlavor.LOGGER.info("sol2f-log: after eat player={} food={} prevMax={} newMax={} eaten={}", serverPlayer.getName().getString(), id, prevMax, newMax, authoritative);
                        }
                    } catch (Throwable t) {
                        // ignore logging failures
                    }
                }
            }
                if (ModConfig.getInstance().developerMode) {
                    SpiceOfLifeFabricFlavor.LOGGER.info("sol2f-log: eat ignored (already eaten) player={} food={} eaten={}", serverPlayer.getName().getString(), id, current);
                }
        }
    }

    public static void initializePlayer(ServerPlayerEntity player) {
        double defaultHealthy = ModConfig.getInstance().defaultHealthy;
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

    // Reflection helper: attempt to call IEntityDataSaver.getPersistentData() if available.
    private static java.lang.reflect.Method persistentGetterMethod = null;
    private static boolean persistentReflectionInitialized = false;

    private static void ensurePersistentReflection() {
        if (persistentReflectionInitialized) return;
        persistentReflectionInitialized = true;
            try {
                // try legacy package then our own package
                Class<?> saver = null;
                try {
                    saver = Class.forName("com.food_advancement.util.IEntityDataSaver");
                } catch (Throwable t) {
                    // ignore
                }
                if (saver == null) {
                    saver = Class.forName("com.sol2f.util.IEntityDataSaver");
                }
                persistentGetterMethod = saver.getMethod("getPersistentData");
        } catch (Throwable t) {
            // no-op: will fallback to read/writeNbt
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

        // fallback: read full player NBT and get or create our root compound
        try {
            NbtCompound nbt = new NbtCompound();
            player.writeNbt(nbt);
            if (nbt.contains(PERSISTENT_ROOT, 10)) {
                return nbt.getCompound(PERSISTENT_ROOT);
            }

            // If legacy key exists at root, migrate into our root compound
            if (nbt.contains(LEGACY_KEY, 9)) {
                NbtList legacy = nbt.getList(LEGACY_KEY, 8);
                NbtCompound root = new NbtCompound();
                root.put(CONSUMED_KEY, legacy);
                root.putInt(DATA_VERSION, CURRENT_VERSION);
                // write back
                nbt.put(PERSISTENT_ROOT, root);
                player.readNbt(nbt);
                return root;
            }

            // create empty root and write
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
                    // copy keys from data into target
                    for (String key : data.getKeys()) {
                        target.put(key, data.get(key));
                    }
                    SpiceOfLifeFabricFlavor.LOGGER.debug("sol2f: attempted reflection write to persistent data for player {}", player.getName().getString());
                    // verify by invoking getter again
                    try {
                        Object verify = persistentGetterMethod.invoke(player);
                        if (verify instanceof NbtCompound) {
                            NbtCompound v = (NbtCompound) verify;
                            if (v.contains(CONSUMED_KEY, 9)) {
                                SpiceOfLifeFabricFlavor.LOGGER.info("sol2f: persistent reflection write verified for player {}: {}", player.getName().getString(), v.getList(CONSUMED_KEY, 8));
                            } else {
                                SpiceOfLifeFabricFlavor.LOGGER.warn("sol2f: persistent reflection write did not find {} for player {}", CONSUMED_KEY, player.getName().getString());
                            }
                        }
                    } catch (Throwable t) {
                        SpiceOfLifeFabricFlavor.LOGGER.debug("sol2f: verification after reflection write failed", t);
                    }
                    // don't return; also perform fallback write to ensure persistence across different implementations
                    SpiceOfLifeFabricFlavor.LOGGER.debug("sol2f: falling back to NBT write after reflection attempt for player {}", player.getName().getString());
                }
            } catch (Throwable t) {
                SpiceOfLifeFabricFlavor.LOGGER.debug("Persistent reflection write failed, falling back", t);
            }
        }

        // fallback: put under PERSISTENT_ROOT in player's main NBT
        try {
            NbtCompound nbt = new NbtCompound();
            player.writeNbt(nbt);
            nbt.put(PERSISTENT_ROOT, data);
            player.readNbt(nbt);
            // verify by reading back player's NBT
            try {
                NbtCompound check = new NbtCompound();
                player.writeNbt(check);
                if (check.contains(PERSISTENT_ROOT, 10)) {
                    NbtCompound root = check.getCompound(PERSISTENT_ROOT);
                    if (root.contains(CONSUMED_KEY, 9)) {
                        SpiceOfLifeFabricFlavor.LOGGER.info("sol2f: fallback write verified for player {}: {}", player.getName().getString(), root.getList(CONSUMED_KEY, 8));
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
            // read back and log authoritative persistent content
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

            double defaultBase = ModConfig.getInstance().defaultHealthy;
            attr.setBaseValue(defaultBase);
            SpiceOfLifeFabricFlavor.LOGGER.info("Set base health to {} for player {}", defaultBase, player.getName().getString());

            attr.removeModifier(HEALTH_MODIFIER_ID);
            SpiceOfLifeFabricFlavor.LOGGER.info("Removed existing health modifier for player {}", player.getName().getString());

            int unique = eatenFoods.size();
            // Interpret healthyGain as HP added per unique food item (in HP units)
            double perHp = ModConfig.getInstance().healthyGain;
            double healthBonus = unique * perHp; // total HP bonus from unique foods

            double maxHealthy = ModConfig.getInstance().maxHealthy;
            double newMaxHealth = Math.min(defaultBase + healthBonus, maxHealthy);

            if (newMaxHealth > defaultBase) {
                EntityAttributeModifier mod = new EntityAttributeModifier(HEALTH_MODIFIER_ID, "sol2f Health Bonus", newMaxHealth - defaultBase, EntityAttributeModifier.Operation.ADDITION);
                attr.addPersistentModifier(mod);
                SpiceOfLifeFabricFlavor.LOGGER.info("Added health modifier: {} for player {} (unique={}, perHp={}, bonus={})", mod, player.getName().getString(), unique, perHp, healthBonus);
            } else {
                SpiceOfLifeFabricFlavor.LOGGER.info("No health bonus applied. Unique foods: {}, perHp: {}, computed bonus: {}, Max health: {}", unique, perHp, healthBonus, maxHealthy);
            }

            player.heal((float) player.getMaxHealth());
            SpiceOfLifeFabricFlavor.LOGGER.info("Player {} healed to max health: {}", player.getName().getString(), player.getMaxHealth());
        } catch (Exception e) {
            SpiceOfLifeFabricFlavor.LOGGER.error("Failed to apply health modifier for player {}", player.getName().getString(), e);
        }
    }
}
