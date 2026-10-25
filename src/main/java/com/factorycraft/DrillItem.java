package com.factorycraft;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;

/**
 * Electric hand drill. Works as a normal pickaxe; with charge in its battery it
 * mines twice as fast and unlocks its tricks:
 *  - VEIN mode: breaking an ore breaks the whole connected vein (up to 64).
 *  - TUNNEL mode: digs a 1x2 passage 3 blocks deep where you look.
 *  - Sneak + right-click: ore scan, 8 blocks around you, 10 seconds.
 *  - Right-click (air): switch mode.
 * Charge it by right-clicking a diesel generator with it.
 */
public final class DrillItem extends Item {
    public static final int CAPACITY = 40_000;
    /** 0 normal, 1 prototype (Mechanic), 2 giant excavation drill. */
    private final int tier;
    private static final int ENERGY_PER_BLOCK = 25;
    private static final int SCAN_COST = 1_500;
    private static final int VEIN_MAX = 64;
    private static final int TUNNEL_DEPTH = 3;

    private enum Mode { VEIN, TUNNEL }

    public DrillItem(Properties properties) {
        this(properties, false);
    }

    /** {@code prototype}: the Mechanic's version, with double battery, 128-block veins and a 3x3 tunnel. */
    public DrillItem(Properties properties, boolean prototype) {
        this(properties, prototype ? 1 : 0);
    }

    /** Tier 2: giant excavation drill, x4 battery, 256-block veins and a 5x5 tunnel 4 deep. */
    public DrillItem(Properties properties, int tier) {
        super(properties);
        this.tier = tier;
    }

    private int capacity() {
        return tier == 2 ? CAPACITY * 4 : tier == 1 ? CAPACITY * 2 : CAPACITY;
    }

    // ---------------------------------------------------------------- stored data

    private static CompoundView data(ItemStack stack) {
        return new CompoundView(stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag());
    }

    private record CompoundView(net.minecraft.nbt.CompoundTag tag) {
        int energy() { return tag.getIntOr("Energy", 0); }
        Mode mode() { return tag.getIntOr("Mode", 0) == 1 ? Mode.TUNNEL : Mode.VEIN; }
    }

    public static int energy(ItemStack stack) {
        return data(stack).energy();
    }

