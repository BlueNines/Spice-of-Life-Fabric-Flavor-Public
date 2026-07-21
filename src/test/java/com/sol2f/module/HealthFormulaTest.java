package com.sol2f.module;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.sol2f.sync.FoodValue;

/**
 * 验证食物饥饿值、饱和度和生命值公式变量。
 */
class HealthFormulaTest {
    /**
     * 生牛肉和熟牛肉应按 3 加 8 点饥饿值计算 1.1 点生命增益。
     */
    @Test
    void calculatesHealthFromTotalHunger() {
        double result = HealthModule.evaluateHealthFormula("totalHunger * 0.1", 2, 11.0D, 14.6D);

        assertEquals(1.1D, result, 0.000001D);
    }

    /**
     * 理论饱和度点数应遵循原版 hunger 乘系数乘二的规则。
     */
    @Test
    void calculatesVanillaSaturationPoints() {
        FoodValue rawBeef = new FoodValue("minecraft:beef", 3, (double) 0.3F);
        FoodValue cookedBeef = new FoodValue("minecraft:cooked_beef", 8, (double) 0.8F);

        assertEquals(0.3D, rawBeef.saturationModifier(), 0.000001D);
        assertEquals(0.8D, cookedBeef.saturationModifier(), 0.000001D);
        assertEquals(1.8D, rawBeef.saturationPoints(), 0.000001D);
        assertEquals(12.8D, cookedBeef.saturationPoints(), 0.000001D);
    }
}
