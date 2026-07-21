package com.sol2f.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.sol2f.config.Sol2FDatabaseConfig;

/**
 * 验证数据库配置的边界和敏感信息保护。
 */
class DatabaseSettingsTest {
    /**
     * 超出范围的性能参数应被限制到安全值。
     */
    @Test
    void clampsExecutorAndPoolLimits() {
        Sol2FDatabaseConfig config = new Sol2FDatabaseConfig();
        config.maximumPoolSize = 100;
        config.minimumIdle = 99;
        config.executorThreads = 0;
        config.executorQueueSize = 1;

        DatabaseSettings settings = DatabaseSettings.from(config);

        assertEquals(16, settings.maximumPoolSize());
        assertEquals(16, settings.minimumIdle());
        assertEquals(1, settings.executorThreads());
        assertEquals(64, settings.executorQueueSize());
    }

    /**
     * 启用数据库时必须修改默认 serverId。
     */
    @Test
    void rejectsPlaceholderServerIdWhenEnabled() {
        Sol2FDatabaseConfig config = new Sol2FDatabaseConfig();
        config.enabled = true;

        assertThrows(IllegalArgumentException.class, () -> DatabaseSettings.from(config));
    }

    /**
     * 诊断文本不能包含数据库密码。
     */
    @Test
    void masksPasswordInDiagnosticText() {
        Sol2FDatabaseConfig config = new Sol2FDatabaseConfig();
        config.password = "secret-password";

        DatabaseSettings settings = DatabaseSettings.from(config);

        org.junit.jupiter.api.Assertions.assertFalse(settings.toString().contains(config.password));
    }
}
