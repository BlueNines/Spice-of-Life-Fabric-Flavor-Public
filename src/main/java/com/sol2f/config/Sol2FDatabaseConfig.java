package com.sol2f.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;

/**
 * 保存专用服务器的 MySQL 同步配置。
 */
@Config(name = "spice-of-life-fabric-flavor-database")
public class Sol2FDatabaseConfig implements ConfigData {
    public boolean enabled = false;
    public String jdbcUrl = "jdbc:mysql://127.0.0.1:3306/minecraft?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai";
    public String username = "sol2f";
    public String password = "change-me";
    public String syncGroup = "main-network";
    public String serverId = "replace-me";
    public int maximumPoolSize = 4;
    public int minimumIdle = 1;
    public long connectionTimeoutMs = 3000L;
    public int queryTimeoutSeconds = 5;
    public int executorThreads = 2;
    public int executorQueueSize = 1024;
    public int loginReconcileDelaySeconds = 5;
    public int retryDelaySeconds = 5;
    public int shutdownWaitSeconds = 5;
}
