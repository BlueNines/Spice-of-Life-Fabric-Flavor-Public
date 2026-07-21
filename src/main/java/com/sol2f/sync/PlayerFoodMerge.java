package com.sol2f.sync;

import java.util.HashSet;
import java.util.Set;

import com.sol2f.network.NetworkChannels;

/**
 * 提供不依赖 Minecraft 对象的玩家食物集合合并规则。
 */
public final class PlayerFoodMerge {
    /**
     * 工具类不允许实例化。
     */
    private PlayerFoodMerge() {
    }

    /**
     * 合并数据库快照、旧 NBT 和当前登录期间的待写数据。
     */
    public static MergeResult merge(
            Set<String> databaseFoods,
            long databaseGeneration,
            boolean firstSuccessfulLoad,
            Set<String> loginFoods,
            long loginGeneration,
            boolean loginDirty,
            Set<String> pendingFoods) {
        Set<String> merged = new HashSet<>();
        addBounded(merged, databaseFoods);
        boolean importLegacy = firstSuccessfulLoad && loginGeneration < 0L && databaseGeneration == 0L;
        boolean restoreDirty = loginDirty && loginGeneration == databaseGeneration;
        if (importLegacy || restoreDirty) {
            addBounded(merged, loginFoods);
        }
        addBounded(merged, pendingFoods);

        Set<String> missingFromDatabase = new HashSet<>(merged);
        missingFromDatabase.removeAll(databaseFoods);
        return new MergeResult(merged, missingFromDatabase);
    }

    /**
     * 按统一上限向目标集合追加食物 ID，避免迁移或异常数据无限扩张。
     */
    private static void addBounded(Set<String> target, Set<String> source) {
        for (String food : source) {
            if (target.size() >= NetworkChannels.MAX_FOOD_ENTRIES) {
                return;
            }
            if (food != null && !food.isBlank() && food.length() <= NetworkChannels.MAX_FOOD_ID_LENGTH) {
                target.add(food);
            }
        }
    }

    /**
     * 保存合并后的在线集合和需要补写到数据库的集合。
     */
    public record MergeResult(Set<String> foods, Set<String> missingFromDatabase) {
        /**
         * 复制结果集合，防止测试或异步调用方修改内部状态。
         */
        public MergeResult {
            foods = Set.copyOf(foods);
            missingFromDatabase = Set.copyOf(missingFromDatabase);
        }
    }
}
