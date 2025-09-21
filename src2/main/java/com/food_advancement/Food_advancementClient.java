package com.food_advancement;

import com.food_advancement.network.ClientNetworkHandler;
import net.fabricmc.api.ClientModInitializer;

public class Food_advancementClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // 初始化客户端网络处理
        ClientNetworkHandler.initializeClientNetworking();
    }
}
