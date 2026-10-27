package com.factorycraft;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** Server → client: production statistics for the statistics terminal. */
public record StatsPayload(int[] total, List<Line> lines) implements CustomPacketPayload {
    public record Line(ItemStack stack, int[] perMinute, long sum) {}

    public static final CustomPacketPayload.Type<StatsPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(FactoryCraft.MOD_ID, "production_stats"));

    private static void writeInts(RegistryFriendlyByteBuf buf, int[] values) {
        buf.writeVarInt(values.length);
        for (int v : values) buf.writeVarInt(v);
    }

    private static int[] readInts(RegistryFriendlyByteBuf buf) {
        int n = Math.min(buf.readVarInt(), 120);
        int[] out = new int[n];
        for (int i = 0; i < n; i++) out[i] = buf.readVarInt();
        return out;
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, StatsPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                writeInts(buf, payload.total());
                buf.writeVarInt(payload.lines().size());
                for (Line line : payload.lines()) {
                    ItemStack.STREAM_CODEC.encode(buf, line.stack());
                    writeInts(buf, line.perMinute());
                    buf.writeVarLong(line.sum());
                }
            },
            buf -> {
                int[] total = readInts(buf);
                int n = Math.min(buf.readVarInt(), 32);
                List<Line> lines = new ArrayList<>(n);
                for (int i = 0; i < n; i++) lines.add(new Line(ItemStack.STREAM_CODEC.decode(buf), readInts(buf), buf.readVarLong()));
                return new StatsPayload(total, lines);
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
