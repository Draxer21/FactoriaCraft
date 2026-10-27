package com.factorycraft;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** Server → client: what the open storage terminal's network holds. */
public record TerminalListPayload(int inventories, List<Entry> entries) implements CustomPacketPayload {
    public record Entry(ItemStack stack, long count) {
        @Override
        public int hashCode() {
            return ItemStack.hashItemAndComponents(stack) * 31 + Long.hashCode(count);
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Entry e && e.count == count && ItemStack.isSameItemSameComponents(e.stack, stack);
        }
    }

    public static final CustomPacketPayload.Type<TerminalListPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(FactoryCraft.MOD_ID, "terminal_list"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalListPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.inventories());
                buf.writeVarInt(payload.entries().size());
                for (Entry entry : payload.entries()) {
                    ItemStack.STREAM_CODEC.encode(buf, entry.stack());
                    buf.writeVarLong(entry.count());
                }
            },
            buf -> {
                int inventories = buf.readVarInt();
                int size = Math.min(buf.readVarInt(), 4096);
                List<Entry> entries = new ArrayList<>(size);
                for (int i = 0; i < size; i++) entries.add(new Entry(ItemStack.STREAM_CODEC.decode(buf), buf.readVarLong()));
                return new TerminalListPayload(inventories, entries);
            });

    /** Last list received (client). */
    static volatile TerminalListPayload latest = new TerminalListPayload(0, List.of());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
