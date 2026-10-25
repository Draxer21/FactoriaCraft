package com.factorycraft;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Sent by the client when the player starts or stops thrusting with the jetpack. */
public record JetpackPayload(boolean thrusting) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<JetpackPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(FactoryCraft.MOD_ID, "jetpack"));
    public static final StreamCodec<ByteBuf, JetpackPayload> STREAM_CODEC =
            ByteBufCodecs.BOOL.map(JetpackPayload::new, JetpackPayload::thrusting);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(JetpackPayload payload, IPayloadContext context) {
        JetpackItem.setThrusting(context.player(), payload.thrusting());
    }
}
