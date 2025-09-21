package com.sol2f.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;

public class S2CFoodListSync {

    public static void sendTo(ServerPlayerEntity player, List<String> list) {
    PacketByteBuf buf = new PacketByteBuf(io.netty.buffer.Unpooled.buffer());
        buf.writeCollection(list, (b, s) -> b.writeString(s));
        ServerPlayNetworking.send(player, FoodPackets.S2C_FOOD_LIST, buf);
    }

    public static List<String> readList(PacketByteBuf buf) {
        List<String> list = new ArrayList<>();
        buf.readCollection(ArrayList::new, PacketByteBuf::readString).forEach(list::add);
        return list;
    }
}
