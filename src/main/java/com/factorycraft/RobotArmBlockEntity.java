package com.factorycraft;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

/** See {@link RobotArmBlock}. Moves up to 8 items every half second, 50 energy per move. */
public final class RobotArmBlockEntity extends BlockEntity implements Energy.Consumer {
    private static final int INTERVAL = 10;
    private static final int BATCH = 8;
    private static final int ENERGY_PER_MOVE = 50;
    private static final int ACTIVE_TICKS = 6;
    private static final int MAX_ITEMS_ON_BELT = 3;

    private final Energy.Link link = new Energy.Link(this);
    private ItemStack filter = ItemStack.EMPTY;
    /** What the arm is holding (when the destination was full). */
    private ItemStack hand = ItemStack.EMPTY;
    private int activeFor;

    public RobotArmBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryCraft.ROBOT_ARM_BLOCK_ENTITY.value(), pos, state);
    }

    public ItemStack getFilter() {
        return filter;
    }

    public void setFilter(ItemStack stack) {
        filter = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        setChanged();
    }

    private boolean wants(ItemStack stack) {
        return !stack.isEmpty() && (filter.isEmpty() || ItemStack.isSameItem(stack, filter));
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, RobotArmBlockEntity arm) {
        if (arm.activeFor > 0 && --arm.activeFor == 0) {
            level.setBlock(pos, state.setValue(RobotArmBlock.ACTIVE, false), Block.UPDATE_CLIENTS);
        }
        if (level.getGameTime() % INTERVAL != 0) return;
        Direction front = state.getValue(RobotArmBlock.FACING);
        BlockPos from = pos.relative(front.getOpposite());
        BlockPos to = pos.relative(front);

        if (arm.hand.isEmpty()) {
            if (arm.link.draw(level, ENERGY_PER_MOVE) < ENERGY_PER_MOVE) return;
            arm.hand = arm.pick(level, from, front);
            if (arm.hand.isEmpty()) return;
        }
        arm.hand = arm.drop(level, to, front, arm.hand);
        arm.setChanged();
        arm.activeFor = ACTIVE_TICKS;
        if (!state.getValue(RobotArmBlock.ACTIVE)) {
            level.setBlock(pos, state.setValue(RobotArmBlock.ACTIVE, true), Block.UPDATE_CLIENTS);
        }
    }

    /** Takes up to a batch from behind: a container's slots, or loose items (on a belt). */
    private ItemStack pick(Level level, BlockPos from, Direction front) {
        Container container = HopperBlockEntity.getContainerAt(level, from);
        if (container != null) {
            Direction side = front; // we take from the container's face that looks at us
            for (int slot : slots(container, side)) {
                ItemStack stack = container.getItem(slot);
                if (!wants(stack)) continue;
                if (container instanceof WorldlyContainer worldly && !worldly.canTakeItemThroughFace(slot, stack, side)) continue;
                ItemStack taken = container.removeItem(slot, Math.min(BATCH, stack.getCount()));
                container.setChanged();
                return taken;
            }
            return ItemStack.EMPTY;
        }
        List<ItemEntity> loose = level.getEntitiesOfClass(ItemEntity.class, new AABB(from).inflate(0.0, 0.5, 0.0),
                e -> e.isAlive() && wants(e.getItem()));
        if (loose.isEmpty()) return ItemStack.EMPTY;
        ItemEntity entity = loose.getFirst();
        ItemStack stack = entity.getItem();
        ItemStack taken = stack.split(Math.min(BATCH, stack.getCount()));
        if (stack.isEmpty()) entity.discard();
        else entity.setItem(stack);
        return taken;
    }

    private static int[] slots(Container container, Direction side) {
        if (container instanceof WorldlyContainer worldly) return worldly.getSlotsForFace(side);
        int[] all = new int[container.getContainerSize()];
        for (int i = 0; i < all.length; i++) all[i] = i;
        return all;
    }

    /** Puts the stack in front; returns what didn't fit (kept in the hand). */
    private ItemStack drop(Level level, BlockPos to, Direction front, ItemStack stack) {
        Container container = HopperBlockEntity.getContainerAt(level, to);
        if (container != null) return HopperBlockEntity.addItem(null, container, stack, front.getOpposite());
        BlockState target = level.getBlockState(to);
        if (!target.getCollisionShape(level, to).isEmpty() && !(target.getBlock() instanceof ConveyorBlock)) return stack;
        AABB space = new AABB(to).inflate(0.0, 0.5, 0.0);
        if (level.getEntitiesOfClass(ItemEntity.class, space).size() >= MAX_ITEMS_ON_BELT) return stack;
        ItemEntity item = new ItemEntity(level, to.getX() + 0.5, to.getY() + 0.3, to.getZ() + 0.5, stack, 0.0, 0.0, 0.0);
        level.addFreshEntity(item);
        return ItemStack.EMPTY;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        filter = input.read("Filter", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        hand = input.read("Hand", ItemStack.CODEC).orElse(ItemStack.EMPTY);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!filter.isEmpty()) output.store("Filter", ItemStack.CODEC, filter);
        if (!hand.isEmpty()) output.store("Hand", ItemStack.CODEC, hand);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null && !hand.isEmpty()) {
            net.minecraft.world.Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, hand);
        }
    }

    @Override
    public Energy.Link energyLink() {
        return link;
    }
}
