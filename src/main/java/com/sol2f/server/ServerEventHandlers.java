package com.sol2f.server;

import java.util.Set;

import com.sol2f.SpiceOfLifeFabricFlavor;
import net.minecraft.server.network.ServerPlayerEntity;
import com.sol2f.config.Sol2FConfig;
import me.shedaniel.autoconfig.AutoConfig;
import com.sol2f.util.IEntityDataSaver;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;

public class ServerEventHandlers {
    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.player;// 获取玩家实例
            if (player == null) return;// 防止为null，避免空指针异常

            Set<String> eaten = FoodUseHandler.getEatenFoods(player);// 返回空集而不是null，避免空指针异常
            FoodUseHandler.applyHealthModifier(player, eaten);// 应用生命值修饰符，eaten不会为null，上面的方法保证了这一点
            FoodUseHandler.syncToClient(player, eaten);// 同步已消耗列表到客户端
        });

        // 复制逻辑
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            if(!alive && !AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig().features.resetOnDeath) {
                if (oldPlayer instanceof IEntityDataSaver olDataSaver && newPlayer instanceof IEntityDataSaver newDataSaver) {// 确保旧玩家和新玩家都实现了我们的IEntityDataSaver接口
                    // 复制持久化数据
                    newDataSaver.getPersistentData().copyFrom(olDataSaver.getPersistentData());
                }
            }
        });

        // 重置和应用逻辑
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if(!alive) {
                if (AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig().features.resetOnDeath) {
                    // 重置已消耗的食物列表
                    FoodUseHandler.clearEatenFoods(newPlayer);
                } else {
                    // 复制旧玩家的已消耗食物列表到新玩家
                    Set<String> eaten = FoodUseHandler.getEatenFoods(newPlayer);
                    FoodUseHandler.applyHealthModifier(newPlayer, eaten);
                    FoodUseHandler.syncToClient(newPlayer, eaten);
                }
            }
        });

        // 响应客户端对已消耗列表的明确请求。客户端可能在加入后注册接收器；
        // 这允许他们立即要求服务器重新发送列表。
        ServerPlayNetworking.registerGlobalReceiver(com.sol2f.network.FoodPackets.C2S_REQUEST_LIST, (server, player, handler, buf, responder) -> {
            try {
                Set<String> eaten = FoodUseHandler.getEatenFoods(player);
                FoodUseHandler.syncToClient(player, eaten);
            } catch (Exception e) {
                SpiceOfLifeFabricFlavor.LOGGER.error("sol2f: error responding to client list request", e);
            }
        });
    }
}