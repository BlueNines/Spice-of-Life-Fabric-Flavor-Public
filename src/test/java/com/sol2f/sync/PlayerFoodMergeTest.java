package com.sol2f.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * 验证多子服食物集合的 generation 合并规则。
 */
class PlayerFoodMergeTest {
    /**
     * 首次 generation 0 加载应导入旧版 NBT。
     */
    @Test
    void importsLegacyNbtOnlyForInitialGeneration() {
        PlayerFoodMerge.MergeResult result = PlayerFoodMerge.merge(
                Set.of("minecraft:apple"),
                0L,
                true,
                Set.of("minecraft:bread"),
                -1L,
                false,
                Set.of());

        assertEquals(Set.of("minecraft:apple", "minecraft:bread"), result.foods());
        assertEquals(Set.of("minecraft:bread"), result.missingFromDatabase());
    }

    /**
     * generation 已递增时必须忽略没有 generation 的旧 NBT。
     */
    @Test
    void ignoresLegacyNbtAfterClearGeneration() {
        PlayerFoodMerge.MergeResult result = PlayerFoodMerge.merge(
                Set.of(),
                1L,
                true,
                Set.of("minecraft:bread"),
                -1L,
                false,
                Set.of());

        assertEquals(Set.of(), result.foods());
        assertEquals(Set.of(), result.missingFromDatabase());
    }

    /**
     * 同 generation 的本地 dirty 数据应恢复并补写数据库。
     */
    @Test
    void restoresDirtyDataForMatchingGeneration() {
        PlayerFoodMerge.MergeResult result = PlayerFoodMerge.merge(
                Set.of("minecraft:apple"),
                3L,
                false,
                Set.of("minecraft:apple", "minecraft:carrot"),
                3L,
                true,
                Set.of());

        assertEquals(Set.of("minecraft:apple", "minecraft:carrot"), result.foods());
        assertEquals(Set.of("minecraft:carrot"), result.missingFromDatabase());
    }

    /**
     * 旧 generation 的 dirty 数据不能覆盖远端清空。
     */
    @Test
    void rejectsDirtyDataFromStaleGeneration() {
        PlayerFoodMerge.MergeResult result = PlayerFoodMerge.merge(
                Set.of(),
                4L,
                false,
                Set.of("minecraft:carrot"),
                3L,
                true,
                Set.of());

        assertEquals(Set.of(), result.foods());
    }

    /**
     * 本次登录期间新增的食物在 generation 变化后仍应写入新 generation。
     */
    @Test
    void preservesCurrentSessionPendingFoodAcrossGenerationChange() {
        PlayerFoodMerge.MergeResult result = PlayerFoodMerge.merge(
                Set.of(),
                5L,
                false,
                Set.of("minecraft:bread"),
                4L,
                true,
                Set.of("minecraft:apple"));

        assertEquals(Set.of("minecraft:apple"), result.foods());
        assertEquals(Set.of("minecraft:apple"), result.missingFromDatabase());
    }

    /**
     * 异常迁移数据和集合并集必须受统一数量上限保护。
     */
    @Test
    void boundsMergedFoodCount() {
        Set<String> databaseFoods = new LinkedHashSet<>();
        for (int index = 0; index < com.sol2f.network.NetworkChannels.MAX_FOOD_ENTRIES; index++) {
            databaseFoods.add("test:food_" + index);
        }

        PlayerFoodMerge.MergeResult result = PlayerFoodMerge.merge(
                databaseFoods,
                0L,
                true,
                Set.of("test:legacy_extra"),
                -1L,
                false,
                Set.of("test:pending_extra"));

        assertEquals(com.sol2f.network.NetworkChannels.MAX_FOOD_ENTRIES, result.foods().size());
        assertEquals(Set.of(), result.missingFromDatabase());
    }

    /**
     * 空白和超长食物 ID 不应进入在线合并结果。
     */
    @Test
    void rejectsInvalidFoodIdsDuringMerge() {
        String oversized = "x".repeat(com.sol2f.network.NetworkChannels.MAX_FOOD_ID_LENGTH + 1);

        PlayerFoodMerge.MergeResult result = PlayerFoodMerge.merge(
                Set.of("minecraft:apple", " ", oversized),
                0L,
                true,
                Set.of(),
                -1L,
                false,
                Set.of());

        assertEquals(Set.of("minecraft:apple"), result.foods());
    }
}
