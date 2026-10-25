package com.factorycraft;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.BoatRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.phys.AABB;

/** Crane renderer: the machine itself plus the structure hanging from the hook. */
public class CraneRenderer extends WorkVehicleRenderer {
    /** Big loads are drawn shrunk so they hang clear of the ground. */
    private static final float MAX_LOAD_HEIGHT = 3.0F;

    public CraneRenderer(EntityRendererProvider.Context context) {
        super(context, "crane", CraneModel.LAYER_LOCATION, 1.5F, CraneModel::new,
                CraneModel.LAMP_PARTS, CraneModel.BEAM_ORIGINS);
    }

    public CraneRenderer(EntityRendererProvider.Context context, String name, net.minecraft.client.model.geom.ModelLayerLocation layer,
                            java.util.function.Function<net.minecraft.client.model.geom.ModelPart,
                                    net.minecraft.client.model.EntityModel<BoatRenderState>> model,
                            String[] lamps, float[][] beams) {
        super(context, name, layer, 1.5F, model, lamps, beams);
    }

    public static final class CraneRenderState extends WorkRenderState {
        public final List<MovingBlockRenderState> blocks = new ArrayList<>();
        public final List<BlockPos> offsets = new ArrayList<>();
        public int loadHeight;
        public float hookForward;
        public float hookHeight;
        public float maxLoadHeight;
    }

    @Override
    public BoatRenderState createRenderState() {
        return new CraneRenderState();
    }

    @Override
    public void extractRenderState(AbstractBoat entity, BoatRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        if (!(state instanceof CraneRenderState crane) || !(entity instanceof CraneEntity craneEntity)) return;
        List<CraneEntity.Piece> load = craneEntity.getClientLoad();
        crane.hookForward = craneEntity.hookForward();
        crane.hookHeight = craneEntity.hookHeight();
        crane.maxLoadHeight = craneEntity.maxLoadHeight();
        crane.offsets.clear();
        crane.loadHeight = 0;
        while (crane.blocks.size() < load.size()) crane.blocks.add(new MovingBlockRenderState());
        if (load.isEmpty()) return;

        float yaw = state.yRot * Mth.DEG_TO_RAD;
        BlockPos hookPos = BlockPos.containing(
                entity.getX() - Mth.sin(yaw) * crane.hookForward,
                entity.getY() + 1.0,
                entity.getZ() + Mth.cos(yaw) * crane.hookForward);
        for (int i = 0; i < load.size(); i++) {
            CraneEntity.Piece piece = load.get(i);
            MovingBlockRenderState block = crane.blocks.get(i);
            block.blockState = piece.state();
            block.blockPos = hookPos;
            block.randomSeedPos = piece.pos();
            if (entity.level() instanceof ClientLevel level) {
                block.biome = level.getBiome(hookPos);
                block.cardinalLighting = level.cardinalLighting();
                block.lightEngine = level.getLightEngine();
            }
            crane.offsets.add(piece.pos());
            crane.loadHeight = Math.max(crane.loadHeight, piece.pos().getY() + 1);
        }
    }

    @Override
    public void submit(BoatRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        super.submit(state, poseStack, collector, camera);
        if (!(state instanceof CraneRenderState crane) || crane.offsets.isEmpty()) return;

        float shrink = Math.min(1.0F, crane.maxLoadHeight / crane.loadHeight);
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yRot));
        poseStack.translate(0.0F, crane.hookHeight, crane.hookForward);
        poseStack.scale(shrink, shrink, shrink);
        for (int i = 0; i < crane.offsets.size(); i++) {
            MovingBlockRenderState block = crane.blocks.get(i);
            if (block.blockState.getRenderShape() != RenderShape.MODEL) continue;
            BlockPos offset = crane.offsets.get(i);
            poseStack.pushPose();
            poseStack.translate(offset.getX() - 0.5F, offset.getY() - crane.loadHeight, offset.getZ() - 0.5F);
            collector.submitMovingBlock(poseStack, block);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    @Override
    protected AABB getBoundingBoxForCulling(AbstractBoat entity) {
        // The boom and the load reach well past the hull.
        float reach = entity instanceof CraneEntity crane ? crane.hookForward() + 5.0F : 6.0F;
        return super.getBoundingBoxForCulling(entity).inflate(reach, 4.0, reach);
    }
}
