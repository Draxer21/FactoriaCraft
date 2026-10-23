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

/** Tower crane: lattice tower, cab and jib at the top, jib pointing to +x. y down, ground at y = 6. */
public final class TowerCraneModel extends EntityModel<BoatRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(FactoryCraft.MOD_ID, "tower_crane"), "main");
    public static final String[] LAMP_PARTS = {"headlight_left", "headlight_right", "tail_light_left", "tail_light_right"};
    public static final float[][] BEAM_ORIGINS = {{21.0F, -179.0F, -3.5F}, {21.0F, -179.0F, 3.5F}};

    private final ModelPart[] lamps;

    public TowerCraneModel(ModelPart root) {
        super(root);
        this.lamps = Headlights.parts(root, LAMP_PARTS);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("base", CubeListBuilder.create().texOffs(52, 0)
                .addBox(-16, -2, -16, 32, 8, 32), PartPose.ZERO);
        root.addOrReplaceChild("tower", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-6, -170, -6, 12, 168, 12), PartPose.ZERO);
        root.addOrReplaceChild("slewing", CubeListBuilder.create().texOffs(0, 180)
                .addBox(-8, -176, -8, 16, 6, 16), PartPose.ZERO);
        // Open cab at the top, in front of the tower.
        root.addOrReplaceChild("cab_floor", CubeListBuilder.create().texOffs(132, 180)
                .addBox(6, -172, -7, 14, 2, 14), PartPose.ZERO);
        root.addOrReplaceChild("seat", CubeListBuilder.create().texOffs(196, 214)
                .addBox(9, -174, -3, 5, 2, 6), PartPose.ZERO);
        root.addOrReplaceChild("console", CubeListBuilder.create().texOffs(148, 214)
                .addBox(17, -178, -3, 2, 6, 6), PartPose.ZERO);
        root.addOrReplaceChild("cab_post_rl", CubeListBuilder.create().texOffs(64, 180)
                .addBox(6, -190, -7, 1, 18, 1), PartPose.ZERO);
        root.addOrReplaceChild("cab_post_rr", CubeListBuilder.create().texOffs(64, 180)
                .addBox(6, -190, 6, 1, 18, 1), PartPose.ZERO);
        root.addOrReplaceChild("cab_post_fl", CubeListBuilder.create().texOffs(64, 180)
                .addBox(19, -190, -7, 1, 18, 1), PartPose.ZERO);
        root.addOrReplaceChild("cab_post_fr", CubeListBuilder.create().texOffs(64, 180)
                .addBox(19, -190, 6, 1, 18, 1), PartPose.ZERO);
        root.addOrReplaceChild("cab_roof", CubeListBuilder.create().texOffs(68, 180)
                .addBox(5, -192, -8, 16, 2, 16), PartPose.ZERO);
        // Jib, counter-jib, peak.
        root.addOrReplaceChild("jib_inner", CubeListBuilder.create().texOffs(0, 202)
                .addBox(8, -198, -3, 75, 6, 6), PartPose.ZERO);
        root.addOrReplaceChild("jib_outer", CubeListBuilder.create().texOffs(0, 202)
                .addBox(83, -198, -3, 75, 6, 6), PartPose.ZERO);
        root.addOrReplaceChild("counter_jib", CubeListBuilder.create().texOffs(0, 214)
                .addBox(-60, -198, -3, 68, 6, 6), PartPose.ZERO);
        root.addOrReplaceChild("counterweight", CubeListBuilder.create().texOffs(180, 0)
                .addBox(-62, -196, -7, 12, 12, 14), PartPose.ZERO);
        root.addOrReplaceChild("peak", CubeListBuilder.create().texOffs(232, 0)
                .addBox(-3, -218, -3, 6, 20, 6), PartPose.ZERO);
        // Trolley, cable and hook.
        root.addOrReplaceChild("trolley", CubeListBuilder.create().texOffs(164, 214)
                .addBox(116, -192, -4, 8, 3, 8), PartPose.ZERO);
        root.addOrReplaceChild("cable", CubeListBuilder.create().texOffs(48, 0)
                .addBox(119.5F, -189, -0.5F, 1, 122, 1), PartPose.ZERO);
        root.addOrReplaceChild("hook", CubeListBuilder.create().texOffs(218, 214)
                .addBox(118, -67, -2, 4, 4, 4), PartPose.ZERO);
        // Lights.
        root.addOrReplaceChild("headlight_left", CubeListBuilder.create().texOffs(234, 214)
                .addBox(20, -180, -5, 1, 2, 3, new CubeDeformation(0.06F)), PartPose.ZERO);
        root.addOrReplaceChild("headlight_right", CubeListBuilder.create().texOffs(234, 214)
                .addBox(20, -180, 2, 1, 2, 3, new CubeDeformation(0.06F)), PartPose.ZERO);
        root.addOrReplaceChild("tail_light_left", CubeListBuilder.create().texOffs(242, 214)
                .addBox(-63, -192, -5, 1, 2, 3, new CubeDeformation(0.06F)), PartPose.ZERO);
        root.addOrReplaceChild("tail_light_right", CubeListBuilder.create().texOffs(242, 214)
                .addBox(-63, -192, 2, 1, 2, 3, new CubeDeformation(0.06F)), PartPose.ZERO);
        return LayerDefinition.create(mesh, 256, 256);
    }

    @Override
    public void setupAnim(BoatRenderState state) {
        super.setupAnim(state);
        boolean lit = state instanceof WorkVehicleRenderer.WorkRenderState work && work.lightsOn;
        for (ModelPart lamp : lamps) lamp.visible = !lit;
    }
}
