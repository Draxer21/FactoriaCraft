package com.factorycraft;

import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import org.jspecify.annotations.Nullable;

/** Client side of stage 8: watching security cameras and riding lift pads. */
@EventBusSubscriber(modid = FactoryCraft.MOD_ID, value = Dist.CLIENT)
public final class DefenseClient {
    private DefenseClient() {}

    private static List<CameraViewPayload.View> views = List.of();
    private static int index;
    private static int age;
    private static @Nullable Entity eye;
    private static boolean jumpWasDown;
    private static boolean sneakWasDown;
    private static int liftCooldown;

    @SubscribeEvent
    public static void registerPayloads(RegisterClientPayloadHandlersEvent event) {
        event.register(CameraViewPayload.TYPE, (payload, context) -> start(payload.views()));
    }

    @SubscribeEvent
    public static void registerHud(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR,
                Identifier.fromNamespaceAndPath(FactoryCraft.MOD_ID, "security_camera"), DefenseClient::renderOverlay);
    }

    public static boolean watching() {
        return eye != null;
    }

    private static void start(List<CameraViewPayload.View> list) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || list.isEmpty()) return;
        views = list;
        index = 0;
        age = 0;
        eye = new DeliveryDroneEntity(Logistics.DELIVERY_DRONE.value(), mc.level);
        if (eye == null) return;
        place();
        mc.setCameraEntity(eye);
        jumpWasDown = true;
        sneakWasDown = true;
    }

    private static void stop() {
        Minecraft mc = Minecraft.getInstance();
        eye = null;
        views = List.of();
        if (mc.player != null) mc.setCameraEntity(mc.player);
    }

    /** Puts the eye in front of the current camera, panning slowly from side to side. */
    private static void place() {
        if (eye == null || views.isEmpty()) return;
        CameraViewPayload.View view = views.get(index % views.size());
        BlockPos pos = view.pos();
        double rad = Math.toRadians(view.yaw());
        double x = pos.getX() + 0.5 - Math.sin(rad) * 0.55;
        double z = pos.getZ() + 0.5 + Math.cos(rad) * 0.55;
        float pan = (float) (Math.sin(age / 60.0) * 35.0);
        boolean jumped = eye.distanceToSqr(x, pos.getY() + 0.2, z) > 0.01;
        eye.snapTo(x, pos.getY() + 0.2, z, view.yaw() + pan, 22.0F);
        if (jumped) {
            eye.xo = x;
            eye.yo = pos.getY() + 0.2;
            eye.zo = z;
            eye.xOld = x;
            eye.yOld = pos.getY() + 0.2;
            eye.zOld = z;
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            eye = null;
            return;
        }
        boolean jump = mc.options.keyJump.isDown();
        boolean sneak = mc.options.keyShift.isDown();
        if (eye != null) {
            if (eye.level() != mc.level || !player.isAlive()) {
                stop();
            } else {
                if (sneak && !sneakWasDown) stop();
                else if (jump && !jumpWasDown) index = (index + 1) % views.size();
                age++;
                place();
            }
        } else if (liftCooldown > 0) {
            liftCooldown--;
        } else if (player.onGround() && mc.level.getBlockState(player.blockPosition().below()).is(Defense.LIFT_PAD.value())) {
            if (jump && !jumpWasDown) {
                ClientPacketDistributor.sendToServer(new LiftPayload(true));
                liftCooldown = 8;
            } else if (sneak && !sneakWasDown) {
                ClientPacketDistributor.sendToServer(new LiftPayload(false));
                liftCooldown = 8;
            }
        }
        jumpWasDown = jump;
        sneakWasDown = sneak;
    }

    private static void renderOverlay(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        if (eye == null || views.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        CameraViewPayload.View view = views.get(index % views.size());
        int w = graphics.guiWidth();
        int h = graphics.guiHeight();
        graphics.fill(0, 0, w, 3, 0x80000000);
        graphics.fill(0, h - 3, w, h, 0x80000000);
        String rec = (age / 10) % 2 == 0 ? "● REC" : "  REC";
        graphics.text(mc.font, rec, 8, 8, 0xFFE03030);
        graphics.text(mc.font, Component.translatable("gui.factorycraft.camera.title", index + 1, views.size(),
                view.pos().getX(), view.pos().getY(), view.pos().getZ()), 8, 20, 0xFFFFFFFF);
        Component help = Component.translatable("gui.factorycraft.camera.help");
        graphics.text(mc.font, help, w - 8 - mc.font.width(help), h - 16, 0xFFBFC7CE);
    }
}
