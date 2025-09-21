package com.food_advancement.data;

import com.food_advancement.ModConfig;
import com.food_advancement.network.NetworkHandler;
import com.food_advancement.util.IEntityDataSaver;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PlayerFoodData {

    private static final String EATEN_FOODS_KEY = "eatenFoods";
    private static final UUID HEALTH_MODIFIER_ID = UUID.fromString("a6b4d2a1-9f23-41a2-a6c6-b56db969f6de");

    // --- NBT Access ---

    private static Set<String> getEatenFoods(PlayerEntity player) {
        NbtCompound data = ((IEntityDataSaver) player).getPersistentData();
        NbtList foodList = data.getList(EATEN_FOODS_KEY, 8); // 8 is the NBT type for String
        Set<String> eatenFoods = new HashSet<>();
        for (int i = 0; i < foodList.size(); i++) {
            eatenFoods.add(foodList.getString(i));
        }
        return eatenFoods;
    }

    private static void saveEatenFoods(PlayerEntity player, Set<String> eatenFoods) {
        NbtCompound data = ((IEntityDataSaver) player).getPersistentData();
        NbtList foodList = new NbtList();
        for (String foodId : eatenFoods) {
            foodList.add(NbtString.of(foodId));
        }
        data.put(EATEN_FOODS_KEY, foodList);
    }

    // --- Public API for Server-Side Logic ---

    public static void addEatenFood(ServerPlayerEntity player, Item food) {
        Set<String> eatenFoods = getEatenFoods(player);
        Identifier id = Registries.ITEM.getId(food);

        if (eatenFoods.add(id.toString())) {
            saveEatenFoods(player, eatenFoods);
            applyHealthModifier(player);
            syncToClient(player);
        }
    }

    public static void applyHealthModifier(ServerPlayerEntity player) {
        EntityAttributeInstance attribute = player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (attribute == null) return;

        // Remove old modifier before applying a new one
        attribute.removeModifier(HEALTH_MODIFIER_ID);

        int uniqueFoodsEaten = getEatenFoods(player).size();
        // 使用 ModConfig.getInstance().getHealthyGain() 来获取配置值
        int healthBonus = (uniqueFoodsEaten / ModConfig.getInstance().getHealthyGain()) * 2; // 2 health points = 1 heart

        if (healthBonus > 0) {
            EntityAttributeModifier modifier = new EntityAttributeModifier(
                    HEALTH_MODIFIER_ID,
                    "Food Advancement Health Bonus",
                    healthBonus,
                    EntityAttributeModifier.Operation.ADDITION
            );
            attribute.addPersistentModifier(modifier);
        }

        // Heal the player to their new max health
        player.heal(player.getMaxHealth());
    }

    public static void syncToClient(ServerPlayerEntity player) {
        Set<String> eatenFoods = getEatenFoods(player);
        NetworkHandler.syncFoodListToClient(player, eatenFoods);
    }

    public static void clearEatenFoods(ServerPlayerEntity player) {
        saveEatenFoods(player, new HashSet<>());
        applyHealthModifier(player);
        syncToClient(player);
    }

    public static boolean hasEaten(PlayerEntity player, Item item) {
        return getEatenFoods(player).contains(Registries.ITEM.getId(item).toString());
    }
}
