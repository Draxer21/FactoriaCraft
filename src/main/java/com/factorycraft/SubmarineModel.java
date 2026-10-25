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

/** Mining submarine: long hull, conning tower with the pilot, drill at the nose (+x), propeller at the back. y down, ground at y = 6. */
public final class SubmarineModel extends EntityModel<BoatRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(FactoryCraft.MOD_ID, "submarine"), "main");
    public static final String[] LAMP_PARTS = {"headlight_left", "headlight_right"};
    public static final float[][] BEAM_ORIGINS = {{31.0F, -8.0F, -5.0F}, {31.0F, -8.0F, 5.0F}};

    private final ModelPart drill;
    private final ModelPart propeller;
    private final ModelPart[] lamps;

    public SubmarineModel(ModelPart root) {
        super(root);
        this.drill = root.getChild("drill");
        this.propeller = root.getChild("propeller");
        this.lamps = Headlights.parts(root, LAMP_PARTS);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("hull", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-30, -16, -10, 56, 18, 20), PartPose.ZERO);
        root.addOrReplaceChild("hull_top", CubeListBuilder.create().texOffs(48, 62)
                .addBox(-26, -18, -7, 48, 2, 14), PartPose.ZERO);
        root.addOrReplaceChild("keel", CubeListBuilder.create().texOffs(0, 82)
                .addBox(-24, 2, -4, 44, 4, 8), PartPose.ZERO);
        root.addOrReplaceChild("nose", CubeListBuilder.create().texOffs(152, 0)
                .addBox(26, -13, -7, 5, 13, 14), PartPose.ZERO);
        root.addOrReplaceChild("stripe", CubeListBuilder.create().texOffs(0, 38)
                .addBox(-28, -9, -10.5F, 52, 3, 21), PartPose.ZERO);
        root.addOrReplaceChild("tower", CubeListBuilder.create().texOffs(162, 38)
                .addBox(-8, -28, -6, 16, 10, 12), PartPose.ZERO);
        root.addOrReplaceChild("tower_rim", CubeListBuilder.create().texOffs(172, 62)
                .addBox(-9, -29, -7, 18, 1, 14), PartPose.ZERO);
        root.addOrReplaceChild("periscope", CubeListBuilder.create().texOffs(124, 82)
                .addBox(-6, -36, -1, 2, 8, 2), PartPose.ZERO);
        root.addOrReplaceChild("fin_left", CubeListBuilder.create().texOffs(132, 82)
                .addBox(-30, -8, -16, 8, 2, 6), PartPose.ZERO);
        root.addOrReplaceChild("fin_right", CubeListBuilder.create().texOffs(132, 82)
                .addBox(-30, -8, 10, 8, 2, 6), PartPose.ZERO);
        root.addOrReplaceChild("rudder", CubeListBuilder.create().texOffs(146, 38)
                .addBox(-34, -20, -1, 6, 22, 2), PartPose.ZERO);
        root.addOrReplaceChild("shaft", CubeListBuilder.create().texOffs(168, 82)
                .addBox(-36, -8, -1, 6, 2, 2), PartPose.ZERO);
        root.addOrReplaceChild("propeller", CubeListBuilder.create().texOffs(40, 62)
                .addBox(-0.5F, -7, -1.5F, 1, 14, 3), PartPose.offset(-36, -7, 0));
        root.addOrReplaceChild("drill", CubeListBuilder.create().texOffs(0, 62)
                .addBox(0, -5, -5, 10, 10, 10), PartPose.offset(31, -7, 0));
        root.addOrReplaceChild("drill_tip", CubeListBuilder.create().texOffs(104, 82)
                .addBox(41, -10, -3, 4, 6, 6), PartPose.ZERO);
        root.addOrReplaceChild("headlight_left", CubeListBuilder.create().texOffs(160, 82)
                .addBox(30, -10, -8, 1, 3, 3, new CubeDeformation(0.06F)), PartPose.ZERO);
        root.addOrReplaceChild("headlight_right", CubeListBuilder.create().texOffs(160, 82)
                .addBox(30, -10, 5, 1, 3, 3, new CubeDeformation(0.06F)), PartPose.ZERO);
        return LayerDefinition.create(mesh, 256, 128);
    }

    @Override
    public void setupAnim(BoatRenderState state) {
        super.setupAnim(state);
        float travelled = state instanceof Headlights.LitBoatRenderState moving ? moving.travelled : 0.0F;
        propeller.xRot = travelled * 3.0F + state.ageInTicks * 0.3F;
        drill.xRot = travelled * 4.0F;
        boolean lit = state instanceof WorkVehicleRenderer.WorkRenderState work && work.lightsOn;
        for (ModelPart lamp : lamps) lamp.visible = !lit;
    }
}
