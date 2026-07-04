package com.sol2f.server;

import java.util.ArrayList;
import java.util.Set;

import com.sol2f.SpiceOfLifeFabricFlavor;
import com.sol2f.interfaces.IEntityDataSaver;
import com.sol2f.module.HealthModule;
import com.sol2f.module.HungerModule;
import com.sol2f.config.Sol2FConfig;
import com.sol2f.network.NetWorkHandler;
import com.sol2f.network.payload.*;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;

public class ServerEvents {
    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {// 玩家加入时
            ServerPlayerEntity player = handler.player;// 获取玩家实例
            if (player == null) return;

            // 获取并应用已消耗的食物列表
            Set<String> eaten = HealthModule.getEatenFoods(player);
            HealthModule.applyHealthModifier(player, eaten);
            NetWorkHandler.SyncConsumedFoodToClient(player, eaten);
            NetWorkHandler.SyncFoodDataToClient(player, HealthModule.calculateTheoreticalMaxHealthBonus());// 同步食物for部分客户端GUI显示
        });

        ServerPlayNetworking.registerGlobalReceiver(C2SRequestAllFoodListPayload.PACKET_ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            ServerPlayNetworking.send(player, new S2CAllFoodListPayload(new ArrayList<>(HealthModule.getAllFoods())));
        });

        // 复制逻辑
        // 注：ServerPlayerEvents.COPY_FROM 在两种场景会被调用：玩家死亡并重生 (alive == false)；玩家在同一会话中切换维度/传送 (alive == true)。（需要在维度切换时也复制持久化数据（例如末地返回），否则会出现数据不同步的问题。）
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            boolean shouldCopy = alive || (!alive && !AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig().health.resetOnDeath);
            if (shouldCopy) {
                if (oldPlayer instanceof IEntityDataSaver olDataSaver && newPlayer instanceof IEntityDataSaver newDataSaver) {
                    // 复制持久化数据
                    newDataSaver.getPersistentData().copyFrom(olDataSaver.getPersistentData());
                }
                HealthModule.applyHealthModifier(newPlayer);
            }
        });

        // 重置和应用逻辑
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if(!alive) {
                if (AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig().health.resetOnDeath) {
                    // 重置已消耗的食物列表
                    HealthModule.cleanEatenFoods(newPlayer);
                } else {
                    // 复制旧玩家的已消耗食物列表到新玩家
                    Set<String> eaten = HealthModule.getEatenFoods(newPlayer);
                    HealthModule.applyHealthModifier(newPlayer, eaten);
                    NetWorkHandler.SyncConsumedFoodToClient(newPlayer, eaten);
                    
                }
                ((IEntityDataSaver) (Object) newPlayer).resetHungerTickCounter();// 重置饥HungerTick计数器
            }
        });

        // 响应客户端请求食物列表
        ServerPlayNetworking.registerGlobalReceiver(C2SRequestFoodListPayload.PACKET_ID, (payload, context) -> {
            try {
                ServerPlayerEntity player = context.player();
                Set<String> eaten = HealthModule.getEatenFoods(player);
                NetWorkHandler.SyncConsumedFoodToClient(player, eaten);
            } catch (Exception e) {
                SpiceOfLifeFabricFlavor.LOGGER.error("sol2f.ServerEvents.respondToClientListRequest | error responding to client list request", e);
            }
        });

        ServerTickEvents.END_SERVER_TICK.register((server) -> {
            Sol2FConfig config = AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig();
            if(!config.hunger.EnableNaturalHunger) {
                return;
            }
            int period = config.hunger.NaturalHungerPeriod;

            if (period <= 0) return;

            for (ServerWorld world : server.getWorlds()) {
                for (ServerPlayerEntity player : world.getPlayers()) {
                    processHungerTick(player, period);
                }
            }
        });

        EntitySleepEvents.START_SLEEPING.register((entity, sleepingPos) -> {
            Sol2FConfig config = AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig();
            if (entity instanceof ServerPlayerEntity player) {
                long TimeOfDay = player.getWorld().getTimeOfDay();
                int NormalizedStartTick = (int) (TimeOfDay % 24000);
                
                IEntityDataSaver accessor = (IEntityDataSaver) player;
                accessor.setSleepStartTick(NormalizedStartTick);

                if (config.dev.DeveloperMode) {
                    SpiceOfLifeFabricFlavor.LOGGER.info("sol2f.ServerEvents.onPlayerSleep | Sleep started at world time: {}", NormalizedStartTick);
                }
            }
        });

        EntitySleepEvents.STOP_SLEEPING.register((entity, sleepingPos) -> {
            Sol2FConfig config = AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig();
            if (entity instanceof ServerPlayerEntity player) {
                long TimeOfDay = player.getWorld().getTimeOfDay();
                int NormalizedEndTick = (int) (TimeOfDay % 24000);
                
                IEntityDataSaver accessor = (IEntityDataSaver) player;
                int StartTick = accessor.getSleepStartTick();

                // 处理跨天回卷 (Wrap-around)
                int sleepDuration;
                if (NormalizedEndTick < StartTick) {
                    // 如果醒来时间 < 入睡时间，说明跨越了午夜 (例如 23000 -> 1000)
                    // 计算公式：(24000 - 入睡时间) + 醒来时间
                    sleepDuration = (24000 - StartTick) + NormalizedEndTick;
                } else {
                    // 未跨天 (正常情况极少见，可能是雷雨天白天小睡或异常唤醒)
                    sleepDuration = NormalizedEndTick - StartTick;
                }

                if (config.dev.DeveloperMode) {
                    SpiceOfLifeFabricFlavor.LOGGER.info("sol2f.ServerEvents.onPlayerSleep | Sleep stopped at world time: {}, calculated sleep duration: {} ticks", NormalizedEndTick, sleepDuration);
                }

                HungerModule.PlayerSleepHungerHandler(player, sleepDuration);
            }
        });
    }

    private static void processHungerTick(ServerPlayerEntity player, int period) {
        Sol2FConfig config = AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig();
        IEntityDataSaver accessor = (IEntityDataSaver) player;
        if (accessor.getHungerTickCounter() >= period) {
            if (config.dev.DeveloperMode) {
                SpiceOfLifeFabricFlavor.LOGGER.info("sol2f.ServerEvents.processHungerTick | Hunger tick counter / period : " + accessor.getHungerTickCounter() + " / " + period);
            }

            accessor.resetHungerTickCounter();
            if (config.dev.DeveloperMode) {
                SpiceOfLifeFabricFlavor.LOGGER.info("sol2f.ServerEvents.processHungerTick | Hunger tick counter reset");
            }

            HungerModule.PlayerNaturalHungerHandler(player);
        } else {
            accessor.increaseHungerTickCounter();
        }
    }
}