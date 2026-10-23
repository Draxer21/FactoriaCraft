package com.factorycraft;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Sent by the client when the driver presses the headlight key (AUTO -> ON -> OFF). */
public record ToggleLightsPayload() implements CustomPacketPayload {
    public static final ToggleLightsPayload INSTANCE = new ToggleLightsPayload();
    public static final CustomPacketPayload.Type<ToggleLightsPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(FactoryCraft.MOD_ID, "toggle_lights"));
    public static final StreamCodec<ByteBuf, ToggleLightsPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(ToggleLightsPayload payload, IPayloadContext context) {
        var player = context.player();
        if (player.getVehicle() instanceof VehicleEngine.Holder holder
                && player.getVehicle().getControllingPassenger() == player) {
            holder.engine().cycleLights(player);
        }
    }
}
