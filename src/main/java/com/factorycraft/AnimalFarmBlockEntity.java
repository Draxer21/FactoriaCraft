package com.factorycraft;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Shearable;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * Animal farm: looks after the animals within 6 blocks. Every 5 seconds it feeds
 * pairs of adults with the food in its slots so they breed (up to 16 of each
 * kind), shears sheep if it has shears, milks cows into empty buckets, and picks
 * up whatever lies on the ground (eggs, wool, feathers) into its outputs.
 */
public final class AnimalFarmBlockEntity extends PoweredMachineBlockEntity {
    private static final int INPUTS = 4;
    private static final int RANGE = 6;
    private static final int CAP = 16;
    private static final int ENERGY_PER_ACTION = 40;
    private int timer;

    public AnimalFarmBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryCraft.ANIMAL_FARM_BLOCK_ENTITY.value(), pos, state, INPUTS, ProcessingMenu.BIG_OUTPUTS);
    }

    @Override protected int time() { return 100; }
    @Override protected int energyPerTick() { return ENERGY_PER_ACTION; }
    @Override protected MenuType<ProcessingMenu> menuType() { return FactoryCraft.ANIMAL_FARM_MENU.value(); }
    @Override protected String nameKey() { return "block.factorycraft.animal_farm"; }

    @Override
    protected boolean accepts(int slot, ItemStack stack) {
        return slot < INPUTS;
    }

    private AABB area() {
        return new AABB(worldPosition).inflate(RANGE, 3, RANGE);
    }

    @Override
    protected boolean work(Level level) {
        if (!(level instanceof ServerLevel server)) return false;
        if (++timer < scaledTime(time())) return true;
        timer = 0;
        boolean busy = false;
        List<Animal> animals = level.getEntitiesOfClass(Animal.class, area(), Animal::isAlive);
        Map<EntityType<?>, Integer> counts = new HashMap<>();
        for (Animal animal : animals) counts.merge(animal.getType(), 1, Integer::sum);
        // Breeding: feed adults that are ready, two at a time, while there's room.
        Map<EntityType<?>, List<Animal>> ready = new HashMap<>();
        for (Animal animal : animals) {
            if (animal.isBaby() || animal.isInLove() || !animal.canFallInLove() || animal.getAge() != 0) continue;
            if (counts.getOrDefault(animal.getType(), 0) >= CAP) continue;
            ready.computeIfAbsent(animal.getType(), k -> new ArrayList<>()).add(animal);
        }
        for (List<Animal> pair : ready.values()) {
            if (pair.size() < 2) continue;
            int food = foodSlot(pair.get(0));
            if (food < 0 || items.get(food).getCount() < 2 || !drawPower(level, ENERGY_PER_ACTION)) continue;
            items.get(food).shrink(2);
            pair.get(0).setInLove(null);
            pair.get(1).setInLove(null);
            busy = true;
        }
        // Shearing and milking.
        int shears = slotOf(Items.SHEARS);
        for (Animal animal : animals) {
            if (animal.isBaby()) continue;
            if (shears >= 0 && animal instanceof Shearable sheep && sheep.readyForShearing() && drawPower(level, ENERGY_PER_ACTION)) {
                sheep.shear(server, SoundSource.BLOCKS, items.get(shears));
                busy = true;
            }
            int bucket = slotOf(Items.BUCKET);
            if (bucket >= 0 && (animal.getType() == EntityType.COW || animal.getType() == EntityType.MOOSHROOM) && canStore(List.of(new ItemStack(Items.MILK_BUCKET)))
                    && drawPower(level, ENERGY_PER_ACTION)) {
                items.get(bucket).shrink(1);
                store(List.of(new ItemStack(Items.MILK_BUCKET)));
                busy = true;
            }
        }
        // Pick up what lies around (eggs, wool, feathers...).
        for (ItemEntity drop : level.getEntitiesOfClass(ItemEntity.class, area(), ItemEntity::isAlive)) {
            ItemStack stack = drop.getItem().copy();
            if (!canStore(List.of(stack))) continue;
            store(List.of(stack));
            drop.discard();
            busy = true;
        }
        if (busy) setChanged();
        return busy || !animals.isEmpty();
    }

    private int foodSlot(Animal animal) {
        for (int i = 0; i < INPUTS; i++) {
            if (!items.get(i).isEmpty() && animal.isFood(items.get(i))) return i;
        }
        return -1;
    }

    private int slotOf(net.minecraft.world.item.Item item) {
        for (int i = 0; i < INPUTS; i++) if (items.get(i).is(item)) return i;
        return -1;
    }

    @Override
    protected @Nullable Operation find(Level level) {
        return null;
    }

    @Override
    protected List<ItemStack> produce(Level level, Operation operation) {
        return List.of();
    }
}
