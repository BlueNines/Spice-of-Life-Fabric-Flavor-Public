package com.sol2f.sync;

import java.util.HashSet;
import java.util.Set;

/**
 * 提供不依赖 Minecraft 对象的玩家食物集合合并规则。
 */
public final class PlayerFoodMerge {
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
        Set<String> merged = new HashSet<>(databaseFoods);
        boolean importLegacy = firstSuccessfulLoad && loginGeneration < 0L && databaseGeneration == 0L;
        boolean restoreDirty = loginDirty && loginGeneration == databaseGeneration;
        if (importLegacy || restoreDirty) {
            merged.addAll(loginFoods);
        }
        merged.addAll(pendingFoods);

        Set<String> missingFromDatabase = new HashSet<>(merged);
        missingFromDatabase.removeAll(databaseFoods);
        return new MergeResult(merged, missingFromDatabase);
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
