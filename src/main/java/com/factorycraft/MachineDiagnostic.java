package com.factorycraft;

import net.minecraft.network.chat.Component;

/** A short, player-facing reason a machine cannot currently do useful work. */
public enum MachineDiagnostic {
    WAITING_FOR_INPUT("waiting_for_input"),
    INVALID_INPUTS("invalid_inputs"),
    OUTPUT_BLOCKED("output_blocked"),
    NO_POWER("no_power"),
    NO_FUEL("no_fuel"),
    REDSTONE_PAUSED("redstone_paused"),
    WORK_COMPLETE("work_complete"),
    WORK_BLOCKED("work_blocked"),
    NEEDS_MAGNETS("needs_magnets"),
    POWER_BUFFER_FULL("power_buffer_full");

    private final String key;

    MachineDiagnostic(String key) {
        this.key = key;
    }

    public Component displayName() {
        return Component.translatable("jade.factorycraft.diagnostic." + key);
    }
}
