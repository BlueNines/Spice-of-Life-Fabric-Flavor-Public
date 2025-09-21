package com.sol2f.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ModConfig {
	public int maxHealthy = 120;
	public int defaultHealthy = 20;
	public int healthyGain = 2;
	public boolean resetOnDeath = false;
	// When true, enable developer logging to run/logs/sol2f/sol2f.log
	public boolean developerMode = false;

	private static final Path CONFIG_PATH = Paths.get("config", "sol2f.json");
	private static ModConfig INSTANCE = new ModConfig();

	public static ModConfig getInstance() {
		return INSTANCE;
	}

	public static void load() {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		try {
			if (Files.exists(CONFIG_PATH)) {
				String json = new String(Files.readAllBytes(CONFIG_PATH), StandardCharsets.UTF_8);
				INSTANCE = gson.fromJson(json, ModConfig.class);
				if (INSTANCE == null) INSTANCE = new ModConfig();
			} else {
				// write default
				INSTANCE = new ModConfig();
				save();
			}
		} catch (IOException e) {
			INSTANCE = new ModConfig();
		}
	}

	public static void save() {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		try {
			if (!Files.exists(CONFIG_PATH.getParent())) Files.createDirectories(CONFIG_PATH.getParent());
			String json = gson.toJson(INSTANCE);
			Files.write(CONFIG_PATH, json.getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			// ignore for now
		}
	}
}
