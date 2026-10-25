package com.factorycraft;

import net.minecraft.world.item.Item;

/** Geiger counter: hold it to hear and see the radiation around you (see {@link GeigerClient}). */
public final class GeigerCounterItem extends Item {
    public GeigerCounterItem(Properties properties) {
        super(properties.stacksTo(1));
    }
}
