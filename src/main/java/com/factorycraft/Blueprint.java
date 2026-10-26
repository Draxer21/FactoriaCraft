package com.factorycraft;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** A copied structure, stored on the blueprint item: block states relative to its lowest corner. */
public record Blueprint(int sizeX, int sizeY, int sizeZ, List<BlockPos> positions, List<BlockState> states) {
    public static final int MAX_SIDE = 32;
    public static final int MAX_BLOCKS = 8192;

    public int count() {
        return positions.size();
    }

    /** Copies the box between two corners (air and fluids left out), or null if it's too big. */
    static @Nullable Blueprint capture(Level level, BlockPos a, BlockPos b) {
        BlockPos min = new BlockPos(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()));
        BlockPos max = new BlockPos(Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ()));
        int sx = max.getX() - min.getX() + 1;
        int sy = max.getY() - min.getY() + 1;
        int sz = max.getZ() - min.getZ() + 1;
        if (sx > MAX_SIDE || sy > MAX_SIDE || sz > MAX_SIDE) return null;
        List<BlockPos> positions = new ArrayList<>();
        List<BlockState> states = new ArrayList<>();
        for (int y = 0; y < sy; y++) {
            for (int z = 0; z < sz; z++) {
                for (int x = 0; x < sx; x++) {
                    BlockState state = level.getBlockState(min.offset(x, y, z));
                    if (state.isAir() || state.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock) continue;
                    if (positions.size() >= MAX_BLOCKS) return null;
                    positions.add(new BlockPos(x, y, z));
                    states.add(state);
                }
            }
        }
        return new Blueprint(sx, sy, sz, positions, states);
    }

    // ---------------------------------------------------------------- item storage

    void writeTo(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        ListTag palette = new ListTag();
        Map<BlockState, Integer> index = new HashMap<>();
        int[] packed = new int[positions.size()];
        for (int i = 0; i < positions.size(); i++) {
            BlockState state = states.get(i);
            Integer id = index.get(state);
            if (id == null) {
                id = palette.size();
                index.put(state, id);
                palette.add(NbtUtils.writeBlockState(state));
            }
            BlockPos p = positions.get(i);
            packed[i] = p.getX() | (p.getY() << 5) | (p.getZ() << 10) | (id << 15);
        }
        tag.put("Palette", palette);
        tag.putIntArray("Blocks", packed);
        tag.putIntArray("Size", new int[] {sizeX, sizeY, sizeZ});
        tag.remove("CornerA");
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    static @Nullable Blueprint readFrom(ItemStack stack, Level level) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return null;
        CompoundTag tag = data.copyTag();
        int[] size = tag.getIntArray("Size").orElse(null);
        int[] packed = tag.getIntArray("Blocks").orElse(null);
        if (size == null || size.length != 3 || packed == null) return null;
        HolderGetter<Block> blocks = level.registryAccess().lookupOrThrow(Registries.BLOCK);
        ListTag palette = tag.getListOrEmpty("Palette");
        List<BlockState> paletteStates = new ArrayList<>();
        for (Tag entry : palette) {
            paletteStates.add(entry instanceof CompoundTag compound ? NbtUtils.readBlockState(blocks, compound)
                    : net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        }
        List<BlockPos> positions = new ArrayList<>(packed.length);
        List<BlockState> states = new ArrayList<>(packed.length);
        for (int value : packed) {
            int id = value >>> 15;
            if (id >= paletteStates.size()) continue;
            positions.add(new BlockPos(value & 31, (value >> 5) & 31, (value >> 10) & 31));
            states.add(paletteStates.get(id));
        }
        return new Blueprint(size[0], size[1], size[2], positions, states);
    }

    /** Size and block count for tooltips, without parsing the palette. */
    static int @Nullable [] summary(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return null;
        CompoundTag tag = data.copyTag();
        int[] size = tag.getIntArray("Size").orElse(null);
        int[] packed = tag.getIntArray("Blocks").orElse(null);
        if (size == null || size.length != 3 || packed == null) return null;
        return new int[] {size[0], size[1], size[2], packed.length};
    }
}
