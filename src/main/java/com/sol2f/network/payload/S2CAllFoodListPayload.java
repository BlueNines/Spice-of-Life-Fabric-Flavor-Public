package com.sol2f.network.payload;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.List;


public record S2CAllFoodListPayload(List<String> allFoods) implements CustomPayload {
    public static final Identifier ID = Identifier.of("sol2f", "s2c_all_food_list");
    public static final CustomPayload.Id<S2CAllFoodListPayload> PACKET_ID = new CustomPayload.Id<>(ID);
    public static final PacketCodec<PacketByteBuf, S2CAllFoodListPayload> CODEC = PacketCodec.tuple(PacketCodecs.STRING.collect(PacketCodecs.toList()), S2CAllFoodListPayload::allFoods, S2CAllFoodListPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }
}
