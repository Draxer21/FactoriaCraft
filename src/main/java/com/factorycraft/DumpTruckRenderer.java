package com.factorycraft;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.AbstractBoatRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.BoatRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;

public final class DumpTruckRenderer extends AbstractBoatRenderer {
    private static final float SCALE = 1.5F;

    private final EntityModel<BoatRenderState> model;
    private final Headlights headlights;

    public DumpTruckRenderer(EntityRendererProvider.Context context) {
        super(context, Identifier.fromNamespaceAndPath(FactoryCraft.MOD_ID, "textures/entity/dump_truck.png"));
        this.model = new DumpTruckModel(context.bakeLayer(DumpTruckModel.LAYER_LOCATION));
        // A second, independent copy of the parts for the full-bright lamp pass.
        this.headlights = new Headlights(context.bakeLayer(DumpTruckModel.LAYER_LOCATION),
                DumpTruckModel.LAMP_PARTS, DumpTruckModel.BEAM_ORIGINS, this.texture);
        this.shadowRadius = 1.8F;
    }

    /** Boat render state plus bed tipping, load level and wheel spin. */
    public static final class TruckRenderState extends Headlights.LitBoatRenderState {
        public float dump;
        public float fill;
        public float wheelRot;
    }

    @Override
    protected EntityModel<BoatRenderState> model() {
        return model;
    }

    @Override
    public BoatRenderState createRenderState() {
        return new TruckRenderState();
    }

    @Override
    public void extractRenderState(AbstractBoat entity, BoatRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        if (state instanceof TruckRenderState truck && entity instanceof DumpTruckEntity dumpTruck) {
            truck.dump = dumpTruck.getDumpProgress(partialTicks);
            truck.fill = dumpTruck.getFill();
            truck.wheelRot = dumpTruck.getWheelRot(partialTicks);
            truck.lightsOn = Headlights.areOn(entity);
            truck.travelled = TrackTreads.travelled(entity, partialTicks);
        }
    }

    @Override
    protected void submitTypeAdditions(BoatRenderState state, PoseStack poseStack, SubmitNodeCollector collector, int lightCoords) {
        super.submitTypeAdditions(state, poseStack, collector, lightCoords);
        if (state instanceof TruckRenderState truck && truck.lightsOn) {
            headlights.submit(poseStack, collector, Integer.MAX_VALUE);
        }
    }

    @Override
    public void submit(BoatRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.scale(SCALE, SCALE, SCALE);
        try {
            super.submit(state, poseStack, collector, camera);
        } finally {
            poseStack.popPose();
        }
    }
}
