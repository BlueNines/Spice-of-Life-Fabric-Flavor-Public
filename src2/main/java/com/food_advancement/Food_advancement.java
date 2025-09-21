package com.food_advancement;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.food_advancement.commands.FoodAdvancementCommands;
import com.food_advancement.network.NetworkHandler;
import com.food_advancement.PlayerStateEvents;

public class Food_advancement implements ModInitializer {
    public static final String MOD_ID = "food_advancement";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Food Advancement Mod");

        // 加载配置
        ModConfig.loadConfig();

        // 注册玩家状态事件
        PlayerStateEvents.register();

        // 注册命令
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            FoodAdvancementCommands.register(dispatcher, registryAccess, environment);
        });

        // 初始化服务端网络处理
        NetworkHandler.initializeServerNetworking();
    }
}
