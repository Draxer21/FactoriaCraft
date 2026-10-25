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

/** Cargo helicopter: open cabin at the front (+x), tail boom, main and tail rotors, skids, winch. y down, ground at y = 6. */
public final class HelicopterModel extends EntityModel<BoatRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(FactoryCraft.MOD_ID, "helicopter"), "main");
    public static final String[] LAMP_PARTS = {"headlight_left", "headlight_right", "tail_light"};
    public static final float[][] BEAM_ORIGINS = {{23.0F, -4.0F, -4.5F}, {23.0F, -4.0F, 4.5F}};

    private final ModelPart rotor;
    private final ModelPart rotorCross;
    private final ModelPart tailRotor;
    private final ModelPart[] lamps;

    public HelicopterModel(ModelPart root) {
        super(root);
        this.rotor = root.getChild("rotor");
        this.rotorCross = root.getChild("rotor_cross");
        this.tailRotor = root.getChild("tail_rotor");
        this.lamps = Headlights.parts(root, LAMP_PARTS);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("skid_left", CubeListBuilder.create().texOffs(176, 121)
                .addBox(-16, 4, -14, 32, 2, 2), PartPose.ZERO);
        root.addOrReplaceChild("skid_right", CubeListBuilder.create().texOffs(176, 121)
                .addBox(-16, 4, 12, 32, 2, 2), PartPose.ZERO);
        root.addOrReplaceChild("strut_fl", CubeListBuilder.create().texOffs(128, 106)
                .addBox(8, -2, -13, 1, 6, 1), PartPose.ZERO);
        root.addOrReplaceChild("strut_fr", CubeListBuilder.create().texOffs(128, 106)
                .addBox(8, -2, 12, 1, 6, 1), PartPose.ZERO);
        root.addOrReplaceChild("strut_bl", CubeListBuilder.create().texOffs(128, 106)
                .addBox(-9, -2, -13, 1, 6, 1), PartPose.ZERO);
        root.addOrReplaceChild("strut_br", CubeListBuilder.create().texOffs(128, 106)
                .addBox(-9, -2, 12, 1, 6, 1), PartPose.ZERO);
        root.addOrReplaceChild("cabin_floor", CubeListBuilder.create().texOffs(46, 81)
                .addBox(-12, -2, -11, 30, 2, 22), PartPose.ZERO);
        root.addOrReplaceChild("cabin_back", CubeListBuilder.create().texOffs(168, 0)
                .addBox(-12, -20, -11, 3, 18, 22), PartPose.ZERO);
        root.addOrReplaceChild("cabin_roof", CubeListBuilder.create().texOffs(46, 81)
                .addBox(-12, -22, -11, 30, 2, 22), PartPose.ZERO);
        root.addOrReplaceChild("side_left", CubeListBuilder.create().texOffs(136, 106)
                .addBox(-9, -7, -11, 27, 5, 1), PartPose.ZERO);
        root.addOrReplaceChild("side_right", CubeListBuilder.create().texOffs(136, 106)
                .addBox(-9, -7, 10, 27, 5, 1), PartPose.ZERO);
        root.addOrReplaceChild("post_left", CubeListBuilder.create().texOffs(0, 106)
                .addBox(16, -20, -11, 2, 13, 2), PartPose.ZERO);
        root.addOrReplaceChild("post_right", CubeListBuilder.create().texOffs(0, 106)
                .addBox(16, -20, 9, 2, 13, 2), PartPose.ZERO);
        root.addOrReplaceChild("nose", CubeListBuilder.create().texOffs(0, 81)
                .addBox(18, -9, -9, 5, 7, 18), PartPose.ZERO);
        root.addOrReplaceChild("seat", CubeListBuilder.create().texOffs(14, 106)
                .addBox(-2, -6, -5, 7, 4, 10), PartPose.ZERO);
        root.addOrReplaceChild("controls", CubeListBuilder.create().texOffs(210, 81)
                .addBox(13, -12, -6, 3, 5, 12), PartPose.ZERO);
        root.addOrReplaceChild("engine", CubeListBuilder.create().texOffs(150, 81)
                .addBox(-10, -28, -6, 18, 6, 12), PartPose.ZERO);
        root.addOrReplaceChild("mast", CubeListBuilder.create().texOffs(208, 106)
                .addBox(1, -32, -1, 2, 4, 2), PartPose.ZERO);
        root.addOrReplaceChild("rotor", CubeListBuilder.create().texOffs(0, 121)
                .addBox(-40, -0.5F, -2, 80, 1, 4), PartPose.offset(2, -32.5F, 0));
        root.addOrReplaceChild("rotor_cross", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-2, -0.5F, -40, 4, 1, 80), PartPose.offset(2, -32.5F, 0));
        root.addOrReplaceChild("tail_boom", CubeListBuilder.create().texOffs(48, 106)
                .addBox(-46, -18, -3, 34, 6, 6), PartPose.ZERO);
        root.addOrReplaceChild("tail_fin", CubeListBuilder.create().texOffs(240, 81)
                .addBox(-48, -30, -1, 6, 14, 2), PartPose.ZERO);
        root.addOrReplaceChild("tail_rotor", CubeListBuilder.create().texOffs(8, 106)
                .addBox(-1, -7, -0.5F, 2, 14, 1), PartPose.offset(-45, -25, 2));
        root.addOrReplaceChild("winch_cable", CubeListBuilder.create().texOffs(132, 106)
                .addBox(-0.5F, 0, -0.5F, 1, 6, 1), PartPose.ZERO);
        root.addOrReplaceChild("winch_hook", CubeListBuilder.create().texOffs(192, 106)
                .addBox(-2, 5, -2, 4, 2, 4), PartPose.ZERO);
        root.addOrReplaceChild("headlight_left", CubeListBuilder.create().texOffs(168, 121)
                .addBox(23, -5, -6, 1, 2, 3, new CubeDeformation(0.06F)), PartPose.ZERO);
        root.addOrReplaceChild("headlight_right", CubeListBuilder.create().texOffs(168, 121)
                .addBox(23, -5, 3, 1, 2, 3, new CubeDeformation(0.06F)), PartPose.ZERO);
        root.addOrReplaceChild("tail_light", CubeListBuilder.create().texOffs(244, 121)
                .addBox(-49, -26, -1, 1, 2, 2, new CubeDeformation(0.06F)), PartPose.ZERO);
        return LayerDefinition.create(mesh, 256, 128);
    }

    @Override
    public void setupAnim(BoatRenderState state) {
        super.setupAnim(state);
        float spin = state.ageInTicks * 1.4F;
        rotor.yRot = spin;
        rotorCross.yRot = spin;
        tailRotor.zRot = spin * 1.6F;
        boolean lit = state instanceof WorkVehicleRenderer.WorkRenderState work && work.lightsOn;
        for (ModelPart lamp : lamps) lamp.visible = !lit;
    }
}
