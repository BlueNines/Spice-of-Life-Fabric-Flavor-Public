package com.sol2f.network.payload;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.List;


public record S2CFoodListPayload(List<String> foods) implements CustomPayload {
    public static final Identifier ID = Identifier.of("sol2f", "s2c_food_list");
    public static final CustomPayload.Id<S2CFoodListPayload> PACKET_ID = new CustomPayload.Id<>(ID);
    public static final PacketCodec<PacketByteBuf, S2CFoodListPayload> CODEC = PacketCodec.tuple(PacketCodecs.STRING.collect(PacketCodecs.toList()), S2CFoodListPayload::foods, S2CFoodListPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }
}
