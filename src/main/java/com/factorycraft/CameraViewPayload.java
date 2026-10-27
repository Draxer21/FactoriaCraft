package com.factorycraft;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server → client: watch these security cameras. */
public record CameraViewPayload(List<View> views) implements CustomPacketPayload {
    public record View(BlockPos pos, float yaw) {}

    public static final CustomPacketPayload.Type<CameraViewPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(FactoryCraft.MOD_ID, "camera_view"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CameraViewPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.views().size());
                for (View view : payload.views()) {
                    buf.writeBlockPos(view.pos());
                    buf.writeFloat(view.yaw());
                }
            },
            buf -> {
                int size = Math.min(buf.readVarInt(), MonitorBlockEntity.MAX_CAMERAS);
                List<View> views = new ArrayList<>(size);
                for (int i = 0; i < size; i++) views.add(new View(buf.readBlockPos(), buf.readFloat()));
                return new CameraViewPayload(views);
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
