package com.sol2f.network;

import net.minecraft.util.Identifier;

public class FoodPackets {
    public static final Identifier S2C_FOOD_LIST = new Identifier("sol2f", "s2c_food_list");
    // client -> server request to ask the server to send the current consumed-food list
    public static final Identifier C2S_REQUEST_LIST = new Identifier("sol2f", "c2s_request_list");
}
