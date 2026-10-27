package com.factorycraft;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

public final class DroneRenderer<T extends net.minecraft.world.entity.Entity> extends EntityRenderer<T, DroneRenderer.DroneRenderState> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(FactoryCraft.MOD_ID, "textures/entity/drone.png");
    private final DroneModel model;

    public static final class DroneRenderState extends EntityRenderState {
        public float yRot;
    }

    public DroneRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new DroneModel(context.bakeLayer(DroneModel.LAYER_LOCATION));
        this.shadowRadius = 0.4F;
    }

    @Override
    public DroneRenderState createRenderState() {
        return new DroneRenderState();
    }

    @Override
    public void extractRenderState(T entity, DroneRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yRot = entity.getYRot(partialTicks);
    }

    @Override
    public void submit(DroneRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - state.yRot));
        poseStack.translate(0.0F, 1.501F, 0.0F);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        collector.submitModel(model, state, poseStack, TEXTURE, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor, null);
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }
}
