package com.factorycraft;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Pumpjack: pumps crude oil out of the chunk it stands in, into canisters.
 * The richer the deposit the faster it pumps (sneak + right-click shows it).
 * Chunks with no oil give nothing: try another spot.
 */
public final class PumpjackBlockEntity extends PoweredMachineBlockEntity implements AreaMachine {
    public PumpjackBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryCraft.PUMPJACK_BLOCK_ENTITY.value(), pos, state, 0, ProcessingMenu.OUTPUTS);
    }

    private int richness() {
        return level == null ? 0 : OilDeposits.richness(level, ChunkPos.containing(worldPosition));
    }

    /** At 100% one canister every 10 seconds; at 10%, every 100. */
    @Override protected int time() { return 200 * 100 / Math.max(10, richness()); }
    @Override protected int energyPerTick() { return 20; }
    @Override protected MenuType<ProcessingMenu> menuType() { return FactoryCraft.PUMPJACK_MENU.value(); }
    @Override protected String nameKey() { return "block.factorycraft.pumpjack"; }

    @Override
    protected boolean accepts(int slot, ItemStack stack) {
        return false;
    }

    @Override
    protected @Nullable Operation find(Level level) {
        if (richness() <= 0) return null;
        return new Operation(new int[0], List.of(new ItemStack(FactoryCraft.CRUDE_OIL.value())));
    }

    @Override
    protected List<ItemStack> produce(Level level, Operation operation) {
        return List.of(new ItemStack(FactoryCraft.CRUDE_OIL.value()));
    }

    /** Sneak + right-click: how much oil is down there. */
    @Override
    public void cycleRadius(Player player) {
        int r = richness();
        player.sendOverlayMessage(r <= 0 ? Component.translatable("message.factorycraft.pumpjack.dry")
                : Component.translatable("message.factorycraft.pumpjack.richness", r));
    }
}
