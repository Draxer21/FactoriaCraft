package com.factorycraft;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Nuclear reactor: burns fuel rods (thorium < uranium < plutonium) for huge
 * amounts of power. While it runs it heats up and boils its cooling water;
 * with water the heat stays down, without it the core climbs to 100% and the
 * reactor MELTS DOWN: a big explosion and a radioactive cloud.
 * A redstone signal is an emergency shutdown (SCRAM): it stops burning at once.
 * Slot 0: fuel rods in. Slot 1: depleted rods out (pushed out the front).
 */
public final class NuclearReactorBlockEntity extends MachineBlockEntity implements MenuProvider, Energy.Source {
    public static final int CAPACITY = 2_000_000;
    public static final int MAX_HEAT = 10_000;
    public static final int WATER_CAPACITY = 16_000;
    public static final int BUCKET = 1_000;

    private int energy;
    private int burnLeft;
    private int burnMax;
    private int power;
    private int heatRate;
    private int heat;
    private int water;
    private int rate;
    private int turbinesThisTick;
    private int shielding = -1;

    public static final net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> SHIELDING =
            net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK,
                    net.minecraft.resources.Identifier.fromNamespaceAndPath(FactoryCraft.MOD_ID, "radiation_shielding"));
    /** Shielding blocks needed among the 26 around the reactor for it to count as contained. */
    public static final int SHIELDING_NEEDED = 16;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int id) {
            return switch (id) {
                case 0 -> energy / 100;
                case 1 -> CAPACITY / 100;
                case 2 -> burnLeft / 10;
                case 3 -> burnMax / 10;
                case 4 -> rate;
                case 5 -> heat;
                case 6 -> water;
                default -> 0;
            };
        }

        @Override
        public void set(int id, int value) {
            switch (id) {
                case 0 -> energy = value * 100;
                case 2 -> burnLeft = value * 10;
                case 3 -> burnMax = value * 10;
                case 4 -> rate = value;
                case 5 -> heat = value;
                case 6 -> water = value;
                default -> { }
            }
        }

        @Override
        public int getCount() {
            return ReactorMenu.DATA_COUNT;
        }
    };

    public NuclearReactorBlockEntity(BlockPos pos, BlockState state) {
        super(Nuclear.REACTOR_BLOCK_ENTITY.value(), pos, state, 2);
    }

    @Override protected boolean isInputSlot(int slot) { return slot == 0; }
    @Override protected boolean isOutputSlot(int slot) { return slot == 1; }

    @Override
    protected boolean accepts(int slot, ItemStack stack) {
        return slot == 0 && Nuclear.fuelOf(stack) != null;
    }

    /** Pours in a bucket of water (by hand or from a pump). */
    public boolean acceptWater() {
        if (water + BUCKET > WATER_CAPACITY) return false;
        water += BUCKET;
        setChanged();
        return true;
    }

    /** Steam for an adjacent turbine: returns the reactor's output, or 0. Uses extra water and takes heat away. */
    public int takeSteam() {
        if (rate <= 0 || water <= 0 || turbinesThisTick >= 4) return 0;
        turbinesThisTick++;
        water = Math.max(0, water - heatRate);
        heat = Math.max(0, heat - 4);
        setChanged();
        return rate;
    }

    /** How many of the 26 blocks around are radiation shielding (lead, concrete, obsidian). */
    public int shielding(Level level) {
        int count = 0;
        for (BlockPos p : BlockPos.betweenClosed(worldPosition.offset(-1, -1, -1), worldPosition.offset(1, 1, 1))) {
            if (p.equals(worldPosition)) continue;
            if (level.getBlockState(p).is(SHIELDING)) count++;
        }
        return count;
    }

    public boolean contained(Level level) {
        return shielding(level) >= SHIELDING_NEEDED;
    }

    public int water() { return water; }
    public int heatPercent() { return heat * 100 / MAX_HEAT; }
    public boolean running() { return rate > 0; }

    @Override
    protected boolean work(Level level) {
        boolean scram = level.hasNeighborSignal(worldPosition);
        rate = 0;
        turbinesThisTick = 0;
        // Load a new rod when the last one is spent and there's room for the power.
        if (!scram && burnLeft <= 0 && energy < CAPACITY && items.get(1).getCount() < items.get(1).getMaxStackSize()) {
            ItemStack rod = items.get(0);
            Nuclear.Fuel fuel = Nuclear.fuelOf(rod);
            if (fuel != null) {
                rod.shrink(1);
                burnLeft = burnMax = fuel.ticks();
                power = fuel.power();
                heatRate = fuel.heat();
                setChanged();
            }
        }
        boolean running = !scram && burnLeft > 0 && energy + power <= CAPACITY;
        if (running) {
            burnLeft--;
            energy += power;
            rate = power;
            heat += heatRate * 3;
            if (water > 0) {
                water = Math.max(0, water - heatRate);
                heat -= heatRate * 3 + 2;
            }
            if (burnLeft == 0) spentRod();
            setChanged();
        } else {
            heat -= water > 0 ? 12 : 4; // shut down: it cools off
        }
        heat = Math.max(0, heat);

        if (level instanceof ServerLevel server) {
            if (heat >= MAX_HEAT) {
                meltdown(server);
                return false;
            }
            if (heat > MAX_HEAT * 7 / 10 && level.getGameTime() % 20 == 0) {
                server.sendParticles(ParticleTypes.LARGE_SMOKE, worldPosition.getX() + 0.5, worldPosition.getY() + 1.1,
                        worldPosition.getZ() + 0.5, 6, 0.3, 0.1, 0.3, 0.02);
                level.playSound(null, worldPosition, SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.BLOCKS, 1.5F,
                        heat > MAX_HEAT * 9 / 10 ? 2.0F : 1.2F);
            }
            // Radiation leak when the reactor isn't shielded.
            if (running && level.getGameTime() % 100 == 0) {
                shielding = shielding(level);
                if (shielding < SHIELDING_NEEDED) {
                    Pollution.contaminate(server, worldPosition, 8, 3);
                    for (net.minecraft.world.entity.LivingEntity e : level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
                            new net.minecraft.world.phys.AABB(worldPosition).inflate(6))) {
                        float exposure = e instanceof Player p ? Radiation.exposure(p) : 1.0F;
                        if (e instanceof Player p && (p.isCreative() || p.isSpectator())) continue;
                        if (exposure > 0) Radiation.apply(e, 20.0F * exposure);
                    }
                }
            }
            if (running && water > 0 && level.getGameTime() % 30 == 0) {
                server.sendParticles(ParticleTypes.CLOUD, worldPosition.getX() + 0.5, worldPosition.getY() + 1.05,
                        worldPosition.getZ() + 0.5, 2, 0.2, 0.05, 0.2, 0.01);
            }
        }
        return running;
    }

    private void spentRod() {
        ItemStack out = items.get(1);
        if (out.isEmpty()) {
            items.set(1, new ItemStack(Nuclear.DEPLETED_FUEL_ROD.value()));
        } else if (out.is(Nuclear.DEPLETED_FUEL_ROD.value())) {
            out.grow(1);
        }
    }

    /** 100% heat: the reactor blows up and leaves a radioactive cloud. */
    private void meltdown(ServerLevel level) {
        BlockPos pos = worldPosition;
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;
        float strength = 6.0F + 4.0F * Math.min(1.0F, heatRate / 12.0F);
        items.clear();
        level.removeBlock(pos, false);
        level.explode(null, x, y, z, strength, true,
                FactoryConfig.meltdownBreaksBlocks() ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE);
        fallout(level, x, y, z, 8.0F, 1200);
    }

    /** A lingering radioactive cloud (meltdowns and mini-nukes). */
    public static void fallout(ServerLevel level, double x, double y, double z, float radius, int duration) {
        AreaEffectCloud cloud = new AreaEffectCloud(level, x, y, z);
        cloud.setRadius(radius);
        cloud.setDuration(duration);
        cloud.setRadiusPerTick(-radius / duration / 2.0F);
        cloud.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 1));
        cloud.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 200, 0));
        cloud.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 0));
        level.addFreshEntity(cloud);
        Pollution.contaminate(level, BlockPos.containing(x, y, z), Math.round(radius * 1.5F), Math.round(radius * 6));
        level.playSound(null, x, y, z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.BLOCKS, 4.0F, 0.5F);
    }

    // ---------------------------------------------------------------- energy

    @Override
    public int extract(int max) {
        int taken = Math.min(max, energy);
        if (taken > 0) {
            energy -= taken;
            setChanged();
        }
        return taken;
    }

    @Override public int stored() { return energy; }
    @Override public int rate() { return rate; }
    @Override public int capacity() { return CAPACITY; }

    // ---------------------------------------------------------------- menu & saving

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.factorycraft.nuclear_reactor");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ReactorMenu(id, inventory, this, data);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energy = input.getIntOr("Energy", 0);
        burnLeft = input.getIntOr("BurnLeft", 0);
        burnMax = input.getIntOr("BurnMax", 0);
        power = input.getIntOr("Power", 0);
        heatRate = input.getIntOr("HeatRate", 0);
        heat = input.getIntOr("Heat", 0);
        water = input.getIntOr("Water", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Energy", energy);
        output.putInt("BurnLeft", burnLeft);
        output.putInt("BurnMax", burnMax);
        output.putInt("Power", power);
        output.putInt("HeatRate", heatRate);
        output.putInt("Heat", heat);
        output.putInt("Water", water);
    }
}
