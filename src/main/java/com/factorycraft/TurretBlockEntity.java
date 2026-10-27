package com.factorycraft;

import java.util.Comparator;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Defense turret: shoots hostile mobs within 20 blocks that it can see, twice a
 * second, 7 damage a shot (150 power). Never aims at players or pets. Kills count
 * as the owner's, so mobs drop experience.
 */
public final class TurretBlockEntity extends MachineBlockEntity implements Energy.Consumer, Owned {
    public static final double RANGE = 20.0;
    private static final int INTERVAL = 10;
    private static final float DAMAGE = 7.0F;
    private static final int ENERGY_PER_SHOT = 150;

    private final Energy.Link link = new Energy.Link(this);
    private @Nullable UUID owner;
    private int timer;
    private int idle;
    int shots;

    public TurretBlockEntity(BlockPos pos, BlockState state) {
        super(Defense.TURRET_BLOCK_ENTITY.value(), pos, state, 0);
    }

    @Override protected boolean isInputSlot(int slot) { return false; }
    @Override protected boolean isOutputSlot(int slot) { return false; }
    @Override protected boolean accepts(int slot, ItemStack stack) { return false; }
    @Override public Energy.Link energyLink() { return link; }
    @Override public void setOwner(UUID owner) { this.owner = owner; setChanged(); }
    @Override public @Nullable UUID owner() { return owner; }

    Vec3 muzzle() {
        return Vec3.atCenterOf(worldPosition).add(0, 0.55, 0);
    }

    static boolean canSee(Level level, Vec3 from, LivingEntity target) {
        Vec3 to = target.getEyePosition();
        return level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, target)).getType() == HitResult.Type.MISS;
    }

    @Nullable LivingEntity target(ServerLevel level) {
        Vec3 from = muzzle();
        return level.getEntitiesOfClass(Mob.class, new AABB(worldPosition).inflate(RANGE),
                        mob -> mob instanceof Enemy && mob.isAlive() && mob.distanceToSqr(from) <= RANGE * RANGE)
                .stream().sorted(Comparator.comparingDouble(mob -> mob.distanceToSqr(from)))
                .filter(mob -> canSee(level, from, mob)).findFirst().orElse(null);
    }

    @Override
    protected boolean work(Level level) {
        if (!(level instanceof ServerLevel server)) return false;
        if (++timer < INTERVAL) return idle < 60;
        timer = 0;
        LivingEntity target = target(server);
        if (target == null) {
            idle += INTERVAL;
            return idle < 60;
        }
        if (link.draw(level, ENERGY_PER_SHOT) < ENERGY_PER_SHOT) return false;
        idle = 0;
        Vec3 from = muzzle();
        Vec3 to = target.getBoundingBox().getCenter();
        Player player = owner == null ? null : server.getPlayerByUUID(owner);
        DamageSource source = player != null ? server.damageSources().playerAttack(player) : server.damageSources().generic();
        target.hurtServer(server, source, DAMAGE);
        shots++;
        Vec3 step = to.subtract(from);
        int points = (int) Math.max(2, step.length() * 2);
        for (int i = 1; i <= points; i++) {
            Vec3 p = from.add(step.scale(i / (double) points));
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
        server.sendParticles(ParticleTypes.CRIT, to.x, to.y, to.z, 6, 0.2, 0.2, 0.2, 0.1);
        server.playSound(null, worldPosition, SoundEvents.CROSSBOW_SHOOT, SoundSource.BLOCKS, 0.8F, 1.6F);
        return true;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        owner = input.read("Owner", UUIDUtil.CODEC).orElse(null);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (owner != null) output.store("Owner", UUIDUtil.CODEC, owner);
    }
}
