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
     * 超过数量上限的数据包必须被拒绝。
     */
    @Test
    void rejectsOversizedFoodList() {
        PacketByteBuf buffer = PacketByteBufs.create();
        buffer.writeVarInt(NetworkChannels.MAX_FOOD_ENTRIES + 1);

        assertThrows(IllegalArgumentException.class, () -> NetworkChannels.readFoodList(buffer));
    }
}
