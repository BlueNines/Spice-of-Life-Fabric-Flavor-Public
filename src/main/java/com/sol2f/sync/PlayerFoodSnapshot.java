package com.sol2f.sync;

import java.util.Map;
import java.util.Set;

/**
 * 保存一次数据库读取返回的不可变玩家食物快照。
 */
public record PlayerFoodSnapshot(boolean created, long generation, Map<String, FoodValue> foodValues) {
    /**
     * 复制集合，避免数据库线程结果在其他线程被修改。
     */
    public PlayerFoodSnapshot {
        foodValues = Map.copyOf(foodValues);
    }

    /**
     * 返回快照中的食物 ID 集合，供现有集合合并规则使用。
     */
    public Set<String> foods() {
        return foodValues.keySet();
    }
}
