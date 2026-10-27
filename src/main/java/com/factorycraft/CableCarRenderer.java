package com.factorycraft;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Draws the cabin, the sagging cable (a chain of thin stretched blocks) and the two station masts. */
public final class CableCarRenderer extends EntityRenderer<CableCarEntity, CableCarRenderer.State> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(FactoryCraft.MOD_ID, "textures/entity/cable_car.png");
    private static final int SEGMENTS = 12;
    private static final float CABLE = 0.09F;
    private static final float MAST = 0.35F;
    private final CableCarModel model;

    /** A thin beam between two points, relative to the cabin. */
    record Beam(Vec3 from, Vec3 to, float thickness, MovingBlockRenderState block) {}

    public static final class State extends EntityRenderState {
        public float yRot;
        public final List<Beam> beams = new ArrayList<>();
    }

    public CableCarRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new CableCarModel(context.bakeLayer(CableCarModel.LAYER_LOCATION));
        this.shadowRadius = 0.0F;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    protected AABB getBoundingBoxForCulling(CableCarEntity entity) {
        Vec3 a = CableCarEntity.anchor(entity.from());
        Vec3 b = CableCarEntity.anchor(entity.to());
        return new AABB(a, b).inflate(1.0, 6.0, 1.0);
    }

    private static MovingBlockRenderState block(ClientLevel level, BlockState state, Vec3 at) {
        MovingBlockRenderState block = new MovingBlockRenderState();
        BlockPos pos = BlockPos.containing(at);
        block.blockState = state;
        block.blockPos = pos;
        block.randomSeedPos = pos;
        block.biome = level.getBiome(pos);
        block.cardinalLighting = level.cardinalLighting();
        block.lightEngine = level.getLightEngine();
        return block;
    }

    @Override
    public void extractRenderState(CableCarEntity entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yRot = entity.getYRot(partialTicks);
        state.beams.clear();
        if (!(entity.level() instanceof ClientLevel level)) return;
        Vec3 origin = entity.getPosition(partialTicks);
        BlockState cable = Blocks.COAL_BLOCK.defaultBlockState();
        BlockState mast = Blocks.SMOOTH_STONE.defaultBlockState();
        Vec3 previous = entity.cable(0);
        for (int i = 1; i <= SEGMENTS; i++) {
            Vec3 next = entity.cable(i / (double) SEGMENTS);
            state.beams.add(new Beam(previous.subtract(origin), next.subtract(origin), CABLE, block(level, cable, previous)));
            previous = next;
        }
        for (BlockPos station : new BlockPos[] {entity.from(), entity.to()}) {
            Vec3 top = CableCarEntity.anchor(station).add(0, 0.2, 0);
            Vec3 bottom = new Vec3(top.x, station.getY() + 1.0, top.z);
            state.beams.add(new Beam(bottom.subtract(origin), top.subtract(origin), MAST, block(level, mast, top)));
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        for (Beam beam : state.beams) {
            Vec3 d = beam.to().subtract(beam.from());
            double length = d.length();
            if (length < 1.0E-3) continue;
            float yaw = (float) Math.atan2(d.x, d.z);
            float pitch = (float) Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z));
            poseStack.pushPose();
            poseStack.translate(beam.from().x, beam.from().y, beam.from().z);
            poseStack.mulPose(Axis.YP.rotation(yaw));
            poseStack.mulPose(Axis.XP.rotation(-pitch));
            poseStack.scale(beam.thickness(), beam.thickness(), (float) length);
            poseStack.translate(-0.5F, -0.5F, 0.0F);
            collector.submitMovingBlock(poseStack, beam.block());
            poseStack.popPose();
        }
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - state.yRot));
        poseStack.translate(0.0F, 1.501F, 0.0F);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        collector.submitModel(model, state, poseStack, TEXTURE, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor, null);
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }
}
