package com.factorycraft;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Diesel engine and upgrade bay shared by every driveable machine.
 *  - Starts by itself when someone gets in (if there is fuel) and stops when they get out.
 *  - The driver can switch it off and on with the engine key (R by default).
 *  - Burns fuel: slowly while idling, faster while driving or working. With the
 *    engine off the machine can't move, work, or light its headlights.
 *  - Refuel by right-clicking the machine with any furnace fuel (sneak to put in
 *    the whole stack). Machines with a bay also burn coal, charcoal, coal blocks
 *    and blaze rods they find in it, so a tunneler can feed on the coal it mines.
 *  - Upgrade modules: right-click with a module to install it (up to 4),
 *    right-click with the wrench to take them all out.
 */
public final class VehicleEngine {
    /** Implemented by machines that have an engine. */
    public interface Holder {
        VehicleEngine engine();
    }

    /** Fuel is measured in furnace burn ticks: one coal = 1600. */
    public static final int MAX_FUEL = 64_000;
    public static final int STARTER_FUEL = 8_000;
    public static final int MAX_UPGRADES = 4;
    /** Fuel units per tick: idling and driving/working, before upgrades. */
    private static final double IDLE_BURN = 0.25;
    private static final double BUSY_BURN = 1.0;
    /** Fuel units per tick a solar panel adds in full daylight. */
    private static final double SOLAR_CHARGE = 0.35;
    private static final int REFUEL_CHECK_INTERVAL = 20;

    /** Drops the ore filter throws away. */
    private static final Set<Item> JUNK = Set.of(
            Items.COBBLESTONE, Items.STONE, Items.COBBLED_DEEPSLATE, Items.DEEPSLATE, Items.DIRT, Items.COARSE_DIRT,
            Items.ROOTED_DIRT, Items.GRAVEL, Items.SAND, Items.RED_SAND, Items.SANDSTONE, Items.RED_SANDSTONE,
            Items.NETHERRACK, Items.TUFF, Items.ANDESITE, Items.DIORITE, Items.GRANITE, Items.CALCITE,
            Items.BLACKSTONE, Items.BASALT, Items.SMOOTH_BASALT, Items.END_STONE, Items.SOUL_SAND, Items.SOUL_SOIL,
            Items.MUD, Items.DRIPSTONE_BLOCK, Items.POINTED_DRIPSTONE, Items.MOSS_BLOCK);

    private final Entity vehicle;
    private final EntityDataAccessor<Boolean> running;
    private final EntityDataAccessor<Integer> fuel;
    private final EntityDataAccessor<Integer> upgrades;
    private boolean wasDriven;
    /** Fractional fuel burnt (or charged) but not yet taken from the tank. */
    private double burnDebt;
    private @Nullable ItemStack fortuneTool;

    public VehicleEngine(Entity vehicle, EntityDataAccessor<Boolean> running, EntityDataAccessor<Integer> fuel,
                         EntityDataAccessor<Integer> upgrades) {
        this.vehicle = vehicle;
        this.running = running;
        this.fuel = fuel;
        this.upgrades = upgrades;
    }

    /** Call from the vehicle's {@code defineSynchedData}. */
    public static void define(SynchedEntityData.Builder builder, EntityDataAccessor<Boolean> running,
                              EntityDataAccessor<Integer> fuel, EntityDataAccessor<Integer> upgrades) {
        builder.define(running, false);
        builder.define(fuel, STARTER_FUEL);
        builder.define(upgrades, 0);
    }

    // ---------------------------------------------------------------- state

    public boolean isRunning() {
        return vehicle.getEntityData().get(running);
    }

    public int getFuel() {
        return vehicle.getEntityData().get(fuel);
    }

    public int maxFuel() {
        return has(Upgrade.BIG_TANK) ? MAX_FUEL * 2 : MAX_FUEL;
    }

