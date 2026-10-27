package com.factorycraft;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

/** Client side of stage 9: the smog haze (closer, browner fog and drifting ash) and the statistics screen. */
@EventBusSubscriber(modid = FactoryCraft.MOD_ID, value = Dist.CLIENT)
public final class EnvironmentClient {
    private EnvironmentClient() {}

    /** Haze strength 0..1, eased towards the server's value. */
    private static float haze;
    private static boolean acid;

    @SubscribeEvent
    public static void registerPayloads(RegisterClientPayloadHandlersEvent event) {
        event.register(PollutionPayload.TYPE, (payload, context) -> PollutionPayload.latest = payload);
        event.register(StatsPayload.TYPE, (payload, context) -> Minecraft.getInstance().gui.setScreen(new StatsScreen(payload)));
    }

    static float target() {
        int air = PollutionPayload.latest.air();
        return Math.max(0.0F, Math.min(1.0F, (air - (AirQuality.HAZE - 50)) / 650.0F));
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            haze = 0;
            return;
        }
        haze += (target() - haze) * 0.05F;
        acid = PollutionPayload.latest.acid();
        if (haze < 0.02F || mc.isPaused()) return;
        RandomSource random = mc.level.getRandom();
        int count = Math.round(haze * 6);
        for (int i = 0; i < count; i++) {
            double x = player.getX() + random.nextGaussian() * 8;
            double y = player.getY() + random.nextDouble() * 6 - 1;
            double z = player.getZ() + random.nextGaussian() * 8;
            mc.level.addParticle(acid ? ParticleTypes.WHITE_ASH : ParticleTypes.ASH, x, y, z, 0, -0.01, 0);
        }
    }

    @SubscribeEvent
    public static void onFog(ViewportEvent.RenderFog event) {
        if (haze < 0.02F) return;
        String type = event.getType().name();
        if (type.equals("WATER") || type.equals("LAVA") || type.equals("POWDER_SNOW")) return;
        event.scaleFarPlaneDistance(1.0F - 0.8F * haze);
        event.scaleNearPlaneDistance(1.0F - 0.95F * haze);
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        if (haze < 0.02F) return;
        float k = 0.75F * haze;
        float r = acid ? 0.42F : 0.47F;
        float g = acid ? 0.47F : 0.40F;
        float b = acid ? 0.26F : 0.30F;
        event.setRed(event.getRed() + (r - event.getRed()) * k);
        event.setGreen(event.getGreen() + (g - event.getGreen()) * k);
        event.setBlue(event.getBlue() + (b - event.getBlue()) * k);
    }
}
