package com.sol2f;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// optional runtime file appender imports
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.time.format.DateTimeFormatter;

import com.sol2f.config.ModConfig;
import com.sol2f.server.FoodUseHandler;
import com.sol2f.server.ServerEventHandlers;
import com.sol2f.command.FoodCommands;
// removed food_record imports

public class SpiceOfLifeFabricFlavor implements ModInitializer {
	public static final String MOD_ID = "sol2f";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	// Fallback writer used when Logback is not available but developerMode=true
	private static BufferedWriter DEV_FILE_WRITER = null;
	private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static final DateTimeFormatter STARTUP_FILE_FMT = DateTimeFormatter.ofPattern("yyyy-M-d-HH-mm-ss");

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("SpiceOfLife: initializing");

		// Load configuration
		ModConfig.load();

		// If developerMode is enabled, add a dedicated file appender under run/logs/sol2f
		// Enable developer file logging via reflection to avoid compile-time dependency on Logback
		try {
			if (ModConfig.getInstance().developerMode) {
				// Use Fabric game directory to avoid creating run/run when cwd differs
				Path gameDir = net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir();
				Path logsDir = gameDir.resolve("logs").resolve("sol2f");
				if (!Files.exists(logsDir)) Files.createDirectories(logsDir);
				String startupTime = java.time.LocalDateTime.now().format(STARTUP_FILE_FMT);
				String logFile = logsDir.resolve("sol2f_" + startupTime + ".log").toString();
				Class<?> loggerFactoryClass = Class.forName("org.slf4j.LoggerFactory");

				// Try to configure Logback dynamically if available
				try {
					Class<?> loggerContextClass = Class.forName("ch.qos.logback.classic.LoggerContext");
					Class<?> patternEncoderClass = Class.forName("ch.qos.logback.classic.encoder.PatternLayoutEncoder");
					// ILoggingEvent type checked via reflection; no local var needed
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
					// open a simple fallback writer for developer logs
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

		// Register server-side handlers
		FoodUseHandler.register();
		ServerEventHandlers.register();
		FoodCommands.register();
		// register Food Record item
	// food_record item removed

		LOGGER.info("SpiceOfLife: initialized");
	}

	public static void writeDevLog(String fmt, Object... args) {
		// Backwards-compatible: log formatted message to console and to structured file line (as "message" field)
		try {
			LOGGER.info(fmt.replace("{}", "%s"), args);
		} catch (Throwable t) {
			// ignore
		}
		if (DEV_FILE_WRITER != null) {
			try {
				String msg = String.format(fmt.replace("{}", "%s"), args);
				java.util.Map<String, String> m = new java.util.LinkedHashMap<>();
				m.put("message", msg);
				writeStructuredDevLog(m);
			} catch (Throwable t) {
				// fail silently
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
				// escape double quotes and backslashes in value
				v = v.replace("\\", "\\\\").replace("\"", "\\\"");
				sb.append('"').append(k).append('"').append("=").append('"').append(v).append('"');
			}
			sb.append('}');
			sb.append('\n');
			DEV_FILE_WRITER.write(sb.toString());
			DEV_FILE_WRITER.flush();
		} catch (Throwable t) {
			// fail silently
		}
	}
}