package com.factorycraft;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * Lightning collector: put a lightning rod on top. Every bolt that hits the
 * rod is stored: 1 000 000 E per strike (up to 4 000 000). In storms the rod
 * draws lightning to itself, so a tall, open spot fills it quickly.
 */
@EventBusSubscriber(modid = FactoryCraft.MOD_ID)
public final class LightningCollectorBlockEntity extends PassiveGeneratorBlockEntity {
    public static final int PER_STRIKE = 1_000_000;

    public LightningCollectorBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryCraft.LIGHTNING_COLLECTOR_BLOCK_ENTITY.value(), pos, state, 4_000_000, 0);
    }

    @Override protected String nameKey() { return "block.factorycraft.lightning_collector"; }
    @Override protected int computeRate(Level level) { return 0; }

    public boolean hasRod(Level level) {
        return level.getBlockState(worldPosition.above()).getBlock() instanceof LightningRodBlock;
    }

    private void strike(ServerLevel level) {
        energy = Math.min(capacity(), energy + PER_STRIKE);
        setChanged();
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0,
                worldPosition.getZ() + 0.5, 30, 0.4, 0.6, 0.4, 0.2);
        level.playSound(null, worldPosition, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 1.5F, 0.6F);
    }

    @SubscribeEvent
    public static void onLightning(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof LightningBolt bolt) || !(event.getLevel() instanceof ServerLevel level)) return;
        BlockPos hit = bolt.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(hit.offset(-1, -3, -1), hit.offset(1, 1, 1))) {
            if (level.getBlockEntity(pos) instanceof LightningCollectorBlockEntity collector && collector.hasRod(level)) {
                collector.strike(level);
                return;
            }
        }
    }
}