    public float getFuelFraction() {
        if (isFusion()) return 1.0F;
        return Math.min(1.0F, getFuel() / (float) maxFuel());
    }

    private void setFuel(int value) {
        vehicle.getEntityData().set(fuel, Math.max(0, Math.min(maxFuel(), value)));
    }

    /** Adds fuel if it all fits (used by the tanker's fuel station). */
    public boolean addFuel(int amount) {
        if (getFuel() + amount > maxFuel()) return false;
        setFuel(getFuel() + amount);
        return true;
    }

    private void setRunning(boolean on) {
        if (isRunning() != on) vehicle.getEntityData().set(running, on);
    }

    // ---------------------------------------------------------------- upgrades

    public boolean has(Upgrade upgrade) {
        return (vehicle.getEntityData().get(upgrades) & upgrade.bit()) != 0;
    }

    public List<Upgrade> installed() {
        List<Upgrade> list = new ArrayList<>();
        for (Upgrade upgrade : Upgrade.values()) {
            if (has(upgrade)) list.add(upgrade);
        }
        return list;
    }

    /** Top speed multiplier. */
    public double speedFactor() {
        if (has(Upgrade.TURBO_II)) return 1.7;
        return has(Upgrade.TURBO) ? 1.35 : 1.0;
    }

    /** Module slots in use (the fusion engine takes two). */
    public int slotsUsed() {
        int used = 0;
        for (Upgrade upgrade : installed()) used += upgrade.slots();
        return used;
    }

    /** Fusion engine: runs forever without fuel. */
    public boolean isFusion() {
        return has(Upgrade.FUSION_ENGINE);
    }

    private double burnFactor() {
        double factor = 1.0;
        if (has(Upgrade.TURBO)) factor *= 1.5;
        if (has(Upgrade.TURBO_II)) factor *= 2.0;
        if (has(Upgrade.EFFICIENT_ENGINE)) factor *= 0.6;
        if (has(Upgrade.HYBRID_ENGINE)) factor *= 0.75;
        return factor;
    }

    public boolean wide() {
        return has(Upgrade.WIDE_DRILL);
    }

    public boolean filtersJunk() {
        return has(Upgrade.ORE_FILTER);
    }

    /** Charging from the sun right now (daylight, open sky, no rain)? */
    public boolean isCharging() {
        if (!has(Upgrade.SOLAR_PANEL)) return false;
        Level level = vehicle.level();
        BlockPos top = BlockPos.containing(vehicle.getX(), vehicle.getBoundingBox().maxY + 0.5, vehicle.getZ());
        return level.isBrightOutside() && !level.isRaining() && level.canSeeSky(top) && getFuel() < maxFuel();
    }

    /** The tool the machine "mines" with: Fortune III with the fortune module, nothing otherwise. */
    public ItemStack tool() {
        if (!has(Upgrade.FORTUNE) && !has(Upgrade.FORTUNE_II)) return ItemStack.EMPTY;
        if (fortuneTool == null) {
            ItemStack pick = new ItemStack(Items.NETHERITE_PICKAXE);
            pick.enchant(vehicle.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(Enchantments.FORTUNE), has(Upgrade.FORTUNE_II) ? 4 : 3);
            fortuneTool = pick;
        }
        return fortuneTool;
    }

    /** Whether a mined drop should be kept (false = thrown away by the ore filter). */
    public boolean keeps(ItemStack drop) {
        return !filtersJunk() || !JUNK.contains(drop.getItem());
    }

