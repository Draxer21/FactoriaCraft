package com.factorycraft;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Zone controller: protects the area around it (17x17, 33x33 or 65x65, full height):
 * only its owner and players carrying one of its access cards may break or place
 * blocks or use doors, chests and machines there, and explosions leave it intact.
 * The owner sneak-right-clicks to change the size, and right-clicks with a blank
 * access card to bind it to this zone.
 */
public final class ZoneControllerBlockEntity extends MachineBlockEntity implements Owned {
    private static final int[] RADII = {8, 16, 32};
    /** Active zones per dimension: centre -> zone, refreshed every tick by the loaded controllers. */
    private static final Map<ResourceKey<Level>, Map<BlockPos, Zone>> ZONES = new ConcurrentHashMap<>();

    record Zone(@Nullable UUID owner, int radius, long seen) {
        boolean contains(BlockPos centre, BlockPos pos) {
            return Math.abs(pos.getX() - centre.getX()) <= radius && Math.abs(pos.getZ() - centre.getZ()) <= radius;
        }
    }

    private @Nullable UUID owner;
    private int radius = 16;

    public ZoneControllerBlockEntity(BlockPos pos, BlockState state) {
        super(Economy.ZONE_CONTROLLER_BLOCK_ENTITY.value(), pos, state, 0);
    }

    @Override protected boolean isInputSlot(int slot) { return false; }
    @Override protected boolean isOutputSlot(int slot) { return false; }
    @Override protected boolean accepts(int slot, ItemStack stack) { return false; }
    @Override public void setOwner(UUID owner) { this.owner = owner; setChanged(); }
    @Override public @Nullable UUID owner() { return owner; }

    public boolean isOwner(Player player) {
        return owner == null || owner.equals(player.getUUID());
    }

    @Override
    protected boolean work(Level level) {
        ZONES.computeIfAbsent(level.dimension(), k -> new ConcurrentHashMap<>())
                .put(worldPosition.immutable(), new Zone(owner, radius, level.getGameTime()));
        return true;
    }

    public void cycle(Player player) {
        int next = RADII[0];
        for (int r : RADII) if (r > radius) { next = r; break; }
        radius = next;
        setChanged();
        player.sendOverlayMessage(Component.translatable("message.factorycraft.zone.size", radius * 2 + 1, radius * 2 + 1));
    }

    public void bind(Player player, ItemStack card) {
        var tag = card.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putIntArray("Zone", new int[] {worldPosition.getX(), worldPosition.getY(), worldPosition.getZ()});
        card.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        player.sendOverlayMessage(Component.translatable("message.factorycraft.zone.card"));
    }

    private static boolean carriesCard(Player player, BlockPos zone) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.is(Economy.ACCESS_CARD.value())) continue;
            CustomData data = stack.get(DataComponents.CUSTOM_DATA);
            if (data == null) continue;
            int[] p = data.copyTag().getIntArray("Zone").orElse(new int[0]);
            if (p.length == 3 && p[0] == zone.getX() && p[1] == zone.getY() && p[2] == zone.getZ()) return true;
        }
        return false;
    }

    /** The zone covering {@code pos}, if any (its centre). */
    static @Nullable BlockPos zoneAt(Level level, BlockPos pos) {
        Map<BlockPos, Zone> zones = ZONES.get(level.dimension());
        if (zones == null) return null;
        long now = level.getGameTime();
        for (Iterator<Map.Entry<BlockPos, Zone>> it = zones.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<BlockPos, Zone> e = it.next();
            if (now - e.getValue().seen() > 40) {
                it.remove();
                continue;
            }
            if (e.getValue().contains(e.getKey(), pos)) return e.getKey();
        }
        return null;
    }

    /** May this player change things at {@code pos}? */
    public static boolean allowed(Level level, BlockPos pos, @Nullable Player player) {
        BlockPos centre = zoneAt(level, pos);
        if (centre == null) return true;
        if (player == null) return false;
        if (player.isCreative()) return true;
        Zone zone = ZONES.get(level.dimension()).get(centre);
        if (zone == null || zone.owner() == null || zone.owner().equals(player.getUUID())) return true;
        return carriesCard(player, centre);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        owner = input.read("Owner", UUIDUtil.CODEC).orElse(null);
        radius = input.getIntOr("Radius", 16);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (owner != null) output.store("Owner", UUIDUtil.CODEC, owner);
        output.putInt("Radius", radius);
    }
}
