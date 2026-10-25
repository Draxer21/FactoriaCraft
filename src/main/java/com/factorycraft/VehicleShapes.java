package com.factorycraft;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;

import static com.factorycraft.VehicleHitbox.box;

/**
 * Collision segments of every vehicle, fitted to its model (measured from the baked models).
 * Each box is {@code back, front, right, left, bottom, top} in blocks, in the vehicle's own frame:
 * forward from the vehicle's centre, left of it, and up from the ground.
 * Moving parts that sweep around (rotor blades, crane booms) are left out.
 */
final class VehicleShapes {
    private VehicleShapes() {
    }

    static final VehicleHitbox.Box[] EXCAVATOR = {
            box(-1.53, 1.53, -1.15, 1.15, 0.0, 1.42),   // tracks and undercarriage
            box(-1.31, 0.66, -0.88, 0.88, 1.42, 4.16),  // house and cab
            box(0.66, 2.82, -0.93, 0.93, 0.99, 1.91),   // loader arms
            box(2.82, 4.55, -1.64, 1.64, 0.51, 1.66),   // bucket
    };

    static final VehicleHitbox.Box[] TUNNELER = {
            box(-1.55, 1.55, -1.28, 1.28, 0.0, 1.34),   // tracks and hull
            box(-1.55, 0.41, -1.03, 1.03, 1.34, 2.17),  // engine deck
            box(-1.03, 0.31, -0.72, 0.72, 2.17, 3.92),  // cab
            box(0.41, 3.51, -0.82, 0.82, 0.62, 2.27),   // drill head
    };

    static final VehicleHitbox.Box[] DUMP_TRUCK = {
            box(-2.72, 3.09, -1.57, 1.57, 0.0, 1.03),   // chassis and tracks
            box(-2.72, 0.94, -1.22, 1.22, 1.03, 2.44),  // bed
            box(0.94, 2.63, -1.13, 1.13, 1.03, 3.56),   // cab
            box(2.63, 3.09, -1.13, 1.13, 1.03, 1.78),   // hood
    };

    static final VehicleHitbox.Box[] TANKER = {
            box(-3.09, 3.09, -1.57, 1.57, 0.0, 1.03),   // chassis and tracks
            box(-2.91, 0.94, -1.08, 1.08, 1.03, 2.91),  // tank, pump and walkway
            box(0.94, 2.63, -1.13, 1.13, 1.03, 3.56),   // cab
            box(2.63, 3.09, -1.13, 1.13, 1.03, 1.78),   // hood
    };

    static final VehicleHitbox.Box[] CARGO_WAGON = {
            box(-1.41, 1.41, -1.06, 1.06, 0.0, 2.06),   // bin on tracks
    };

    static final VehicleHitbox.Box[] PAVER = {
            box(-1.78, 1.22, -1.22, 1.22, 0.0, 1.31),   // body and rear wheels
            box(-1.50, -0.09, -1.03, 1.03, 1.31, 3.56), // canopy
            box(1.22, 2.53, -1.41, 1.41, 0.0, 1.50),    // front roller
    };

    static final VehicleHitbox.Box[] HARVESTER = {
            box(-1.97, 1.69, -1.50, 1.50, 0.0, 1.88),   // body and tracks
            box(-1.69, 0.28, -0.94, 0.94, 1.88, 2.81),  // grain tank
            box(0.28, 1.97, -1.03, 1.03, 1.88, 3.94),   // cab
            box(1.69, 2.81, -2.25, 2.25, 0.38, 1.59),   // header and reel
    };

    static final VehicleHitbox.Box[] CRANE = {
            box(-2.53, 2.44, -1.66, 1.66, 0.0, 1.31),   // tracks, chassis, turntable
            box(-2.53, 0.0, -1.03, 1.03, 1.31, 2.63),   // machine house and counterweight
            box(0.0, 2.16, -1.03, 1.03, 1.31, 3.56),    // cab
    };

    static final VehicleHitbox.Box[] TOWER_CRANE = {
            box(-1.50, 1.50, -1.50, 1.50, 0.0, 0.75),       // base
            box(-0.56, 0.56, -0.56, 0.56, 0.75, 16.50),     // mast
            box(-0.75, 1.97, -0.75, 0.75, 16.50, 18.56),    // slewing unit and cab
            box(-5.91, -0.75, -0.66, 0.66, 17.81, 19.13),   // counter-jib and counterweight
            box(0.75, 14.81, -0.28, 0.28, 18.56, 19.13),    // jib
    };

    static final VehicleHitbox.Box[] HELICOPTER = {
            box(-1.13, 2.25, -1.31, 1.31, 0.0, 2.63),   // cabin and skids
            box(-0.94, 0.75, -0.56, 0.56, 2.63, 3.19),  // engine
            box(-4.59, -1.13, -0.28, 0.28, 1.69, 3.38), // tail boom and fin
    };

    static final VehicleHitbox.Box[] SUBMARINE = {
            box(-2.81, 2.91, -0.98, 0.98, 0.0, 2.25),   // hull
            box(-0.84, 0.84, -0.66, 0.66, 2.25, 3.28),  // tower
            box(-3.42, -2.81, -0.15, 0.15, 0.38, 2.44), // rudder and propeller
            box(2.91, 4.22, -0.47, 0.47, 0.75, 1.69),   // drill
    };

    static final VehicleHitbox.Box[] LOCOMOTIVE = {
            box(-3.0, 3.28, -1.22, 1.22, 0.0, 1.31),    // frame, wheels, cowcatcher
            box(-3.0, -0.66, -1.22, 1.22, 1.31, 3.75),  // cab
            box(-0.66, 2.91, -0.89, 0.89, 1.31, 2.44),  // hood
    };

    static final VehicleHitbox.Box[] FORESTER = {
            box(-2.25, 2.25, -1.57, 1.57, 0.0, 1.31),   // tracks and deck
            box(-2.06, 0.09, -1.22, 1.22, 1.31, 3.75),  // cab
            box(0.0, 1.50, -0.94, 0.94, 1.31, 2.25),    // engine
            box(2.25, 4.21, -1.02, 1.02, 0.94, 3.09),   // felling head
    };

    static final VehicleHitbox.Box[] BULLDOZER = {
            box(-2.81, 2.72, -1.69, 1.69, 0.0, 1.31),   // tracks, deck, ripper, push arms
            box(-2.06, 0.09, -1.22, 1.22, 1.31, 3.75),  // cab
            box(0.0, 2.34, -0.84, 0.84, 1.31, 2.25),    // hood
            box(2.72, 3.09, -2.53, 2.53, 0.09, 1.78),   // blade
    };

    static VehicleHitbox.Box[] of(Entity vehicle) {
        return switch (BuiltInRegistries.ENTITY_TYPE.getKey(vehicle.getType()).getPath()) {
            case "excavator" -> EXCAVATOR;
            case "tunneler" -> TUNNELER;
            case "dump_truck" -> DUMP_TRUCK;
            case "tanker" -> TANKER;
            case "paver" -> PAVER;
            case "harvester" -> HARVESTER;
            case "crane" -> CRANE;
            case "tower_crane" -> TOWER_CRANE;
            case "helicopter" -> HELICOPTER;
            case "submarine" -> SUBMARINE;
            case "locomotive" -> LOCOMOTIVE;
            case "forester" -> FORESTER;
            case "bulldozer" -> BULLDOZER;
            default -> CARGO_WAGON; // cargo and tank wagons share the model
        };
    }
}
