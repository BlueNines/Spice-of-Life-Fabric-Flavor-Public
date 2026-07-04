package com.sol2f.gui;

import com.sol2f.config.Sol2FConfig;
import com.sol2f.config.SOL2FClientConfig;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class Sol2FConfigGUI {

    public static Screen openConfigScreen(Screen parent) {
        
        System.out.println("Sol2FConfigGUI: openConfigScreen called!");

        Sol2FConfig Config = AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig();
        SOL2FClientConfig ClientConfig = AutoConfig.getConfigHolder(SOL2FClientConfig.class).getConfig();

        System.out.println("Config loaded");

        ConfigBuilder builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(Text.translatable("sol2f.gui.config.title"))
            .setSavingRunnable(() -> {
                AutoConfig.getConfigHolder(Sol2FConfig.class).save();
                AutoConfig.getConfigHolder(SOL2FClientConfig.class).save();
            });

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        // ===== 血量设置 =====
        ConfigCategory healthCat = builder.getOrCreateCategory(
            Text.translatable("sol2f.gui.config.option.health")
        );

        healthCat.addEntry(
            entryBuilder.startIntField(
                Text.translatable("sol2f.gui.config.option.health.maxHealth"),
                Config.health.maxHealth
            )
            .setTooltip(
                Text.translatable("sol2f.gui.config.option.health.maxHealth.@Tooltip")
            )
            .setMin(2)
            .setMax(5000)
            .setSaveConsumer(newValue -> Config.health.maxHealth = newValue)
            .build()
        );


        healthCat.addEntry(
            entryBuilder.startIntField(
                Text.translatable("sol2f.gui.config.option.health.healthGain"),
                Config.health.healthGain
            )
            .setTooltip(
                Text.translatable("sol2f.gui.config.option.health.healthGain.@Tooltip")
            )
            .setMin(0).setMax(20)
            .setSaveConsumer(v -> Config.health.healthGain = v)
            .build()
        );

        healthCat.addEntry(
            entryBuilder.startBooleanToggle(
                Text.translatable("sol2f.gui.config.option.health.resetOnDeath"),
                Config.health.resetOnDeath
            )
            .setTooltip(
                Text.translatable("sol2f.gui.config.option.health.resetOnDeath.@Tooltip")
            )
            .setSaveConsumer(v -> Config.health.resetOnDeath = v)
            .build()
        );

        healthCat.addEntry(
            entryBuilder.startBooleanToggle(
                Text.translatable("sol2f.gui.config.option.health.healthToMaxOnIncrease"),
                Config.health.healthToMaxOnIncrease
            )
            .setTooltip(
                Text.translatable("sol2f.gui.config.option.health.healthToMaxOnIncrease.@Tooltip")
            )
            .setSaveConsumer(v -> Config.health.healthToMaxOnIncrease = v)
            .build()
        );

        healthCat.addEntry(
            entryBuilder.startIntField(
                Text.translatable("sol2f.gui.config.option.health.healthIncreaseOnIncrease"),
                Config.health.healthIncreaseOnIncrease
            )
            .setTooltip(
                Text.translatable("sol2f.gui.config.option.health.healthIncreaseOnIncrease.@Tooltip")
            )
            .setMin(0).setMax(20)
            .setSaveConsumer(v -> Config.health.healthIncreaseOnIncrease = v)
            .build()
        );

        healthCat.addEntry(
            entryBuilder.startStrField(
                Text.translatable("sol2f.gui.config.option.health.Expression"),
                Config.health.Expression
            )
            .setTooltip(
                Text.translatable("sol2f.gui.config.option.health.Expression.@Tooltip[1]"),
                Text.translatable("sol2f.gui.config.option.health.Expression.@Tooltip[2]"),
                Text.translatable("sol2f.gui.config.option.health.Expression.@Tooltip[3]"),
                Text.translatable("sol2f.gui.config.option.health.Expression.@Tooltip[4]")
            )
            .setSaveConsumer(v -> Config.health.Expression = v)
            .build()
        );

        healthCat.addEntry(
            entryBuilder.startStrList(
                Text.translatable("sol2f.gui.config.option.health.blacklist"),
                Config.health.BlackList
            )
            .setTooltip(
                Text.translatable("sol2f.gui.config.option.health.blacklist.@Tooltip[1]"),
                Text.translatable("sol2f.gui.config.option.health.blacklist.@Tooltip[2]"),
                Text.translatable("sol2f.gui.config.option.health.blacklist.@Tooltip[3]")
            )
            .setSaveConsumer(v -> Config.health.BlackList = v)
            .build()
        );

        healthCat.addEntry(
            entryBuilder.startStrList(
                Text.translatable("sol2f.gui.config.option.health.whitelist"),
                Config.health.WhiteList
            )
            .setTooltip(
                Text.translatable("sol2f.gui.config.option.health.whitelist.@Tooltip[1]"),
                Text.translatable("sol2f.gui.config.option.health.whitelist.@Tooltip[2]"),
                Text.translatable("sol2f.gui.config.option.health.whitelist.@Tooltip[3]")
            )
            .setSaveConsumer(v -> Config.health.WhiteList = v)
            .build()
        );

        // ===== 饥饿设置 =====
        ConfigCategory hungerCat = builder.getOrCreateCategory(
            Text.translatable("sol2f.gui.config.option.hunger")
        );

        hungerCat.addEntry(
            entryBuilder.startBooleanToggle(
                Text.translatable("sol2f.gui.config.option.hunger.EnableNaturalHunger"),
                Config.hunger.EnableNaturalHunger
            )
            .setTooltip(
                Text.translatable("sol2f.gui.config.option.hunger.EnableNaturalHunger.@Tooltip")
            )
            .setSaveConsumer(v -> Config.hunger.EnableNaturalHunger = v)
            .build()
        );

        hungerCat.addEntry(
            entryBuilder.startIntField(
                Text.translatable("sol2f.gui.config.option.hunger.NaturalHungerPeriod"),
                Config.hunger.NaturalHungerPeriod
            )
            .setTooltip(
                Text.translatable("sol2f.gui.config.option.hunger.NaturalHungerPeriod.@Tooltip")
            )
            .setMin(1)
            .setSaveConsumer(v -> Config.hunger.NaturalHungerPeriod = v)
            .build()
        );

        hungerCat.addEntry(
            entryBuilder.startStrField(
                Text.translatable("sol2f.gui.config.option.hunger.NaturalHungerExpression"),
                Config.hunger.NaturalHungerExpression
            )
            .setTooltip(
                Text.translatable("sol2f.gui.config.option.hunger.NaturalHungerExpression.@Tooltip[1]"),
                Text.translatable("sol2f.gui.config.option.hunger.NaturalHungerExpression.@Tooltip[2]")
            )
            .setSaveConsumer(v -> Config.hunger.NaturalHungerExpression = v)
            .build()
        );

        hungerCat.addEntry(
            entryBuilder.startBooleanToggle(
                Text.translatable("sol2f.gui.config.option.hunger.EnableSleepHunger"),
                Config.hunger.EnableSleepHunger
            )
            .setTooltip(
                Text.translatable("sol2f.gui.config.option.hunger.EnableSleepHunger.@Tooltip")
            )
            .setSaveConsumer(v -> Config.hunger.EnableSleepHunger = v)
            .build()
        );

        hungerCat.addEntry(
            entryBuilder.startStrField(
                Text.translatable("sol2f.gui.config.option.hunger.SleepHungerExpression"),
                Config.hunger.SleepHungerExpression
            )
            .setTooltip(
                Text.translatable("sol2f.gui.config.option.hunger.SleepHungerExpression.@Tooltip[1]"),
                Text.translatable("sol2f.gui.config.option.hunger.SleepHungerExpression.@Tooltip[2]")
            )
            .setSaveConsumer(v -> Config.hunger.SleepHungerExpression = v)
            .build()
        );

        hungerCat.addEntry(
            entryBuilder.startIntField(
                Text.translatable("sol2f.gui.config.option.hunger.MinFoodLevel"),
                Config.hunger.MinFoodLevel
            )
            .setTooltip(
                Text.translatable("sol2f.gui.config.option.hunger.MinFoodLevel.@Tooltip")
            )
            .setMin(0).setMax(20)
            .setSaveConsumer(v -> Config.hunger.MinFoodLevel = v)
            .build()
        );

        // ===== GUI 设置 =====
        ConfigCategory guiCat = builder.getOrCreateCategory(
            Text.translatable("sol2f.gui.config.option.gui")
        );
        guiCat.addEntry(
            entryBuilder.startBooleanToggle(
                Text.translatable("sol2f.gui.config.option.gui.ShowUnconsumedTooltips"),
                ClientConfig.GUISetting.ShowUnconsumedTooltips
            )
            .setTooltip(
                Text.translatable("sol2f.gui.config.option.gui.ShowUnconsumedTooltips.@Tooltip")
            )
            .setSaveConsumer(v -> ClientConfig.GUISetting.ShowUnconsumedTooltips = v)
            .build()
        );
        guiCat.addEntry(
            entryBuilder.startBooleanToggle(
                Text.translatable("sol2f.gui.config.option.gui.ShowConsumedTooltips"),
                ClientConfig.GUISetting.ShowConsumedTooltips
            )
            .setTooltip(
                Text.translatable("sol2f.gui.config.option.gui.ShowConsumedTooltips.@Tooltip")
            )
            .setSaveConsumer(v -> ClientConfig.GUISetting.ShowConsumedTooltips = v)
            .build()
        );

        ConfigCategory devCat = builder.getOrCreateCategory(
            Text.translatable("sol2f.gui.config.option.dev")
        );
        devCat.addEntry(
            entryBuilder.startBooleanToggle(
                Text.translatable("sol2f.gui.config.option.dev.developerMode"),
                Config.dev.DeveloperMode
            )
            .setTooltip(
                Text.translatable("sol2f.gui.config.option.dev.developerMode.@Tooltip")
            )
            .setSaveConsumer(v -> Config.dev.DeveloperMode = v)
            .build()
        );

        return builder.build();
    }
}