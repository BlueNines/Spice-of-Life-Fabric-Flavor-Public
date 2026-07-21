package com.sol2f.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
