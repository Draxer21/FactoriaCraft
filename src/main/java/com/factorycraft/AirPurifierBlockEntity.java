package com.factorycraft;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Air purifier: runs on power (40 E/t). While working, diesel generators
 * within 12 blocks don't damage the environment, and it cleans contaminated
 * soil around it back into grass, one block a second.
 */
public final class AirPurifierBlockEntity extends MachineBlockEntity implements Energy.Consumer {
    private static final int ENERGY = 40;
    private final Energy.Link link = new Energy.Link(this);

    public AirPurifierBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryCraft.AIR_PURIFIER_BLOCK_ENTITY.value(), pos, state, 0);
    }

    @Override protected boolean isInputSlot(int slot) { return false; }
    @Override protected boolean isOutputSlot(int slot) { return false; }
    @Override protected boolean accepts(int slot, ItemStack stack) { return false; }

    @Override
    protected boolean work(Level level) {
        if (link.draw(level, ENERGY) < ENERGY) return false;
        Pollution.purifierActive(level, worldPosition);
        if (level.getGameTime() % 20 == 0 && level instanceof ServerLevel server) {
            RandomSource random = level.getRandom();
            int r = Pollution.PURIFIER_RANGE;
            for (int tries = 0; tries < 40; tries++) {
                BlockPos pos = worldPosition.offset(random.nextInt(r * 2 + 1) - r, random.nextInt(9) - 4, random.nextInt(r * 2 + 1) - r);
                if (level.getBlockState(pos).is(Nuclear.CONTAMINATED_SOIL.value())) {
                    level.setBlock(pos, Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
                    server.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 3, 0.3, 0.1, 0.3, 0);
                    break;
                }
            }
            server.sendParticles(ParticleTypes.CLOUD, worldPosition.getX() + 0.5, worldPosition.getY() + 1.1, worldPosition.getZ() + 0.5, 1, 0.1, 0.05, 0.1, 0.01);
        }
        return true;
    }

    @Override
    public Energy.Link energyLink() {
        return link;
    }
}
