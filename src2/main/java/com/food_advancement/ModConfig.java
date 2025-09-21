package com.food_advancement;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("food_advancement.json").toFile();
    private static ModConfig INSTANCE;

    // 配置项（所有数值按照生命值计算，1心 = 2点生命值）
    // 最大生命值上限（默认60心）
    private int maxHealthy = 120;
    // 玩家初始生命值（默认10心）
    private int defaultHealthy = 20;
    // 每食用一种新食物增加的生命值（默认1心）
    public static int healthyGain = 1;
    // 死亡后是否重置食物列表和血量
    private boolean resetOnDeath = false;
    // 死亡惩罚：降低的血量上限（0表示禁用，默认禁用）
    private int deathPenalty = 0;

    private static final String CONFIG_COMMENT = """
        // 食物进度系统配置文件
        // maxHealthy: 生命值上限（默认120，即60心）
        // defaultHealthy: 初始生命值（默认20，即10心）
        // healthyGain: 每种新食物增加的生命值（默认2，即1心）
        // resetOnDeath: 死亡后是否重置食物列表和血量（默认false）
        // deathPenalty: 死亡惩罚，降低的血量上限点数（默认0表示禁用，设置为2则表示死亡后降低1心）
        // 注意：所有数值都是以生命值点数计算，1心 = 2点生命值
        """;

    public static ModConfig getInstance() {
        if (INSTANCE == null) {
            loadConfig();
        }
        return INSTANCE;
    }

    public static void loadConfig() {
        try {
            if (CONFIG_FILE.exists()) {
                try (FileReader reader = new FileReader(CONFIG_FILE)) {
                    INSTANCE = GSON.fromJson(reader, ModConfig.class);
                    Food_advancement.LOGGER.info("已加载配置文件：maxHealthy={}, defaultHealthy={}, healthyGain={}",
                        INSTANCE.maxHealthy, INSTANCE.defaultHealthy, INSTANCE.healthyGain);
                }
            } else {
                INSTANCE = new ModConfig();
                saveConfig();
                Food_advancement.LOGGER.info("已创建默认配置文件：maxHealthy={}, defaultHealthy={}, healthyGain={}",
                    INSTANCE.maxHealthy, INSTANCE.defaultHealthy, INSTANCE.healthyGain);
            }
        } catch (IOException e) {
            Food_advancement.LOGGER.error("加载配置文件失败: " + e.getMessage());
            INSTANCE = new ModConfig();
            Food_advancement.LOGGER.info("使用默认配置：maxHealthy={}, defaultHealthy={}, healthyGain={}",
                INSTANCE.maxHealthy, INSTANCE.defaultHealthy, INSTANCE.healthyGain);
        }
    }

    public static void saveConfig() {
        try {
            if (!CONFIG_FILE.exists()) {
                CONFIG_FILE.getParentFile().mkdirs();
                CONFIG_FILE.createNewFile();
            }

            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                // 写入注释
                writer.write(CONFIG_COMMENT);
                // 写入配置
                GSON.toJson(INSTANCE, writer);
            }
        } catch (IOException e) {
            Food_advancement.LOGGER.error("Failed to save config: " + e.getMessage());
        }
    }

    public int getMaxHealthy() {
        return maxHealthy;
    }

    public void setMaxHealthy(int maxHealthy) {
        this.maxHealthy = maxHealthy;
        saveConfig();
    }

    public int getDefaultHealthy() {
        return defaultHealthy;
    }

    public void setDefaultHealthy(int defaultHealthy) {
        this.defaultHealthy = defaultHealthy;
        saveConfig();
    }

    public int getHealthyGain() {
        return healthyGain;
    }

    public boolean isResetOnDeath() {
        return resetOnDeath;
    }

    public void setResetOnDeath(boolean resetOnDeath) {
        this.resetOnDeath = resetOnDeath;
        saveConfig();
    }

    public int getDeathPenalty() {
        return deathPenalty;
    }

    public void setDeathPenalty(int deathPenalty) {
        this.deathPenalty = deathPenalty;
        saveConfig();
    }

    public void setHealthyGain(int healthyGain) {
        this.healthyGain = healthyGain;
        saveConfig();
    }
}
