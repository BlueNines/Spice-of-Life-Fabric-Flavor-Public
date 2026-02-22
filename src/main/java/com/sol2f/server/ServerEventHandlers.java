package com.sol2f.server;

import java.util.ArrayList;
import java.util.Set;

import com.sol2f.SpiceOfLifeFabricFlavor;
import com.sol2f.util.IEntityDataSaver;
import com.sol2f.config.Sol2FConfig;
import com.sol2f.network.payload.*;

import net.minecraft.server.network.ServerPlayerEntity;
import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;

public class ServerEventHandlers {
    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.player;// 获取玩家实例
            if (player == null) return;// 防止为null，避免空指针异常

            Set<String> eaten = FoodUseHandler.getEatenFoods(player);
            FoodUseHandler.applyHealthModifier(player, eaten);// 应用生命值修饰符，eaten不会为null，上面的方法保证了这一点
            FoodUseHandler.syncToClient(player, eaten);// 同步已消耗列表到客户端

            ServerPlayNetworking.send(player, new S2CAllFoodListPayload(new ArrayList<>(FoodUseHandler.getAllFoods())));// 发送所有食物的列表
            ServerPlayNetworking.send(player, new S2CFoodListPayload(new ArrayList<>(eaten)));// 发送食物的列表
            ServerPlayNetworking.send(player, new S2CHealthMaxPayload(FoodUseHandler.calculateTheoreticalMaxHealthBonus()));
        });

        ServerPlayNetworking.registerGlobalReceiver(C2SRequestAllFoodListPayload.PACKET_ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            ServerPlayNetworking.send(player, new S2CAllFoodListPayload(new ArrayList<>(FoodUseHandler.getAllFoods())));
        });

        // 复制逻辑
        // ServerPlayerEvents.COPY_FROM 在两种场景会被调用：
        // 1) 玩家死亡并重生 (alive == false)
        // 2) 玩家在同一会话中切换维度/传送 (alive == true)
        // 需要在维度切换时也复制持久化数据（例如末地返回），否则会出现数据不同步的问题。
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            boolean shouldCopy = alive || (!alive && !AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig().features.resetOnDeath);
            if (shouldCopy) {
                if (oldPlayer instanceof IEntityDataSaver olDataSaver && newPlayer instanceof IEntityDataSaver newDataSaver) {
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

        // 响应客户端请求食物列表
        ServerPlayNetworking.registerGlobalReceiver(C2SRequestFoodListPayload.PACKET_ID, (payload, context) -> {
            try {
                ServerPlayerEntity player = context.player();
                Set<String> eaten = FoodUseHandler.getEatenFoods(player);
                FoodUseHandler.syncToClient(player, eaten);
            } catch (Exception e) {
                SpiceOfLifeFabricFlavor.LOGGER.error("sol2f: error responding to client list request", e);
            }
        });
    }
}