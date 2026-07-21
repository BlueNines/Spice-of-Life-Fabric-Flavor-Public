package com.sol2f.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 使用外部真实 MySQL 验证表结构、幂等写入和 generation 清空。
 */
class MySqlStorageIntegrationTest {
    private String jdbcUrl;
    private String username;
    private String password;

    /**
     * 读取集成测试连接并清理上一次测试表。
     */
    @BeforeEach
    void prepareDatabase() throws Exception {
        jdbcUrl = System.getenv("SOL2F_TEST_MYSQL_URL");
        username = System.getenv("SOL2F_TEST_MYSQL_USER");
        password = System.getenv("SOL2F_TEST_MYSQL_PASSWORD");
        Assumptions.assumeTrue(jdbcUrl != null && username != null && password != null,
                "Real MySQL integration environment is not configured");
        dropTables();
    }

    /**
     * 测试结束后移除隔离测试表。
     */
    @AfterEach
    void cleanDatabase() throws Exception {
        if (jdbcUrl != null && username != null && password != null) {
            dropTables();
        }
    }

    /**
     * 验证首次创建、重复写入、generation 清空和旧写入隔离。
     */
    @Test
    void persistsFoodsAndPreventsStaleGenerationResurrection() throws Exception {
        DatabaseSettings settings = createSettings(4);
        UUID playerUuid = UUID.randomUUID();

        try (MySqlStorage storage = new MySqlStorage(settings)) {
            storage.initializeSchema();
            PlayerFoodSnapshot first = storage.loadPlayer(playerUuid, "Tester");
            assertTrue(first.created());
            assertEquals(0L, first.generation());
            assertEquals(Set.of(), first.foods());

            storage.insertFoodValues(playerUuid, 0L, Set.of(
                    new FoodValue("minecraft:apple", 4, 0.3D),
                    new FoodValue("minecraft:bread", 5, 0.6D)));
            storage.insertFoodValues(playerUuid, 0L, Set.of(new FoodValue("minecraft:apple", 4, 0.3D)));
            PlayerFoodSnapshot stored = storage.loadPlayer(playerUuid, "Tester");
            assertFalse(stored.created());
            assertEquals(Set.of("minecraft:apple", "minecraft:bread"), stored.foods());
            assertEquals(4, stored.foodValues().get("minecraft:apple").hungerPoints());
            assertEquals(0.3D, stored.foodValues().get("minecraft:apple").saturationModifier(), 0.0001D);

            long generation = storage.advanceGeneration(playerUuid, "Tester");
            assertEquals(1L, generation);
            storage.insertFoodValues(playerUuid, 0L, Set.of(new FoodValue("minecraft:carrot", 3, 0.6D)));
            assertEquals(Set.of(), storage.loadPlayer(playerUuid, "Tester").foods());

            storage.insertFoodValues(playerUuid, generation, Set.of(new FoodValue("minecraft:potato", 1, 0.6D)));
            assertEquals(Set.of("minecraft:potato"), storage.loadPlayer(playerUuid, "Tester").foods());
        }
    }

    /**
     * 验证多个数据库线程并发幂等写入时不会丢失集合并集。
     */
    @Test
    void mergesConcurrentIdempotentInserts() throws Exception {
        DatabaseSettings settings = createSettings(4);
        UUID playerUuid = UUID.randomUUID();
        ExecutorService workers = Executors.newFixedThreadPool(4);

        try (MySqlStorage storage = new MySqlStorage(settings)) {
            storage.initializeSchema();
            storage.loadPlayer(playerUuid, "ConcurrentTester");
            List<Future<?>> futures = new ArrayList<>();
            for (int index = 0; index < 20; index++) {
                String foodId = "test:food_" + index;
                int hungerPoints = index + 1;
                futures.add(workers.submit(() -> {
                    storage.insertFoodValues(playerUuid, 0L, Set.of(
                            new FoodValue(foodId, hungerPoints, 0.5D),
                            new FoodValue("test:shared", 1, 0.5D)));
                    return null;
                }));
            }
            for (Future<?> future : futures) {
                future.get();
            }

            PlayerFoodSnapshot snapshot = storage.loadPlayer(playerUuid, "ConcurrentTester");
            assertEquals(21, snapshot.foods().size());
            assertTrue(snapshot.foods().contains("test:shared"));
        } finally {
            workers.shutdownNow();
        }
    }

    /**
     * 验证已有旧表会自动补充食物数值列并可继续写入。
     */
    @Test
    void migratesLegacyFoodTableColumns() throws Exception {
        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
                Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE sol2f_consumed_food (
                        sync_group VARCHAR(64) CHARACTER SET ascii NOT NULL,
                        player_uuid CHAR(36) CHARACTER SET ascii NOT NULL,
                        generation BIGINT UNSIGNED NOT NULL,
                        food_id VARCHAR(191) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
                        source_server VARCHAR(64) CHARACTER SET ascii NOT NULL DEFAULT '',
                        first_eaten_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
                        PRIMARY KEY (sync_group, player_uuid, generation, food_id)
                    ) ENGINE=InnoDB
                    """);
        }

        UUID playerUuid = UUID.randomUUID();
        try (MySqlStorage storage = new MySqlStorage(createSettings(2))) {
            storage.initializeSchema();
            storage.loadPlayer(playerUuid, "LegacyTester");
            storage.insertFoodValues(playerUuid, 0L,
                    Set.of(new FoodValue("minecraft:cooked_beef", 8, 0.8D)));

            FoodValue stored = storage.loadPlayer(playerUuid, "LegacyTester")
                    .foodValues()
                    .get("minecraft:cooked_beef");
            assertEquals(8, stored.hungerPoints());
            assertEquals(0.8D, stored.saturationModifier(), 0.0001D);
        }
    }

    /**
     * 创建当前测试使用的数据库参数。
     */
    private DatabaseSettings createSettings(int poolSize) {
        return new DatabaseSettings(
                true,
                jdbcUrl,
                username,
                password,
                "integration-test",
                "test-server",
                poolSize,
                1,
                3000L,
                5,
                2,
                128,
                5,
                2,
                5);
    }

    /**
     * 删除集成测试创建的固定业务表。
     */
    private void dropTables() throws Exception {
        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
                Statement statement = connection.createStatement()) {
            statement.executeUpdate("DROP TABLE IF EXISTS sol2f_consumed_food");
            statement.executeUpdate("DROP TABLE IF EXISTS sol2f_player_state");
        }
    }
}
