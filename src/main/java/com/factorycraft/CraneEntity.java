package com.factorycraft;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Mobile crane. Drive it like the other machines; with the crane control in
 * hand (while seated) right-click a block to lift the N×N×N structure whose
 * bottom centre is that block, then right-click somewhere else to set it
 * down there. The load turns with the crane, so it can be placed rotated.
 * Right-click the air (with nothing on the hook) to switch between 1, 3 and 5.
 * Chests and other block entities travel with their contents.
 */
public class CraneEntity extends WorkVehicleEntity implements HudInfo {
    private static final EntityDataAccessor<Byte> SIZE =
            SynchedEntityData.defineId(CraneEntity.class, EntityDataSerializers.BYTE);

    /** How far the operator can aim, and how far from the crane a target may be. */
    public static final double REACH = 24.0;
    private static final double MAX_DISTANCE = 18.0;
    private static final int REMOVE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE
            | Block.UPDATE_SUPPRESS_DROPS | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS;
    private static final int PLACE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    /** Hook position in blocks: ahead of the centre and above the ground (from the model). */
    public static final float HOOK_FORWARD = 4.97F;
    public static final float HOOK_HEIGHT = 3.375F;

    /** One carried block: offset from the load's bottom centre, in the crane's frame (forward = +z). */
    public record Piece(BlockPos pos, BlockState state, Optional<CompoundTag> data) {
        public static final Codec<Piece> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockPos.CODEC.fieldOf("pos").forGetter(Piece::pos),
                BlockState.CODEC.fieldOf("state").forGetter(Piece::state),
                CompoundTag.CODEC.optionalFieldOf("data").forGetter(Piece::data)
        ).apply(i, Piece::new));
    }

    private List<Piece> load = new ArrayList<>();

    // Client side: the load as synced (CraneLoadPayload), and how many blocks it has.
    private List<Piece> clientLoad = List.of();
    private int clientLoadCount;

    public CraneEntity(EntityType<? extends CraneEntity> type, Level level) {
        this(type, level, () -> FactoryCraft.CRANE_ITEM.value());
    }

    protected CraneEntity(EntityType<? extends CraneEntity> type, Level level,
                          java.util.function.Supplier<net.minecraft.world.item.Item> drop) {
        super(type, level, drop);
    }

    // ---------------------------------------------------------------- per crane (the tower crane overrides these)

    /** How far the operator can aim. */
    public double reach() { return REACH; }

    /** How far from the crane a lift or drop may be. */
    protected double maxDistance() { return MAX_DISTANCE; }

    /** Lift sizes the operator cycles through (edge of the cube, in blocks). */
    protected int[] liftSizes() { return new int[] {1, 3, 5}; }

    protected final int maxLiftSize() {
        int[] sizes = liftSizes();
        return sizes[sizes.length - 1];
    }

    /** Hook position in blocks: ahead of the centre and above the ground (from the model). */
    public float hookForward() { return HOOK_FORWARD; }

    public float hookHeight() { return HOOK_HEIGHT; }

    /** Loads taller than this are drawn shrunk so they hang clear of the ground. */
    public float maxLoadHeight() { return 3.0F; }

    @Override protected double maxSpeed() { return 0.20; }
    @Override protected double reverseSpeed() { return 0.12; }
    @Override protected double seatHeight() { return 1.69; }
    @Override protected double seatForward() { return 0.98; }
    @Override protected double hullShift() { return -0.05; }
    @Override protected double hullHalfLength() { return 2.48; }
    @Override protected double hullHalfWidth() { return 1.60; }

    @Override
    protected void work(ServerLevel level, Player driver) {
        // The crane works with its control, not while driving.
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SIZE, (byte) 3);
    }

    public int getLiftSize() {
        return entityData.get(SIZE);
    }

    public boolean isCarrying() {
        return !load.isEmpty();
    }

    // ---------------------------------------------------------------- operating

    /** Right-click with the control while seated. {@code target} is the aimed block and face, or null for air. */
    public void operate(ServerLevel level, Player player, BlockPos target, Direction face) {
        if (!engine().isRunning()) {
            player.sendOverlayMessage(Component.translatable("message.factorycraft.engine.off"));
            return;
        }
        if (target == null) {
            if (isCarrying()) {
                player.sendOverlayMessage(Component.translatable("message.factorycraft.crane.carrying"));
                return;
            }
            int next = liftSizes()[0];
            for (int candidate : liftSizes()) {
                if (candidate > getLiftSize()) {
                    next = candidate;
                    break;
                }
            }
            entityData.set(SIZE, (byte) next);
            player.sendOverlayMessage(Component.translatable("message.factorycraft.crane.size", next, next, next));
            return;
        }
        if (Vec3.atCenterOf(target).distanceTo(position()) > maxDistance()) {
            player.sendOverlayMessage(Component.translatable("message.factorycraft.crane.too_far"));
            return;
        }
        if (isCarrying()) {
            place(level, player, target.relative(face));
        } else {
            lift(level, player, target);
        }
    }

    private void lift(ServerLevel level, Player player, BlockPos base) {
        int size = getLiftSize();
        int half = size / 2;
        int far = size - 1 - half; // even sizes reach one block less on the far side
        Rotation toCrane = rotationBetween(getDirection(), Direction.SOUTH);
        List<Piece> pieces = new ArrayList<>();
        List<BlockPos> taken = new ArrayList<>();
        for (int y = 0; y < size; y++) {
            for (int x = -half; x <= far; x++) {
                for (int z = -half; z <= far; z++) {
                    BlockPos pos = base.offset(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (!canLift(level, pos, state)) continue;
                    Optional<CompoundTag> data = Optional.empty();
                    BlockEntity be = level.getBlockEntity(pos);
                    if (be != null) data = Optional.of(be.saveWithFullMetadata(level.registryAccess()));
                    pieces.add(new Piece(new BlockPos(x, y, z).rotate(toCrane), state.rotate(toCrane), data));
                    taken.add(pos);
                }
            }
        }
        if (pieces.isEmpty()) {
            player.sendOverlayMessage(Component.translatable("message.factorycraft.crane.nothing"));
            return;
        }
        // Remove from the top down, without drops or neighbour pops.
        for (int i = taken.size() - 1; i >= 0; i--) {
            BlockPos pos = taken.get(i);
            if (level.getBlockEntity(pos) instanceof Container container) container.clearContent();
            level.removeBlockEntity(pos);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), REMOVE_FLAGS);
        }
        load = pieces;
        syncLoad();
        playWinch(level, 1.0F);
        player.sendOverlayMessage(Component.translatable("message.factorycraft.crane.lifted", pieces.size()));
    }

    private void place(ServerLevel level, Player player, BlockPos base) {
        Rotation toWorld = rotationBetween(Direction.SOUTH, getDirection());
        AABB hull = getBoundingBox();
        for (Piece piece : load) {
            BlockPos pos = base.offset(piece.pos().rotate(toWorld));
            BlockState there = level.getBlockState(pos);
            boolean free = there.isAir() || there.canBeReplaced() || there.is(FactoryCraft.VEHICLE_LIGHT.value());
            if (!free || level.isOutsideBuildHeight(pos) || hull.intersects(new AABB(pos))) {
                player.sendOverlayMessage(Component.translatable("message.factorycraft.crane.blocked"));
                return;
            }
        }
        List<Piece> ordered = new ArrayList<>(load);
        ordered.sort(Comparator.comparingInt(p -> p.pos().getY()));
        for (Piece piece : ordered) {
            BlockPos pos = base.offset(piece.pos().rotate(toWorld));
            BlockState state = piece.state().rotate(toWorld);
            level.setBlock(pos, state, PLACE_FLAGS);
            if (piece.data().isPresent()) {
                BlockEntity be = BlockEntity.loadStatic(pos, state, piece.data().get(), level.registryAccess());
                if (be != null) level.setBlockEntity(be);
            }
        }
        // Let everything settle now that the whole structure is in place.
        for (Piece piece : ordered) {
            BlockPos pos = base.offset(piece.pos().rotate(toWorld));
            level.updateNeighborsAt(pos, level.getBlockState(pos).getBlock());
        }
        int count = load.size();
        load = new ArrayList<>();
        syncLoad();
        playWinch(level, 0.85F);
        player.sendOverlayMessage(Component.translatable("message.factorycraft.crane.placed", count));
    }

    private void playWinch(ServerLevel level, float pitch) {
        Vec3 hook = position().add(forward().scale(hookForward())).add(0.0, hookHeight(), 0.0);
        level.playSound(null, hook.x, hook.y, hook.z, FactoryCraft.CRANE_WINCH.value(),
                net.minecraft.sounds.SoundSource.NEUTRAL, 1.2F, pitch);
    }

    private static boolean canLift(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.isAir() || state.getBlock() instanceof LiquidBlock) return false;
        if (state.is(FactoryCraft.VEHICLE_LIGHT.value())) return false;
        return state.getDestroySpeed(level, pos) >= 0;
    }

    /** The rotation that turns direction {@code from} into {@code to}. */
    private static Rotation rotationBetween(Direction from, Direction to) {
        for (Rotation rotation : Rotation.values()) {
            if (rotation.rotate(from) == to) return rotation;
        }
        return Rotation.NONE;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        // The operator aims through the crane: let the control item handle the click.
        if (player.getVehicle() == this && player.getItemInHand(hand).getItem() instanceof CraneControlItem) {
            return InteractionResult.PASS;
        }
        return super.interact(player, hand, location);
    }

    @Override
    public Component hudLine() {
        int size = getLiftSize();
        return clientLoadCount == 0
                ? Component.translatable("hud.factorycraft.crane_size", size, size, size)
                : Component.translatable("hud.factorycraft.crane_load", clientLoadCount);
    }

    // ---------------------------------------------------------------- client sync

    private void syncLoad() {
        if (level() instanceof ServerLevel) {
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingEntity(this, loadPayload());
        }
    }

    private CraneLoadPayload loadPayload() {
        int[] packed = new int[load.size() * 4];
        for (int i = 0; i < load.size(); i++) {
            Piece piece = load.get(i);
            packed[i * 4] = piece.pos().getX();
            packed[i * 4 + 1] = piece.pos().getY();
            packed[i * 4 + 2] = piece.pos().getZ();
            packed[i * 4 + 3] = Block.getId(piece.state());
        }
        return new CraneLoadPayload(getId(), packed);
    }

    @Override
    public void startSeenByPlayer(net.minecraft.server.level.ServerPlayer player) {
        super.startSeenByPlayer(player);
        if (!load.isEmpty()) net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, loadPayload());
    }

    /**
     * Client: a new load arrived. Only blocks with an open side are kept for drawing,
     * so a solid 20×20×20 cube draws about 2,200 blocks instead of 8,000.
     */
    public void setClientLoad(int[] packed) {
        int count = packed.length / 4;
        java.util.Set<BlockPos> occupied = new java.util.HashSet<>();
        for (int i = 0; i < count; i++) occupied.add(new BlockPos(packed[i * 4], packed[i * 4 + 1], packed[i * 4 + 2]));
        List<Piece> visible = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            BlockPos pos = new BlockPos(packed[i * 4], packed[i * 4 + 1], packed[i * 4 + 2]);
            boolean open = false;
            for (Direction direction : Direction.values()) {
                if (!occupied.contains(pos.relative(direction))) {
                    open = true;
                    break;
                }
            }
            if (open) visible.add(new Piece(pos, Block.stateById(packed[i * 4 + 3]), Optional.empty()));
        }
        clientLoad = visible;
        clientLoadCount = count;
    }

    /** The carried blocks as the client draws them (crane frame, forward = +z; hidden inner blocks left out). */
    public List<Piece> getClientLoad() {
        return clientLoad;
    }

    // ---------------------------------------------------------------- saving / breaking

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putByte("LiftSize", (byte) getLiftSize());
        if (!load.isEmpty()) output.store("Load", Piece.CODEC.listOf(), load);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        entityData.set(SIZE, (byte) Math.max(1, Math.min(maxLiftSize(), input.getByteOr("LiftSize", (byte) 3))));
        load = new ArrayList<>(input.read("Load", Piece.CODEC.listOf()).orElse(List.of()));
        syncLoad();
    }

    @Override
    public void remove(RemovalReason reason) {
        if (reason.shouldDestroy() && !load.isEmpty() && level() instanceof ServerLevel level) {
            // Broken while carrying: the load falls apart into items.
            for (Piece piece : load) {
                ItemStack block = new ItemStack(piece.state().getBlock().asItem());
                if (!block.isEmpty()) Containers.dropItemStack(level, getX(), getY() + 1.0, getZ(), block);
                if (piece.data().isPresent()) {
                    BlockEntity be = BlockEntity.loadStatic(blockPosition(), piece.state(), piece.data().get(), level.registryAccess());
                    if (be instanceof Container container) Containers.dropContents(level, blockPosition(), container);
                }
            }
            load = new ArrayList<>();
        }
        super.remove(reason);
    }
}
