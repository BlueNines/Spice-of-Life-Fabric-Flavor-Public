package com.sol2f.server;

import com.sol2f.SpiceOfLifeFabricFlavor;
import com.sol2f.config.Sol2FConfig;
import me.shedaniel.autoconfig.AutoConfig;
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

        // 重生时，重新应用生命值修饰符并可选择重置已消耗列表
        net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            try {
                    if (!alive && AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig().features.resetOnDeath) {
                    FoodUseHandler.clearEatenFoods(newPlayer);
                } else {
                    FoodUseHandler.initializePlayer(newPlayer);
                }
            } catch (Exception e) {
                SpiceOfLifeFabricFlavor.LOGGER.error("sol2f: error during player respawn init", e);
            }
        });

        // 响应客户端对已消耗列表的明确请求。客户端可能在加入后注册接收器；
        // 这允许他们立即要求服务器重新发送列表。
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