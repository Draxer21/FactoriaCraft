package com.factorycraft;

import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/** A vehicle whose working tool (drill, saw) the driver can switch off, so it stops turning and cutting. */
public interface ToolToggle {
    /** Whether this vehicle has a tool to switch (a crane, for instance, doesn't). */
    default boolean hasTool() {
        return true;
    }

    boolean toolOn();

    void setToolOn(boolean on);

    /** Client side: the tool's spin angle (radians), easing in and out. */
    float toolSpin(float partialTicks);

    default void toggleTool(Player player) {
        boolean on = !toolOn();
        setToolOn(on);
        player.sendOverlayMessage(Component.translatable(on ? "message.factorycraft.tool_on" : "message.factorycraft.tool_off"));
    }

    /** Client-side spin that speeds up and slows down instead of snapping. */
    final class Spin {
        private float angle;
        private float angleO;
        private float speed;

        public void tick(boolean turning, float maxSpeed) {
            angleO = angle;
            speed = Mth.approach(speed, turning ? maxSpeed : 0.0F, maxSpeed / 20.0F);
            angle += speed;
        }

        public float get(float partialTicks) {
            return Mth.lerp(partialTicks, angleO, angle);
        }
    }
}
