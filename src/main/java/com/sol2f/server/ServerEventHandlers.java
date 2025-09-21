package com.sol2f.server;

import com.sol2f.config.ModConfig;
import com.sol2f.SpiceOfLifeFabricFlavor;
import com.sol2f.network.S2CFoodListSync;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class ServerEventHandlers {
    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            try {
                if (handler.player == null) return;
                FoodUseHandler.initializePlayer(handler.player);
            } catch (Exception e) {
                SpiceOfLifeFabricFlavor.LOGGER.error("sol2f: error during player join init", e);
            }
        });

        // On respawn, re-apply health modifier and optionally reset consumed list
        net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            try {
                if (!alive && ModConfig.getInstance().resetOnDeath) {
                    FoodUseHandler.clearEatenFoods(newPlayer);
                } else {
                    FoodUseHandler.initializePlayer(newPlayer);
                }
            } catch (Exception e) {
                SpiceOfLifeFabricFlavor.LOGGER.error("sol2f: error during player respawn init", e);
            }
        });

        // respond to explicit client requests for the consumed list. Clients may register receiver
        // after join; this lets them ask the server to re-send the list immediately.
        ServerPlayNetworking.registerGlobalReceiver(com.sol2f.network.FoodPackets.C2S_REQUEST_LIST, (server, player, handler, buf, responder) -> {
            try {
                net.minecraft.nbt.NbtCompound nbt = new net.minecraft.nbt.NbtCompound();
                player.writeNbt(nbt);
                net.minecraft.nbt.NbtList consumed = nbt.contains("sol2f:consumed_foods") ? nbt.getList("sol2f:consumed_foods", 8) : new net.minecraft.nbt.NbtList();
                java.util.List<String> list = new java.util.ArrayList<>();
                for (int i = 0; i < consumed.size(); i++) list.add(consumed.getString(i));
                com.sol2f.network.S2CFoodListSync.sendTo(player, list);
            } catch (Exception e) {
                SpiceOfLifeFabricFlavor.LOGGER.error("sol2f: error responding to client list request", e);
            }
        });
    }
}
