package com.factorycraft;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * Digital storage: a storage terminal plus every chest, barrel, industrial chest or
 * other mod's inventory touching it or touching the data cables leading from it.
 * Lists everything as one big inventory, and puts in / takes out across all of them.
 * Plain storage is filled first; machines only get what nothing else takes.
 */
public final class StorageNetwork {
    private static final int MAX_CABLES = 512;

    /** One inventory of the network. */
    private record Member(BlockPos pos, ResourceHandler<ItemResource> handler, boolean machine) {}

    private final List<Member> members = new ArrayList<>();

    private StorageNetwork() {}

    private static boolean isLink(Level level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof DataCableBlock
                || level.getBlockEntity(pos) instanceof StorageTerminalBlockEntity;
    }

    /** Scans the network from a terminal (or any cable). */
    public static StorageNetwork scan(Level level, BlockPos start) {
        StorageNetwork network = new StorageNetwork();
        Set<BlockPos> seen = new HashSet<>();
        Set<BlockPos> inventories = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        seen.add(start);
        queue.add(start);
        while (!queue.isEmpty() && seen.size() < MAX_CABLES) {
            BlockPos pos = queue.poll();
            for (Direction dir : Direction.values()) {
                BlockPos next = pos.relative(dir);
                if (!level.isLoaded(next)) continue;
                if (isLink(level, next)) {
                    if (seen.add(next)) queue.add(next);
                    continue;
                }
                if (inventories.contains(next)) continue;
                BlockEntity be = level.getBlockEntity(next);
                if (be == null || be instanceof DronePortBlockEntity) continue;
                ResourceHandler<ItemResource> handler = level.getCapability(Capabilities.Item.BLOCK, next, dir.getOpposite());
                if (handler == null || handler.size() == 0) continue;
                inventories.add(next);
                boolean machine = be instanceof MachineBlockEntity && !(be instanceof IndustrialChestBlockEntity);
                network.members.add(new Member(next.immutable(), handler, machine));
            }
        }
        // Storage first, machines last.
        network.members.sort((a, b) -> Boolean.compare(a.machine(), b.machine()));
        return network;
    }

    public int inventories() {
        return members.size();
    }

    /** Everything in the network, summed per item type (order: first seen). */
    public Map<ItemResource, Long> contents() {
        Map<ItemResource, Long> all = new LinkedHashMap<>();
        for (Member member : members) {
            ResourceHandler<ItemResource> handler = member.handler();
            for (int i = 0; i < handler.size(); i++) {
                ItemResource resource = handler.getResource(i);
                long amount = handler.getAmountAsLong(i);
                if (resource.isEmpty() || amount <= 0) continue;
                all.merge(resource, amount, Long::sum);
            }
        }
        return all;
    }

    public long count(ItemResource resource) {
        long total = 0;
        for (Member member : members) {
            ResourceHandler<ItemResource> handler = member.handler();
            for (int i = 0; i < handler.size(); i++) {
                if (handler.getResource(i).equals(resource)) total += handler.getAmountAsLong(i);
            }
        }
        return total;
    }

    /** Puts {@code stack} in; returns what didn't fit. Inventories already holding the item come first. */
    public ItemStack insert(ItemStack stack) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemResource resource = ItemResource.of(stack);
        int left = stack.getCount();
        for (int pass = 0; pass < 2 && left > 0; pass++) {
            for (Member member : members) {
                if (left <= 0) break;
                boolean holds = false;
                ResourceHandler<ItemResource> handler = member.handler();
                for (int i = 0; i < handler.size() && !holds; i++) holds = handler.getResource(i).equals(resource);
                if (pass == 0 && !holds) continue;
                if (pass == 1 && holds) continue;
                try (Transaction transaction = Transaction.openRoot()) {
                    int moved = handler.insert(resource, left, transaction);
                    if (moved > 0) {
                        transaction.commit();
                        left -= moved;
                    }
                }
            }
        }
        return left <= 0 ? ItemStack.EMPTY : resource.toStack(left);
    }

    /** Takes up to {@code amount} of {@code resource} out. */
    public ItemStack extract(ItemResource resource, int amount) {
        if (resource.isEmpty() || amount <= 0) return ItemStack.EMPTY;
        int got = 0;
        // Machines first when taking out, so their outputs get emptied.
        for (int i = members.size() - 1; i >= 0 && got < amount; i--) {
            try (Transaction transaction = Transaction.openRoot()) {
                int moved = members.get(i).handler().extract(resource, amount - got, transaction);
                if (moved > 0) {
                    transaction.commit();
                    got += moved;
                }
            }
        }
        return got == 0 ? ItemStack.EMPTY : resource.toStack(got);
    }
}
