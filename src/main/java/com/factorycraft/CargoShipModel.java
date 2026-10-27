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

/** Cargo ship: hull, deck with stacked containers, bridge and funnel at the stern, helm in front of it. Bow at +x, y down, keel at y = 6. */
public final class CargoShipModel extends EntityModel<BoatRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(FactoryCraft.MOD_ID, "cargo_ship"), "main");
    public static final String[] LAMP_PARTS = {"headlight_left", "headlight_right"};
    public static final float[][] BEAM_ORIGINS = {{50.0F, -9.5F, -6.5F}, {50.0F, -9.5F, 6.5F}};

    private final ModelPart propeller;
    private final ModelPart[] lamps;

    public CargoShipModel(ModelPart root) {
        super(root);
        this.propeller = root.getChild("propeller");
        this.lamps = Headlights.parts(root, LAMP_PARTS);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("hull_bottom", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-42F, -2F, -15F, 82F, 8F, 30F), PartPose.ZERO);
        root.addOrReplaceChild("hull", CubeListBuilder.create().texOffs(224, 0)
                .addBox(-44F, -12F, -17F, 86F, 10F, 34F), PartPose.ZERO);
        root.addOrReplaceChild("bow", CubeListBuilder.create().texOffs(0, 44)
                .addBox(42F, -12F, -11F, 8F, 14F, 22F), PartPose.ZERO);
        root.addOrReplaceChild("bow_tip", CubeListBuilder.create().texOffs(60, 44)
                .addBox(50F, -12F, -6F, 4F, 8F, 12F), PartPose.ZERO);
        root.addOrReplaceChild("stripe", CubeListBuilder.create().texOffs(92, 44)
                .addBox(-44F, -10F, -17.3F, 94F, 2F, 34.6F), PartPose.ZERO);
        root.addOrReplaceChild("deck", CubeListBuilder.create().texOffs(0, 81)
                .addBox(-44F, -13F, -16F, 94F, 1F, 32F), PartPose.ZERO);
        root.addOrReplaceChild("rail_left", CubeListBuilder.create().texOffs(252, 81)
                .addBox(-44F, -16F, -17F, 90F, 3F, 1F), PartPose.ZERO);
        root.addOrReplaceChild("rail_right", CubeListBuilder.create().texOffs(0, 114)
                .addBox(-44F, -16F, 16F, 90F, 3F, 1F), PartPose.ZERO);
        root.addOrReplaceChild("container_1", CubeListBuilder.create().texOffs(182, 114)
                .addBox(-20F, -23F, -15F, 16F, 10F, 14F), PartPose.ZERO);
        root.addOrReplaceChild("container_2", CubeListBuilder.create().texOffs(242, 114)
                .addBox(-20F, -23F, 1F, 16F, 10F, 14F), PartPose.ZERO);
        root.addOrReplaceChild("container_3", CubeListBuilder.create().texOffs(302, 114)
                .addBox(-2F, -23F, -15F, 16F, 10F, 14F), PartPose.ZERO);
        root.addOrReplaceChild("container_4", CubeListBuilder.create().texOffs(362, 114)
                .addBox(-2F, -23F, 1F, 16F, 10F, 14F), PartPose.ZERO);
        root.addOrReplaceChild("container_5", CubeListBuilder.create().texOffs(422, 114)
                .addBox(16F, -21F, -14F, 16F, 8F, 13F), PartPose.ZERO);
        root.addOrReplaceChild("container_6", CubeListBuilder.create().texOffs(0, 138)
                .addBox(16F, -21F, 1F, 16F, 8F, 13F), PartPose.ZERO);
        root.addOrReplaceChild("container_7", CubeListBuilder.create().texOffs(58, 138)
                .addBox(-20F, -31F, -8F, 16F, 8F, 16F), PartPose.ZERO);
        root.addOrReplaceChild("bridge", CubeListBuilder.create().texOffs(122, 138)
                .addBox(-44F, -34F, -14F, 10F, 21F, 28F), PartPose.ZERO);
        root.addOrReplaceChild("bridge_windows", CubeListBuilder.create().texOffs(198, 138)
                .addBox(-34F, -31F, -13F, 1F, 5F, 26F), PartPose.ZERO);
        root.addOrReplaceChild("bridge_roof", CubeListBuilder.create().texOffs(252, 138)
                .addBox(-45F, -35F, -15F, 12F, 1F, 30F), PartPose.ZERO);
        root.addOrReplaceChild("funnel", CubeListBuilder.create().texOffs(336, 138)
                .addBox(-42F, -46F, -4F, 7F, 11F, 8F), PartPose.ZERO);
        root.addOrReplaceChild("funnel_band", CubeListBuilder.create().texOffs(366, 138)
                .addBox(-42.2F, -43F, -4.2F, 7.4F, 2F, 8.4F), PartPose.ZERO);
        root.addOrReplaceChild("helm", CubeListBuilder.create().texOffs(400, 138)
                .addBox(-29F, -19F, -1F, 2F, 6F, 2F), PartPose.ZERO);
        root.addOrReplaceChild("wheel", CubeListBuilder.create().texOffs(408, 138)
                .addBox(-28F, -23F, -3F, 1F, 5F, 6F), PartPose.ZERO);
        root.addOrReplaceChild("mast", CubeListBuilder.create().texOffs(422, 138)
                .addBox(44F, -38F, -1F, 2F, 25F, 2F), PartPose.ZERO);
        root.addOrReplaceChild("crane_arm", CubeListBuilder.create().texOffs(430, 138)
                .addBox(34F, -36F, -1F, 10F, 2F, 2F), PartPose.ZERO);
        root.addOrReplaceChild("propeller", CubeListBuilder.create().texOffs(454, 138)
                .addBox(-0.5F, -4F, -1.5F, 1F, 8F, 3F), PartPose.offset(-46.5F, 2F, 0F));
        root.addOrReplaceChild("headlight_left", CubeListBuilder.create().texOffs(462, 138)
                .addBox(49F, -11F, -8F, 1F, 3F, 3F, new CubeDeformation(0.06F)), PartPose.ZERO);
        root.addOrReplaceChild("headlight_right", CubeListBuilder.create().texOffs(470, 138)
                .addBox(49F, -11F, 5F, 1F, 3F, 3F, new CubeDeformation(0.06F)), PartPose.ZERO);
        return LayerDefinition.create(mesh, 512, 512);
    }

    @Override
    public void setupAnim(BoatRenderState state) {
        super.setupAnim(state);
        float travelled = state instanceof Headlights.LitBoatRenderState moving ? moving.travelled : 0.0F;
        propeller.xRot = travelled * 3.0F + state.ageInTicks * 0.2F;
        boolean lit = state instanceof WorkVehicleRenderer.WorkRenderState work && work.lightsOn;
        for (ModelPart lamp : lamps) lamp.visible = !lit;
    }
}
