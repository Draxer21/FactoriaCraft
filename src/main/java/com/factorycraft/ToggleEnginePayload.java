package com.factorycraft;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Sent by the client when the driver presses the engine key. */
public record ToggleEnginePayload() implements CustomPacketPayload {
    public static final ToggleEnginePayload INSTANCE = new ToggleEnginePayload();
    public static final CustomPacketPayload.Type<ToggleEnginePayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(FactoryCraft.MOD_ID, "toggle_engine"));
    public static final StreamCodec<ByteBuf, ToggleEnginePayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void handle(ToggleEnginePayload payload, IPayloadContext context) {
        var player = context.player();
        if (player.getVehicle() instanceof VehicleEngine.Holder holder
                && player.getVehicle().getControllingPassenger() == player) {
            holder.engine().toggle(player);
        }
    }

    @EventBusSubscriber(modid = FactoryCraft.MOD_ID)
    public static final class Registration {
        private Registration() {}

        @SubscribeEvent
        public static void register(RegisterPayloadHandlersEvent event) {
            event.registrar("1")
                    .playToServer(TYPE, STREAM_CODEC, ToggleEnginePayload::handle)
                    .playToServer(ToggleLightsPayload.TYPE, ToggleLightsPayload.STREAM_CODEC, ToggleLightsPayload::handle)
                    .playToServer(ToggleToolPayload.TYPE, ToggleToolPayload.STREAM_CODEC, ToggleToolPayload::handle)
                    .playToServer(JetpackPayload.TYPE, JetpackPayload.STREAM_CODEC, JetpackPayload::handle)
                    .playToClient(StationDataPayload.TYPE, StationDataPayload.STREAM_CODEC)
                    .playToClient(CraneLoadPayload.TYPE, CraneLoadPayload.STREAM_CODEC)
                    .playToServer(TerminalActionPayload.TYPE, TerminalActionPayload.STREAM_CODEC, TerminalActionPayload::handle)
                    .playToClient(TerminalListPayload.TYPE, TerminalListPayload.STREAM_CODEC)
                    .playToClient(CameraViewPayload.TYPE, CameraViewPayload.STREAM_CODEC)
                    .playToClient(PollutionPayload.TYPE, PollutionPayload.STREAM_CODEC)
                    .playToClient(StatsPayload.TYPE, StatsPayload.STREAM_CODEC)
                    .playToClient(ResearchPayload.TYPE, ResearchPayload.STREAM_CODEC)
                    .playToServer(ResearchUnlockPayload.TYPE, ResearchUnlockPayload.STREAM_CODEC, ResearchUnlockPayload::handle)
                    .playToServer(LiftPayload.TYPE, LiftPayload.STREAM_CODEC, LiftPayload::handle);
        }
    }
}
