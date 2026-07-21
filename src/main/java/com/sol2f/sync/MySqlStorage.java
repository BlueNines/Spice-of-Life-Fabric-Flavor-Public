package com.sol2f.sync;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * 封装本模组使用的同步 JDBC 操作。
 *
 * 所有公开方法都必须由专用数据库执行器调用。
 */
public final class MySqlStorage implements AutoCloseable {
    private static final String CREATE_STATE_TABLE = """
            CREATE TABLE IF NOT EXISTS sol2f_player_state (
                sync_group VARCHAR(64) CHARACTER SET ascii NOT NULL,
                player_uuid CHAR(36) CHARACTER SET ascii NOT NULL,
                generation BIGINT UNSIGNED NOT NULL DEFAULT 0,
                last_player_name VARCHAR(64) NOT NULL DEFAULT '',
                updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                    ON UPDATE CURRENT_TIMESTAMP(3),
                PRIMARY KEY (sync_group, player_uuid)
            ) ENGINE=InnoDB
            """;
    private static final String CREATE_FOOD_TABLE = """
            CREATE TABLE IF NOT EXISTS sol2f_consumed_food (
                sync_group VARCHAR(64) CHARACTER SET ascii NOT NULL,
                player_uuid CHAR(36) CHARACTER SET ascii NOT NULL,
                generation BIGINT UNSIGNED NOT NULL,
                food_id VARCHAR(191) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
                hunger_points INT UNSIGNED NULL,
                saturation_modifier DOUBLE NULL,
                source_server VARCHAR(64) CHARACTER SET ascii NOT NULL DEFAULT '',
                first_eaten_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
                PRIMARY KEY (sync_group, player_uuid, generation, food_id)
            ) ENGINE=InnoDB
            """;

    private final DatabaseSettings settings;
    private final HikariDataSource dataSource;

    /**
     * 创建连接池，但不在调用线程执行玩家查询。
     */
    public MySqlStorage(DatabaseSettings settings) {
        this.settings = settings;

        HikariConfig hikari = new HikariConfig();
        hikari.setPoolName("sol2f-" + settings.serverId());
        hikari.setJdbcUrl(settings.jdbcUrl());
        hikari.setUsername(settings.username());
        hikari.setPassword(settings.password());
        hikari.setMaximumPoolSize(settings.maximumPoolSize());
        hikari.setMinimumIdle(settings.minimumIdle());
        hikari.setConnectionTimeout(settings.connectionTimeoutMs());
        hikari.setValidationTimeout(Math.min(settings.connectionTimeoutMs(), 5000L));
        hikari.setInitializationFailTimeout(-1L);
        hikari.setAutoCommit(true);
        hikari.addDataSourceProperty("cachePrepStmts", "true");
        hikari.addDataSourceProperty("prepStmtCacheSize", "128");
        hikari.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikari.addDataSourceProperty("useServerPrepStmts", "true");
        this.dataSource = new HikariDataSource(hikari);
    }

