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
            });// 保存当前 config 实例（AutoConfig 自动写入文件）

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        // ===== 血量设置 =====
        ConfigCategory healthyCat = builder.getOrCreateCategory(
            Text.translatable("sol2f.gui.config.option.healthy")
        );

        healthyCat.addEntry(
            entryBuilder.startIntField(
                Text.translatable("sol2f.gui.config.option.healthy.maxHealthy"),
                Config.healthy.maxHealthy
            )
            .setTooltip(
                Text.translatable("sol2f.gui.config.option.healthy.maxHealthy.@Tooltip")
            )
            .setMin(2)
            .setMax(5000)
            .setSaveConsumer(newValue -> Config.healthy.maxHealthy = newValue)
            .build()
        );

        // ===== 功能设置 =====
        ConfigCategory featureCat = builder.getOrCreateCategory(
            Text.translatable("sol2f.gui.config.option.features")
        );

        featureCat.addEntry(
            entryBuilder.startIntField(
                Text.translatable("sol2f.gui.config.option.features.healthyGain"),
                Config.features.healthyGain
            )
            .setTooltip(
                Text.translatable("sol2f.gui.config.option.features.healthyGain.@Tooltip")
            )
            .setMin(0).setMax(20)
            .setSaveConsumer(v -> Config.features.healthyGain = v)
            .build()
        );

        featureCat.addEntry(
            entryBuilder.startIntField(
                Text.translatable("sol2f.gui.config.option.features.Increasefrequency"),
                Config.features.Increasefrequency
            )
            .setTooltip(
                Text.translatable("sol2f.gui.config.option.features.Increasefrequency.@Tooltip")
            )
            .setMin(0).setMax(20)
            .setSaveConsumer(v -> Config.features.Increasefrequency = v)
            .build()
        );

        featureCat.addEntry(
            entryBuilder.startIntField(
                Text.translatable("sol2f.gui.config.option.features.frequencyGain"),
                Config.features.frequencyGain
            )
            .setTooltip(
                Text.translatable("sol2f.gui.config.option.features.frequencyGain.@Tooltip")
            )
            .setMin(1).setMax(30)
            .setSaveConsumer(v -> Config.features.frequencyGain = v)
            .build()
        );

        featureCat.addEntry(
            entryBuilder.startBooleanToggle(
                Text.translatable("sol2f.gui.config.option.features.resetOnDeath"),
                Config.features.resetOnDeath
            )
            .setTooltip(
                Text.translatable("sol2f.gui.config.option.features.resetOnDeath.@Tooltip")
            )
            .setSaveConsumer(v -> Config.features.resetOnDeath = v)
            .build()
        );

        featureCat.addEntry(
            entryBuilder.startBooleanToggle(
                Text.translatable("sol2f.gui.config.option.features.healthToMaxOnIncrease"),
                Config.features.healthToMaxOnIncrease
            )
            .setTooltip(
                Text.translatable("sol2f.gui.config.option.features.healthToMaxOnIncrease.@Tooltip")
            )
            .setSaveConsumer(v -> Config.features.healthToMaxOnIncrease = v)
            .build()
        );

        featureCat.addEntry(
            entryBuilder.startIntField(
                Text.translatable("sol2f.gui.config.option.features.healthIncreaseOnIncrease"),
                Config.features.healthIncreaseOnIncrease
            )
            .setTooltip(
                Text.translatable("sol2f.gui.config.option.features.healthIncreaseOnIncrease.@Tooltip")
            )
            .setMin(0).setMax(20)
            .setSaveConsumer(v -> Config.features.healthIncreaseOnIncrease = v)
            .build()
        );

        featureCat.addEntry(
            entryBuilder.startStrField(
                Text.translatable("sol2f.gui.config.option.features.Expression"),
                Config.features.Expression
            )
            .setTooltip(
                Text.translatable("sol2f.gui.config.option.features.Expression.@Tooltip[1]"),
                Text.translatable("sol2f.gui.config.option.features.Expression.@Tooltip[2]"),
                Text.translatable("sol2f.gui.config.option.features.Expression.@Tooltip[3]"),
                Text.translatable("sol2f.gui.config.option.features.Expression.@Tooltip[4]"),
                Text.translatable("sol2f.gui.config.option.features.Expression.@Tooltip[5]"),
                Text.translatable("sol2f.gui.config.option.features.Expression.@Tooltip[6]"),
                Text.translatable("sol2f.gui.config.option.features.Expression.@Tooltip[7]"),
                Text.translatable("sol2f.gui.config.option.features.Expression.@Tooltip[8]"),
                Text.translatable("sol2f.gui.config.option.features.Expression.@Tooltip[9]"),
                Text.translatable("sol2f.gui.config.option.features.Expression.@Tooltip[10]"),
                Text.translatable("sol2f.gui.config.option.features.Expression.@Tooltip[11]"),
                Text.translatable("sol2f.gui.config.option.features.Expression.@Tooltip[12]")
            )
            .setSaveConsumer(v -> Config.features.Expression = v)
            .build()
        );

        featureCat.addEntry(
            entryBuilder.startStrList(
                Text.translatable("sol2f.gui.config.option.features.blacklist"),
                Config.features.blacklist
            )
            .setTooltip(
                Text.translatable("sol2f.gui.config.option.features.blacklist.@Tooltip[1]"),
                Text.translatable("sol2f.gui.config.option.features.blacklist.@Tooltip[2]"),
                Text.translatable("sol2f.gui.config.option.features.blacklist.@Tooltip[3]")
            )
            .setSaveConsumer(v -> Config.features.blacklist = v)
            .build()
        );

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

        return builder.build();
    }
}