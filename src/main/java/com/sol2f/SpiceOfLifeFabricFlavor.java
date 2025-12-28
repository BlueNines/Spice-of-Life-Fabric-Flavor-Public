package com.sol2f;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registry;
import net.minecraft.item.ItemGroups;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.time.format.DateTimeFormatter;

import com.sol2f.config.Sol2FConfig;
import com.sol2f.item.RightClickItem;
import com.sol2f.network.FoodPackets;
import com.sol2f.server.FoodUseHandler;
import com.sol2f.server.ServerEventHandlers;
import com.sol2f.command.FoodCommands;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;;

public class SpiceOfLifeFabricFlavor implements ModInitializer {
	public static final String MOD_ID = "sol2f";

	// 这个日志记录器用于向控制台和日志文件写入文本
	// 最佳实践是使用你的模组ID作为日志记录器的名称
	// 这样，就可以清楚地知道是哪个模组写入了信息、警告和错误
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	// 当Logback不可用但developerMode=true时使用的备用写入器
	private static BufferedWriter DEV_FILE_WRITER = null;
	private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static final DateTimeFormatter STARTUP_FILE_FMT = DateTimeFormatter.ofPattern("yyyy-M-d-HH-mm-ss");

	public static final Item FOOD_BOOK_ITEM = new RightClickItem(// 创建食物书物品
        new Item.Settings().maxCount(1),
        (player, stack) -> {
            // 发送打开食物书界面的数据包给客户端
            ServerPlayNetworking.send(player, FoodPackets.OPEN_FOOD_BOOK_SCREEN, PacketByteBufs.empty());
        }
    );

