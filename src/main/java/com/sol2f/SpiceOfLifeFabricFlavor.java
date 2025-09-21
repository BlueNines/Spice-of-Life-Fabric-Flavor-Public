package com.sol2f;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// optional runtime file appender imports
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
				Path logsDir = Paths.get("run", "logs", "sol2f");
				if (!Files.exists(logsDir)) Files.createDirectories(logsDir);
				String logFile = logsDir.resolve("sol2f.log").toString();
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
				} catch (Throwable t) {
					LOGGER.warn("sol2f: Logback not available for developerMode file logging, skipping", t);
				}
			}
		} catch (Throwable t) {
			LOGGER.warn("sol2f: failed to enable developerMode file logger", t);
		}

		// Register server-side handlers
		FoodUseHandler.register();
		ServerEventHandlers.register();
		FoodCommands.register();
		// register Food Record item
	// food_record item removed

		LOGGER.info("SpiceOfLife: initialized");
	}
}