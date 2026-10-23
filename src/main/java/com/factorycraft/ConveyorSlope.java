package com.factorycraft;

import net.minecraft.util.StringRepresentable;

/** Shape of a conveyor segment along its facing direction. */
public enum ConveyorSlope implements StringRepresentable {
    /** Level belt. */
    FLAT("flat"),
    /** Rises one block toward its facing (the next segment is one block higher). */
    UP("up"),
    /** Drops one block toward its facing (the previous segment is one block higher). */
    DOWN("down");

    private final String name;

    ConveyorSlope(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    /** Belt surface height (blocks above the block's bottom) at progress 0..1 along the belt. */
    public double surfaceAt(double progress) {
        return switch (this) {
            case FLAT -> 0.25;
            case UP -> 0.25 + progress;
            case DOWN -> 1.25 - progress;
        };
    }
}
