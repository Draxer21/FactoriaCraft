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

    private final TrackTreads treads;

    public ForesterModel(ModelPart root) {
        super(root);
        this.treads = new TrackTreads(root, -24, 24, 2, 1.5F);
        this.rollers = Headlights.parts(root, ROLLERS);
        this.lamps = Headlights.parts(root, LAMP_PARTS);
        this.saw = root.getChild("saw");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("left_track", CubeListBuilder.create().texOffs(0, 58)
                .addBox(-24, -4, -16, 48, 10, 6), PartPose.ZERO);
        root.addOrReplaceChild("right_track", CubeListBuilder.create().texOffs(0, 58)
                .addBox(-24, -4, 10, 48, 10, 6), PartPose.ZERO);
        TrackTreads.add(root, 0, 58, -24, 24, -4, 6, new float[] {-16, -10}, new float[] {10, 16});
        String[] sides = {"l", "r"};
        int[] xs = {-16, -5, 6, 17};
        for (String side : sides) {
            boolean left = side.equals("l");
            for (int i = 0; i < xs.length; i++) {
                root.addOrReplaceChild("roller_" + side + i, CubeListBuilder.create().texOffs(128, 74)
                        .addBox(-2.5F, -2.5F, left ? -0.7F : 0, 5, 5, 0.7F), PartPose.offset(xs[i], 1.0F, left ? -16 : 16));
            }
        }
        root.addOrReplaceChild("deck", CubeListBuilder.create().texOffs(102, 0)
                .addBox(-23, -8, -12, 44, 4, 24), PartPose.ZERO);
        root.addOrReplaceChild("engine", CubeListBuilder.create().texOffs(0, 0)
                .addBox(0, -18, -10, 16, 10, 20), PartPose.ZERO);
        root.addOrReplaceChild("exhaust", CubeListBuilder.create().texOffs(240, 58)
                .addBox(2, -26, 6, 2, 8, 2), PartPose.ZERO);
        // Cab.
        root.addOrReplaceChild("seat", CubeListBuilder.create().texOffs(218, 58)
                .addBox(-13, -12, -3, 5, 4, 6), PartPose.ZERO);
        root.addOrReplaceChild("controls", CubeListBuilder.create().texOffs(108, 58)
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
        root.addOrReplaceChild("boom_post", CubeListBuilder.create().texOffs(186, 58)
                .addBox(8, -26, -3, 6, 8, 6), PartPose.ZERO);
        root.addOrReplaceChild("boom", CubeListBuilder.create().texOffs(56, 74)
                .addBox(8, -29, -2, 24, 3, 4), PartPose.ZERO);
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(72, 0)
                .addBox(29, -27, -5, 5, 20, 10), PartPose.ZERO);
        root.addOrReplaceChild("grapple_left", CubeListBuilder.create().texOffs(210, 58)
                .addBox(34, -24, -7, 2, 10, 2), PartPose.ZERO);
        root.addOrReplaceChild("grapple_right", CubeListBuilder.create().texOffs(210, 58)
                .addBox(34, -24, 5, 2, 10, 2), PartPose.ZERO);
        // Circular saw blade under the felling head: overlapping plates and a 45°-turned
        // plate make a round disc, with teeth around the rim and a hub in the middle.
        PartDefinition saw = root.addOrReplaceChild("saw", CubeListBuilder.create().texOffs(0, 74)
                .addBox(-10, -0.5F, -4, 20, 1, 8), PartPose.offset(34, -5.5F, 0));
        saw.addOrReplaceChild("saw_cross", CubeListBuilder.create().texOffs(102, 30)
                .addBox(-4, -0.5F, -10, 8, 1, 20), PartPose.ZERO);
        saw.addOrReplaceChild("saw_square", CubeListBuilder.create().texOffs(130, 58)
                .addBox(-7, -0.5F, -7, 14, 1, 14), PartPose.ZERO);
        saw.addOrReplaceChild("saw_diamond", CubeListBuilder.create().texOffs(130, 58)
                .addBox(-7, -0.5F, -7, 14, 1, 14), PartPose.rotation(0, (float) (Math.PI / 4), 0));
        saw.addOrReplaceChild("saw_hub", CubeListBuilder.create().texOffs(112, 74)
                .addBox(-2, -1.5F, -2, 4, 3, 4), PartPose.ZERO);
        for (int k = 0; k < 12; k++) {
            saw.addOrReplaceChild("saw_tooth_" + k, CubeListBuilder.create().texOffs(166, 74)
                    .addBox(9.5F, -0.5F, -1, 2, 1, 2), PartPose.rotation(0, (float) (k * Math.PI / 6 + Math.PI / 12), 0));
        }
        root.addOrReplaceChild("saw_arm", CubeListBuilder.create().texOffs(140, 74)
                .addBox(32, -8, -1, 3, 3, 2), PartPose.ZERO);
        // Lights.
        root.addOrReplaceChild("headlight_left", CubeListBuilder.create().texOffs(150, 74)
                .addBox(0, -33, -11, 1, 2, 3, new CubeDeformation(0.06F)), PartPose.ZERO);
        root.addOrReplaceChild("headlight_right", CubeListBuilder.create().texOffs(150, 74)
                .addBox(0, -33, 8, 1, 2, 3, new CubeDeformation(0.06F)), PartPose.ZERO);
        root.addOrReplaceChild("tail_light_left", CubeListBuilder.create().texOffs(158, 74)
                .addBox(-24, -12, -11, 1, 2, 3, new CubeDeformation(0.06F)), PartPose.ZERO);
        root.addOrReplaceChild("tail_light_right", CubeListBuilder.create().texOffs(158, 74)
                .addBox(-24, -12, 8, 1, 2, 3, new CubeDeformation(0.06F)), PartPose.ZERO);
        return LayerDefinition.create(mesh, 256, 128);
    }

    @Override
    public void setupAnim(BoatRenderState state) {
        super.setupAnim(state);
        if (state instanceof Headlights.LitBoatRenderState moving) treads.animate(moving.travelled);
        boolean lit = false;
        float travelled = 0.0F;
        if (state instanceof WorkVehicleRenderer.WorkRenderState work) {
            travelled = work.travelled;
            lit = work.lightsOn;
        }
        for (ModelPart roller : rollers) roller.zRot = travelled / ROLLER_RADIUS;
        // The saw spins (horizontally) as long as the machine moves.
        saw.yRot = state.ageInTicks * 0.9F + travelled / SAW_RADIUS;
        for (ModelPart lamp : lamps) lamp.visible = !lit;
    }
}
