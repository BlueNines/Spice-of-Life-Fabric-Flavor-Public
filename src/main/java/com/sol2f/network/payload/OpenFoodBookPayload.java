package com.sol2f.network.payload;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;


public record OpenFoodBookPayload() implements CustomPayload {
    public static final Identifier ID = Identifier.of("sol2f", "open_food_book_screen");
    public static final CustomPayload.Id<OpenFoodBookPayload> PACKET_ID = new CustomPayload.Id<>(ID);
    public static final PacketCodec<PacketByteBuf, OpenFoodBookPayload> CODEC = PacketCodec.unit(new OpenFoodBookPayload());

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }
}
