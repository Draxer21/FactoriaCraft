package com.factorycraft;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Repair station: runs on power from cables/generators.
 *  - Tools and armour put in its 4 input slots are repaired (2 durability per
 *    tick each, 20 E per point) and moved to the outputs once like new.
 *  - It also repairs what nearby players (6 blocks) are using: the items in
 *    their hands and the armour they wear, a little every half second.
 */
public final class RepairStationBlockEntity extends MachineBlockEntity implements MenuProvider, Energy.Consumer {
    private static final int INPUTS = 4;
    private static final int ENERGY_PER_POINT = 20;
    private static final int POINTS_PER_TICK = 2;
    private static final int NEARBY_POINTS = 6;
    private static final double RANGE = 6.0;
    private static final EquipmentSlot[] WORN = {EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private final Energy.Link link = new Energy.Link(this);
    private int progress;
    private int networkStored;
    private int networkCapacity;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int id) {
            return switch (id) {
                case 0 -> progress;
                case 1 -> 100;
                case 2 -> networkStored;
                case 3 -> networkCapacity;
                case 4 -> 1;
                default -> 0;
            };
        }

        @Override
        public void set(int id, int value) {
            switch (id) {
                case 0 -> progress = value;
                case 2 -> networkStored = value;
                case 3 -> networkCapacity = value;
                default -> { }
            }
        }

        @Override
        public int getCount() {
            return ProcessingMenu.DATA_COUNT;
        }
    };

    public RepairStationBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryCraft.REPAIR_STATION_BLOCK_ENTITY.value(), pos, state, INPUTS + ProcessingMenu.OUTPUTS);
    }

    @Override protected boolean isInputSlot(int slot) { return slot < INPUTS; }
    @Override protected boolean isOutputSlot(int slot) { return slot >= INPUTS; }

    @Override
    protected boolean accepts(int slot, ItemStack stack) {
        return slot < INPUTS && stack.isDamageableItem() && stack.getCount() == 1;
    }

    /** Repairs up to {@code points}; returns true if anything was repaired. */
    private boolean repair(Level level, ItemStack stack, int points) {
        if (stack.isEmpty() || !stack.isDamageableItem() || !stack.isDamaged()) return false;
        int want = Math.min(points, stack.getDamageValue());
        int got = link.draw(level, want * ENERGY_PER_POINT) / ENERGY_PER_POINT;
        if (got <= 0) return false;
        stack.setDamageValue(stack.getDamageValue() - got);
        return true;
    }

    @Override
    protected boolean work(Level level) {
        if (level.getGameTime() % 10 == 0) {
            networkStored = link.stored(level);
            networkCapacity = link.capacity(level);
        }
        boolean working = false;
        progress = 0;
        for (int i = 0; i < INPUTS; i++) {
            ItemStack stack = items.get(i);
            if (stack.isEmpty()) continue;
            if (!stack.isDamaged()) {
                // Like new: move it to the outputs.
                if (canStore(List.of(stack))) {
                    store(List.of(stack));
                    items.set(i, ItemStack.EMPTY);
                    setChanged();
                }
                continue;
            }
            if (repair(level, stack, POINTS_PER_TICK)) {
                working = true;
                setChanged();
            }
            if (progress == 0 && stack.getMaxDamage() > 0) {
                progress = 100 - 100 * stack.getDamageValue() / stack.getMaxDamage();
            }
        }
        // Players nearby: what they hold and wear.
        if (level.getGameTime() % 10 == 0) {
            for (Player player : level.getEntitiesOfClass(Player.class, new AABB(worldPosition).inflate(RANGE))) {
                for (EquipmentSlot slot : WORN) {
                    if (repair(level, player.getItemBySlot(slot), NEARBY_POINTS)) working = true;
                }
            }
        }
        return working;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.factorycraft.repair_station");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ProcessingMenu(FactoryCraft.REPAIR_STATION_MENU.value(), id, inventory, this, data, false, INPUTS);
    }
}