	@Override
	public void onInitialize() {

		LOGGER.info("SpiceOfLife: initializing");
		AutoConfig.register(Sol2FConfig.class, GsonConfigSerializer::new);
		// 注册并加载配置（Cloth Config / Auto Config）
		// 如果启用了developerMode，则在run/logs/sol2f下添加专用的文件附加器
		// 通过反射启用开发者文件日志记录，以避免对Logback的编译时依赖
			try {
				if (AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig().features.developerMode) {
				Path gameDir = net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir();
				Path logsDir = gameDir.resolve("logs").resolve("sol2f");
				if (!Files.exists(logsDir)) Files.createDirectories(logsDir);
				String startupTime = java.time.LocalDateTime.now().format(STARTUP_FILE_FMT);
				String logFile = logsDir.resolve("sol2f_" + startupTime + ".log").toString();
				Class<?> loggerFactoryClass = Class.forName("org.slf4j.LoggerFactory");

				// 尝试动态配置Logback（如果可用）
				try {
					Class<?> loggerContextClass = Class.forName("ch.qos.logback.classic.LoggerContext");
					Class<?> patternEncoderClass = Class.forName("ch.qos.logback.classic.encoder.PatternLayoutEncoder");
					// 通过反射检查ILoggingEvent类型；不需要本地变量
					Class<?> fileAppenderClass = Class.forName("ch.qos.logback.core.FileAppender");

					Object ctx = loggerFactoryClass.getMethod("getILoggerFactory").invoke(null);
					java.lang.reflect.Constructor<?> encCtor = patternEncoderClass.getConstructor();
					Object ple = encCtor.newInstance();
					patternEncoderClass.getMethod("setPattern", String.class).invoke(ple, "%d{HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n");
					patternEncoderClass.getMethod("setContext", loggerContextClass).invoke(ple, ctx);
					patternEncoderClass.getMethod("start").invoke(ple);

					java.lang.reflect.Constructor<?> faCtor = fileAppenderClass.getConstructor();
					Object fa = faCtor.newInstance();
					fileAppenderClass.getMethod("setFile", String.class).invoke(fa, logFile);
					fileAppenderClass.getMethod("setEncoder", Class.forName("ch.qos.logback.core.encoder.Encoder")).invoke(fa, ple);
					fileAppenderClass.getMethod("setContext", loggerContextClass).invoke(fa, ctx);
					fileAppenderClass.getMethod("setName", String.class).invoke(fa, "SOL2F_FILE_APPENDER");
					fileAppenderClass.getMethod("start").invoke(fa);

					Class<?> classicLoggerClass = Class.forName("ch.qos.logback.classic.Logger");
					Object root = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
					classicLoggerClass.getMethod("addAppender", Class.forName("ch.qos.logback.core.Appender")).invoke(root, fa);
					LOGGER.info("sol2f: developerMode enabled - logging to {}", logFile);
					writeDevLog("sol2f: developerMode enabled - logging to %s", logFile);
				} catch (Throwable t) {
					LOGGER.warn("sol2f: Logback not available for developerMode file logging, falling back to simple file writer", t);
					writeDevLog("sol2f: Logback not available for developerMode file logging, falling back to simple file writer: %s", t.toString());
					// 为开发者日志打开一个简单的备用写入器
					try {
						DEV_FILE_WRITER = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(logFile, true), java.nio.charset.StandardCharsets.UTF_8));
						DEV_FILE_WRITER.write("--- sol2f developer log started at " + java.time.LocalDateTime.now().format(TIME_FMT) + " ---\n");
						DEV_FILE_WRITER.flush();
					} catch (Throwable ioe) {
						LOGGER.warn("sol2f: failed to open fallback dev log file {}", logFile, ioe);
						DEV_FILE_WRITER = null;
					}
				}
			}
		} catch (Throwable t) {
			LOGGER.warn("sol2f: failed to enable developerMode file logger", t);
			writeDevLog("sol2f: failed to enable developerMode file logger: %s", t.toString());
		}

		// 注册服务端处理器
		FoodUseHandler.register();
		ServerEventHandlers.register();
		FoodCommands.register();

		Registry.register(Registries.ITEM, new Identifier("sol2f", "food_book"), FOOD_BOOK_ITEM);// 注册食物书物品
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> {
			entries.add(FOOD_BOOK_ITEM);
		});

		LOGGER.info("SpiceOfLife: initialized");
	}

	public static void writeDevLog(String fmt, Object... args) {
		// 向后兼容：将格式化消息记录到控制台和结构化文件行（作为"message"字段）
		try {
			LOGGER.info(fmt.replace("{}", "%s"), args);
		} catch (Throwable t) {
			// 忽略
		}
		if (DEV_FILE_WRITER != null) {
			try {
				String msg = String.format(fmt.replace("{}", "%s"), args);
				java.util.Map<String, String> m = new java.util.LinkedHashMap<>();
				m.put("message", msg);
				writeStructuredDevLog(m);
			} catch (Throwable t) {
				// 静默失败
			}
		}
	}

	/**
	 * 写入结构化开发日志行。
	 * 每行格式: [yyyy-MM-dd HH:mm:ss]{"key"="value";"k2"="v2";}
	 */
	public static void writeStructuredDevLog(java.util.Map<String, String> fields) {
		if (DEV_FILE_WRITER == null) return;
		try {
			StringBuilder sb = new StringBuilder();
			String ts = java.time.LocalDateTime.now().format(TIME_FMT);
			sb.append('[').append(ts).append(']');
			sb.append('{');
			boolean first = true;
			for (java.util.Map.Entry<String, String> e : fields.entrySet()) {
				if (!first) sb.append(';');
				first = false;
				String k = e.getKey();
				String v = e.getValue();
				if (v == null) v = "";
				// 转义值中的双引号和反斜杠
				v = v.replace("\\", "\\\\").replace("\"", "\\\"");
				sb.append('"').append(k).append('"').append("=").append('"').append(v).append('"');
			}
			sb.append('}');
			sb.append('\n');
			DEV_FILE_WRITER.write(sb.toString());
			DEV_FILE_WRITER.flush();
		} catch (Throwable t) {
			// 静默失败
		}
	}
}