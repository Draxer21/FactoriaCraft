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

/** Bulldozer: blade at the front (+x), engine hood, cab at the rear. y down, ground at y = 6. */
public final class BulldozerModel extends EntityModel<BoatRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(FactoryCraft.MOD_ID, "bulldozer"), "main");
    public static final String[] LAMP_PARTS = {"headlight_left", "headlight_right", "tail_light_left", "tail_light_right"};
    public static final float[][] BEAM_ORIGINS = {{1.0F, -32.0F, -9.5F}, {1.0F, -32.0F, 9.5F}};
    private static final String[] ROLLERS = {"roller_l0", "roller_l1", "roller_l2", "roller_l3",
            "roller_r0", "roller_r1", "roller_r2", "roller_r3"};
    private static final float ROLLER_RADIUS = 0.234F;

    private final ModelPart[] rollers;
    private final ModelPart[] lamps;

    public BulldozerModel(ModelPart root) {
        super(root);
        this.rollers = Headlights.parts(root, ROLLERS);
        this.lamps = Headlights.parts(root, LAMP_PARTS);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("left_track", CubeListBuilder.create().texOffs(122, 100)
                .addBox(-24, -4, -16, 48, 10, 6), PartPose.ZERO);
        root.addOrReplaceChild("right_track", CubeListBuilder.create().texOffs(122, 100)
                .addBox(-24, -4, 10, 48, 10, 6), PartPose.ZERO);
        String[] sides = {"l", "r"};
        int[] xs = {-16, -5, 6, 17};
        for (String side : sides) {
            boolean left = side.equals("l");
            for (int i = 0; i < xs.length; i++) {
                root.addOrReplaceChild("roller_" + side + i, CubeListBuilder.create().texOffs(100, 128)
                        .addBox(-2.5F, -2.5F, left ? -0.7F : 0, 5, 5, 0.7F), PartPose.offset(xs[i], 1.0F, left ? -16 : 16));
            }
        }
        root.addOrReplaceChild("deck", CubeListBuilder.create().texOffs(0, 72)
                .addBox(-22, -8, -12, 46, 4, 24), PartPose.ZERO);
        root.addOrReplaceChild("hood", CubeListBuilder.create().texOffs(0, 100)
                .addBox(0, -18, -9, 24, 10, 18), PartPose.ZERO);
        root.addOrReplaceChild("grille", CubeListBuilder.create().texOffs(88, 100)
                .addBox(24, -17, -8, 1, 8, 16), PartPose.ZERO);
        root.addOrReplaceChild("exhaust", CubeListBuilder.create().texOffs(70, 128)
                .addBox(18, -27, -5, 2, 9, 2), PartPose.ZERO);

        // Cab.
        root.addOrReplaceChild("seat", CubeListBuilder.create().texOffs(78, 128)
                .addBox(-14, -12, -3, 5, 4, 6), PartPose.ZERO);
        root.addOrReplaceChild("controls", CubeListBuilder.create().texOffs(40, 128)
                .addBox(-5, -16, -4, 3, 8, 8), PartPose.ZERO);
        root.addOrReplaceChild("cab_post_rl", CubeListBuilder.create().texOffs(84, 100)
                .addBox(-21, -32, -12, 1, 24, 1), PartPose.ZERO);
        root.addOrReplaceChild("cab_post_rr", CubeListBuilder.create().texOffs(84, 100)
                .addBox(-21, -32, 11, 1, 24, 1), PartPose.ZERO);
        root.addOrReplaceChild("cab_post_fl", CubeListBuilder.create().texOffs(84, 100)
                .addBox(-1, -32, -12, 1, 24, 1), PartPose.ZERO);
        root.addOrReplaceChild("cab_post_fr", CubeListBuilder.create().texOffs(84, 100)
                .addBox(-1, -32, 11, 1, 24, 1), PartPose.ZERO);
        root.addOrReplaceChild("cab_roof", CubeListBuilder.create().texOffs(140, 72)
                .addBox(-22, -34, -13, 23, 2, 26), PartPose.ZERO);

        // Blade with push arms, and a ripper at the back.
        root.addOrReplaceChild("blade", CubeListBuilder.create().texOffs(0, 0)
                .addBox(29, -13, -27, 3, 18, 54), PartPose.ZERO);
        root.addOrReplaceChild("blade_edge", CubeListBuilder.create().texOffs(114, 0)
                .addBox(32, 3, -27, 1, 2, 54), PartPose.ZERO);
        root.addOrReplaceChild("push_arm_left", CubeListBuilder.create().texOffs(112, 128)
                .addBox(10, -3, -18, 19, 3, 2), PartPose.ZERO);
        root.addOrReplaceChild("push_arm_right", CubeListBuilder.create().texOffs(112, 128)
                .addBox(10, -3, 16, 19, 3, 2), PartPose.ZERO);
        root.addOrReplaceChild("lift_ram_left", CubeListBuilder.create().texOffs(170, 128)
                .addBox(14, -14, -11, 15, 2, 2), PartPose.ZERO);
        root.addOrReplaceChild("lift_ram_right", CubeListBuilder.create().texOffs(170, 128)
                .addBox(14, -14, 9, 15, 2, 2), PartPose.ZERO);
        root.addOrReplaceChild("ripper_frame", CubeListBuilder.create().texOffs(0, 128)
                .addBox(-28, -8, -7, 6, 2, 14), PartPose.ZERO);
        root.addOrReplaceChild("ripper_tooth", CubeListBuilder.create().texOffs(62, 128)
                .addBox(-30, -8, -1, 2, 12, 2), PartPose.ZERO);

        root.addOrReplaceChild("headlight_left", CubeListBuilder.create().texOffs(154, 128)
                .addBox(0, -33, -11, 1, 2, 3), PartPose.ZERO);
        root.addOrReplaceChild("headlight_right", CubeListBuilder.create().texOffs(154, 128)
                .addBox(0, -33, 8, 1, 2, 3), PartPose.ZERO);
        root.addOrReplaceChild("tail_light_left", CubeListBuilder.create().texOffs(162, 128)
                .addBox(-23, -12, -11, 1, 2, 3), PartPose.ZERO);
        root.addOrReplaceChild("tail_light_right", CubeListBuilder.create().texOffs(162, 128)
                .addBox(-23, -12, 8, 1, 2, 3), PartPose.ZERO);
        return LayerDefinition.create(mesh, 256, 256);
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
