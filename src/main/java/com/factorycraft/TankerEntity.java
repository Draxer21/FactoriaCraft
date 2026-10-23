package com.factorycraft;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Tanker truck: carries up to 128 buckets of water or lava.
 *  - Modes (driver right-clicks the tanker with an empty hand to switch):
 *    TRANSPORT does nothing, DRAIN sucks up fluid source blocks ahead while
 *    driving (drains flooded caves, lava lakes), FILL pours sources into the
 *    holes ahead (pools, moats).
 *  - Right-click with an empty bucket to fill it, or with a full bucket to pour it in.
 *  - Mobile fuel station: when it carries lava, it refuels every machine within
 *    8 blocks (itself included) that has room for a bucket's worth (20 000).
 */
public final class TankerEntity extends WorkVehicleEntity implements HudInfo {
    private static final EntityDataAccessor<Byte> FLUID =
            SynchedEntityData.defineId(TankerEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> AMOUNT =
            SynchedEntityData.defineId(TankerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> MODE =
            SynchedEntityData.defineId(TankerEntity.class, EntityDataSerializers.BYTE);

    public static final int CAPACITY = 128;
    private static final int LAVA_FUEL = 20_000;
    private static final double SERVICE_RANGE = 8.0;

    public enum Fluid { NONE, WATER, LAVA }

    public enum Mode { TRANSPORT, DRAIN, FILL }

    public TankerEntity(EntityType<? extends TankerEntity> type, Level level) {
        super(type, level, () -> FactoryCraft.TANKER_ITEM.value());
    }

    @Override protected double maxSpeed() { return 0.38; }
    @Override protected double reverseSpeed() { return 0.18; }
    @Override protected double seatHeight() { return 1.875; }
    @Override protected double seatForward() { return 1.69; }
    @Override protected double hullShift() { return 0.09; }
    @Override protected double hullHalfLength() { return 3.0; }
    @Override protected double hullHalfWidth() { return 1.57; }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FLUID, (byte) 0);
        builder.define(AMOUNT, 0);
        builder.define(MODE, (byte) 0);
    }

    public Fluid getFluid() {
        return Fluid.values()[Math.min(entityData.get(FLUID), 2)];
    }

    public int getAmount() {
        return entityData.get(AMOUNT);
    }

    public Mode getMode() {
        return Mode.values()[Math.min(entityData.get(MODE), 2)];
    }

    private void setTank(Fluid fluid, int amount) {
        if (amount <= 0) {
            fluid = Fluid.NONE;
            amount = 0;
        }
        entityData.set(FLUID, (byte) fluid.ordinal());
        entityData.set(AMOUNT, Math.min(CAPACITY, amount));
    }

    /** Pumped in through pipes. Returns false if it doesn't fit. */
    public boolean receive(Fluid fluid) {
        if (fluid == Fluid.NONE || !canTake(fluid)) return false;
        setTank(fluid, getAmount() + 1);
        return true;
    }

    private boolean canTake(Fluid fluid) {
        return getAmount() < CAPACITY && (getFluid() == Fluid.NONE || getFluid() == fluid);
    }

    // ---------------------------------------------------------------- working

    @Override
    protected void work(ServerLevel level, Player driver) {
        Mode mode = getMode();
        if (mode == Mode.TRANSPORT) return;
        int ground = groundY();
        int half = engine().wide() ? 2 : 1;
        for (double ahead = hullFront() + 0.5; ahead <= hullFront() + 1.5; ahead += 1.0) {
            for (int side = -half; side <= half; side++) {
                if (mode == Mode.DRAIN) {
                    for (int y = ground + 1; y >= ground - 3; y--) drain(level, columnAt(ahead, side, y));
                } else {
                    for (int d = 3; d >= 1; d--) fill(level, columnAt(ahead, side, ground - d));
                }
            }
        }
    }

