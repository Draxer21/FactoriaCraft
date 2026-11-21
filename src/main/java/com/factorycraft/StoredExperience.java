package com.factorycraft;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.phys.Vec3;

/**
 * Furnaces keep the experience of what they smelted, like the vanilla furnace:
 * it is handed out when a player takes products from an output slot, or
 * dropped when the machine is broken. Products taken by hoppers, belts or
 * robot arms leave their experience inside for the next player.
 */
public interface StoredExperience {
    float storedExperience();

    void setStoredExperience(float experience);

    default void addExperience(float experience) {
        if (experience > 0) setStoredExperience(storedExperience() + experience);
    }

    /** Spawns all stored experience as orbs at {@code pos} and empties the store. */
    default void popExperience(ServerLevel level, Vec3 pos) {
        float stored = storedExperience();
        if (stored <= 0) return;
        setStoredExperience(0);
        int whole = Mth.floor(stored);
        // The fraction becomes one more point by chance, as in the vanilla furnace.
        if (level.getRandom().nextFloat() < stored - whole) whole++;
        if (whole > 0) ExperienceOrb.award(level, pos, whole);
    }
}
