package com.factorycraft;

import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Block entities that remember who placed them (turrets and alarms leave their owner alone). */
public interface Owned {
    void setOwner(UUID owner);

    @Nullable UUID owner();
}
