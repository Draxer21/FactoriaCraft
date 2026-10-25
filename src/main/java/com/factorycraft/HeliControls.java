package com.factorycraft;

/**
 * Vertical control for flying/diving vehicles. Set by the client every tick
 * (jump = up, sprint = down) and read in the vehicle's movement, which runs
 * on the driver's own client.
 */
public final class HeliControls {
    private HeliControls() {}

    /** +1 up, -1 down, 0 hold. */
    public static volatile int vertical;
}
