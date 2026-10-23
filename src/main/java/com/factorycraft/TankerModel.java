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

/** Tanker truck: cab at the front (+x), tank behind, pump at the rear. y down, ground at y = 6. */
public final class TankerModel extends EntityModel<BoatRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(FactoryCraft.MOD_ID, "tanker"), "main");
    public static final String[] LAMP_PARTS = {"headlight_left", "headlight_right", "tail_light_left", "tail_light_right"};
    public static final float[][] BEAM_ORIGINS = {{32.0F, -9.0F, -8.5F}, {32.0F, -9.0F, 8.5F}};
    private static final String[] ROLLERS = {"roller_l0", "roller_l1", "roller_l2", "roller_l3",
            "roller_r0", "roller_r1", "roller_r2", "roller_r3"};
    private static final float ROLLER_RADIUS = 0.234F;

    private final ModelPart[] rollers;
    private final ModelPart[] lamps;

    public TankerModel(ModelPart root) {
        super(root);
        this.rollers = Headlights.parts(root, ROLLERS);
        this.lamps = Headlights.parts(root, LAMP_PARTS);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("chassis", CubeListBuilder.create().texOffs(52, 70)
                .addBox(-28, -5, -11, 56, 3, 22), PartPose.ZERO);
        root.addOrReplaceChild("left_track", CubeListBuilder.create().texOffs(124, 96)
                .addBox(-27, -3, -16, 54, 9, 5), PartPose.ZERO);
        root.addOrReplaceChild("right_track", CubeListBuilder.create().texOffs(124, 96)
                .addBox(-27, -3, 11, 54, 9, 5), PartPose.ZERO);
        String[] sides = {"l", "r"};
        int[] xs = {-19, -7, 5, 17};
        for (String side : sides) {
            boolean left = side.equals("l");
            for (int i = 0; i < xs.length; i++) {
                root.addOrReplaceChild("roller_" + side + i, CubeListBuilder.create().texOffs(58, 116)
                        .addBox(-2.5F, -2.5F, left ? -0.7F : 0, 5, 5, 0.7F), PartPose.offset(xs[i], 1.5F, left ? -16 : 16));
            }
        }
        // Cab (same layout as the dump truck).
        root.addOrReplaceChild("cab_lower", CubeListBuilder.create().texOffs(172, 0)
                .addBox(12, -14, -12, 16, 9, 24), PartPose.ZERO);
        root.addOrReplaceChild("cab_back", CubeListBuilder.create().texOffs(0, 0)
                .addBox(12, -30, -12, 2, 16, 24), PartPose.ZERO);
        root.addOrReplaceChild("cab_roof", CubeListBuilder.create().texOffs(140, 40)
                .addBox(12, -32, -12, 16, 2, 24), PartPose.ZERO);
        root.addOrReplaceChild("cab_post_left", CubeListBuilder.create().texOffs(116, 96)
                .addBox(26, -30, -12, 2, 16, 2), PartPose.ZERO);
        root.addOrReplaceChild("cab_post_right", CubeListBuilder.create().texOffs(116, 96)
                .addBox(26, -30, 10, 2, 16, 2), PartPose.ZERO);
        root.addOrReplaceChild("steering", CubeListBuilder.create().texOffs(20, 116)
                .addBox(23, -18, -3, 1, 4, 6), PartPose.ZERO);
        root.addOrReplaceChild("hood", CubeListBuilder.create().texOffs(0, 40)
                .addBox(28, -13, -11, 3, 8, 22), PartPose.ZERO);
        root.addOrReplaceChild("bumper", CubeListBuilder.create().texOffs(0, 70)
                .addBox(31, -6, -12, 2, 2, 24), PartPose.ZERO);
        root.addOrReplaceChild("exhaust", CubeListBuilder.create().texOffs(108, 96)
                .addBox(10, -31, 10, 2, 18, 2), PartPose.ZERO);
        // Tank.
        root.addOrReplaceChild("tank", CubeListBuilder.create().texOffs(52, 0)
                .addBox(-28, -21, -11, 38, 16, 22), PartPose.ZERO);
        root.addOrReplaceChild("tank_top", CubeListBuilder.create().texOffs(0, 96)
                .addBox(-27, -23, -9, 36, 2, 18), PartPose.ZERO);
        root.addOrReplaceChild("tank_band", CubeListBuilder.create().texOffs(50, 40)
                .addBox(-20, -16, -11.5F, 22, 3, 23), PartPose.ZERO);
        root.addOrReplaceChild("hatch", CubeListBuilder.create().texOffs(34, 116)
                .addBox(-12, -25, -3, 6, 2, 6), PartPose.ZERO);
        root.addOrReplaceChild("walkway", CubeListBuilder.create().texOffs(86, 116)
                .addBox(-26, -24, -10, 34, 1, 1), PartPose.ZERO);
        root.addOrReplaceChild("pump", CubeListBuilder.create().texOffs(208, 70)
                .addBox(-31, -14, -6, 3, 9, 12), PartPose.ZERO);
        root.addOrReplaceChild("hose_reel", CubeListBuilder.create().texOffs(0, 116)
                .addBox(-33, -12, -4, 2, 6, 8), PartPose.ZERO);
        root.addOrReplaceChild("headlight_left", CubeListBuilder.create().texOffs(70, 116)
                .addBox(31, -10, -10, 1, 2, 3), PartPose.ZERO);
        root.addOrReplaceChild("headlight_right", CubeListBuilder.create().texOffs(70, 116)
                .addBox(31, -10, 7, 1, 2, 3), PartPose.ZERO);
        root.addOrReplaceChild("tail_light_left", CubeListBuilder.create().texOffs(78, 116)
                .addBox(-29, -5, -11, 1, 2, 3), PartPose.ZERO);
        root.addOrReplaceChild("tail_light_right", CubeListBuilder.create().texOffs(78, 116)
                .addBox(-29, -5, 8, 1, 2, 3), PartPose.ZERO);
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
