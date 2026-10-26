package com.factorycraft;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server → client: what a crane carries, for drawing it. Up to 20×20×20 blocks, too
 * much for synced entity data, so it travels in its own packet: per block its offset
 * (three bytes) and its block-state id.
 */
public record CraneLoadPayload(int entityId, int[] packed) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<CraneLoadPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(FactoryCraft.MOD_ID, "crane_load"));

    /** Most blocks a load can have (20³). */
    public static final int MAX_PIECES = 8000;

    public static final StreamCodec<RegistryFriendlyByteBuf, CraneLoadPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.entityId());
                int pieces = payload.packed().length / 4;
                buf.writeVarInt(pieces);
                for (int i = 0; i < pieces; i++) {
                    buf.writeByte(payload.packed()[i * 4]);
                    buf.writeByte(payload.packed()[i * 4 + 1]);
                    buf.writeByte(payload.packed()[i * 4 + 2]);
                    buf.writeVarInt(payload.packed()[i * 4 + 3]);
                }
            },
            buf -> {
                int id = buf.readVarInt();
                int pieces = Math.min(buf.readVarInt(), MAX_PIECES);
                int[] packed = new int[pieces * 4];
                for (int i = 0; i < pieces; i++) {
                    packed[i * 4] = buf.readByte();
                    packed[i * 4 + 1] = buf.readByte();
                    packed[i * 4 + 2] = buf.readByte();
                    packed[i * 4 + 3] = buf.readVarInt();
                }
                return new CraneLoadPayload(id, packed);
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