    private void drain(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        FluidState fluidState = state.getFluidState();
        if (fluidState.isEmpty() || !fluidState.isSource()) return;
        Fluid fluid = fluidState.getType() == Fluids.LAVA ? Fluid.LAVA
                : fluidState.getType() == Fluids.WATER ? Fluid.WATER : Fluid.NONE;
        if (fluid == Fluid.NONE || !canTake(fluid)) return;
        if (state.getBlock() instanceof LiquidBlock) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        } else if (state.hasProperty(BlockStateProperties.WATERLOGGED)) {
            level.setBlock(pos, state.setValue(BlockStateProperties.WATERLOGGED, false), Block.UPDATE_ALL);
        } else {
            return;
        }
        setTank(fluid, getAmount() + 1);
        if (tickCount % 6 == 0) {
            level.playSound(null, pos, fluid == Fluid.LAVA ? SoundEvents.BUCKET_FILL_LAVA : SoundEvents.BUCKET_FILL,
                    SoundSource.NEUTRAL, 0.6F, 0.8F);
        }
    }

    private void fill(ServerLevel level, BlockPos pos) {
        if (getFluid() == Fluid.NONE || getAmount() <= 0) return;
        BlockState state = level.getBlockState(pos);
        if (!state.isAir() && !(state.canBeReplaced() && state.getFluidState().isEmpty())) return;
        BlockState source = getFluid() == Fluid.LAVA ? Blocks.LAVA.defaultBlockState() : Blocks.WATER.defaultBlockState();
        level.setBlock(pos, source, Block.UPDATE_ALL);
        setTank(getFluid(), getAmount() - 1);
        if (tickCount % 6 == 0) {
            level.playSound(null, pos, getFluid() == Fluid.LAVA ? SoundEvents.BUCKET_EMPTY_LAVA : SoundEvents.BUCKET_EMPTY,
                    SoundSource.NEUTRAL, 0.6F, 0.8F);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel level && tickCount % 20 == 0 && getFluid() == Fluid.LAVA) {
            serviceMachines(level);
        }
    }

    /** Fuel station: tops up nearby machines with lava, one bucket at a time. */
    private void serviceMachines(ServerLevel level) {
        AABB area = getBoundingBox().inflate(SERVICE_RANGE);
        for (Entity entity : level.getEntities(this, area, e -> e instanceof VehicleEngine.Holder)) {
            refuel(((VehicleEngine.Holder) entity).engine());
        }
        refuel(engine());
    }

    private void refuel(VehicleEngine target) {
        while (getFluid() == Fluid.LAVA && getAmount() > 0 && target.addFuel(LAVA_FUEL)) {
            setTank(Fluid.LAVA, getAmount() - 1);
        }
    }

    // ---------------------------------------------------------------- interaction

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        ItemStack held = player.getItemInHand(hand);
        if (held.is(Items.BUCKET) || held.is(Items.WATER_BUCKET) || held.is(Items.LAVA_BUCKET)) {
            if (level().isClientSide()) return InteractionResult.SUCCESS;
            return handleBucket(player, hand, held);
        }
        if (held.isEmpty() && player.getVehicle() == this && getControllingPassenger() == player) {
            if (level().isClientSide()) return InteractionResult.SUCCESS;
            Mode next = Mode.values()[(getMode().ordinal() + 1) % Mode.values().length];
            entityData.set(MODE, (byte) next.ordinal());
            player.sendOverlayMessage(Component.translatable("message.factorycraft.tanker.mode",
                    Component.translatable("hud.factorycraft.tanker.mode." + next.name().toLowerCase())));
            return InteractionResult.SUCCESS_SERVER;
        }
        return super.interact(player, hand, location);
    }

    private InteractionResult handleBucket(Player player, InteractionHand hand, ItemStack held) {
        if (held.is(Items.BUCKET)) {
            if (getAmount() <= 0) return InteractionResult.FAIL;
            ItemStack full = new ItemStack(getFluid() == Fluid.LAVA ? Items.LAVA_BUCKET : Items.WATER_BUCKET);
            setTank(getFluid(), getAmount() - 1);
            give(player, hand, held, full);
            playSound(getFluid() == Fluid.LAVA ? SoundEvents.BUCKET_FILL_LAVA : SoundEvents.BUCKET_FILL, 1.0F, 1.0F);
        } else {
            Fluid fluid = held.is(Items.LAVA_BUCKET) ? Fluid.LAVA : Fluid.WATER;
            if (!canTake(fluid)) {
                player.sendOverlayMessage(Component.translatable("message.factorycraft.tanker.cant"));
                return InteractionResult.FAIL;
            }
            setTank(fluid, getAmount() + 1);
            give(player, hand, held, new ItemStack(Items.BUCKET));
            playSound(fluid == Fluid.LAVA ? SoundEvents.BUCKET_EMPTY_LAVA : SoundEvents.BUCKET_EMPTY, 1.0F, 1.0F);
        }
        player.sendOverlayMessage(hudLine());
        return InteractionResult.SUCCESS_SERVER;
    }

    private static void give(Player player, InteractionHand hand, ItemStack held, ItemStack result) {
        if (player.getAbilities().instabuild) return;
        held.shrink(1);
        if (held.isEmpty()) {
            player.setItemInHand(hand, result);
        } else if (!player.getInventory().add(result)) {
            player.drop(result, false);
        }
    }

    @Override
    public Component hudLine() {
        Component fluid = Component.translatable("hud.factorycraft.tanker.fluid." + getFluid().name().toLowerCase());
        Component mode = Component.translatable("hud.factorycraft.tanker.mode." + getMode().name().toLowerCase());
        return Component.translatable("hud.factorycraft.tanker", fluid, getAmount(), CAPACITY, mode);
    }

    // ---------------------------------------------------------------- saving

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putByte("Fluid", (byte) getFluid().ordinal());
        output.putInt("Amount", getAmount());
        output.putByte("Mode", (byte) getMode().ordinal());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setTank(Fluid.values()[Math.min(input.getByteOr("Fluid", (byte) 0), 2)], input.getIntOr("Amount", 0));
        entityData.set(MODE, (byte) Math.min(input.getByteOr("Mode", (byte) 0), 2));
    }
}
