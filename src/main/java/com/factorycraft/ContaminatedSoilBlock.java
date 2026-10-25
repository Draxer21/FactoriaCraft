package com.factorycraft;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ground poisoned by radiation. Walking on it makes you sick (lead boots
 * protect). Nothing grows on it. It slowly recovers into dirt on its own;
 * air purifiers and decontamination kits clean it much faster.
 */
public final class ContaminatedSoilBlock extends Block {
    public ContaminatedSoilBlock(Properties properties) {
        super(properties.randomTicks());
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide() && entity instanceof LivingEntity living && level.getGameTime() % 40 == 0) {
            if (living instanceof Player player && (player.isCreative()
                    || player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET).is(Nuclear.LEAD_BOOTS.value()))) {
                return;
            }
            Radiation.apply(living, 14.0F);
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(40) == 0) {
            level.setBlock(pos, Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
        } else if (random.nextInt(4) == 0) {
            level.sendParticles(ParticleTypes.GLOW, pos.getX() + random.nextDouble(), pos.getY() + 1.05,
                    pos.getZ() + random.nextDouble(), 1, 0, 0.02, 0, 0.0);
        }
    }
}
