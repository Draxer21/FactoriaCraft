package com.factorycraft;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** A rocket lifting a satellite: ignition, a slow climb that speeds up, then orbit far above the build limit. */
public final class RocketEntity extends Entity {
    private static final int IGNITION = 30;
    private boolean survey;
    private int age;

    public RocketEntity(EntityType<? extends RocketEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    static void launch(ServerLevel level, double x, double y, double z, boolean survey) {
        RocketEntity rocket = new RocketEntity(Endgame.ROCKET_ENTITY.value(), level);
        rocket.survey = survey;
        rocket.setPos(x, y, z);
        level.addFreshEntity(rocket);
        level.playSound(null, x, y, z, SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.BLOCKS, 4.0F, 0.4F);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server)) return;
        age++;
        double speed = age < IGNITION ? 0.0 : Math.min(2.5, (age - IGNITION) * 0.015);
        setPos(getX(), getY() + speed, getZ());
        int smoke = age < IGNITION ? 6 : 3;
        server.sendParticles(ParticleTypes.FLAME, true, true, getX(), getY() - 0.2, getZ(), 8, 0.15, 0.1, 0.15, 0.05);
        server.sendParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, true, true, getX(), getY() - 0.5, getZ(), smoke, 0.4, 0.2, 0.4, 0.02);
        if (age % 20 == 0) server.playSound(null, getX(), getY(), getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.BLOCKS, 3.0F, 0.3F);
        if (getY() > server.getMaxY() + 48) {
            Orbit.launch(server, survey);
            Orbit orbit = Orbit.get(server);
            for (ServerPlayer player : server.players()) {
                if (player.distanceToSqr(getX(), player.getY(), getZ()) < 256 * 256) {
                    player.sendSystemMessage(Component.translatable("message.factorycraft.orbit.reached",
                            Component.translatable(survey ? "item.factorycraft.survey_satellite" : "item.factorycraft.solar_satellite"),
                            orbit.solar(), orbit.survey()));
                }
            }
            discard();
        }
    }

    public int age() {
        return age;
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 512 * 512;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        survey = input.getBooleanOr("Survey", false);
        age = input.getIntOr("Age", 0);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putBoolean("Survey", survey);
        output.putInt("Age", age);
    }
}
