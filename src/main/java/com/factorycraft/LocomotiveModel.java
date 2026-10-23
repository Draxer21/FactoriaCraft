package com.factorycraft;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.BoatRenderState;
import net.minecraft.resources.Identifier;

/** Diesel locomotive: long hood in front (+x), cab at the rear, two bogies. y down, ground at y = 6. */
public final class LocomotiveModel extends EntityModel<BoatRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(FactoryCraft.MOD_ID, "locomotive"), "main");
    public static final String[] LAMP_PARTS = {"headlight_left", "headlight_right", "tail_light_left", "tail_light_right"};
    public static final float[][] BEAM_ORIGINS = {{31.0F, -14.5F, -4.5F}, {31.0F, -14.5F, 4.5F}};
    private static final String[] WHEELS = {"roller_l0", "roller_l1", "roller_l2", "roller_l3",
            "roller_r0", "roller_r1", "roller_r2", "roller_r3"};
    /** Wheel radius in blocks (3.5 px * 1.5 / 16). */
    private static final float WHEEL_RADIUS = 0.33F;

    private final ModelPart[] wheels;
    private final ModelPart[] lamps;

    public LocomotiveModel(ModelPart root) {
        super(root);
        this.wheels = Headlights.parts(root, WHEELS);
        this.lamps = Headlights.parts(root, LAMP_PARTS);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("frame", CubeListBuilder.create().texOffs(0, 76)
                .addBox(-31, -6, -12, 62, 3, 24), PartPose.ZERO);
        root.addOrReplaceChild("bogie_rear", CubeListBuilder.create().texOffs(98, 103)
                .addBox(-28, -3, -11, 18, 3, 22), PartPose.ZERO);
        root.addOrReplaceChild("bogie_front", CubeListBuilder.create().texOffs(98, 103)
                .addBox(10, -3, -11, 18, 3, 22), PartPose.ZERO);
        String[] sides = {"l", "r"};
        int[] xs = {-24, -14, 14, 24};
        for (String side : sides) {
            boolean left = side.equals("l");
            for (int i = 0; i < xs.length; i++) {
                root.addOrReplaceChild("roller_" + side + i, CubeListBuilder.create().texOffs(178, 129)
                        .addBox(-3.5F, -3.5F, left ? -1 : 0, 7, 7, 1), PartPose.offset(xs[i], 2.5F, left ? -12 : 12));
            }
        }
        root.addOrReplaceChild("hood", CubeListBuilder.create().texOffs(52, 0)
                .addBox(-8, -20, -9, 38, 14, 18), PartPose.ZERO);
        root.addOrReplaceChild("hood_stripe", CubeListBuilder.create().texOffs(0, 129)
                .addBox(-8, -12, -9.5F, 38, 2, 19), PartPose.ZERO);
        root.addOrReplaceChild("stack", CubeListBuilder.create().texOffs(162, 129)
                .addBox(18, -26, -2, 4, 6, 4), PartPose.ZERO);
        root.addOrReplaceChild("cowcatcher", CubeListBuilder.create().texOffs(102, 48)
                .addBox(31, -5, -11, 4, 6, 22), PartPose.ZERO);
        // Cab.
        root.addOrReplaceChild("cab_floor", CubeListBuilder.create().texOffs(0, 103)
                .addBox(-30, -8, -12, 22, 2, 24), PartPose.ZERO);
        root.addOrReplaceChild("seat", CubeListBuilder.create().texOffs(140, 129)
                .addBox(-22, -12, -3, 5, 4, 6), PartPose.ZERO);
        root.addOrReplaceChild("controls", CubeListBuilder.create().texOffs(114, 129)
                .addBox(-13, -16, -5, 3, 8, 10), PartPose.ZERO);
        root.addOrReplaceChild("cab_wall_back", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-31, -32, -12, 2, 24, 24), PartPose.ZERO);
        root.addOrReplaceChild("cab_post_fl", CubeListBuilder.create().texOffs(92, 103)
                .addBox(-9, -32, -12, 1, 24, 2), PartPose.ZERO);
        root.addOrReplaceChild("cab_post_fr", CubeListBuilder.create().texOffs(92, 103)
                .addBox(-9, -32, 10, 1, 24, 2), PartPose.ZERO);
        root.addOrReplaceChild("cab_roof", CubeListBuilder.create().texOffs(0, 48)
                .addBox(-32, -34, -13, 25, 2, 26), PartPose.ZERO);
        // Lights.
        root.addOrReplaceChild("headlight_left", CubeListBuilder.create().texOffs(194, 129)
                .addBox(30, -16, -6, 1, 3, 3, new CubeDeformation(0.06F)), PartPose.ZERO);
        root.addOrReplaceChild("headlight_right", CubeListBuilder.create().texOffs(194, 129)
                .addBox(30, -16, 3, 1, 3, 3, new CubeDeformation(0.06F)), PartPose.ZERO);
        root.addOrReplaceChild("tail_light_left", CubeListBuilder.create().texOffs(202, 129)
                .addBox(-32, -14, -10, 1, 2, 3, new CubeDeformation(0.06F)), PartPose.ZERO);
        root.addOrReplaceChild("tail_light_right", CubeListBuilder.create().texOffs(202, 129)
                .addBox(-32, -14, 7, 1, 2, 3, new CubeDeformation(0.06F)), PartPose.ZERO);
        return LayerDefinition.create(mesh, 256, 256);
    }

    @Override
    public void setupAnim(BoatRenderState state) {
        super.setupAnim(state);
        boolean lit = false;
        float roll = 0.0F;
        if (state instanceof WorkVehicleRenderer.WorkRenderState work) {
            roll = work.travelled / WHEEL_RADIUS;
            lit = work.lightsOn;
        }
        for (ModelPart wheel : wheels) wheel.zRot = roll;
        for (ModelPart lamp : lamps) lamp.visible = !lit;
    }
}
