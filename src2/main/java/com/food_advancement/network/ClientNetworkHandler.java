package com.food_advancement.network;

import com.food_advancement.data.ClientFoodData;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import java.util.HashSet;
import java.util.Set;

public class ClientNetworkHandler {
    public static void initializeClientNetworking() {
        // 注册接收食物状态的处理器
        ClientPlayNetworking.registerGlobalReceiver(NetworkHandler.SYNC_FOOD_STATE, (client, handler, buf, responseSender) -> {
            // 读取食物列表
            int count = buf.readVarInt();
            Set<String> eatenFoods = new HashSet<>();
            for (int i = 0; i < count; i++) {
                eatenFoods.add(buf.readString());
            }
            
            // 在主线程中更新状态
            client.execute(() -> {
                PlayerEntity player = MinecraftClient.getInstance().player;
                if (player != null) {
                    ClientFoodData.setEatenFoods(eatenFoods);
                }
            });
        });
    }
}
