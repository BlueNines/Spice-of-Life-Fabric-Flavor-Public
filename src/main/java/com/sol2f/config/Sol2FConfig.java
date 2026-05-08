package com.sol2f.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;

import java.util.List;
import java.util.Arrays;

@Config(name = "spice-of-life-fabric-flavor")
public class Sol2FConfig implements ConfigData {
    public HealthSetting health = new HealthSetting();
    public HungerSetting hunger = new HungerSetting();
    public DeveloperSetting dev = new DeveloperSetting();

    public static class HealthSetting {
        public int maxHealth = 5000;

        public int healthGain = 2;

        public boolean resetOnDeath = false;

        public boolean healthToMaxOnIncrease = false;
        public int healthIncreaseOnIncrease = 0;

        public String Expression = "0";

        public List<String> BlackList = Arrays.asList(
            "minecraft:rotten_flesh",
            "minecraft:spider_eye"
        );
    }

    public static class HungerSetting {
        public boolean EnableNaturalHunger = false;
        public int NaturalHungerPeriod = 12000; // ticks
        public String NaturalHungerExpression = "1";

        public boolean EnableSleepHunger = false;
        public String SleepHungerExpression = "SleepDuration / 3000";

        public int MinFoodLevel = 1;
    }

    public static class DeveloperSetting {
        public boolean DeveloperMode = false;
    }
}