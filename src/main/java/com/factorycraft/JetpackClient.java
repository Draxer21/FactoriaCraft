package com.factorycraft;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** Client side of the jetpack: thrust while jump is held in the air. */
@EventBusSubscriber(modid = FactoryCraft.MOD_ID, value = Dist.CLIENT)
public final class JetpackClient {
    private JetpackClient() {}

    private static boolean thrusting;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        boolean want = false;
        if (player != null) {
            ItemStack jetpack = JetpackItem.worn(player);
            want = !jetpack.isEmpty() && mc.options.keyJump.isDown() && !player.onGround()
                    && !player.getAbilities().flying && (player.isCreative() || ItemEnergy.get(jetpack) >= JetpackItem.PER_TICK)
                    && player.getVehicle() == null;
            boolean mk2 = !jetpack.isEmpty() && jetpack.getItem() instanceof JetpackItem jet && jet.tier() == 2;
            if (want) {
                Vec3 motion = player.getDeltaMovement();
                double up = Math.min(motion.y + (mk2 ? 0.22 : 0.15), mk2 ? 0.8 : 0.5);
                double push = mk2 ? 0.05 : 0.02;
                Vec3 look = player.getLookAngle();
                player.setDeltaMovement(motion.x + look.x * push, up, motion.z + look.z * push);
                player.resetFallDistance();
            } else if (mk2 && player.isShiftKeyDown() && !player.onGround() && player.getDeltaMovement().y < -0.12
                    && (player.isCreative() || ItemEnergy.get(jetpack) > 0)) {
                // Glide: fall slowly and drift forward.
                Vec3 motion = player.getDeltaMovement();
                Vec3 look = player.getLookAngle();
                player.setDeltaMovement(motion.x * 0.9 + look.x * 0.06, -0.12, motion.z * 0.9 + look.z * 0.06);
            }
        }
        if (want != thrusting && player != null) {
            thrusting = want;
            ClientPacketDistributor.sendToServer(new JetpackPayload(want));
        }
    }
}
