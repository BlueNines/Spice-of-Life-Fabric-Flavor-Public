package com.sol2f.sync;

/**
 * 保存食物在首次发现时的原始饥饿值和饱和度系数。
 */
public record FoodValue(String foodId, int hungerPoints, double saturationModifier, boolean known) {
    /**
     * 创建一条已经从 FoodComponent 读取到数值的记录。
     */
    public FoodValue(String foodId, int hungerPoints, double saturationModifier) {
        this(foodId, hungerPoints, saturationModifier, true);
    }

    /**
     * 创建并校验一条可持久化的食物数值记录。
     */
    public FoodValue {
        if (foodId == null || foodId.isBlank()) {
            throw new IllegalArgumentException("foodId must not be blank");
        }
        if (known && hungerPoints < 0) {
            throw new IllegalArgumentException("hungerPoints must not be negative");
        }
        if (known && (!Double.isFinite(saturationModifier) || saturationModifier < 0.0D)) {
            throw new IllegalArgumentException("saturationModifier must be finite and non-negative");
        }
        if (known) {
            saturationModifier = Math.round(saturationModifier * 1_000_000.0D) / 1_000_000.0D;
        } else {
            hungerPoints = 0;
            saturationModifier = 0.0D;
        }
    }

    /**
     * 创建仅知道食物 ID 的旧数据占位记录。
     */
    public static FoodValue unknown(String foodId) {
        return new FoodValue(foodId, 0, 0.0D, false);
    }

    /**
     * 返回食物理论增加的饱和度点数，不考虑玩家当前饥饿值和上限裁剪。
     */
    public double saturationPoints() {
        return known ? hungerPoints * saturationModifier * 2.0D : 0.0D;
    }
}
