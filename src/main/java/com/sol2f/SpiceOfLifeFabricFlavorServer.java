package com.sol2f;

import com.sol2f.config.Sol2FDatabaseConfig;
import com.sol2f.sync.PlayerDataSyncService;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

/**
 * 初始化仅在专用服务器运行的 MySQL 同步服务。
 */
public final class SpiceOfLifeFabricFlavorServer implements DedicatedServerModInitializer {
    /**
     * 注册数据库配置和服务器生命周期事件。
     */
    @Override
    public void onInitializeServer() {
        AutoConfig.register(Sol2FDatabaseConfig.class, GsonConfigSerializer::new);
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            Sol2FDatabaseConfig config = AutoConfig.getConfigHolder(Sol2FDatabaseConfig.class).getConfig();
            PlayerDataSyncService.start(server, config);
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> PlayerDataSyncService.stop());
    }
}
