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

/** Forestry harvester: cab at the rear, boom with a felling head and saw at the front (+x). y down, ground at 6. */
public final class ForesterModel extends EntityModel<BoatRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(FactoryCraft.MOD_ID, "forester"), "main");
    public static final String[] LAMP_PARTS = {"headlight_left", "headlight_right", "tail_light_left", "tail_light_right"};
    public static final float[][] BEAM_ORIGINS = {{1.0F, -32.0F, -9.5F}, {1.0F, -32.0F, 9.5F}};
    private static final String[] ROLLERS = {"roller_l0", "roller_l1", "roller_l2", "roller_l3",
            "roller_r0", "roller_r1", "roller_r2", "roller_r3"};
    private static final float ROLLER_RADIUS = 0.234F;
    /** Saw disc radius in blocks (6 px * 1.5 / 16). */
    private static final float SAW_RADIUS = 0.56F;

    private final ModelPart[] rollers;
    private final ModelPart[] lamps;
    private final ModelPart saw;

    public ForesterModel(ModelPart root) {
        super(root);
        this.rollers = Headlights.parts(root, ROLLERS);
        this.lamps = Headlights.parts(root, LAMP_PARTS);
        this.saw = root.getChild("saw");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("left_track", CubeListBuilder.create().texOffs(102, 30)
                .addBox(-24, -4, -16, 48, 10, 6), PartPose.ZERO);
        root.addOrReplaceChild("right_track", CubeListBuilder.create().texOffs(102, 30)
                .addBox(-24, -4, 10, 48, 10, 6), PartPose.ZERO);
        String[] sides = {"l", "r"};
        int[] xs = {-16, -5, 6, 17};
        for (String side : sides) {
            boolean left = side.equals("l");
            for (int i = 0; i < xs.length; i++) {
                root.addOrReplaceChild("roller_" + side + i, CubeListBuilder.create().texOffs(142, 58)
                        .addBox(-2.5F, -2.5F, left ? -0.7F : 0, 5, 5, 0.7F), PartPose.offset(xs[i], 1.0F, left ? -16 : 16));
            }
        }
        root.addOrReplaceChild("deck", CubeListBuilder.create().texOffs(102, 0)
                .addBox(-23, -8, -12, 44, 4, 24), PartPose.ZERO);
        root.addOrReplaceChild("engine", CubeListBuilder.create().texOffs(0, 0)
                .addBox(0, -18, -10, 16, 10, 20), PartPose.ZERO);
        root.addOrReplaceChild("exhaust", CubeListBuilder.create().texOffs(78, 58)
                .addBox(2, -26, 6, 2, 8, 2), PartPose.ZERO);
        // Cab.
        root.addOrReplaceChild("seat", CubeListBuilder.create().texOffs(56, 58)
                .addBox(-13, -12, -3, 5, 4, 6), PartPose.ZERO);
        root.addOrReplaceChild("controls", CubeListBuilder.create().texOffs(210, 30)
                .addBox(-5, -16, -4, 3, 8, 8), PartPose.ZERO);
        root.addOrReplaceChild("cab_post_rl", CubeListBuilder.create().texOffs(98, 30)
                .addBox(-21, -32, -12, 1, 24, 1), PartPose.ZERO);
        root.addOrReplaceChild("cab_post_rr", CubeListBuilder.create().texOffs(98, 30)
                .addBox(-21, -32, 11, 1, 24, 1), PartPose.ZERO);
        root.addOrReplaceChild("cab_post_fl", CubeListBuilder.create().texOffs(98, 30)
                .addBox(-1, -32, -12, 1, 24, 1), PartPose.ZERO);
        root.addOrReplaceChild("cab_post_fr", CubeListBuilder.create().texOffs(98, 30)
                .addBox(-1, -32, 11, 1, 24, 1), PartPose.ZERO);
        root.addOrReplaceChild("cab_roof", CubeListBuilder.create().texOffs(0, 30)
                .addBox(-22, -34, -13, 23, 2, 26), PartPose.ZERO);
        // Boom and felling head.
        root.addOrReplaceChild("boom_post", CubeListBuilder.create().texOffs(232, 30)
                .addBox(8, -26, -3, 6, 8, 6), PartPose.ZERO);
        root.addOrReplaceChild("boom", CubeListBuilder.create().texOffs(86, 58)
                .addBox(8, -29, -2, 24, 3, 4), PartPose.ZERO);
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(72, 0)
                .addBox(29, -27, -5, 5, 20, 10), PartPose.ZERO);
        root.addOrReplaceChild("grapple_left", CubeListBuilder.create().texOffs(48, 58)
                .addBox(34, -24, -7, 2, 10, 2), PartPose.ZERO);
        root.addOrReplaceChild("grapple_right", CubeListBuilder.create().texOffs(48, 58)
                .addBox(34, -24, 5, 2, 10, 2), PartPose.ZERO);
        root.addOrReplaceChild("saw", CubeListBuilder.create().texOffs(0, 58)
                .addBox(-6, -0.5F, -6, 12, 1, 12), PartPose.offset(31, -6, 0));
        // Lights.
        root.addOrReplaceChild("headlight_left", CubeListBuilder.create().texOffs(154, 58)
                .addBox(0, -33, -11, 1, 2, 3), PartPose.ZERO);
        root.addOrReplaceChild("headlight_right", CubeListBuilder.create().texOffs(154, 58)
                .addBox(0, -33, 8, 1, 2, 3), PartPose.ZERO);
        root.addOrReplaceChild("tail_light_left", CubeListBuilder.create().texOffs(162, 58)
                .addBox(-24, -12, -11, 1, 2, 3), PartPose.ZERO);
        root.addOrReplaceChild("tail_light_right", CubeListBuilder.create().texOffs(162, 58)
                .addBox(-24, -12, 8, 1, 2, 3), PartPose.ZERO);
        return LayerDefinition.create(mesh, 256, 128);
    }

    @Override
    public void setupAnim(BoatRenderState state) {
        super.setupAnim(state);
        boolean lit = false;
        float travelled = 0.0F;
        if (state instanceof WorkVehicleRenderer.WorkRenderState work) {
            travelled = work.travelled;
            lit = work.lightsOn;
        }
        for (ModelPart roller : rollers) roller.zRot = travelled / ROLLER_RADIUS;
        // The saw spins (horizontally) as long as the machine moves.
        saw.yRot = travelled / SAW_RADIUS * 4.0F;
        for (ModelPart lamp : lamps) lamp.visible = !lit;
    }
}
