package com.factorycraft;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.BoatRenderState;
import net.minecraft.resources.Identifier;

/** Combine harvester. +x is the front (cutter header and reel), y down, ground at y = 6. */
public final class HarvesterModel extends EntityModel<BoatRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(FactoryCraft.MOD_ID, "harvester"), "main");
    public static final String[] LAMP_PARTS = {"headlight_left", "headlight_right", "tail_light_left", "tail_light_right"};
    public static final float[][] BEAM_ORIGINS = {{21.0F, -34.0F, -6.5F}, {21.0F, -34.0F, 6.5F}};

    private final ModelPart reel;
    private final ModelPart[] lamps;

    public HarvesterModel(ModelPart root) {
        super(root);
        this.reel = root.getChild("reel");
        this.lamps = Headlights.parts(root, LAMP_PARTS);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("header", CubeListBuilder.create().texOffs(0, 0)
                .addBox(22, -4, -24, 8, 6, 48), PartPose.ZERO);
        root.addOrReplaceChild("reel", CubeListBuilder.create().texOffs(112, 0)
                .addBox(-3, -3, -23, 6, 6, 46), PartPose.offset(26, -8, 0));
        root.addOrReplaceChild("reel_arm_left", CubeListBuilder.create().texOffs(0, 116)
                .addBox(16, -10, -24, 10, 2, 2), PartPose.ZERO);
        root.addOrReplaceChild("reel_arm_right", CubeListBuilder.create().texOffs(0, 116)
                .addBox(16, -10, 22, 10, 2, 2), PartPose.ZERO);
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 54)
                .addBox(-20, -14, -12, 38, 14, 24), PartPose.ZERO);
        root.addOrReplaceChild("left_track", CubeListBuilder.create().texOffs(120, 92)
                .addBox(-20, -2, -16, 36, 8, 4), PartPose.ZERO);
        root.addOrReplaceChild("right_track", CubeListBuilder.create().texOffs(120, 92)
                .addBox(-20, -2, 12, 36, 8, 4), PartPose.ZERO);
        root.addOrReplaceChild("grain_tank", CubeListBuilder.create().texOffs(124, 54)
                .addBox(-18, -24, -10, 20, 10, 20), PartPose.ZERO);
        root.addOrReplaceChild("unloader", CubeListBuilder.create().texOffs(84, 92)
                .addBox(-6, -28, 8, 2, 2, 16), PartPose.ZERO);
        root.addOrReplaceChild("seat", CubeListBuilder.create().texOffs(200, 92)
                .addBox(8, -18, -3, 5, 4, 6), PartPose.ZERO);
        root.addOrReplaceChild("steering", CubeListBuilder.create().texOffs(222, 92)
                .addBox(17, -22, -2, 1, 4, 4), PartPose.ZERO);
        root.addOrReplaceChild("cab_post_rl", CubeListBuilder.create().texOffs(80, 92)
                .addBox(4, -34, -10, 1, 20, 1), PartPose.ZERO);
        root.addOrReplaceChild("cab_post_rr", CubeListBuilder.create().texOffs(80, 92)
                .addBox(4, -34, 9, 1, 20, 1), PartPose.ZERO);
        root.addOrReplaceChild("cab_post_fl", CubeListBuilder.create().texOffs(80, 92)
                .addBox(19, -34, -10, 1, 20, 1), PartPose.ZERO);
        root.addOrReplaceChild("cab_post_fr", CubeListBuilder.create().texOffs(80, 92)
                .addBox(19, -34, 9, 1, 20, 1), PartPose.ZERO);
        root.addOrReplaceChild("cab_roof", CubeListBuilder.create().texOffs(0, 92)
                .addBox(3, -36, -11, 18, 2, 22), PartPose.ZERO);
        root.addOrReplaceChild("headlight_left", CubeListBuilder.create().texOffs(232, 92)
                .addBox(20, -35, -8, 1, 2, 3), PartPose.ZERO);
        root.addOrReplaceChild("headlight_right", CubeListBuilder.create().texOffs(232, 92)
                .addBox(20, -35, 5, 1, 2, 3), PartPose.ZERO);
        root.addOrReplaceChild("tail_light_left", CubeListBuilder.create().texOffs(240, 92)
                .addBox(-21, -8, -10, 1, 2, 3), PartPose.ZERO);
        root.addOrReplaceChild("tail_light_right", CubeListBuilder.create().texOffs(240, 92)
                .addBox(-21, -8, 7, 1, 2, 3), PartPose.ZERO);
        return LayerDefinition.create(mesh, 256, 128);
    }

    @Override
    public void setupAnim(BoatRenderState state) {
        super.setupAnim(state);
        reel.zRot = -state.ageInTicks * 0.15F; // top of the reel sweeps toward the machine
        boolean lit = state instanceof WorkVehicleRenderer.WorkRenderState work && work.lightsOn;
        for (ModelPart lamp : lamps) lamp.visible = !lit;
    }
}
