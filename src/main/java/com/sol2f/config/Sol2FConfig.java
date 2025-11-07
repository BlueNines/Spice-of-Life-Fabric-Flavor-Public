package com.sol2f.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "spice-of-life-fabric-flavor")
public class Sol2FConfig implements ConfigData {

    @ConfigEntry.Gui.CollapsibleObject(startExpanded = true)
    public HealthySetting healthy = new HealthySetting();

    @ConfigEntry.Gui.CollapsibleObject(startExpanded = true)
    public FeatureSetting features = new FeatureSetting();

    public static class HealthySetting {
        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 2, max = 240)
        public int maxHealthy = 20;

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 2, max = 100)
        public int defaultHealthy = 10;
    }

    public static class FeatureSetting {
        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 0, max = 30)
        public int healthyGain = 2;

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 0, max = 20)
        public int Increasefrequency = 0;

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 2, max = 30)
        public int frequencyGain = 2;

        @ConfigEntry.Gui.Tooltip
        public boolean resetOnDeath = false;

        @ConfigEntry.Gui.Excluded
        public boolean developerMode = false;

        @ConfigEntry.Gui.Tooltip
        public boolean healToMaxOnIncrease = false;

        @ConfigEntry.Gui.Tooltip// 恢复血量配置
        public int healthIncrease = 0;
    }
}