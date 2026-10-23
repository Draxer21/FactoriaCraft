package com.factorycraft;

import net.minecraft.util.StringRepresentable;

/**
 * Where a flat belt is fed from. Purely visual (the belt always pushes toward
 * its facing, so items already follow corners); it selects the curved model.
 * Left/right are as seen looking along the belt's facing.
 */
public enum ConveyorTurn implements StringRepresentable {
    STRAIGHT("straight"),
    /** Fed from the belt on its left side: a left-hand curve for the item. */
    FROM_LEFT("from_left"),
    /** Fed from the belt on its right side. */
    FROM_RIGHT("from_right");

    private final String name;

    ConveyorTurn(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
