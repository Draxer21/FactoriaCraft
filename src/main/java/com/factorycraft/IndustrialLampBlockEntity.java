package com.factorycraft;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;

/**
 * Industrial lamp: a bright floodlight (light 15) that, while powered (5 E/t),
 * stops hostile mobs from spawning naturally within 32 blocks. Keeps a whole
 * factory yard safe at night.
 */
@EventBusSubscriber(modid = FactoryCraft.MOD_ID)
public final class IndustrialLampBlockEntity extends MachineBlockEntity implements Energy.Consumer {
    public static final int RANGE = 32;
    private static final int ENERGY = 5;
    /** Powered lamps: position -> last tick they had power (per dimension). */
    private static final Map<ResourceKey<Level>, Map<BlockPos, Long>> LAMPS = new ConcurrentHashMap<>();

    private final Energy.Link link = new Energy.Link(this);

    public IndustrialLampBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryCraft.INDUSTRIAL_LAMP_BLOCK_ENTITY.value(), pos, state, 0);
    }

    @Override protected boolean isInputSlot(int slot) { return false; }
    @Override protected boolean isOutputSlot(int slot) { return false; }
    @Override protected boolean accepts(int slot, ItemStack stack) { return false; }

    @Override
    protected boolean work(Level level) {
        if (link.draw(level, ENERGY) < ENERGY) return false;
        LAMPS.computeIfAbsent(level.dimension(), k -> new ConcurrentHashMap<>()).put(worldPosition.immutable(), level.getGameTime());
        return true;
    }

    @Override
    public Energy.Link energyLink() {
        return link;
    }

    static boolean covered(Level level, BlockPos pos) {
        Map<BlockPos, Long> map = LAMPS.get(level.dimension());
        if (map == null) return false;
        long now = level.getGameTime();
        for (Iterator<Map.Entry<BlockPos, Long>> it = map.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<BlockPos, Long> entry = it.next();
            if (now - entry.getValue() > 40) {
                it.remove();
                continue;
            }
            if (entry.getKey().distSqr(pos) <= RANGE * RANGE) return true;
        }
        return false;
    }

    @SubscribeEvent
    public static void onSpawn(FinalizeSpawnEvent event) {
        if (!(event.getEntity() instanceof Enemy)) return;
        EntitySpawnReason reason = event.getSpawnType();
        if (reason != EntitySpawnReason.NATURAL && reason != EntitySpawnReason.CHUNK_GENERATION) return;
        if (covered(event.getEntity().level(), event.getEntity().blockPosition())) event.setSpawnCancelled(true);
    }
}
