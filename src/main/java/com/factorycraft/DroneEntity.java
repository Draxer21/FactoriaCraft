package com.factorycraft;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import org.jspecify.annotations.Nullable;

/**
 * Scout drone. Sent with the drone controller, it flies to the point you aim at,
 * hovers there and scans a 16-block radius for ores, which light up through the
 * walls (each ore in its own colour) for 30 seconds. Sneak + right-click with the
 * controller calls it back; right-click the drone itself to pick it up.
 */
public final class DroneEntity extends Entity {
    private static final double SPEED = 0.6;
    private static final int SCAN_RADIUS = 16;
    private static final int MAX_MARKERS = 80;
    private static final double HOVER = 3.0;

    private @Nullable UUID owner;
    private @Nullable Vec3 target;
    private boolean returning;
    private boolean scanned = true;

    public DroneEntity(EntityType<? extends DroneEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public void setOwner(Player player) {
        this.owner = player.getUUID();
    }

    public boolean isOwnedBy(Player player) {
        return player.getUUID().equals(owner);
    }

    /** Fly to {@code point} (hovering a few blocks above it) and scan there. */
    public void sendTo(Vec3 point) {
        this.target = point.add(0.0, HOVER, 0.0);
        this.returning = false;
        this.scanned = false;
    }

    public void recall() {
        this.returning = true;
        this.scanned = true;
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) return;

        Player player = owner == null ? null : level.getPlayerByUUID(owner);
        Vec3 goal = target;
        if (returning) {
            if (player == null) {
                returning = false;
            } else {
                goal = player.getEyePosition().add(0.0, 0.5, 0.0);
                if (distanceToSqr(player) < 4.0) {
                    if (!player.getInventory().add(new ItemStack(FactoryCraft.DRONE_ITEM.value()))) {
                        spawnAtLocation(level, new ItemStack(FactoryCraft.DRONE_ITEM.value()));
                    }
                    level.playSound(null, getX(), getY(), getZ(), SoundEvents.ITEM_PICKUP, SoundSource.NEUTRAL, 0.6F, 1.2F);
                    discard();
                    return;
                }
            }
        }

        Vec3 motion = Vec3.ZERO;
        if (goal != null) {
            Vec3 delta = goal.subtract(position());
            double distance = delta.length();
            if (distance > 0.3) {
                motion = delta.scale(Math.min(SPEED, distance * 0.3) / distance);
                setYRot((float) Math.toDegrees(Math.atan2(-delta.x, delta.z)));
            } else if (!scanned && !returning) {
                scanned = true;
                scan(level, player);
            }
        }
        // Gentle bobbing while hovering.
        motion = motion.add(0.0, Math.sin(tickCount * 0.15) * 0.01, 0.0);
        setDeltaMovement(motion);
        move(MoverType.SELF, motion);
    }

    private void scan(ServerLevel level, @Nullable Player player) {
        int found = markOres(level, blockPosition(), SCAN_RADIUS, MAX_MARKERS, 600);
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.NEUTRAL, 0.6F, 1.8F);
        if (player != null) {
            player.sendOverlayMessage(Component.translatable("message.factorycraft.drone.scan", found));
        }
    }

    /** Marks the ores within {@code radius} of {@code center} (closest first); returns how many ores there are. */
    public static int markOres(ServerLevel level, BlockPos center, int radius, int maxMarkers, int lifetime) {
        List<BlockPos> ores = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius),
                center.offset(radius, radius, radius))) {
            if (pos.distSqr(center) > radius * radius) continue;
            BlockState state = level.getBlockState(pos);
            if (state.is(Tags.Blocks.ORES) || state.is(Blocks.ANCIENT_DEBRIS)) ores.add(pos.immutable());
        }
        ores.sort(Comparator.comparingDouble(pos -> pos.distSqr(center)));
        int count = 0;
        for (BlockPos pos : ores) {
            if (count >= maxMarkers) break;
            OreMarkerEntity marker = FactoryCraft.ORE_MARKER.value().create(level, EntitySpawnReason.TRIGGERED);
            if (marker == null) continue;
            marker.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            marker.setOre(level.getBlockState(pos));
            marker.setLifetime(lifetime);
            level.addFreshEntity(marker);
            count++;
        }
        return ores.size();
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (level().isClientSide()) return InteractionResult.SUCCESS;
        if (!player.getInventory().add(new ItemStack(FactoryCraft.DRONE_ITEM.value()))) {
            player.drop(new ItemStack(FactoryCraft.DRONE_ITEM.value()), false);
        }
        discard();
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (isRemoved()) return true;
        spawnAtLocation(level, new ItemStack(FactoryCraft.DRONE_ITEM.value()));
        discard();
        return true;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        owner = input.read("Owner", UUIDUtil.CODEC).orElse(null);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        if (owner != null) output.store("Owner", UUIDUtil.CODEC, owner);
    }
}
