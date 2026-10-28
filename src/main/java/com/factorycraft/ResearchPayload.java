package com.factorycraft;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server → client: the world's research (opens or refreshes the tree screen). */
public record ResearchPayload(int points, List<String> unlocked, boolean required) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ResearchPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(FactoryCraft.MOD_ID, "research"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ResearchPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.points());
                buf.writeVarInt(payload.unlocked().size());
                for (String id : payload.unlocked()) buf.writeUtf(id);
                buf.writeBoolean(payload.required());
            },
            buf -> {
                int points = buf.readVarInt();
                int n = Math.min(buf.readVarInt(), 256);
                List<String> unlocked = new ArrayList<>(n);
                for (int i = 0; i < n; i++) unlocked.add(buf.readUtf());
                return new ResearchPayload(points, unlocked, buf.readBoolean());
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
