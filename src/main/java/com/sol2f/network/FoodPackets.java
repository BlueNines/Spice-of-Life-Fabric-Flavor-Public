package com.sol2f.network;

import net.minecraft.util.Identifier;

public class FoodPackets {
    public static final Identifier S2C_FOOD_LIST = new Identifier("sol2f", "s2c_food_list");
    // 客户端 -> 服务器请求，要求服务器发送当前已消耗的食物列表
    public static final Identifier C2S_REQUEST_LIST = new Identifier("sol2f", "c2s_request_list");
}