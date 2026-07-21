package com.sol2f.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;

/**
 * 验证 1.20.1 网络列表的编码边界。
 */
class NetworkChannelsTest {
    /**
     * 正常食物列表应可完整往返。
     */
    @Test
    void roundTripsFoodList() {
        List<String> foods = List.of("minecraft:apple", "minecraft:bread");
        PacketByteBuf buffer = NetworkChannels.writeFoodList(foods);

        assertEquals(foods, NetworkChannels.readFoodList(buffer));
    }

    /**
     * 小数生命增益应通过双精度网络载荷完整传输。
     */
    @Test
    void roundTripsFractionalHealthValue() {
        PacketByteBuf buffer = NetworkChannels.writeDouble(0.8D);

        assertEquals(0.8D, buffer.readDouble(), 0.000001D);
        assertEquals(0, buffer.readableBytes());
    }

    /**
     * 超过数量上限的数据包必须被拒绝。
     */
    @Test
    void rejectsOversizedFoodList() {
        PacketByteBuf buffer = PacketByteBufs.create();
        buffer.writeVarInt(NetworkChannels.MAX_FOOD_ENTRIES + 1);

        assertThrows(IllegalArgumentException.class, () -> NetworkChannels.readFoodList(buffer));
    }

    /**
     * 合法列表后附加的多余载荷必须被拒绝。
     */
    @Test
    void rejectsTrailingPayloadBytes() {
        PacketByteBuf buffer = NetworkChannels.writeFoodList(List.of("minecraft:apple"));
        buffer.writeByte(1);

        assertThrows(IllegalArgumentException.class, () -> NetworkChannels.readFoodList(buffer));
    }
}
