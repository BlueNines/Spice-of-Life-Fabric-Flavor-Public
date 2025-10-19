package com.sol2f;

import com.sol2f.client.FoodClient;
import net.fabricmc.api.ClientModInitializer;

public class SpiceOfLifeFabricFlavorClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// 委托给FoodClient进行客户端特定的初始化
		new FoodClient().onInitializeClient();
	}
}