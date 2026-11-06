package com.factorycraft;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client → server: set (or clear) one ghost slot of the open assembler pattern or bus filter. */
public record AssemblerPatternPayload(int slot, ItemStack stack) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<AssemblerPatternPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(FactoryCraft.MOD_ID, "assembler_pattern"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AssemblerPatternPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.slot());
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, payload.stack());
            },
            buf -> new AssemblerPatternPayload(buf.readVarInt(), ItemStack.OPTIONAL_STREAM_CODEC.decode(buf)));

    public static void handle(AssemblerPatternPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof GhostMenu menu) {
                menu.setGhost(payload.slot(), payload.stack());
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
