package com.sol2f.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;

import java.util.List;
import java.util.Arrays;

@Config(name = "spice-of-life-fabric-flavor")
public class Sol2FConfig implements ConfigData {
    public HealthySetting healthy = new HealthySetting();
    public FeatureSetting features = new FeatureSetting();

    public static class HealthySetting {
        public int maxHealthy = 5000;
    }

    public static class FeatureSetting {
        public int healthyGain = 2;
        public int Increasefrequency = 0;
        public int frequencyGain = 2;
        public boolean resetOnDeath = false;
        public boolean developerMode = false;
        public boolean healthToMaxOnIncrease = false;
        public int healthIncreaseOnIncrease = 0;
        public String Expression = "0";
        public List<String> blacklist = Arrays.asList(
            "minecraft:rotten_flesh",
            "minecraft:spider_eye"
        );
    }
}