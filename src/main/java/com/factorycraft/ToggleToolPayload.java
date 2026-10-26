package com.factorycraft;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Sent by the client when the driver presses the tool key: drill / saw on or off. */
public record ToggleToolPayload() implements CustomPacketPayload {
    public static final ToggleToolPayload INSTANCE = new ToggleToolPayload();
    public static final CustomPacketPayload.Type<ToggleToolPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(FactoryCraft.MOD_ID, "toggle_tool"));
    public static final StreamCodec<ByteBuf, ToggleToolPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(ToggleToolPayload payload, IPayloadContext context) {
        var player = context.player();
        var vehicle = player.getVehicle();
        if (vehicle instanceof ToolToggle tool && tool.hasTool() && vehicle.getControllingPassenger() == player) {
            tool.toggleTool(player);
        }
    }
}
