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

public final class ExcavatorRenderer extends AbstractBoatRenderer {
    private final EntityModel<BoatRenderState> model;
    private final Headlights headlights;

    public ExcavatorRenderer(EntityRendererProvider.Context context) {
        super(context, Identifier.fromNamespaceAndPath(FactoryCraft.MOD_ID, "textures/entity/excavator.png"));
        this.model = new ExcavatorModel(context.bakeLayer(ExcavatorModel.LAYER_LOCATION));
        // A second, independent copy of the parts for the full-bright lamp pass.
        this.headlights = new Headlights(context.bakeLayer(ExcavatorModel.LAYER_LOCATION),
                ExcavatorModel.LAMP_PARTS, ExcavatorModel.BEAM_ORIGINS, this.texture);
        this.shadowRadius = 2.4F;
    }

    @Override
    protected EntityModel<BoatRenderState> model() {
        return model;
    }

    @Override
    public BoatRenderState createRenderState() {
        return new Headlights.LitBoatRenderState();
    }

    @Override
    public void extractRenderState(AbstractBoat entity, BoatRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        if (state instanceof Headlights.LitBoatRenderState lit) {
            lit.lightsOn = Headlights.areOn(entity);
            lit.travelled = TrackTreads.travelled(entity, partialTicks);
        }
    }

    @Override
    protected void submitTypeAdditions(BoatRenderState state, PoseStack poseStack, SubmitNodeCollector collector, int lightCoords) {
        super.submitTypeAdditions(state, poseStack, collector, lightCoords);
        if (state instanceof Headlights.LitBoatRenderState lit && lit.lightsOn) {
            headlights.submit(poseStack, collector, Integer.MAX_VALUE);
        }
    }

    @Override
    public void submit(BoatRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.scale(1.75F, 1.75F, 1.75F);
        try {
            super.submit(state, poseStack, collector, camera);
        } finally {
            poseStack.popPose();
        }
    }
}
