package com.factorycraft;

import java.util.EnumSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;

/**
 * Locomotive. On rails it steers itself along the track (curves and slopes
 * included) and runs fast; off rails it drives slowly like the other machines.
 * With rails in its bay it lays track in front of itself as it goes, so you can
 * build a line just by driving. Cargo wagons hitch behind it like behind the
 * tunneler, so it can haul a long train.
 */
public final class LocomotiveEntity extends WorkVehicleEntity {
    /** Rear coupler distance from the centre (blocks). */
    public static final double REAR_HITCH_OFFSET = 3.1;
    private static final float TURN_RATE = 9.0F;

    public LocomotiveEntity(EntityType<? extends LocomotiveEntity> type, Level level) {
        super(type, level, () -> FactoryCraft.LOCOMOTIVE_ITEM.value());
    }

    @Override protected double maxSpeed() { return onRails() ? 0.55 : 0.22; }
    @Override protected double reverseSpeed() { return onRails() ? 0.25 : 0.12; }
    @Override protected double seatHeight() { return 1.69; }
    @Override protected double seatForward() { return -1.83; }
    @Override protected double hullShift() { return 0.23; }
    @Override protected double hullHalfLength() { return 3.14; }
    @Override protected double hullHalfWidth() { return 1.25; }

    // ---------------------------------------------------------------- rails

    private BlockPos railPos() {
        return BlockPos.containing(getX(), getY() + 0.3, getZ());
    }

    private RailShape railShape() {
        for (BlockPos pos : new BlockPos[] {railPos(), railPos().below()}) {
            BlockState state = level().getBlockState(pos);
            if (state.getBlock() instanceof BaseRailBlock rail) return state.getValue(rail.getShapeProperty());
        }
        return null;
    }

    public boolean onRails() {
        return railShape() != null;
    }

    private static Set<Direction> ends(RailShape shape) {
        return switch (shape) {
            case NORTH_SOUTH, ASCENDING_NORTH, ASCENDING_SOUTH -> EnumSet.of(Direction.NORTH, Direction.SOUTH);
            case EAST_WEST, ASCENDING_EAST, ASCENDING_WEST -> EnumSet.of(Direction.EAST, Direction.WEST);
            case SOUTH_EAST -> EnumSet.of(Direction.SOUTH, Direction.EAST);
            case SOUTH_WEST -> EnumSet.of(Direction.SOUTH, Direction.WEST);
            case NORTH_WEST -> EnumSet.of(Direction.NORTH, Direction.WEST);
            case NORTH_EAST -> EnumSet.of(Direction.NORTH, Direction.EAST);
        };
    }

    @Override
    public void setInput(boolean left, boolean right, boolean up, boolean down) {
        // On rails the track steers: A / D do nothing.
        boolean rails = onRails();
        super.setInput(left && !rails, right && !rails, up, down);
    }

    @Override
    protected boolean autopilotSteers() {
        return !onRails();
    }

    @Override
    public void tick() {
        if (isLocalInstanceAuthoritative()) followRails();
        super.tick();
    }

    /** Turns the locomotive along the rail under it and keeps it centred on the track. */
    private void followRails() {
        RailShape shape = railShape();
        if (shape == null) return;
        Set<Direction> ends = ends(shape);
        Direction heading = Direction.fromYRot(getYRot());
        Direction exit;
        if (ends.contains(heading) && ends.contains(heading.getOpposite())) {
            exit = heading;
        } else if (ends.contains(heading.getOpposite())) {
            exit = otherEnd(ends, heading.getOpposite());
        } else if (ends.contains(heading)) {
            exit = heading;
        } else {
            // Arriving sideways into a curve: take the end closest to our heading.
            exit = closest(ends, getYRot());
        }
        float target = exit.toYRot();
        setYRot(Mth.approachDegrees(getYRot(), target, TURN_RATE));

        // Pull towards the middle of the track, across the direction of travel.
        BlockPos rail = railPos();
        double cx = rail.getX() + 0.5;
        double cz = rail.getZ() + 0.5;
        double nx = getX();
        double nz = getZ();
        if (exit.getAxis() == Direction.Axis.Z) nx += (cx - getX()) * 0.25;
        else nz += (cz - getZ()) * 0.25;
        if (Math.abs(nx - getX()) > 1.0E-3 || Math.abs(nz - getZ()) > 1.0E-3) setPos(nx, getY(), nz);
    }

    private static Direction otherEnd(Set<Direction> ends, Direction known) {
        for (Direction dir : ends) {
            if (dir != known) return dir;
        }
        return known;
    }

    private static Direction closest(Set<Direction> ends, float yaw) {
        Direction best = null;
        float bestDiff = Float.MAX_VALUE;
        for (Direction dir : ends) {
            float diff = Math.abs(Mth.degreesDifference(yaw, dir.toYRot()));
            if (diff < bestDiff) {
                bestDiff = diff;
                best = dir;
            }
        }
        return best;
    }

    // ---------------------------------------------------------------- laying track

    @Override
    protected void work(ServerLevel level, Player driver) {
        BlockPos ahead = columnAt(hullFront() + 0.5, 0, groundY());
        BlockState there = level.getBlockState(ahead);
        if (there.getBlock() instanceof BaseRailBlock) return;
        if (!(there.isAir() || there.canBeReplaced()) || !there.getFluidState().isEmpty()) return;
        if (!level.getBlockState(ahead.below()).isFaceSturdy(level, ahead.below(), Direction.UP)) return;
        ItemStack rail = takeOne(stack -> stack.getItem() instanceof BlockItem item && item.getBlock() instanceof BaseRailBlock);
        if (rail.isEmpty()) return;
        Block block = ((BlockItem) rail.getItem()).getBlock();
        level.setBlock(ahead, block.defaultBlockState(), Block.UPDATE_ALL);
    }

    // ---------------------------------------------------------------- train

    @Override
    public boolean canCollideWith(Entity other) {
        return !(VehicleHitbox.owner(other) instanceof CargoWagonEntity) && super.canCollideWith(other);
    }

    @Override
    public void push(Entity entity) {
        if (entity instanceof CargoWagonEntity) return;
        super.push(entity);
    }
}
