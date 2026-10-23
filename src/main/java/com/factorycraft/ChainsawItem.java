package com.factorycraft;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Chainsaw: an axe that fells the whole tree when you cut a log of it.
 * Only natural trees (logs touching natural leaves) are felled, so wooden
 * buildings are safe; sneak to cut a single block. Each extra log costs one
 * durability, and it stops before the chainsaw would break.
 */
public final class ChainsawItem extends AxeItem {
    private static final int MAX_LOGS = 256;
    private final boolean industrial;

    public ChainsawItem(ToolMaterial material, float damage, float speed, Properties properties) {
        this(material, damage, speed, properties, false);
    }

    /** {@code industrial}: the Mechanic's version, fells giant trees (1024 logs) and clears their leaves too. */
    public ChainsawItem(ToolMaterial material, float damage, float speed, Properties properties, boolean industrial) {
        super(material, damage, speed, properties);
        this.industrial = industrial;
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity owner) {
        boolean result = super.mineBlock(stack, level, state, pos, owner);
        if (level.isClientSide() || !state.is(BlockTags.LOGS) || !(owner instanceof Player player) || player.isShiftKeyDown()) {
            return result;
        }
        List<BlockPos> logs = new ArrayList<>();
        List<BlockPos> leaves = new ArrayList<>();
        if (!findTree(level, pos, logs, leaves, industrial ? MAX_LOGS * 4 : MAX_LOGS)) return result;
        if (industrial) logs.addAll(leaves);
        for (BlockPos log : logs) {
            if (stack.isEmpty() || stack.getMaxDamage() - stack.getDamageValue() <= 1) break;
            boolean isLog = level.getBlockState(log).is(BlockTags.LOGS);
            if (level.destroyBlock(log, true, player, 512) && isLog) {
                stack.hurtAndBreak(1, owner, EquipmentSlot.MAINHAND);
            }
        }
        return result;
    }

    /** Logs connected to {@code start} (diagonals included, not below the cut); true if it's a natural tree. */
    private static boolean findTree(Level level, BlockPos start, List<BlockPos> logs, List<BlockPos> leaves, int maxLogs) {
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        seen.add(start);
        queue.add(start);
        boolean naturalLeaves = false;
        while (!queue.isEmpty() && logs.size() < maxLogs) {
            BlockPos pos = queue.poll();
            for (BlockPos next : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
                if (next.getY() < start.getY() || seen.contains(next)) continue;
                BlockState state = level.getBlockState(next);
                if (state.is(BlockTags.LOGS)) {
                    BlockPos immutable = next.immutable();
                    seen.add(immutable);
                    logs.add(immutable);
                    queue.add(immutable);
                } else if (state.is(BlockTags.LEAVES) && !state.getValue(LeavesBlock.PERSISTENT)) {
                    naturalLeaves = true;
                    BlockPos immutable = next.immutable();
                    if (seen.add(immutable) && leaves.size() < maxLogs * 3) leaves.add(immutable);
                }
            }
        }
        // Fell from the top down so nothing is left floating mid-way.
        logs.sort((a, b) -> Integer.compare(b.getY(), a.getY()));
        return naturalLeaves;
    }
}
