package com.sol2f.Gui;

import com.sol2f.config.Sol2FConfig;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class Sol2FConfigGUI {

    public static Screen openConfigScreen(Screen parent) {
        
        System.out.println("Sol2FConfigGUI: openConfigScreen called!");

        Sol2FConfig config = AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig();

        System.out.println("Config loaded");

        ConfigBuilder builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(Text.translatable("sol2f.gui.config.title"))
            .setSavingRunnable(() -> AutoConfig.getConfigHolder(Sol2FConfig.class).save());// 保存当前 config 实例（AutoConfig 自动写入文件）

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        // ===== 血量设置 =====
        ConfigCategory healthyCat = builder.getOrCreateCategory(
            Text.translatable("sol2f.gui.config.option.healthy")
        );

        healthyCat.addEntry(
            entryBuilder.startIntField(
                Text.translatable("sol2f.gui.config.option.healthy.maxHealthy"),
                config.healthy.maxHealthy
            )
            .setTooltip(
                Text.translatable("sol2f.gui.config.option.healthy.maxHealthy.@Tooltip")
            )
            .setMin(2)
            .setMax(240)
            .setSaveConsumer(newValue -> config.healthy.maxHealthy = newValue)
            .build()
        );

        // ===== 功能设置 =====
        ConfigCategory featureCat = builder.getOrCreateCategory(
            Text.translatable("sol2f.gui.config.option.features")
        );

        featureCat.addEntry(
            entryBuilder.startIntField(
                Text.translatable("sol2f.gui.config.option.features.healthyGain"),
                config.features.healthyGain
            )
            .setTooltip(
                Text.translatable("sol2f.gui.config.option.features.healthyGain.@Tooltip")
            )
            .setMin(0).setMax(20)
            .setSaveConsumer(v -> config.features.healthyGain = v)
            .build()
        );

        featureCat.addEntry(
            entryBuilder.startIntField(
                Text.translatable("sol2f.gui.config.option.features.Increasefrequency"),
                config.features.Increasefrequency
            )
            .setTooltip(
                Text.translatable("sol2f.gui.config.option.features.Increasefrequency.@Tooltip")
            )
            .setMin(0).setMax(20)
            .setSaveConsumer(v -> config.features.Increasefrequency = v)
            .build()
        );

        featureCat.addEntry(
            entryBuilder.startIntField(
                Text.translatable("sol2f.gui.config.option.features.frequencyGain"),
                config.features.frequencyGain
            )
            .setTooltip(
                Text.translatable("sol2f.gui.config.option.features.frequencyGain.@Tooltip")
            )
            .setMin(1).setMax(30)  // 注意：语言文件说 1~30，但原注解是 min=2，按语言文件为准
            .setSaveConsumer(v -> config.features.frequencyGain = v)
            .build()
        );

        featureCat.addEntry(
            entryBuilder.startBooleanToggle(
                Text.translatable("sol2f.gui.config.option.features.resetOnDeath"),
                config.features.resetOnDeath
            )
            .setTooltip(
                Text.translatable("sol2f.gui.config.option.features.resetOnDeath.@Tooltip")
            )
            .setSaveConsumer(v -> config.features.resetOnDeath = v)
            .build()
        );

        featureCat.addEntry(
            entryBuilder.startBooleanToggle(
                Text.translatable("sol2f.gui.config.option.features.healthToMaxOnIncrease"),
                config.features.healthToMaxOnIncrease
            )
            .setTooltip(
                Text.translatable("sol2f.gui.config.option.features.healthToMaxOnIncrease.@Tooltip")
            )
            .setSaveConsumer(v -> config.features.healthToMaxOnIncrease = v)
            .build()
        );

        featureCat.addEntry(
            entryBuilder.startIntField(
                Text.translatable("sol2f.gui.config.option.features.healthIncreaseOnIncrease"),
                config.features.healthIncreaseOnIncrease
            )
            .setTooltip(
                Text.translatable("sol2f.gui.config.option.features.healthIncreaseOnIncrease.@Tooltip")
            )
            .setMin(0).setMax(20)
            .setSaveConsumer(v -> config.features.healthIncreaseOnIncrease = v)
            .build()
        );

        featureCat.addEntry(
            entryBuilder.startStrField(
                Text.translatable("sol2f.gui.config.option.features.Expression"),
                config.features.Expression
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
                Text.translatable("sol2f.gui.config.option.features.Expression.@Tooltip[12]"),
                Text.translatable("sol2f.gui.config.option.features.Expression.@Tooltip[13]")
            )
            .setSaveConsumer(v -> config.features.Expression = v)
            .build()
        );

        return builder.build();
    }
}