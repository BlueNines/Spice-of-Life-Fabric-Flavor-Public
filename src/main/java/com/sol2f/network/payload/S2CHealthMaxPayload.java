package com.sol2f.network.payload;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;


public record S2CHealthMaxPayload(int maxHealth) implements CustomPayload {
    public static final Identifier ID = Identifier.of("sol2f", "s2c_health_max");
    public static final CustomPayload.Id<S2CHealthMaxPayload> PACKET_ID = new CustomPayload.Id<>(ID);
    public static final PacketCodec<PacketByteBuf, S2CHealthMaxPayload> CODEC = PacketCodec.tuple(PacketCodecs.VAR_INT, S2CHealthMaxPayload::maxHealth, S2CHealthMaxPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }
}