    private void setEnergy(ItemStack stack, int energy) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt("Energy", Math.max(0, Math.min(capacity(), energy))));
    }

    /** Puts up to {@code max} energy into a drill; returns how much it took (0 if it isn't a drill or is full). */
    public static int charge(ItemStack stack, int max) {
        if (!(stack.getItem() instanceof DrillItem drill)) return 0;
        int taken = Math.max(0, Math.min(drill.capacity() - energy(stack), max));
        if (taken > 0) drill.setEnergy(stack, energy(stack) + taken);
        return taken;
    }

    public static boolean isFull(ItemStack stack) {
        return stack.getItem() instanceof DrillItem drill && energy(stack) >= drill.capacity();
    }

    private boolean use(ItemStack stack, int amount) {
        int energy = energy(stack);
        if (energy < amount) return false;
        setEnergy(stack, energy - amount);
        return true;
    }

    // ---------------------------------------------------------------- mining

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        float speed = super.getDestroySpeed(stack, state);
        return energy(stack) >= ENERGY_PER_BLOCK && speed > 1.0F ? speed * 2.0F : speed;
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity owner) {
        boolean result = super.mineBlock(stack, level, state, pos, owner);
        if (level.isClientSide() || !(owner instanceof Player player) || !use(stack, ENERGY_PER_BLOCK)) return result;
        if (data(stack).mode() == Mode.VEIN) {
            if (state.is(Tags.Blocks.ORES)) mineVein(stack, level, state, pos, player);
        } else {
            mineTunnel(stack, level, pos, player);
        }
        return result;
    }

    private void mineVein(ItemStack stack, Level level, BlockState ore, BlockPos start, Player player) {
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        List<BlockPos> vein = new ArrayList<>();
        seen.add(start);
        queue.add(start);
        while (!queue.isEmpty() && vein.size() < (VEIN_MAX << tier)) {
            BlockPos pos = queue.poll();
            for (BlockPos next : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
                if (seen.contains(next)) continue;
                if (!level.getBlockState(next).is(ore.getBlock())) continue;
                BlockPos immutable = next.immutable();
                seen.add(immutable);
                vein.add(immutable);
                queue.add(immutable);
            }
        }
        for (BlockPos pos : vein) {
            if (!breakExtra(stack, level, pos, player)) break;
        }
    }

    private void mineTunnel(ItemStack stack, Level level, BlockPos hit, Player player) {
        Vec3 look = player.getLookAngle();
        if (Math.abs(look.y) > 0.75) {
            // Looking straight up or down: a 1x1 shaft.
            Direction dir = look.y > 0 ? Direction.UP : Direction.DOWN;
            for (int d = 1; d < TUNNEL_DEPTH; d++) {
                if (!breakExtra(stack, level, hit.relative(dir, d), player)) return;
            }
            return;
        }
        Direction dir = Direction.fromYRot(player.getYRot());
        Direction side = dir.getClockWise();
        int floor = player.blockPosition().getY();
        int halfWidth = tier;
        int height = tier == 2 ? 5 : tier == 1 ? 3 : 2;
        int depth = tier == 2 ? TUNNEL_DEPTH + 1 : TUNNEL_DEPTH;
        for (int d = 0; d < depth; d++) {
            BlockPos column = hit.relative(dir, d);
            for (int w = -halfWidth; w <= halfWidth; w++) {
                for (int h = 0; h < height; h++) {
                    BlockPos base = column.relative(side, w);
                    BlockPos pos = new BlockPos(base.getX(), floor + h, base.getZ());
                    if (pos.equals(hit)) continue;
                    if (!breakExtra(stack, level, pos, player)) return;
                }
            }
        }
    }

    /** Breaks one extra block if the drill can mine it and has charge and durability left. */
    private boolean breakExtra(ItemStack stack, Level level, BlockPos pos, Player player) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.getDestroySpeed(level, pos) < 0 || !stack.isCorrectToolForDrops(state)) return true;
        if (stack.isEmpty() || stack.getMaxDamage() - stack.getDamageValue() <= 1) return false;
        if (!use(stack, ENERGY_PER_BLOCK)) return false;
        if (level.destroyBlock(pos, true, player, 512)) stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        return true;
    }

    // ---------------------------------------------------------------- right-click

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        // Charge from a generator.
        if (!(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof Energy.Source source)) {
            return InteractionResult.PASS;
        }
        if (context.getLevel().isClientSide()) return InteractionResult.SUCCESS;
        int taken = source.extract(capacity() - energy(stack));
        setEnergy(stack, energy(stack) + taken);
        Player player = context.getPlayer();
        if (player != null) {
            player.sendOverlayMessage(Component.translatable("message.factorycraft.drill.charge",
                    Math.round(100.0F * energy(stack) / capacity())));
        }
        context.getLevel().playSound(null, context.getClickedPos(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.5F, 1.8F);
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        if (player.isSecondaryUseActive()) {
            if (player.getCooldowns().isOnCooldown(stack)) return InteractionResult.FAIL;
            if (!use(stack, SCAN_COST)) {
                player.sendOverlayMessage(Component.translatable("message.factorycraft.drill.no_charge"));
                return InteractionResult.FAIL;
            }
            int found = DroneEntity.markOres(server, player.blockPosition(), 8, 40, 200);
            player.getCooldowns().addCooldown(stack, 100);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.5F, 2.0F);
            player.sendOverlayMessage(Component.translatable("message.factorycraft.drone.scan", found));
            return InteractionResult.SUCCESS_SERVER;
        }
        Mode next = data(stack).mode() == Mode.VEIN ? Mode.TUNNEL : Mode.VEIN;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt("Mode", next.ordinal()));
        player.sendOverlayMessage(Component.translatable("message.factorycraft.drill.mode",
                Component.translatable("message.factorycraft.drill.mode." + next.name().toLowerCase())));
        return InteractionResult.SUCCESS_SERVER;
    }

    // ---------------------------------------------------------------- display

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        CompoundView view = data(stack);
        tooltip.accept(Component.translatable("tooltip.factorycraft.drill.charge",
                Math.round(100.0F * view.energy() / capacity())).withStyle(ChatFormatting.AQUA));
        tooltip.accept(Component.translatable("tooltip.factorycraft.drill.mode",
                Component.translatable("message.factorycraft.drill.mode." + view.mode().name().toLowerCase()))
                .withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.factorycraft.drill.help").withStyle(ChatFormatting.DARK_GRAY));
    }
}
