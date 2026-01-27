package com.sol2f.network.payload;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;


public record C2SRequestAllFoodListPayload() implements CustomPayload {
    public static final Identifier ID = Identifier.of("sol2f", "c2s_request_all_food_list");
    public static final CustomPayload.Id<C2SRequestAllFoodListPayload> PACKET_ID = new CustomPayload.Id<>(ID);
    public static final PacketCodec<PacketByteBuf, C2SRequestAllFoodListPayload> CODEC = PacketCodec.unit(new C2SRequestAllFoodListPayload());

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }
}
