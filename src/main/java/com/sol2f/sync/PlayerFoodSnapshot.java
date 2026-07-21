package com.sol2f.sync;

import java.util.Set;

/**
 * 保存一次数据库读取返回的不可变玩家食物快照。
 */
public record PlayerFoodSnapshot(boolean created, long generation, Set<String> foods) {
    /**
     * 复制集合，避免数据库线程结果在其他线程被修改。
     */
    public PlayerFoodSnapshot {
        foods = Set.copyOf(foods);
    }
}