    /**
     * 创建同步业务表并验证连接。
     */
    public void initializeSchema() throws SQLException {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(settings.queryTimeoutSeconds());
            statement.executeUpdate(CREATE_STATE_TABLE);
            statement.executeUpdate(CREATE_FOOD_TABLE);
            ensureFoodValueColumns(connection);
        }
    }

    /**
     * 读取玩家当前 generation 和食物集合。
     */
    public PlayerFoodSnapshot loadPlayer(UUID playerUuid, String playerName) throws SQLException {
        String uuid = playerUuid.toString();
        try (Connection connection = dataSource.getConnection()) {
            connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            connection.setAutoCommit(false);
            try {
                boolean created = insertPlayerState(connection, uuid, playerName);
                updatePlayerName(connection, uuid, playerName);
                long generation = selectGeneration(connection, uuid);
                Map<String, FoodValue> foods = selectFoods(connection, uuid, generation);
                connection.commit();
                return new PlayerFoodSnapshot(created, generation, foods);
            } catch (SQLException exception) {
                rollbackQuietly(connection);
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    /**
     * 幂等写入玩家当前 generation 的新食物。
     */
    public void insertFoodValues(UUID playerUuid, long generation, Collection<FoodValue> foods) throws SQLException {
        if (foods.isEmpty()) {
            return;
        }

        String sql = "INSERT INTO sol2f_consumed_food "
                + "(sync_group, player_uuid, generation, food_id, hunger_points, saturation_modifier, source_server) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE "
                + "hunger_points = COALESCE(hunger_points, VALUES(hunger_points)), "
                + "saturation_modifier = COALESCE(saturation_modifier, VALUES(saturation_modifier))";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setQueryTimeout(settings.queryTimeoutSeconds());
            int count = 0;
            for (FoodValue food : foods) {
                if (count >= com.sol2f.network.NetworkChannels.MAX_FOOD_ENTRIES) {
                    break;
                }
                if (food == null || food.foodId().length() > com.sol2f.network.NetworkChannels.MAX_FOOD_ID_LENGTH) {
                    continue;
                }
                statement.setString(1, settings.syncGroup());
                statement.setString(2, playerUuid.toString());
                statement.setLong(3, generation);
                statement.setString(4, food.foodId());
                if (food.known()) {
                    statement.setInt(5, food.hungerPoints());
                    statement.setDouble(6, food.saturationModifier());
                } else {
                    statement.setNull(5, Types.INTEGER);
                    statement.setNull(6, Types.DOUBLE);
                }
                statement.setString(7, settings.serverId());
                statement.addBatch();
                count++;
            }
            statement.executeBatch();
        }
    }

    /**
     * 在事务中递增玩家 generation，实现不会被旧写入复活的清空。
     */
    public long advanceGeneration(UUID playerUuid, String playerName) throws SQLException {
        String uuid = playerUuid.toString();
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                insertPlayerState(connection, uuid, playerName);
                String sql = "UPDATE sol2f_player_state SET generation = generation + 1, last_player_name = ? "
                        + "WHERE sync_group = ? AND player_uuid = ?";
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setQueryTimeout(settings.queryTimeoutSeconds());
                    statement.setString(1, playerName);
                    statement.setString(2, settings.syncGroup());
                    statement.setString(3, uuid);
                    statement.executeUpdate();
                }
                long generation = selectGeneration(connection, uuid);
                connection.commit();
                return generation;
            } catch (SQLException exception) {
                rollbackQuietly(connection);
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    /**
     * 关闭连接池及其后台线程。
     */
    @Override
    public void close() {
        dataSource.close();
    }

    /**
     * 缺失时创建玩家状态行。
     */
    private boolean insertPlayerState(Connection connection, String uuid, String playerName) throws SQLException {
        String sql = "INSERT IGNORE INTO sol2f_player_state "
                + "(sync_group, player_uuid, generation, last_player_name) VALUES (?, ?, 0, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setQueryTimeout(settings.queryTimeoutSeconds());
            statement.setString(1, settings.syncGroup());
            statement.setString(2, uuid);
            statement.setString(3, playerName);
            return statement.executeUpdate() == 1;
        }
    }

    /**
     * 更新玩家最后使用的名称。
     */
    private void updatePlayerName(Connection connection, String uuid, String playerName) throws SQLException {
        String sql = "UPDATE sol2f_player_state SET last_player_name = ? WHERE sync_group = ? AND player_uuid = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setQueryTimeout(settings.queryTimeoutSeconds());
            statement.setString(1, playerName);
            statement.setString(2, settings.syncGroup());
            statement.setString(3, uuid);
            statement.executeUpdate();
        }
    }

    /**
     * 查询玩家当前 generation。
     */
    private long selectGeneration(Connection connection, String uuid) throws SQLException {
        String sql = "SELECT generation FROM sol2f_player_state WHERE sync_group = ? AND player_uuid = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setQueryTimeout(settings.queryTimeoutSeconds());
            statement.setString(1, settings.syncGroup());
            statement.setString(2, uuid);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException("Missing player state after insert: " + uuid);
                }
                return result.getLong(1);
            }
        }
    }

    /**
     * 查询指定 generation 的食物集合。
     */
    private Map<String, FoodValue> selectFoods(Connection connection, String uuid, long generation) throws SQLException {
        String sql = "SELECT food_id, hunger_points, saturation_modifier FROM sol2f_consumed_food "
                + "WHERE sync_group = ? AND player_uuid = ? AND generation = ? "
                + "LIMIT " + com.sol2f.network.NetworkChannels.MAX_FOOD_ENTRIES;
        Map<String, FoodValue> foods = new HashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setQueryTimeout(settings.queryTimeoutSeconds());
            statement.setString(1, settings.syncGroup());
            statement.setString(2, uuid);
            statement.setLong(3, generation);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    String foodId = result.getString(1);
                    Number hungerPoints = (Number) result.getObject(2);
                    Number saturationModifier = (Number) result.getObject(3);
                    FoodValue food = hungerPoints == null || saturationModifier == null
                            ? FoodValue.unknown(foodId)
                            : new FoodValue(foodId, hungerPoints.intValue(), saturationModifier.doubleValue());
                    foods.put(foodId, food);
                }
            }
        }
        return foods;
    }

    /**
     * 为旧版已存在的食物表补充数值列，避免要求服主手工迁移。
     */
    private void ensureFoodValueColumns(Connection connection) throws SQLException {
        Set<String> columns = new HashSet<>();
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet result = metadata.getColumns(connection.getCatalog(), null, "sol2f_consumed_food", null)) {
            while (result.next()) {
                columns.add(result.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));
            }
        }
        try (Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(settings.queryTimeoutSeconds());
            if (!columns.contains("hunger_points")) {
                statement.executeUpdate("ALTER TABLE sol2f_consumed_food ADD COLUMN hunger_points INT UNSIGNED NULL AFTER food_id");
            }
            if (!columns.contains("saturation_modifier")) {
                statement.executeUpdate("ALTER TABLE sol2f_consumed_food ADD COLUMN saturation_modifier DOUBLE NULL AFTER hunger_points");
            }
        }
    }

    /**
     * 尽力回滚失败事务，不覆盖原始异常。
     */
    private void rollbackQuietly(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
            // 原始数据库异常更重要。
        }
    }
}
