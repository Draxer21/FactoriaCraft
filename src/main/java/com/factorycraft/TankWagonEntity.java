package com.factorycraft;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Tank wagon: a train wagon that carries up to 128 buckets of water or lava.
 * Pumps fill it through pipes; an unloading station empties it into a reactor
 * (water), a diesel generator (lava) or a cauldron. Buckets work by hand too.
 */
public final class TankWagonEntity extends CargoWagonEntity {
    public static final int CAPACITY = 128;
    private TankerEntity.Fluid fluid = TankerEntity.Fluid.NONE;
    private int amount;

    public TankWagonEntity(EntityType<? extends TankWagonEntity> type, Level level) {
        super(type, level, () -> FactoryCraft.TANK_WAGON_ITEM.value());
    }

    @Override
    public int getContainerSize() {
        return 0;
    }

    @Override
    public net.minecraft.world.inventory.@org.jspecify.annotations.Nullable AbstractContainerMenu createMenu(int id,
            net.minecraft.world.entity.player.Inventory inventory, Player player) {
        return null;
    }

    @Override
    protected void updateFill() {
        setFill((float) amount / CAPACITY);
    }

    public TankerEntity.Fluid fluid() {
        return fluid;
    }

    public int amount() {
        return amount;
    }

    /** Pumped in: returns false if it doesn't fit. */
    public boolean receive(TankerEntity.Fluid incoming) {
        if (incoming == TankerEntity.Fluid.NONE || amount >= CAPACITY || (fluid != TankerEntity.Fluid.NONE && fluid != incoming)) return false;
        fluid = incoming;
        amount++;
        updateFill();
        return true;
    }

    /** Takes one bucket out, or NONE if empty. */
    public TankerEntity.Fluid drainOne() {
        if (amount <= 0) return TankerEntity.Fluid.NONE;
        TankerEntity.Fluid out = fluid;
        amount--;
        if (amount == 0) fluid = TankerEntity.Fluid.NONE;
        updateFill();
        return out;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        ItemStack held = player.getItemInHand(hand);
        if (level().isClientSide()) return InteractionResult.SUCCESS;
        if (held.is(Items.BUCKET) && amount > 0) {
            TankerEntity.Fluid out = drainOne();
            give(player, hand, held, new ItemStack(out == TankerEntity.Fluid.LAVA ? Items.LAVA_BUCKET : Items.WATER_BUCKET));
            playSound(out == TankerEntity.Fluid.LAVA ? SoundEvents.BUCKET_FILL_LAVA : SoundEvents.BUCKET_FILL, 1.0F, 1.0F);
        } else if (held.is(Items.WATER_BUCKET) || held.is(Items.LAVA_BUCKET)) {
            TankerEntity.Fluid in = held.is(Items.LAVA_BUCKET) ? TankerEntity.Fluid.LAVA : TankerEntity.Fluid.WATER;
            if (!receive(in)) return InteractionResult.FAIL;
            give(player, hand, held, new ItemStack(Items.BUCKET));
            playSound(in == TankerEntity.Fluid.LAVA ? SoundEvents.BUCKET_EMPTY_LAVA : SoundEvents.BUCKET_EMPTY, 1.0F, 1.0F);
        }
        player.sendOverlayMessage(Component.translatable("message.factorycraft.tank_wagon",
                Component.translatable("hud.factorycraft.tanker.fluid." + fluid.name().toLowerCase()), amount, CAPACITY));
        return InteractionResult.SUCCESS_SERVER;
    }

    private static void give(Player player, InteractionHand hand, ItemStack held, ItemStack result) {
        if (player.getAbilities().instabuild) return;
        held.shrink(1);
        if (held.isEmpty()) player.setItemInHand(hand, result);
        else if (!player.getInventory().add(result)) player.drop(result, false);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putByte("Fluid", (byte) fluid.ordinal());
        output.putInt("Amount", amount);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        fluid = TankerEntity.Fluid.values()[Math.min(input.getByteOr("Fluid", (byte) 0), 2)];
        amount = input.getIntOr("Amount", 0);
    }
}
