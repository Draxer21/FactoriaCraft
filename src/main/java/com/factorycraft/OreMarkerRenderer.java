package com.factorycraft;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.RenderShape;

/** Draws the marked ore block slightly enlarged; the glowing outline is what shows through walls. */
public final class OreMarkerRenderer extends EntityRenderer<OreMarkerEntity, OreMarkerRenderer.MarkerState> {
    public static final class MarkerState extends EntityRenderState {
        public final MovingBlockRenderState block = new MovingBlockRenderState();
    }

    public OreMarkerRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public MarkerState createRenderState() {
        return new MarkerState();
    }

    @Override
    public void extractRenderState(OreMarkerEntity entity, MarkerState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        BlockPos pos = entity.blockPosition();
        state.block.blockState = entity.getOre();
        state.block.blockPos = pos;
        state.block.randomSeedPos = pos;
        if (entity.level() instanceof ClientLevel level) {
            state.block.biome = level.getBiome(pos);
            state.block.cardinalLighting = level.cardinalLighting();
            state.block.lightEngine = level.getLightEngine();
        }
    }

    @Override
    public void submit(MarkerState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.block.blockState.getRenderShape() != RenderShape.MODEL) return;
        poseStack.pushPose();
        poseStack.translate(-0.51F, -0.01F, -0.51F);
        poseStack.scale(1.02F, 1.02F, 1.02F);
        collector.submitMovingBlock(poseStack, state.block);
        poseStack.popPose();
    }
}
