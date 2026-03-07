package com.sol2f.network;

import java.util.ArrayList;
import java.util.Set;

import com.sol2f.SpiceOfLifeFabricFlavor;
import com.sol2f.network.payload.*;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

public class NetWorkHandler {

    public static void SyncConsumedFoodToClient(ServerPlayerEntity player, Set<String> eatenFoods) {
        try {
            ServerPlayNetworking.send(player, new S2CFoodListPayload(new ArrayList<>(eatenFoods)));
            SpiceOfLifeFabricFlavor.LOGGER.info("Synced food list to client: {}", eatenFoods);
        } catch (Exception e) {
            SpiceOfLifeFabricFlavor.LOGGER.error("Failed to sync food list to client", e);
        }
    } 

    public static void SyncFoodDataToClient(ServerPlayerEntity player, Set<String> AllFoodList, int MaxHealth) {
        try {
            ServerPlayNetworking.send(player, new S2CAllFoodListPayload(new ArrayList<>(AllFoodList)));
            ServerPlayNetworking.send(player, new S2CHealthPayload(MaxHealth));

            SpiceOfLifeFabricFlavor.LOGGER.info("Synced food data to client");
        } catch (Exception e) {
            SpiceOfLifeFabricFlavor.LOGGER.error("Failed to sync food data to client", e);
        }
    }
}
