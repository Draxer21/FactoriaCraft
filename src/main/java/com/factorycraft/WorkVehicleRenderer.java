package com.factorycraft;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.function.Function;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.AbstractBoatRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.BoatRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;

/** Renderer for {@link WorkVehicleEntity} machines: scaled model, headlights and travel-driven animation. */
public class WorkVehicleRenderer extends AbstractBoatRenderer {
    private final EntityModel<BoatRenderState> model;
    private final Headlights headlights;
    private final float scale;

    public WorkVehicleRenderer(EntityRendererProvider.Context context, String name, ModelLayerLocation layer, float scale,
                               Function<ModelPart, EntityModel<BoatRenderState>> modelFactory,
                               String[] lampParts, float[][] beamOrigins) {
        super(context, Identifier.fromNamespaceAndPath(FactoryCraft.MOD_ID, "textures/entity/" + name + ".png"));
        this.model = modelFactory.apply(context.bakeLayer(layer));
        this.headlights = new Headlights(context.bakeLayer(layer), lampParts, beamOrigins, this.texture);
        this.scale = scale;
        this.shadowRadius = 1.8F;
    }

    /** Boat state plus the distance driven, for rollers and wheels. */
    public static class WorkRenderState extends Headlights.LitBoatRenderState {
    }

    @Override
    protected EntityModel<BoatRenderState> model() {
        return model;
    }

    @Override
    public BoatRenderState createRenderState() {
        return new WorkRenderState();
    }

    @Override
    public void extractRenderState(AbstractBoat entity, BoatRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        if (state instanceof WorkRenderState work && entity instanceof WorkVehicleEntity vehicle) {
            work.travelled = vehicle.getTravelled(partialTicks);
            work.lightsOn = Headlights.areOn(entity);
        }
    }

    @Override
    protected void submitTypeAdditions(BoatRenderState state, PoseStack poseStack, SubmitNodeCollector collector, int lightCoords) {
        super.submitTypeAdditions(state, poseStack, collector, lightCoords);
        if (state instanceof WorkRenderState work && work.lightsOn) {
            headlights.submit(poseStack, collector, Integer.MAX_VALUE);
        }
    }

    @Override
    public void submit(BoatRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.scale(scale, scale, scale);
        try {
            super.submit(state, poseStack, collector, camera);
        } finally {
            poseStack.popPose();
        }
    }
}