    private InteractionResult install(Player player, ItemStack held, Upgrade upgrade) {
        if (!upgrade.fits(vehicle)) {
            player.sendOverlayMessage(Component.translatable("message.factorycraft.upgrade.wrong_machine"));
            return InteractionResult.FAIL;
        }
        if (has(upgrade)) {
            player.sendOverlayMessage(Component.translatable("message.factorycraft.upgrade.already"));
            return InteractionResult.FAIL;
        }
        if (slotsUsed() + upgrade.slots() > MAX_UPGRADES) {
            player.sendOverlayMessage(Component.translatable("message.factorycraft.upgrade.full", MAX_UPGRADES));
            return InteractionResult.FAIL;
        }
        vehicle.getEntityData().set(upgrades, vehicle.getEntityData().get(upgrades) | upgrade.bit());
        fortuneTool = null;
        if (!player.getAbilities().instabuild) held.shrink(1);
        vehicle.level().playSound(null, vehicle.getX(), vehicle.getY(), vehicle.getZ(),
                SoundEvents.ANVIL_USE, SoundSource.NEUTRAL, 0.5F, 1.6F);
        player.sendOverlayMessage(Component.translatable("message.factorycraft.upgrade.installed",
                Component.translatable(upgrade.translationKey())));
        return InteractionResult.SUCCESS_SERVER;
    }

    private InteractionResult removeAll(Player player) {
        List<Upgrade> list = installed();
        if (list.isEmpty()) {
            player.sendOverlayMessage(Component.translatable("message.factorycraft.upgrade.none"));
            return InteractionResult.FAIL;
        }
        for (Upgrade upgrade : list) {
            ItemStack module = new ItemStack(FactoryCraft.UPGRADE_ITEMS.get(upgrade).value());
            if (!player.getInventory().add(module)) player.drop(module, false);
        }
        vehicle.getEntityData().set(upgrades, 0);
        fortuneTool = null;
        setFuel(getFuel()); // clamp to the normal tank
        vehicle.level().playSound(null, vehicle.getX(), vehicle.getY(), vehicle.getZ(),
                SoundEvents.ANVIL_USE, SoundSource.NEUTRAL, 0.5F, 1.2F);
        player.sendOverlayMessage(Component.translatable("message.factorycraft.upgrade.removed", list.size()));
        return InteractionResult.SUCCESS_SERVER;
    }

    // ---------------------------------------------------------------- ticking (server)

    /**
     * Call every tick after {@code super.tick()}.
     * @param driver the controlling player, or null
     * @param busy   whether the machine is driving or working this tick
     * @param bay    the machine's storage, searched for fuel, or null
     */
    public void tick(@Nullable Player driver, boolean busy, @Nullable Container bay) {
        if (!(vehicle.level() instanceof ServerLevel)) return;
        boolean driven = driver != null;
        if (driven && !wasDriven) {
            if (getFuel() > 0 || isFusion()) setRunning(true);
            else driver.sendOverlayMessage(Component.translatable("message.factorycraft.engine.no_fuel"));
        } else if (!driven && wasDriven) {
            setRunning(false);
        }
        wasDriven = driven;

        if (bay != null && vehicle.tickCount % REFUEL_CHECK_INTERVAL == 0) refuelFromBay(bay);

        double delta = 0.0;
        if (isRunning() && !isFusion()) {
            boolean electricIdle = !busy && has(Upgrade.HYBRID_ENGINE);
            if (!electricIdle) delta -= (busy ? BUSY_BURN : IDLE_BURN) * burnFactor();
        }
        if (isCharging()) delta += SOLAR_CHARGE;
        burnDebt += delta;
        int whole = (int) burnDebt; // truncates toward zero
        if (whole != 0) {
            setFuel(getFuel() + whole);
            burnDebt -= whole;
        }

        if (isRunning() && getFuel() <= 0 && !isFusion()) {
            setRunning(false);
            if (driver != null) driver.sendOverlayMessage(Component.translatable("message.factorycraft.engine.no_fuel"));
        }
    }

    /** Engine key pressed by the driver. */
    public void toggle(Player driver) {
        if (isRunning()) {
            setRunning(false);
        } else if (getFuel() > 0 || isFusion()) {
            setRunning(true);
        } else {
            driver.sendOverlayMessage(Component.translatable("message.factorycraft.engine.no_fuel"));
        }
    }

