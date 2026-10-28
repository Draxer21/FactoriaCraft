package com.factorycraft;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client → server: spend points on a research node. */
public record ResearchUnlockPayload(String id) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ResearchUnlockPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(FactoryCraft.MOD_ID, "research_unlock"));
    public static final StreamCodec<ByteBuf, ResearchUnlockPayload> STREAM_CODEC =
            ByteBufCodecs.STRING_UTF8.map(ResearchUnlockPayload::new, ResearchUnlockPayload::id);

    public static void handle(ResearchUnlockPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        player.sendOverlayMessage(Research.unlock(player.level(), payload.id()));
        ResearchBlocks.sendTree(player);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
