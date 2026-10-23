package com.factorycraft;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server → client: the machine list shown by the control station. */
public record StationDataPayload(List<Line> lines) implements CustomPacketPayload {
    /**
     * One machine. {@code name} is a translation key; {@code level} is fuel or
     * stored energy (0-100); {@code fill} is cargo fill (0-100, or -1).
     */
    public record Line(String name, int x, int y, int z, int level, boolean on, int fill) {}

    public static final CustomPacketPayload.Type<StationDataPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(FactoryCraft.MOD_ID, "station_data"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StationDataPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.lines().size());
                for (Line line : payload.lines()) {
                    buf.writeUtf(line.name());
                    buf.writeVarInt(line.x());
                    buf.writeVarInt(line.y());
                    buf.writeVarInt(line.z());
                    buf.writeVarInt(line.level());
                    buf.writeBoolean(line.on());
                    buf.writeVarInt(line.fill());
                }
            },
            buf -> {
                int size = Math.min(buf.readVarInt(), 256);
                List<Line> lines = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    lines.add(new Line(buf.readUtf(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                            buf.readVarInt(), buf.readBoolean(), buf.readVarInt()));
                }
                return new StationDataPayload(lines);
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
