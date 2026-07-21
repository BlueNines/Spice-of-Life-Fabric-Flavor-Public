package com.sol2f.sync;

import java.util.regex.Pattern;

import com.sol2f.config.Sol2FDatabaseConfig;

/**
 * 保存经过校验的数据库运行参数，避免运行期反复读取可变配置。
 */
public record DatabaseSettings(
        boolean enabled,
        String jdbcUrl,
        String username,
        String password,
        String syncGroup,
        String serverId,
        int maximumPoolSize,
        int minimumIdle,
        long connectionTimeoutMs,
        int queryTimeoutSeconds,
        int executorThreads,
        int executorQueueSize,
        int loginReconcileDelaySeconds,
        int retryDelaySeconds,
        int shutdownWaitSeconds) {
    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    /**
     * 从磁盘配置创建并校验不可变运行参数。
     *
     * @param config 磁盘配置
     * @return 已校验的运行参数
     */
    public static DatabaseSettings from(Sol2FDatabaseConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("Database config is missing");
        }
        if (!config.jdbcUrl.startsWith("jdbc:mysql://")) {
            throw new IllegalArgumentException("jdbcUrl must start with jdbc:mysql://");
        }
        validateId("syncGroup", config.syncGroup);
        validateId("serverId", config.serverId);
        if (config.enabled && "replace-me".equals(config.serverId)) {
            throw new IllegalArgumentException("serverId must be unique when MySQL synchronization is enabled");
        }
        if (config.enabled && (config.username == null || config.username.isBlank())) {
            throw new IllegalArgumentException("username must not be blank when MySQL synchronization is enabled");
        }

        int maximumPoolSize = clamp(config.maximumPoolSize, 1, 16);
        int minimumIdle = clamp(config.minimumIdle, 0, maximumPoolSize);
        return new DatabaseSettings(
                config.enabled,
                config.jdbcUrl,
                config.username,
                config.password,
                config.syncGroup,
                config.serverId,
                maximumPoolSize,
                minimumIdle,
                clamp(config.connectionTimeoutMs, 1000L, 30000L),
                clamp(config.queryTimeoutSeconds, 1, 30),
                clamp(config.executorThreads, 1, 8),
                clamp(config.executorQueueSize, 64, 16384),
                clamp(config.loginReconcileDelaySeconds, 1, 30),
                clamp(config.retryDelaySeconds, 1, 300),
                clamp(config.shutdownWaitSeconds, 1, 30));
    }

    /**
     * 返回不包含数据库密码的诊断文本。
     */
    @Override
    public String toString() {
        return "DatabaseSettings[enabled=" + enabled + ", syncGroup=" + syncGroup + ", serverId=" + serverId
                + ", maximumPoolSize=" + maximumPoolSize + ", executorThreads=" + executorThreads + "]";
    }

    /**
     * 校验数据库分组或子服 ID。
     */
    private static void validateId(String field, String value) {
        if (value == null || !SAFE_ID.matcher(value).matches()) {
            throw new IllegalArgumentException(field + " must match " + SAFE_ID.pattern());
        }
    }

    /**
     * 将整数限制到安全范围。
     */
    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    /**
     * 将长整数限制到安全范围。
     */
    private static long clamp(long value, long minimum, long maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
