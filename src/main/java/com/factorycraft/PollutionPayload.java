package com.factorycraft;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server → client: how polluted the air is where the player stands, and whether acid rain is falling. */
public record PollutionPayload(int air, boolean acid) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PollutionPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(FactoryCraft.MOD_ID, "pollution"));
    public static final StreamCodec<ByteBuf, PollutionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PollutionPayload::air, ByteBufCodecs.BOOL, PollutionPayload::acid, PollutionPayload::new);

    /** Last value received (client). */
    static volatile PollutionPayload latest = new PollutionPayload(0, false);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
