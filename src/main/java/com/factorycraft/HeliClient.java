package com.factorycraft;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Reads jump / sprint for the helicopter and submarine. */
@EventBusSubscriber(modid = FactoryCraft.MOD_ID, value = Dist.CLIENT)
public final class HeliClient {
    private HeliClient() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !(mc.player.getVehicle() instanceof HelicopterEntity || mc.player.getVehicle() instanceof SubmarineEntity)) {
            HeliControls.vertical = 0;
            return;
        }
        boolean up = mc.options.keyJump.isDown();
        boolean down = mc.options.keySprint.isDown();
        HeliControls.vertical = up == down ? 0 : up ? 1 : -1;
    }
}
