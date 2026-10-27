package com.factorycraft;

import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Vending machine: a shop that runs itself. Its owner sets the price in the first
 * slot (item and amount) and the goods in the second, and stocks the next four
 * (hoppers and belts can stock them too). Other players right-click it holding the
 * price to buy; payments collect in the 8 output slots and are pushed out the front
 * into a chest. Right-click with anything else shows the offer.
 */
public final class VendingMachineBlockEntity extends PoweredMachineBlockEntity implements Owned {
    public static final int PRICE = 0;
    public static final int GOODS = 1;
    private static final int STOCK_FROM = 2;
    private static final int STOCK_TO = 6;
    private @Nullable UUID owner;

    public VendingMachineBlockEntity(BlockPos pos, BlockState state) {
        super(Economy.VENDING_MACHINE_BLOCK_ENTITY.value(), pos, state, ProcessingMenu.BIG_INPUTS, ProcessingMenu.BIG_OUTPUTS);
    }

    @Override protected int time() { return 1; }
    @Override protected int energyPerTick() { return 0; }
    @Override protected MenuType<ProcessingMenu> menuType() { return Economy.VENDING_MACHINE_MENU.value(); }
    @Override protected String nameKey() { return "block.factorycraft.vending_machine"; }
    @Override public boolean supportsUpgrades() { return false; }
    @Override protected boolean accepts(int slot, ItemStack stack) { return slot < STOCK_TO; }
    @Override protected @Nullable Operation find(Level level) { return null; }
    @Override protected List<ItemStack> produce(Level level, Operation operation) { return List.of(); }
    @Override protected boolean work(Level level) { return stock() >= Math.max(1, items.get(GOODS).getCount()) && !items.get(PRICE).isEmpty(); }
    @Override public void setOwner(UUID owner) { this.owner = owner; setChanged(); }
    @Override public @Nullable UUID owner() { return owner; }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
        // Hoppers and belts only stock the shelves; the price and goods slots are the owner's.
        return slot >= STOCK_FROM && slot < STOCK_TO && ItemStack.isSameItemSameComponents(stack, items.get(GOODS));
    }

    public boolean isOwner(Player player) {
        return owner == null || owner.equals(player.getUUID());
    }

    int stock() {
        ItemStack goods = items.get(GOODS);
        if (goods.isEmpty()) return 0;
        int n = 0;
        for (int i = STOCK_FROM; i < STOCK_TO; i++) if (ItemStack.isSameItemSameComponents(items.get(i), goods)) n += items.get(i).getCount();
        return n;
    }

    public Component offer() {
        ItemStack price = items.get(PRICE);
        ItemStack goods = items.get(GOODS);
        if (price.isEmpty() || goods.isEmpty()) return Component.translatable("message.factorycraft.vending.closed");
        return Component.translatable("message.factorycraft.vending.offer", goods.getCount(), goods.getHoverName(),
                price.getCount(), price.getHoverName(), stock() / goods.getCount());
    }

    /** A customer pays with what they hold. Returns the message to show. */
    public Component buy(Player player, InteractionHand hand) {
        ItemStack price = items.get(PRICE);
        ItemStack goods = items.get(GOODS);
        ItemStack held = player.getItemInHand(hand);
        if (price.isEmpty() || goods.isEmpty()) return Component.translatable("message.factorycraft.vending.closed");
        if (!ItemStack.isSameItemSameComponents(held, price) || held.getCount() < price.getCount()) return offer();
        if (stock() < goods.getCount()) return Component.translatable("message.factorycraft.vending.empty");
        ItemStack payment = held.copyWithCount(price.getCount());
        if (!canStore(List.of(payment))) return Component.translatable("message.factorycraft.vending.full");
        held.shrink(price.getCount());
        // Take the goods off the shelves.
        int left = goods.getCount();
        ItemStack bought = goods.copyWithCount(goods.getCount());
        for (int i = STOCK_FROM; i < STOCK_TO && left > 0; i++) {
            ItemStack shelf = items.get(i);
            if (!ItemStack.isSameItemSameComponents(shelf, goods)) continue;
            int take = Math.min(left, shelf.getCount());
            shelf.shrink(take);
            left -= take;
        }
        items.set(PRICE, price);
        storePayment(payment);
        if (!player.getInventory().add(bought)) player.drop(bought, false);
        if (level != null) level.playSound(null, worldPosition, SoundEvents.VILLAGER_YES, SoundSource.BLOCKS, 0.6F, 1.4F);
        setChanged();
        return Component.translatable("message.factorycraft.vending.sold", goods.getCount(), goods.getHoverName());
    }

    private void storePayment(ItemStack payment) {
        // Payments aren't production: bypass the statistics.
        for (int i = ProcessingMenu.BIG_INPUTS; i < getContainerSize() && !payment.isEmpty(); i++) {
            ItemStack slot = items.get(i);
            if (slot.isEmpty()) {
                items.set(i, payment.copy());
                payment.setCount(0);
            } else if (ItemStack.isSameItemSameComponents(slot, payment)) {
                int move = Math.min(payment.getCount(), slot.getMaxStackSize() - slot.getCount());
                slot.grow(move);
                payment.shrink(move);
            }
        }
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
