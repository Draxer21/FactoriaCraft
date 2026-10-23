package com.factorycraft;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.AbstractBoatRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.BoatRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import org.joml.Quaternionf;

public final class TunnelerRenderer extends AbstractBoatRenderer {
    private static final float SCALE = 1.65F;

    private final EntityModel<BoatRenderState> model;
    private final Headlights headlights;

    public TunnelerRenderer(EntityRendererProvider.Context context) {
        super(context, Identifier.fromNamespaceAndPath(FactoryCraft.MOD_ID, "textures/entity/tunneler.png"));
        this.model = new TunnelerModel(context.bakeLayer(TunnelerModel.LAYER_LOCATION));
        // A second, independent copy of the parts for the full-bright lamp pass.
        this.headlights = new Headlights(context.bakeLayer(TunnelerModel.LAYER_LOCATION),
                TunnelerModel.LAMP_PARTS, TunnelerModel.BEAM_ORIGINS, this.texture);
        this.shadowRadius = 2.0F;
    }

    /** Boat render state plus the machine's nose-up/down tilt. */
    public static final class TunnelerRenderState extends Headlights.LitBoatRenderState {
        public float pitch;
        /** True when the local player is driving in first person: hide the cab frame. */
        public boolean pilotView;
    }

    @Override
    protected EntityModel<BoatRenderState> model() {
        return model;
    }

    @Override
    public BoatRenderState createRenderState() {
        return new TunnelerRenderState();
    }

    @Override
    public void extractRenderState(AbstractBoat entity, BoatRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        if (state instanceof TunnelerRenderState tunneler) {
            tunneler.pitch = entity.getXRot(partialTicks);
            Minecraft minecraft = Minecraft.getInstance();
            tunneler.pilotView = minecraft.player != null && entity.hasPassenger(minecraft.player)
                    && minecraft.options.getCameraType().isFirstPerson();
            tunneler.lightsOn = Headlights.areOn(entity);
            tunneler.travelled = TrackTreads.travelled(entity, partialTicks);
        }
    }

    @Override
    protected void submitTypeAdditions(BoatRenderState state, PoseStack poseStack, SubmitNodeCollector collector, int lightCoords) {
        super.submitTypeAdditions(state, poseStack, collector, lightCoords);
        if (state instanceof TunnelerRenderState tunneler && tunneler.lightsOn) {
            // The cab lamps sit behind the pilot's head; skip them in first person.
            headlights.submit(poseStack, collector, tunneler.pilotView ? 2 : Integer.MAX_VALUE);
        }
    }

    @Override
    public void submit(BoatRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        float pitch = state instanceof TunnelerRenderState tunneler ? tunneler.pitch : 0.0F;
        if (pitch != 0.0F) {
            // Tilt around the seat, along the machine's sideways axis, so the pilot
            // (who always stays upright) remains in the same spot inside the cab.
            float yaw = state.yRot * Mth.DEG_TO_RAD;
            float forwardX = -Mth.sin(yaw);
            float forwardZ = Mth.cos(yaw);
            float pivotX = -forwardX * TunnelerEntity.SEAT_OFFSET_BACK;
            float pivotZ = -forwardZ * TunnelerEntity.SEAT_OFFSET_BACK;
            float pivotY = TunnelerEntity.SEAT_HEIGHT;
            poseStack.translate(pivotX, pivotY, pivotZ);
            poseStack.mulPose(new Quaternionf().rotationAxis(pitch * Mth.DEG_TO_RAD, forwardZ, 0.0F, -forwardX));
            poseStack.translate(-pivotX, -pivotY, -pivotZ);
        }
        poseStack.scale(SCALE, SCALE, SCALE);
        try {
            super.submit(state, poseStack, collector, camera);
        } finally {
            poseStack.popPose();
        }
    }
}
