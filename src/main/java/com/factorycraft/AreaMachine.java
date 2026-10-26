package com.factorycraft;

import net.minecraft.world.entity.player.Player;

/** A machine whose work area can be cycled by sneak + right-click with an empty hand. */
public interface AreaMachine {
    void cycleRadius(Player player);
}
