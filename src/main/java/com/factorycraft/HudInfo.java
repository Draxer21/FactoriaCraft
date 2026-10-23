package com.factorycraft;

import net.minecraft.network.chat.Component;

/** A machine with an extra status line on the dashboard HUD (crane load, tanker contents...). */
public interface HudInfo {
    Component hudLine();
}
