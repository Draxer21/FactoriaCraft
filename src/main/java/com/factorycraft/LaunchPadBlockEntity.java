package com.factorycraft;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Launch pad: load a rocket, a satellite and 8 rocket fuel, then sneak + right-click
 * (or give it a redstone signal) to launch. A 5-second countdown draws 20 000 power,
 * then the rocket lifts off and puts the satellite in orbit. Needs open sky.
 */
public final class LaunchPadBlockEntity extends PoweredMachineBlockEntity {
    public static final int FUEL = 8;
    private static final int COUNTDOWN = 100;
    private static final int ENERGY_PER_TICK = 200;
    private int countdown = -1;

    public LaunchPadBlockEntity(BlockPos pos, BlockState state) {
        super(Endgame.LAUNCH_PAD_BLOCK_ENTITY.value(), pos, state, 3, 0);
    }

    @Override protected int time() { return COUNTDOWN; }
    @Override protected int energyPerTick() { return ENERGY_PER_TICK; }
    @Override protected MenuType<ProcessingMenu> menuType() { return Endgame.LAUNCH_PAD_MENU.value(); }
    @Override protected String nameKey() { return "block.factorycraft.launch_pad"; }
    @Override public boolean supportsUpgrades() { return false; }
    @Override protected @Nullable Operation find(Level level) { return null; }
    @Override protected List<ItemStack> produce(Level level, Operation operation) { return List.of(); }

    @Override
    protected boolean accepts(int slot, ItemStack stack) {
        return switch (slot) {
            case 0 -> stack.is(Endgame.ROCKET.value());
            case 1 -> stack.is(Endgame.SOLAR_SATELLITE.value()) || stack.is(Endgame.SURVEY_SATELLITE.value());
            case 2 -> stack.is(Endgame.ROCKET_FUEL.value());
            default -> false;
        };
    }

    /** What's missing for a launch, or null if ready. */
    @Nullable Component missing(Level level) {
        if (!items.get(0).is(Endgame.ROCKET.value())) return Component.translatable("message.factorycraft.launch.no_rocket");
        if (!accepts(1, items.get(1))) return Component.translatable("message.factorycraft.launch.no_payload");
        if (!items.get(2).is(Endgame.ROCKET_FUEL.value()) || items.get(2).getCount() < FUEL) return Component.translatable("message.factorycraft.launch.no_fuel", FUEL);
        for (int dy = 1; dy <= 12; dy++) {
            if (!level.getBlockState(worldPosition.above(dy)).getCollisionShape(level, worldPosition.above(dy)).isEmpty()) {
                return Component.translatable("message.factorycraft.launch.blocked");
            }
        }
        return null;
    }

    public Component start(Level level) {
        if (countdown >= 0) return Component.translatable("message.factorycraft.launch.counting");
        Component missing = missing(level);
        if (missing != null) return missing;
        countdown = COUNTDOWN;
        setChanged();
        return Component.translatable("message.factorycraft.launch.countdown");
    }

    boolean counting() {
        return countdown >= 0;
    }

    @Override
    protected boolean work(Level level) {
        if (countdown < 0 && level.hasNeighborSignal(worldPosition) && level.getGameTime() % 20 == 0 && missing(level) == null) start(level);
        if (countdown < 0 || !(level instanceof ServerLevel server)) return false;
        if (missing(level) != null) {
            countdown = -1;
            return false;
        }
        if (!drawPower(level, ENERGY_PER_TICK)) return true; // hold the count until there's power
        setProgress(COUNTDOWN - countdown);
        if (countdown % 20 == 0) {
            server.playSound(null, worldPosition, SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.BLOCKS, 2.0F, countdown == 0 ? 2.0F : 1.0F);
            server.sendParticles(ParticleTypes.CLOUD, worldPosition.getX() + 0.5, worldPosition.getY() + 1.1, worldPosition.getZ() + 0.5,
                    10, 0.6, 0.1, 0.6, 0.02);
        }
        if (countdown-- > 0) return true;
        boolean survey = items.get(1).is(Endgame.SURVEY_SATELLITE.value());
        items.get(0).shrink(1);
        items.get(1).shrink(1);
        items.get(2).shrink(FUEL);
        setProgress(0);
        setChanged();
        RocketEntity.launch(server, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5, survey);
        return true;
    }
}
