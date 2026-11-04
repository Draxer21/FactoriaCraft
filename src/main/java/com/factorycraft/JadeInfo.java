package com.factorycraft;

import java.util.List;
import net.minecraft.network.chat.Component;

/**
 * Extra lines a block shows in Jade/WTHIT-style overlays (see JadeCompat).
 * Plain Minecraft types only, so it costs nothing without Jade.
 */
public interface JadeInfo {
    void jadeInfo(List<Component> lines);
}
