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

/** Road roller / paver. +x is the front (big roller drum), y down, ground at y = 6. */
public final class PaverModel extends EntityModel<BoatRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(FactoryCraft.MOD_ID, "paver"), "main");
    public static final String[] LAMP_PARTS = {"headlight_left", "headlight_right", "tail_light_left", "tail_light_right"};
    public static final float[][] BEAM_ORIGINS = {{27.0F, -11.0F, -10.5F}, {27.0F, -11.0F, 10.5F}};
    /** Drum radius in blocks (7 px * 1.5 / 16). */
    private static final float DRUM_RADIUS = 0.656F;

    private final ModelPart drum;
    private final ModelPart[] lamps;

    public PaverModel(ModelPart root) {
        super(root);
        this.drum = root.getChild("front_roller");
        this.lamps = Headlights.parts(root, LAMP_PARTS);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // A 16-sided drum: round, not a square box turning over.
        PartDefinition drum = root.addOrReplaceChild("front_roller", CubeListBuilder.create(), PartPose.offset(20, -1, 0));
        RoundParts.prismZ(drum, "drum", 0, 0, 7.0F, 28.0F, 16);
        root.addOrReplaceChild("roller_yoke", CubeListBuilder.create().texOffs(108, 42)
                .addBox(13, -10, -15, 14, 2, 30), PartPose.ZERO);
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 42)
                .addBox(-18, -8, -11, 32, 10, 22), PartPose.ZERO);
        root.addOrReplaceChild("rear_wheels", CubeListBuilder.create().texOffs(84, 0)
                .addBox(-16, -2, -13, 10, 8, 26), PartPose.ZERO);
        root.addOrReplaceChild("seat", CubeListBuilder.create().texOffs(86, 74)
                .addBox(-10, -12, -3, 5, 4, 6), PartPose.ZERO);
        root.addOrReplaceChild("steering", CubeListBuilder.create().texOffs(108, 74)
                .addBox(-2, -16, -2, 1, 4, 4), PartPose.ZERO);
        root.addOrReplaceChild("post_rl", CubeListBuilder.create().texOffs(74, 74)
                .addBox(-15, -30, -10, 1, 22, 1), PartPose.ZERO);
        root.addOrReplaceChild("post_rr", CubeListBuilder.create().texOffs(74, 74)
                .addBox(-15, -30, 9, 1, 22, 1), PartPose.ZERO);
        root.addOrReplaceChild("post_fl", CubeListBuilder.create().texOffs(74, 74)
                .addBox(-3, -30, -10, 1, 22, 1), PartPose.ZERO);
        root.addOrReplaceChild("post_fr", CubeListBuilder.create().texOffs(74, 74)
                .addBox(-3, -30, 9, 1, 22, 1), PartPose.ZERO);
        root.addOrReplaceChild("roof", CubeListBuilder.create().texOffs(0, 74)
                .addBox(-16, -32, -11, 15, 2, 22), PartPose.ZERO);
        root.addOrReplaceChild("exhaust", CubeListBuilder.create().texOffs(78, 74)
                .addBox(-17, -20, 6, 2, 12, 2), PartPose.ZERO);
        root.addOrReplaceChild("headlight_left", CubeListBuilder.create().texOffs(118, 74)
                .addBox(26, -12, -12, 1, 2, 3, new CubeDeformation(0.06F)), PartPose.ZERO);
        root.addOrReplaceChild("headlight_right", CubeListBuilder.create().texOffs(118, 74)
                .addBox(26, -12, 9, 1, 2, 3, new CubeDeformation(0.06F)), PartPose.ZERO);
        root.addOrReplaceChild("tail_light_left", CubeListBuilder.create().texOffs(126, 74)
                .addBox(-19, -6, -10, 1, 2, 3, new CubeDeformation(0.06F)), PartPose.ZERO);
        root.addOrReplaceChild("tail_light_right", CubeListBuilder.create().texOffs(126, 74)
                .addBox(-19, -6, 7, 1, 2, 3, new CubeDeformation(0.06F)), PartPose.ZERO);
        return LayerDefinition.create(mesh, 256, 128);
    }

    @Override
    public void setupAnim(BoatRenderState state) {
        super.setupAnim(state);
        boolean lit = false;
        if (state instanceof WorkVehicleRenderer.WorkRenderState work) {
            drum.zRot = work.travelled / DRUM_RADIUS;
            lit = work.lightsOn;
        }
        for (ModelPart lamp : lamps) lamp.visible = !lit;
    }
}
