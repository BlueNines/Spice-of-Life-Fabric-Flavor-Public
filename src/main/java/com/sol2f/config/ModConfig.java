package com.sol2f.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class ModConfig {
	public int maxHealthy = 120;
	public int defaultHealthy = 20;
	public int healthyGain = 2;
	public boolean resetOnDeath = false;
	// When true, enable developer logging to run/logs/sol2f/sol2f.log
	public boolean developerMode = false;

	// Prefer the Fabric game directory (run/) config path so we don't create run/run when cwd != game dir
	private static final Path CONFIG_PATH = FabricLoader.getInstance().getGameDir().resolve("config").resolve("sol2f.json");
	private static final Path RUN_CONFIG_PATH = FabricLoader.getInstance().getGameDir().resolve("config").resolve("sol2f.json");
	private static ModConfig INSTANCE = new ModConfig();

	public static ModConfig getInstance() {
		return INSTANCE;
	}

	public static void load() {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		try {
			Path toRead = Files.exists(RUN_CONFIG_PATH) ? RUN_CONFIG_PATH : CONFIG_PATH;
			if (Files.exists(toRead)) {
				String json = new String(Files.readAllBytes(toRead), StandardCharsets.UTF_8);
				INSTANCE = gson.fromJson(json, ModConfig.class);
				if (INSTANCE == null) INSTANCE = new ModConfig();
				// write back merged config to both locations so they're consistent
				try {
					save();
				} catch (Exception ignore) {
					// best-effort only
				}
			} else {
				// write default
				INSTANCE = new ModConfig();
				save();
			}
		} catch (IOException e) {
			INSTANCE = new ModConfig();
			// try to persist defaults if read fails
			try {
				save();
			} catch (Exception ignore) {
			}
		}
	}

	public static void save() {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String json = gson.toJson(INSTANCE);
		// write main config
		try {
			if (!Files.exists(CONFIG_PATH.getParent())) Files.createDirectories(CONFIG_PATH.getParent());
			Files.write(CONFIG_PATH, json.getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			// ignore for now
		}
		// also attempt to write run/config for dev runs
		try {
			if (!Files.exists(RUN_CONFIG_PATH.getParent())) Files.createDirectories(RUN_CONFIG_PATH.getParent());
			Files.write(RUN_CONFIG_PATH, json.getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			// ignore
		}
	}
}
