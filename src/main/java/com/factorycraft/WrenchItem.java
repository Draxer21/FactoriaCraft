package com.factorycraft;

import net.minecraft.world.item.Item;

/** Right-click a machine to take out all its upgrade modules. */
public final class WrenchItem extends Item {
    public WrenchItem(Properties properties) {
        super(properties.stacksTo(1));
    }
}
