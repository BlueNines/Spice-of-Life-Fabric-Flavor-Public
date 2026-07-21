package com.sol2f.network;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

/**
 * 定义 1.20.1 客户端与服务端使用的网络通道和编解码边界。
 */
public final class NetworkChannels {
    public static final int MAX_FOOD_ENTRIES = 4096;
    public static final int MAX_FOOD_ID_LENGTH = 191;

    public static final Identifier S2C_EATEN_FOOD_LIST = new Identifier("sol2f", "s2c_food_list");
    public static final Identifier S2C_ALL_FOOD_LIST = new Identifier("sol2f", "s2c_all_food_list");
    public static final Identifier S2C_HEALTH = new Identifier("sol2f", "s2c_health");
    public static final Identifier S2C_HEALTH_MAX = new Identifier("sol2f", "s2c_health_max");
    public static final Identifier S2C_OPEN_FOOD_BOOK = new Identifier("sol2f", "open_food_book_screen");
    public static final Identifier C2S_REQUEST_FOOD_LIST = new Identifier("sol2f", "c2s_request_food_list");
    public static final Identifier C2S_REQUEST_ALL_FOOD_LIST = new Identifier("sol2f", "c2s_request_all_food_list");

    private NetworkChannels() {
    }

    /**
     * 将食物 ID 集合编码为带数量上限的数据包。
     *
     * @param foods 食物 ID 集合
     * @return 可发送的数据包缓冲区
     */
    public static PacketByteBuf writeFoodList(Collection<String> foods) {
        PacketByteBuf buffer = PacketByteBufs.create();
        int size = Math.min(foods.size(), MAX_FOOD_ENTRIES);
        buffer.writeVarInt(size);

        int written = 0;
        for (String food : foods) {
            if (written >= size) {
                break;
            }
            buffer.writeString(food, MAX_FOOD_ID_LENGTH);
            written++;
        }
        return buffer;
    }

    /**
     * 从网络缓冲区读取并校验食物 ID 列表。
     *
     * @param buffer 网络缓冲区
     * @return 已校验的食物 ID 列表
     */
    public static List<String> readFoodList(PacketByteBuf buffer) {
        int size = buffer.readVarInt();
        if (size < 0 || size > MAX_FOOD_ENTRIES) {
            throw new IllegalArgumentException("Invalid food list size: " + size);
        }

        List<String> foods = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            foods.add(buffer.readString(MAX_FOOD_ID_LENGTH));
        }
        return foods;
    }

    /**
     * 创建只包含一个整数的数据包。
     *
     * @param value 要写入的整数
     * @return 可发送的数据包缓冲区
     */
    public static PacketByteBuf writeInt(int value) {
        PacketByteBuf buffer = PacketByteBufs.create();
        buffer.writeVarInt(value);
        return buffer;
    }
}
