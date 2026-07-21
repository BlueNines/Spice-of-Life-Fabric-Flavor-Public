package com.sol2f.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * 验证新生成服务端配置采用按食物饥饿值计算生命增益的默认值。
 */
class Sol2FConfigDefaultsTest {
    /**
     * 默认配置应关闭固定增益并启用 totalHunger 公式。
     */
    @Test
    void usesHungerWeightedHealthDefaults() {
        Sol2FConfig config = new Sol2FConfig();

        assertEquals(0, config.health.healthGain);
        assertEquals("totalHunger * 0.1", config.health.Expression);
    }
}