    // ---------------------------------------------------------------- fuel

    /** Bay fuel the engine helps itself to: never blocks, planks or logs a paver might need. */
    private static boolean isBayFuel(ItemStack stack) {
        return stack.is(Items.COAL) || stack.is(Items.CHARCOAL) || stack.is(Items.COAL_BLOCK) || stack.is(Items.BLAZE_ROD);
    }

    private void refuelFromBay(Container bay) {
        for (int i = 0; i < bay.getContainerSize(); i++) {
            ItemStack stack = bay.getItem(i);
            if (stack.isEmpty() || !isBayFuel(stack)) continue;
            int burn = vehicle.level().fuelValues().burnDuration(stack);
            while (burn > 0 && !stack.isEmpty() && getFuel() + burn <= maxFuel()) {
                setFuel(getFuel() + burn);
                stack.shrink(1);
            }
            bay.setItem(i, stack.isEmpty() ? ItemStack.EMPTY : stack);
            if (getFuel() + 1600 > maxFuel()) return;
        }
    }

    /**
     * Right-click on the machine: installs a module, removes modules with the
     * wrench, or refuels with a fuel item. Returns null when none of those apply,
     * so the machine's normal interaction can run instead.
     */
    public @Nullable InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        boolean module = held.getItem() instanceof UpgradeModuleItem;
        boolean wrench = held.getItem() instanceof WrenchItem;
        int burn = held.isEmpty() || module || wrench ? 0 : vehicle.level().fuelValues().burnDuration(held);
        if (!module && !wrench && burn <= 0) return null;
        if (vehicle.level().isClientSide()) return InteractionResult.SUCCESS;

        if (module) return install(player, held, ((UpgradeModuleItem) held.getItem()).upgrade());
        if (wrench) return removeAll(player);

        int added = 0;
        int wanted = player.isSecondaryUseActive() ? held.getCount() : 1;
        while (added < wanted && !held.isEmpty() && getFuel() + burn <= maxFuel()) {
            setFuel(getFuel() + burn);
            added++;
            if (player.getAbilities().instabuild) continue;
            ItemStackTemplate remainder = held.getCraftingRemainder();
            held.shrink(1);
            if (remainder != null) {
                ItemStack rest = remainder.create();
                if (held.isEmpty()) {
                    player.setItemInHand(hand, rest);
                    held = rest;
                    break;
                } else if (!player.getInventory().add(rest)) {
                    player.drop(rest, false);
                }
            }
        }
        if (added == 0) {
            player.sendOverlayMessage(Component.translatable("message.factorycraft.engine.full"));
            return InteractionResult.FAIL;
        }
        vehicle.level().playSound(null, vehicle.getX(), vehicle.getY(), vehicle.getZ(),
                SoundEvents.BUCKET_FILL_LAVA, SoundSource.NEUTRAL, 0.6F, 1.4F);
        player.sendOverlayMessage(Component.translatable("message.factorycraft.engine.fuel",
                Math.round(getFuelFraction() * 100)));
        return InteractionResult.SUCCESS_SERVER;
    }

    // ---------------------------------------------------------------- saving

    public void save(ValueOutput output) {
        output.putInt("Fuel", getFuel());
        output.putInt("Upgrades", vehicle.getEntityData().get(upgrades));
    }

    public void load(ValueInput input) {
        vehicle.getEntityData().set(upgrades, input.getIntOr("Upgrades", 0));
        setFuel(input.getIntOr("Fuel", STARTER_FUEL));
    }

    /** True while {@code entity} is a machine whose engine is off (for lights, sounds, the HUD). */
    public static boolean isOff(Entity entity) {
        return entity instanceof Holder holder && !holder.engine().isRunning();
    }

    /** The engine bay of {@code entity}, if it has one. */
    public static @Nullable VehicleEngine of(Entity entity) {
        return entity instanceof Holder holder ? holder.engine() : null;
    }
}
