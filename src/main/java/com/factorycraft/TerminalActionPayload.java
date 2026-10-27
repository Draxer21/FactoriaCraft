package com.factorycraft;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client → server: a click on the storage terminal's grid. */
public record TerminalActionPayload(int action, ItemStack stack, boolean single, boolean toInventory) implements CustomPacketPayload {
    public static final int EXTRACT = 0;
    public static final int INSERT_CARRIED = 1;

    public static final CustomPacketPayload.Type<TerminalActionPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(FactoryCraft.MOD_ID, "terminal_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalActionPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.action());
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, payload.stack());
                buf.writeBoolean(payload.single());
                buf.writeBoolean(payload.toInventory());
            },
            buf -> new TerminalActionPayload(buf.readVarInt(), ItemStack.OPTIONAL_STREAM_CODEC.decode(buf), buf.readBoolean(), buf.readBoolean()));

    public static void handle(TerminalActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof StorageTerminalMenu menu) {
                menu.handle(payload);
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
