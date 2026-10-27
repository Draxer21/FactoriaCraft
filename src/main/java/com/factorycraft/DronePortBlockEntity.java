package com.factorycraft;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Drone port: sends what's in its 6 cargo slots to another port by delivery drone,
 * up to 4 stacks a trip, and receives deliveries into its 8 output slots (pushed
 * out the front like any machine). Dock drones by right-clicking with a delivery
 * drone; link with the wrench (sneak-click the sending port, then the receiving one).
 * Range 512 blocks; 400 power a trip. Drones fly 1 block per tick and come home.
 */
public final class DronePortBlockEntity extends PoweredMachineBlockEntity {
    public static final int CARGO = 6;
    public static final int RANGE = 512;
    public static final int MAX_DRONES = 4;
    private static final int STACKS_PER_TRIP = 4;
    private static final int ENERGY_PER_TRIP = 400;

    private @Nullable BlockPos target;
    private int drones;
    /** Deliveries on their way here, and when they land. */
    private final NonNullList<ItemStack> transit = NonNullList.withSize(STACKS_PER_TRIP, ItemStack.EMPTY);
    private long arrival = -1;
    /** Game times our drones come back home. */
    private final List<Long> returns = new ArrayList<>();
    private int timer;

    public DronePortBlockEntity(BlockPos pos, BlockState state) {
        super(Logistics.DRONE_PORT_BLOCK_ENTITY.value(), pos, state, CARGO, ProcessingMenu.BIG_OUTPUTS);
    }

    @Override protected int time() { return 40; }
    @Override protected int energyPerTick() { return 0; }
    @Override protected MenuType<ProcessingMenu> menuType() { return Logistics.DRONE_PORT_MENU.value(); }
    @Override protected String nameKey() { return "block.factorycraft.drone_port"; }
    @Override public boolean supportsUpgrades() { return false; }
    @Override protected boolean accepts(int slot, ItemStack stack) { return slot < CARGO; }
    @Override protected @Nullable Operation find(Level level) { return null; }
    @Override protected List<ItemStack> produce(Level level, Operation operation) { return List.of(); }

    @Override
    public Component getDisplayName() {
        Component to = target == null ? Component.translatable("gui.factorycraft.drone_port.unlinked")
                : Component.literal(target.getX() + " " + target.getY() + " " + target.getZ());
        return super.getDisplayName().copy().append(" · ").append(Component.translatable("gui.factorycraft.drone_port.info", drones + returns.size(), drones, to));
    }

    public int drones() {
        return drones;
    }

    public @Nullable BlockPos target() {
        return target;
    }

    public boolean dock() {
        if (drones + returns.size() >= MAX_DRONES) return false;
        drones++;
        setChanged();
        return true;
    }

    /** Takes a docked drone back as an item (sneak + empty hand). */
    public boolean undock() {
        if (drones <= 0) return false;
        drones--;
        setChanged();
        return true;
    }

    public void link(Player player, BlockPos to) {
        if (to.equals(worldPosition)) return;
        if (to.distSqr(worldPosition) > (double) RANGE * RANGE) {
            player.sendOverlayMessage(Component.translatable("message.factorycraft.drone_port.far", RANGE));
            return;
        }
        target = to.immutable();
        setChanged();
        player.sendOverlayMessage(Component.translatable("message.factorycraft.drone_port.linked", to.getX(), to.getY(), to.getZ()));
    }

    void setTarget(BlockPos to) {
        target = to.immutable();
        setChanged();
    }

    /** Can a delivery land here now? */
    boolean canReceive() {
        if (arrival >= 0) return false;
        int free = 0;
        for (int i = CARGO; i < CARGO + ProcessingMenu.BIG_OUTPUTS; i++) if (items.get(i).isEmpty()) free++;
        return free >= STACKS_PER_TRIP;
    }

    void receive(List<ItemStack> cargo, long when) {
        for (int i = 0; i < cargo.size() && i < transit.size(); i++) transit.set(i, cargo.get(i));
        arrival = when;
        setChanged();
    }

    @Override
    protected boolean work(Level level) {
        if (!(level instanceof ServerLevel server)) return false;
        long now = level.getGameTime();
        boolean busy = arrival >= 0 || !returns.isEmpty();
        // A delivery lands.
        if (arrival >= 0 && now >= arrival) {
            List<ItemStack> cargo = new ArrayList<>();
            for (ItemStack stack : transit) if (!stack.isEmpty()) cargo.add(stack);
            if (canStore(cargo)) {
                store(cargo);
                for (int i = 0; i < transit.size(); i++) transit.set(i, ItemStack.EMPTY);
                arrival = -1;
                level.playSound(null, worldPosition, SoundEvents.BEEHIVE_EXIT, SoundSource.BLOCKS, 0.8F, 1.4F);
                setChanged();
            }
        }
        // Drones come home.
        if (returns.removeIf(t -> now >= t)) {
            drones = Math.min(MAX_DRONES, drones + 1);
            setChanged();
        }
        if (++timer < 40) return busy;
        timer = 0;
        // Send a trip.
        if (target == null || drones <= 0 || !level.isLoaded(target)) return busy;
        if (!(level.getBlockEntity(target) instanceof DronePortBlockEntity destination) || !destination.canReceive()) return busy;
        List<ItemStack> cargo = new ArrayList<>();
        for (int i = 0; i < CARGO && cargo.size() < STACKS_PER_TRIP; i++) {
            if (!items.get(i).isEmpty()) cargo.add(items.get(i));
        }
        if (cargo.isEmpty() || !drawPower(level, ENERGY_PER_TRIP)) return busy;
        List<ItemStack> sent = new ArrayList<>();
        for (int i = 0; i < CARGO && sent.size() < STACKS_PER_TRIP; i++) {
            if (items.get(i).isEmpty()) continue;
            sent.add(items.get(i));
            items.set(i, ItemStack.EMPTY);
        }
        int flight = (int) Math.max(40, Math.sqrt(worldPosition.distSqr(target)) + 40);
        destination.receive(sent, now + flight);
        drones--;
        returns.add(now + 2L * flight);
        DeliveryDroneEntity.launch(server, Vec3.atCenterOf(worldPosition).add(0, 0.8, 0), Vec3.atCenterOf(target).add(0, 0.8, 0), flight);
        level.playSound(null, worldPosition, SoundEvents.BEEHIVE_ENTER, SoundSource.BLOCKS, 0.8F, 1.2F);
        setChanged();
        return true;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        int[] t = input.getIntArray("Target").orElse(new int[0]);
        target = t.length == 3 ? new BlockPos(t[0], t[1], t[2]) : null;
        drones = input.getIntOr("Drones", 0);
        arrival = input.getLongOr("Arrival", -1);
        List<ItemStack> stored = input.read("Transit", ItemStack.OPTIONAL_CODEC.listOf()).orElse(List.of());
        for (int i = 0; i < transit.size(); i++) transit.set(i, i < stored.size() ? stored.get(i) : ItemStack.EMPTY);
        returns.clear();
        int count = input.getIntOr("Returns", 0);
        for (int i = 0; i < count; i++) returns.add(input.getLongOr("Return" + i, 0));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (target != null) output.putIntArray("Target", new int[] {target.getX(), target.getY(), target.getZ()});
        output.putInt("Drones", drones);
        output.putLong("Arrival", arrival);
        output.store("Transit", ItemStack.OPTIONAL_CODEC.listOf(), new ArrayList<>(transit));
        output.putInt("Returns", returns.size());
        for (int i = 0; i < returns.size(); i++) output.putLong("Return" + i, returns.get(i));
    }
}
