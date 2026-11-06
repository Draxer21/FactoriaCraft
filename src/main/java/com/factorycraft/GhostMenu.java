package com.factorycraft;

import net.minecraft.world.item.ItemStack;

/** A menu with ghost slots (assembler pattern, bus filter) set from the client. */
interface GhostMenu {
    void setGhost(int slot, ItemStack stack);
}
