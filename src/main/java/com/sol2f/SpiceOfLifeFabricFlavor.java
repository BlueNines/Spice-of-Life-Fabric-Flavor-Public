package com.sol2f;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.api.ModInitializer;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registry;
import net.minecraft.item.ItemGroups;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.file.Files;
import java.nio.file.Path;

import com.sol2f.config.Sol2FConfig;
import com.sol2f.item.SOL2FRightClickItem;
import com.sol2f.network.NetWorkHandler;
import com.sol2f.server.ServerEvents;
import com.sol2f.command.FoodCommands;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;

public class SpiceOfLifeFabricFlavor implements ModInitializer {
	public static final String MOD_ID = "sol2f";

	// 这个日志记录器用于向控制台和日志文件写入文本
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final Item FOOD_BOOK_ITEM = new SOL2FRightClickItem(// 创建食物书物品
        new Item.Settings().maxCount(1),
        (player, stack) -> {
            NetWorkHandler.openFoodBook(player); // 发送打开食物书界面的数据包给客户端
        }
    );

    @Override
    public void onInitialize() {
        LOGGER.info("SpiceOfLife: initializing");
        AutoConfig.register(Sol2FConfig.class, GsonConfigSerializer::new);

        if (AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig().dev.DeveloperMode) {
            try {
                Path gameDir = net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir();
                Path logsDir = gameDir.resolve("logs").resolve("sol2f");
                if (!Files.exists(logsDir)) Files.createDirectories(logsDir);
                String startupTime = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-M-d-HH-mm-ss"));
                String logFile = logsDir.resolve("sol2f_" + startupTime + ".log").toString();

                Class<?> loggerContextClass = Class.forName("ch.qos.logback.classic.LoggerContext");
                Class<?> patternEncoderClass = Class.forName("ch.qos.logback.classic.encoder.PatternLayoutEncoder");
                Class<?> fileAppenderClass = Class.forName("ch.qos.logback.core.FileAppender");

                Object ctx = LoggerFactory.getILoggerFactory();
                Object ple = patternEncoderClass.getDeclaredConstructor().newInstance();
                patternEncoderClass.getMethod("setPattern", String.class).invoke(ple, "%d{HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n");
                patternEncoderClass.getMethod("setContext", loggerContextClass).invoke(ple, ctx);
                patternEncoderClass.getMethod("start").invoke(ple);

                Object fa = fileAppenderClass.getDeclaredConstructor().newInstance();
                fileAppenderClass.getMethod("setFile", String.class).invoke(fa, logFile);
                fileAppenderClass.getMethod("setEncoder", Class.forName("ch.qos.logback.core.encoder.Encoder")).invoke(fa, ple);
                fileAppenderClass.getMethod("setContext", loggerContextClass).invoke(fa, ctx);
                fileAppenderClass.getMethod("setName", String.class).invoke(fa, "SOL2F_FILE_APPENDER");
                fileAppenderClass.getMethod("start").invoke(fa);

                Class<?> classicLoggerClass = Class.forName("ch.qos.logback.classic.Logger");
                Object root = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
                classicLoggerClass.getMethod("addAppender", Class.forName("ch.qos.logback.core.Appender")).invoke(root, fa);
                LOGGER.info("sol2f: developerMode enabled - logging to {}", logFile);
            } catch (Throwable t) {
                LOGGER.error("sol2f: failed to enable developerMode file logger", t);
            }
        }

        // 注册事件、命令和物品
        ServerEvents.register();
        FoodCommands.register();
        Registry.register(Registries.ITEM, new Identifier("sol2f", "food_book"), FOOD_BOOK_ITEM);
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> {
            entries.add(FOOD_BOOK_ITEM);
        });

        LOGGER.info("SpiceOfLife: initialized");
    }

	public static void writeDevLog(String fmt, Object... args) {
		LOGGER.info(fmt.replace("{}", "%s"), args);
	}

}
