package com.food_advancement.network;

import com.food_advancement.Food_advancement;
import com.food_advancement.data.PlayerFoodData;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import java.util.Set;

public class NetworkHandler {
    public static final Identifier SYNC_FOOD_STATE = new Identifier(Food_advancement.MOD_ID, "sync_food_state");

    public static void initializeServerNetworking() {
        // Server does not need to register a receiver, as all data flows from server to client.
    }

    public static void syncFoodStateToClient(ServerPlayerEntity player) {
    Set<String> eatenFoods = PlayerFoodData.getEatenFoods(player);
        PacketByteBuf buf = PacketByteBufs.create();
        
        buf.writeVarInt(eatenFoods.size());
        for (String foodId : eatenFoods) {
            buf.writeString(foodId);
        }
        
        ServerPlayNetworking.send(player, SYNC_FOOD_STATE, buf);
    }
}
