package com.factorycraft;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.common.Tags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A glowing copy of an ore block placed by the drone's scan: its outline shows
 * through walls (in the ore's colour) for 30 seconds, then it disappears.
 */
public final class OreMarkerEntity extends Entity {
    private static final EntityDataAccessor<BlockState> ORE =
            SynchedEntityData.defineId(OreMarkerEntity.class, EntityDataSerializers.BLOCK_STATE);
    private int lifetime = 600;

    public OreMarkerEntity(EntityType<? extends OreMarkerEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        setNoGravity(true);
    }

    public void setOre(BlockState state) {
        entityData.set(ORE, state);
        setGlowingTag(true);
    }

    /** How long the marker glows, in ticks (default 30 s). */
    public void setLifetime(int ticks) {
        this.lifetime = ticks;
    }

    public BlockState getOre() {
        return entityData.get(ORE);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(ORE, Blocks.STONE.defaultBlockState());
    }

    @Override
    public void tick() {
        super.tick();
        // Gone after its lifetime, or once the ore has been mined.
        if (!level().isClientSide() && (tickCount > lifetime || !level().getBlockState(blockPosition()).is(getOre().getBlock()))) {
            discard();
        }
    }

    @Override
    public int getTeamColor() {
        BlockState ore = getOre();
        if (ore.is(Tags.Blocks.ORES_DIAMOND)) return 0x4FF0E6;
        if (ore.is(Tags.Blocks.ORES_EMERALD)) return 0x3CE070;
        if (ore.is(Tags.Blocks.ORES_GOLD) || ore.is(Blocks.NETHER_GOLD_ORE)) return 0xFFD84A;
        if (ore.is(Tags.Blocks.ORES_IRON)) return 0xE0B48C;
        if (ore.is(Tags.Blocks.ORES_COPPER)) return 0xE07A3C;
        if (ore.is(Tags.Blocks.ORES_REDSTONE)) return 0xFF3030;
        if (ore.is(Tags.Blocks.ORES_LAPIS)) return 0x3060FF;
        if (ore.is(Tags.Blocks.ORES_COAL)) return 0x505050;
        if (ore.is(Blocks.ANCIENT_DEBRIS)) return 0xA050C0;
        return 0xFFFFFF;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
    }
}
