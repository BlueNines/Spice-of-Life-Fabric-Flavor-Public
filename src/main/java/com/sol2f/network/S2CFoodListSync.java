package com.sol2f.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class S2CFoodListSync {

    public static void sendTo(ServerPlayerEntity player, List<String> list) {
    PacketByteBuf buf = new PacketByteBuf(io.netty.buffer.Unpooled.buffer());
        buf.writeCollection(list, (b, s) -> b.writeString(s));
        ServerPlayNetworking.send(player, FoodPackets.S2C_FOOD_LIST, buf);
    }

    public static void sendAllFoods(ServerPlayerEntity player, Set<String> foods) {// 发送所有食物
        PacketByteBuf buf = new PacketByteBuf(io.netty.buffer.Unpooled.buffer());
        buf.writeCollection(foods, PacketByteBuf::writeString);
        ServerPlayNetworking.send(player, FoodPackets.S2C_ALL_FOOD_LIST, buf);
    }

    public static void sendHealth(ServerPlayerEntity player, int health) {
        PacketByteBuf buf = new PacketByteBuf(io.netty.buffer.Unpooled.buffer());
        buf.writeInt(health);
        ServerPlayNetworking.send(player, FoodPackets.S2C_HEALTH, buf);
    }

    public static void sendHealthMax(ServerPlayerEntity player, int healthMax) {
        PacketByteBuf buf = new PacketByteBuf(io.netty.buffer.Unpooled.buffer());
        buf.writeInt(healthMax);
        ServerPlayNetworking.send(player, FoodPackets.S2C_HEALTH_MAX, buf);
    }

    public static List<String> readList(PacketByteBuf buf) {
        List<String> list = new ArrayList<>();
        buf.readCollection(ArrayList::new, PacketByteBuf::readString).forEach(list::add);
        return list;
    }
}
