package com.sol2f;

import com.sol2f.client.FoodClient;
import net.fabricmc.api.ClientModInitializer;

public class SpiceOfLifeFabricFlavorClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Delegate to FoodClient for client-specific initialization
		new FoodClient().onInitializeClient();
	}
}