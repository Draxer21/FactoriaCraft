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

/** Crawler crane. +x is the front, y points down, ground at y = 6. The boom rises over the cab. */
public final class CraneModel extends EntityModel<BoatRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(FactoryCraft.MOD_ID, "crane"), "main");
    public static final String[] LAMP_PARTS = {"headlight_left", "headlight_right", "tail_light_left", "tail_light_right"};
    public static final float[][] BEAM_ORIGINS = {{23.0F, -9.0F, -7.5F}, {23.0F, -9.0F, 7.5F}};

    private static final String[] ROLLERS = {"roller_l0", "roller_l1", "roller_l2", "roller_l3",
            "roller_r0", "roller_r1", "roller_r2", "roller_r3"};
    /** Roller radius in blocks (2.5 px * 1.5 / 16). */
    private static final float ROLLER_RADIUS = 0.234F;
    /** Boom angle: raised about 34 degrees. */
    private static final float BOOM_ANGLE = -0.6F;

    private final ModelPart[] rollers;
    private final ModelPart[] lamps;

    public CraneModel(ModelPart root) {
        super(root);
        this.rollers = Headlights.parts(root, ROLLERS);
        this.lamps = Headlights.parts(root, LAMP_PARTS);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Undercarriage: crawler tracks and road wheels.
        root.addOrReplaceChild("left_track", CubeListBuilder.create().texOffs(0, 84)
                .addBox(-26, -3, -17, 52, 9, 6), PartPose.ZERO);
        root.addOrReplaceChild("right_track", CubeListBuilder.create().texOffs(0, 84)
                .addBox(-26, -3, 11, 52, 9, 6), PartPose.ZERO);
        root.addOrReplaceChild("chassis", CubeListBuilder.create().texOffs(0, 37)
                .addBox(-24, -6, -11, 48, 3, 22), PartPose.ZERO);
        String[] sides = {"l", "r"};
        int[] xs = {-17, -6, 5, 16};
        for (String side : sides) {
            boolean left = side.equals("l");
            for (int i = 0; i < xs.length; i++) {
                root.addOrReplaceChild("roller_" + side + i, CubeListBuilder.create().texOffs(214, 99)
                        .addBox(-2.5F, -2.5F, left ? -0.7F : 0, 5, 5, 0.7F), PartPose.offset(xs[i], 1.5F, left ? -17 : 17));
            }
        }

        // Upper works: turntable, engine house, counterweight, boom mount.
        root.addOrReplaceChild("turntable", CubeListBuilder.create().texOffs(142, 0)
                .addBox(-12, -8, -12, 24, 2, 24), PartPose.ZERO);
        root.addOrReplaceChild("house", CubeListBuilder.create().texOffs(4, 0)
                .addBox(-22, -22, -10, 22, 14, 20), PartPose.ZERO);
        root.addOrReplaceChild("counterweight", CubeListBuilder.create().texOffs(88, 0)
                .addBox(-27, -20, -11, 5, 12, 22), PartPose.ZERO);
        root.addOrReplaceChild("boom_mount", CubeListBuilder.create().texOffs(84, 62)
                .addBox(-10, -30, -4, 8, 8, 8), PartPose.ZERO);
        root.addOrReplaceChild("exhaust", CubeListBuilder.create().texOffs(168, 99)
                .addBox(-4, -30, 6, 2, 8, 2), PartPose.ZERO);

        // Open cab in front of the house.
        root.addOrReplaceChild("cab_floor", CubeListBuilder.create().texOffs(0, 62)
                .addBox(2, -10, -10, 20, 2, 20), PartPose.ZERO);
        root.addOrReplaceChild("seat", CubeListBuilder.create().texOffs(176, 99)
                .addBox(8, -12, -3, 5, 2, 6), PartPose.ZERO);
        root.addOrReplaceChild("console", CubeListBuilder.create().texOffs(152, 99)
                .addBox(18, -16, -3, 2, 6, 6), PartPose.ZERO);
        root.addOrReplaceChild("cab_post_rl", CubeListBuilder.create().texOffs(80, 62)
                .addBox(3, -30, -10, 1, 20, 1), PartPose.ZERO);
        root.addOrReplaceChild("cab_post_rr", CubeListBuilder.create().texOffs(80, 62)
                .addBox(3, -30, 9, 1, 20, 1), PartPose.ZERO);
        root.addOrReplaceChild("cab_post_fl", CubeListBuilder.create().texOffs(80, 62)
                .addBox(20, -30, -10, 1, 20, 1), PartPose.ZERO);
        root.addOrReplaceChild("cab_post_fr", CubeListBuilder.create().texOffs(80, 62)
                .addBox(20, -30, 9, 1, 20, 1), PartPose.ZERO);
        root.addOrReplaceChild("cab_roof", CubeListBuilder.create().texOffs(140, 37)
                .addBox(2, -32, -11, 20, 2, 22), PartPose.ZERO);

        // Lattice boom, pivoting on the mount and rising over the cab.
        PartPose boomPose = PartPose.offsetAndRotation(-6, -30, 0, 0, 0, BOOM_ANGLE);
        root.addOrReplaceChild("boom", CubeListBuilder.create().texOffs(0, 99)
                .addBox(0, -3, -3, 70, 6, 6), boomPose);
        root.addOrReplaceChild("boom_tip", CubeListBuilder.create().texOffs(116, 62)
                .addBox(68, -4, -4, 6, 8, 8), boomPose);
        // Cable and hook hang straight down from the boom tip.
        root.addOrReplaceChild("cable", CubeListBuilder.create().texOffs(0, 0)
                .addBox(52.5F, -70, -0.5F, 1, 36, 1), PartPose.ZERO);
        root.addOrReplaceChild("hook", CubeListBuilder.create().texOffs(198, 99)
                .addBox(51, -34, -2, 4, 4, 4), PartPose.ZERO);

        // Lights.
        root.addOrReplaceChild("headlight_left", CubeListBuilder.create().texOffs(226, 99)
                .addBox(22, -10, -9, 1, 2, 3), PartPose.ZERO);
        root.addOrReplaceChild("headlight_right", CubeListBuilder.create().texOffs(226, 99)
                .addBox(22, -10, 6, 1, 2, 3), PartPose.ZERO);
        root.addOrReplaceChild("tail_light_left", CubeListBuilder.create().texOffs(234, 99)
                .addBox(-28, -14, -9, 1, 2, 3), PartPose.ZERO);
        root.addOrReplaceChild("tail_light_right", CubeListBuilder.create().texOffs(234, 99)
                .addBox(-28, -14, 6, 1, 2, 3), PartPose.ZERO);
        return LayerDefinition.create(mesh, 256, 128);
    }

    @Override
    public void setupAnim(BoatRenderState state) {
        super.setupAnim(state);
        boolean lit = false;
        float roll = 0.0F;
        if (state instanceof WorkVehicleRenderer.WorkRenderState work) {
            roll = work.travelled / ROLLER_RADIUS;
            lit = work.lightsOn;
        }
        for (ModelPart roller : rollers) roller.zRot = roll;
        for (ModelPart lamp : lamps) lamp.visible = !lit;
    }
}
