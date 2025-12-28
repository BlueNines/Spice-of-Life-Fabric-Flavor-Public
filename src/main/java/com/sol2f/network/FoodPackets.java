package com.sol2f.network;

import net.minecraft.util.Identifier;

public class FoodPackets {
    public static final Identifier S2C_FOOD_LIST = new Identifier("sol2f", "s2c_food_list");// 服务器 -> 客户端，发送当前已消耗的食物列表
    
    public static final Identifier C2S_REQUEST_LIST = new Identifier("sol2f", "c2s_request_list");// 客户端 -> 服务器请求，要求服务器发送当前已消耗的食物列表

    public static final Identifier S2C_ALL_FOOD_LIST = new Identifier("sol2f", "s2c_all_food_list");// 服务器 -> 客户端，发送所有食物列表

    public static final Identifier C2S_REQUEST_ALL_FOOD_LIST = new Identifier("sol2f", "c2s_request_all_food_list");// 客户端 -> 服务器请求，要求服务器发送所有食物列表

    public static final Identifier S2C_HEALTH = new Identifier("sol2f", "s2c_health");// 服务器 -> 客户端，发送当前生命值

    public static final Identifier S2C_HEALTH_MAX = new Identifier("sol2f", "s2c_health_max");// 服务器 -> 客户端，发送最大生命值

    public static final Identifier OPEN_FOOD_BOOK_SCREEN = new Identifier("sol2f", "open_food_book_screen");// 服务器 -> 客户端，打开食物书界面
}