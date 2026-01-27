package com.sol2f.network.payload;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;


public record S2CHealthPayload(int health) implements CustomPayload {
    public static final Identifier ID = Identifier.of("sol2f", "s2c_health");
    public static final CustomPayload.Id<S2CHealthPayload> PACKET_ID = new CustomPayload.Id<>(ID);
    public static final PacketCodec<PacketByteBuf, S2CHealthPayload> CODEC = PacketCodec.tuple(PacketCodecs.VAR_INT, S2CHealthPayload::health, S2CHealthPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }
}
