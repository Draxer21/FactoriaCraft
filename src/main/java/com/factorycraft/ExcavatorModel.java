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

public final class ExcavatorModel extends EntityModel<BoatRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(FactoryCraft.MOD_ID, "excavator"), "main");

    private final ModelPart boomBase;
    private final ModelPart boomArm;
    private final ModelPart bucket;
    private final ModelPart[] lamps;

    /** Lamp parts drawn full-bright by the renderer when the headlights are on. */
    public static final String[] LAMP_PARTS = {"headlight_left", "headlight_right", "work_lights"};
    /** Beam origins (model px): front face centre of each headlight. */
    public static final float[][] BEAM_ORIGINS = {{11.0F, -5.0F, -5.5F}, {11.0F, -5.0F, 5.5F}};

    public ExcavatorModel(ModelPart root) {
        super(root);
        this.boomBase = root.getChild("boom_base");
        this.boomArm = boomBase.getChild("boom_arm");
        this.bucket = boomArm.getChild("bucket");
        this.lamps = Headlights.parts(root, LAMP_PARTS);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Twin steel tracks, with raised cleats so the running gear reads as a machine,
        // not as two plain rectangular blocks.
        root.addOrReplaceChild("left_track", CubeListBuilder.create().texOffs(102, 52)
                .addBox(-14, -1, -10, 28, 5, 7), PartPose.ZERO);
        root.addOrReplaceChild("right_track", CubeListBuilder.create().texOffs(102, 52)
                .addBox(-14, -1, 3, 28, 5, 7), PartPose.ZERO);
        for (int x = -12; x <= 12; x += 4) {
            root.addOrReplaceChild("left_track_cleat_" + x, CubeListBuilder.create().texOffs(14, 67)
                    .addBox(x, -1.6F, -10.35F, 2, 1, 7.7F), PartPose.ZERO);
            root.addOrReplaceChild("right_track_cleat_" + x, CubeListBuilder.create().texOffs(14, 67)
                    .addBox(x, -1.6F, 2.65F, 2, 1, 7.7F), PartPose.ZERO);
        }

        // Lower frame and rollers visible along the outside of each track.
        root.addOrReplaceChild("undercarriage", CubeListBuilder.create().texOffs(118, 32)
                .addBox(-11, -5, -6, 22, 4, 12), PartPose.ZERO);
        for (int x = -9; x <= 9; x += 6) {
            root.addOrReplaceChild("left_roller_" + x, CubeListBuilder.create().texOffs(163, 67)
                    .addBox(x - 1.5F, -1, -10.55F, 3, 3, 0.7F), PartPose.ZERO);
            root.addOrReplaceChild("right_roller_" + x, CubeListBuilder.create().texOffs(163, 67)
                    .addBox(x - 1.5F, -1, 9.85F, 3, 3, 0.7F), PartPose.ZERO);
        }

        root.addOrReplaceChild("chassis", CubeListBuilder.create().texOffs(50, 32)
                .addBox(-10, -7, -7, 20, 5, 14), PartPose.ZERO);
        root.addOrReplaceChild("turntable", CubeListBuilder.create().texOffs(0, 52)
                .addBox(-7, -10, -6, 14, 3, 12), PartPose.ZERO);
        root.addOrReplaceChild("counterweight", CubeListBuilder.create().texOffs(0, 32)
                .addBox(-12, -14, -7, 9, 6, 14), PartPose.ZERO);

        // Open operator cab: floor, seat, controls and corner pillars. The
        // open center lets the rider look forward from the driver's position.
        root.addOrReplaceChild("cab_floor", CubeListBuilder.create().texOffs(52, 52)
                .addBox(-8, -11, -6, 13, 2, 12), PartPose.ZERO);
        root.addOrReplaceChild("seat_base", CubeListBuilder.create().texOffs(228, 52)
                .addBox(-4.5F, -13, -2.5F, 5, 5, 5), PartPose.ZERO);
        root.addOrReplaceChild("seat_back", CubeListBuilder.create().texOffs(0, 67)
                .addBox(-6, -17, -2.5F, 2, 5, 5), PartPose.ZERO);
        root.addOrReplaceChild("cab_rear_panel", CubeListBuilder.create().texOffs(190, 0)
                .addBox(-8, -21, -6, 1, 10, 12), PartPose.ZERO);
        root.addOrReplaceChild("cab_rear_left_post", CubeListBuilder.create().texOffs(46, 32)
                .addBox(-7.5F, -30, -6, 1, 19, 1), PartPose.ZERO);
        root.addOrReplaceChild("cab_rear_right_post", CubeListBuilder.create().texOffs(46, 32)
                .addBox(-7.5F, -30, 5, 1, 19, 1), PartPose.ZERO);
        root.addOrReplaceChild("cab_front_left_post", CubeListBuilder.create().texOffs(46, 32)
                .addBox(3.5F, -30, -6, 1, 19, 1), PartPose.ZERO);
        root.addOrReplaceChild("cab_front_right_post", CubeListBuilder.create().texOffs(46, 32)
                .addBox(3.5F, -30, 5, 1, 19, 1), PartPose.ZERO);
        // A high canopy encloses the rider's silhouette; the front remains open for visibility.
        root.addOrReplaceChild("cab_roof", CubeListBuilder.create().texOffs(186, 32)
                .addBox(-9, -32, -7, 15, 2, 14), PartPose.ZERO);
        root.addOrReplaceChild("windshield_top", CubeListBuilder.create().texOffs(172, 52)
                .addBox(3.5F, -30, -5, 1, 1, 10), PartPose.ZERO);
        root.addOrReplaceChild("windshield_bottom", CubeListBuilder.create().texOffs(172, 52)
                .addBox(3.5F, -12, -5, 1, 1, 10), PartPose.ZERO);
        root.addOrReplaceChild("left_joystick", CubeListBuilder.create().texOffs(177, 67)
                .addBox(-1, -14, -3.5F, 0.7F, 3, 0.7F), PartPose.ZERO);
        root.addOrReplaceChild("right_joystick", CubeListBuilder.create().texOffs(177, 67)
                .addBox(-1, -14, 2.8F, 0.7F, 3, 0.7F), PartPose.ZERO);
        root.addOrReplaceChild("joystick_knobs", CubeListBuilder.create().texOffs(232, 67)
                .addBox(-1.5F, -14.5F, -3.8F, 1.7F, 1, 1.3F)
                .addBox(-1.5F, -14.5F, 2.5F, 1.7F, 1, 1.3F), PartPose.ZERO);
        root.addOrReplaceChild("exhaust", CubeListBuilder.create().texOffs(54, 67)
                .addBox(-8, -16, -4, 2, 7, 2), PartPose.ZERO);
        root.addOrReplaceChild("exhaust_cap", CubeListBuilder.create().texOffs(148, 67)
                .addBox(-8.8F, -16.5F, -4.8F, 3.6F, 1, 3.6F), PartPose.ZERO);
        root.addOrReplaceChild("steps", CubeListBuilder.create().texOffs(198, 67)
                .addBox(-3, -8, -8, 5, 1, 2), PartPose.ZERO);
        root.addOrReplaceChild("work_lights", CubeListBuilder.create().texOffs(212, 67)
                .addBox(-6, -24.5F, -4.5F, 3, 1, 2), PartPose.ZERO);
        // Front headlights on the chassis nose, below the loader arms.
        root.addOrReplaceChild("headlight_left", CubeListBuilder.create().texOffs(171, 67)
                .addBox(10, -6, -6.5F, 1, 2, 2), PartPose.ZERO);
        root.addOrReplaceChild("headlight_right", CubeListBuilder.create().texOffs(171, 67)
                .addBox(10, -6, 4.5F, 1, 2, 2), PartPose.ZERO);

        // Twin loader arms support the bucket and keep the front assembly connected.
        root.addOrReplaceChild("loader_arm_left", CubeListBuilder.create().texOffs(94, 67)
                .addBox(0, -1.5F, -1.5F, 24, 3, 3),
                PartPose.offsetAndRotation(4, -10, -7, 0, 0, 0.16F));
        root.addOrReplaceChild("loader_arm_right", CubeListBuilder.create().texOffs(94, 67)
                .addBox(0, -1.5F, -1.5F, 24, 3, 3),
                PartPose.offsetAndRotation(4, -10, 5, 0, 0, 0.16F));

        // The central linkage carries the scoop between the twin arms.
        PartDefinition boom = root.addOrReplaceChild("boom_base", CubeListBuilder.create().texOffs(194, 52)
                .addBox(0, -2.5F, -2.5F, 12, 5, 5), PartPose.offsetAndRotation(6, -7, 0, 0, 0, -0.12F));
        PartDefinition arm = boom.addOrReplaceChild("boom_arm", CubeListBuilder.create().texOffs(62, 67)
                .addBox(0, -2, -2, 12, 4, 4),
                PartPose.offsetAndRotation(10, 2, 0, 0, 0, 0.22F));
        PartDefinition bucketPart = arm.addOrReplaceChild("bucket", CubeListBuilder.create().texOffs(64, 0)
                // Back plate and floor form an open scoop facing the direction of travel.
                .addBox(0, -4, -12, 3, 7, 24)
                .texOffs(118, 0)
                .addBox(3, 3, -14, 8, 2, 28)
                // Raised side cheeks give the scoop its loader-bucket profile.
                .texOffs(34, 67)
                .addBox(3, -4, -14, 8, 7, 2)
                .addBox(3, -4, 12, 8, 7, 2)
                // Reinforced cutting edge across the entire bucket mouth.
                .texOffs(0, 0)
                .addBox(11, 3, -15, 2, 2, 30), PartPose.offsetAndRotation(10, 0, 0, 0, 0, 0.02F));
        CubeListBuilder teeth = CubeListBuilder.create().texOffs(222, 67);
        for (int z = -13; z <= 11; z += 4) {
            teeth.addBox(13, 3.4F, z, 3, 1, 2);
        }
        bucketPart.addOrReplaceChild("bucket_teeth", teeth, PartPose.ZERO);
        arm.addOrReplaceChild("hydraulic_cylinder", CubeListBuilder.create().texOffs(180, 67)
                .addBox(1, -4.3F, -0.9F, 7, 1.2F, 1.8F), PartPose.ZERO);
        arm.addOrReplaceChild("hydraulic_rod", CubeListBuilder.create().texOffs(238, 67)
                .addBox(7, -4.1F, -0.45F, 5, 0.6F, 0.9F), PartPose.ZERO);

        return LayerDefinition.create(mesh, 256, 128);
    }

    @Override
    public void setupAnim(BoatRenderState state) {
        super.setupAnim(state);
        // When lit, the renderer draws the lamps in a separate full-bright pass.
        boolean lit = state instanceof Headlights.LitBoatRenderState lamp && lamp.lightsOn;
        for (ModelPart part : lamps) part.visible = !lit;
        float cycle = state.ageInTicks * 0.12F;
        this.boomBase.zRot = -0.12F + (float) Math.sin(cycle) * 0.025F;
        this.boomArm.zRot = 0.22F + (float) Math.sin(cycle + 1.1F) * 0.035F;
        this.bucket.zRot = 0.02F + (float) Math.sin(cycle + 2.0F) * 0.06F;
    }
}
