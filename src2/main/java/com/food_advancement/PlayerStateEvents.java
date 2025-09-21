package com.food_advancement;

import com.food_advancement.network.NetworkHandler;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

public class PlayerStateEvents {
    public static void register() {
        // 监听玩家加入服务器事件
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            // 当玩家加入时，应用生命值修饰符
            PlayerFoodState.applyHealthModifier(handler.player);
            // 同步食物数据到客户端，确保tooltip正确显示
            NetworkHandler.syncFoodStateToClient(handler.player);
        });

        // 监听玩家重生事件
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            // 玩家重生后，NBT数据会自动复制，我们只需重新应用生命值加成
            PlayerFoodState.applyHealthModifier(newPlayer);
            // 无需再次同步数据到客户端，因为客户端没有断开连接，数据仍然存在
        });
    }
}
