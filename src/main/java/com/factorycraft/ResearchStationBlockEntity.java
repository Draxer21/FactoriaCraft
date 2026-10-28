package com.factorycraft;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Research station: studies materials into research points for the whole world
 * (one item every 2 seconds, 60 power a tick). Advanced materials are worth more:
 * a circuit board 4, an advanced circuit 12, a nether star 100. Sneak + right-click
 * opens the research tree.
 */
public final class ResearchStationBlockEntity extends PoweredMachineBlockEntity {
    public ResearchStationBlockEntity(BlockPos pos, BlockState state) {
        super(ResearchBlocks.STATION_BLOCK_ENTITY.value(), pos, state, ProcessingMenu.BIG_INPUTS, 0);
    }

    @Override protected int time() { return 40; }
    @Override protected int energyPerTick() { return 60; }
    @Override protected MenuType<ProcessingMenu> menuType() { return ResearchBlocks.STATION_MENU.value(); }
    @Override protected String nameKey() { return "block.factorycraft.research_station"; }

    @Override
    protected boolean accepts(int slot, ItemStack stack) {
        return slot < ProcessingMenu.BIG_INPUTS && Research.value(stack) > 0;
    }

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        int points = level == null ? 0 : Research.state(level).points();
        return super.getDisplayName().copy().append(" · ").append(net.minecraft.network.chat.Component.translatable("gui.factorycraft.research.points", points));
    }

    @Override
    protected @Nullable Operation find(Level level) {
        for (int i = 0; i < ProcessingMenu.BIG_INPUTS; i++) {
            if (Research.value(items.get(i)) > 0) {
                int[] consume = new int[ProcessingMenu.BIG_INPUTS];
                consume[i] = 1;
                return new Operation(consume, List.of());
            }
        }
        return null;
    }

    @Override
    protected List<ItemStack> produce(Level level, Operation operation) {
        for (int i = 0; i < operation.consume().length; i++) {
            if (operation.consume()[i] > 0) {
                Research.addPoints(level, Research.value(items.get(i)));
                if (level instanceof ServerLevel server) {
                    server.sendParticles(ParticleTypes.ENCHANT, worldPosition.getX() + 0.5, worldPosition.getY() + 1.2, worldPosition.getZ() + 0.5,
                            12, 0.3, 0.3, 0.3, 0.5);
                }
                break;
            }
        }
        return List.of();
    }
}
