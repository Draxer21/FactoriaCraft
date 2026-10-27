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
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.Identifier;

/** Cable car cabin with open windows, hanging from its grip. y down, floor at y = 24. */
public final class CableCarModel extends EntityModel<EntityRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(FactoryCraft.MOD_ID, "cable_car"), "main");

    public CableCarModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("floor", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-14F, 22F, -15F, 28F, 2F, 30F), PartPose.ZERO);
        root.addOrReplaceChild("wall_front", CubeListBuilder.create().texOffs(116, 0)
                .addBox(-15F, 13F, -16F, 30F, 9F, 1F), PartPose.ZERO);
        root.addOrReplaceChild("wall_back", CubeListBuilder.create().texOffs(178, 0)
                .addBox(-15F, 13F, 15F, 30F, 9F, 1F), PartPose.ZERO);
        root.addOrReplaceChild("wall_left", CubeListBuilder.create().texOffs(0, 32)
                .addBox(-15F, 13F, -15F, 1F, 9F, 30F), PartPose.ZERO);
        root.addOrReplaceChild("wall_right", CubeListBuilder.create().texOffs(62, 32)
                .addBox(14F, 13F, -15F, 1F, 9F, 30F), PartPose.ZERO);
        root.addOrReplaceChild("band", CubeListBuilder.create().texOffs(124, 32)
                .addBox(-15.2F, 12F, -16.2F, 30.4F, 1F, 32.4F), PartPose.ZERO);
        root.addOrReplaceChild("pillar_1", CubeListBuilder.create().texOffs(0, 71)
                .addBox(-15F, 1F, -16F, 2F, 11F, 2F), PartPose.ZERO);
        root.addOrReplaceChild("pillar_2", CubeListBuilder.create().texOffs(8, 71)
                .addBox(13F, 1F, -16F, 2F, 11F, 2F), PartPose.ZERO);
        root.addOrReplaceChild("pillar_3", CubeListBuilder.create().texOffs(16, 71)
                .addBox(-15F, 1F, 14F, 2F, 11F, 2F), PartPose.ZERO);
        root.addOrReplaceChild("pillar_4", CubeListBuilder.create().texOffs(24, 71)
                .addBox(13F, 1F, 14F, 2F, 11F, 2F), PartPose.ZERO);
        root.addOrReplaceChild("roof", CubeListBuilder.create().texOffs(32, 71)
                .addBox(-16F, -1F, -17F, 32F, 2F, 34F), PartPose.ZERO);
        root.addOrReplaceChild("roof_top", CubeListBuilder.create().texOffs(0, 107)
                .addBox(-12F, -3F, -12F, 24F, 2F, 24F), PartPose.ZERO);
        root.addOrReplaceChild("arm", CubeListBuilder.create().texOffs(96, 107)
                .addBox(-1F, -21F, -1F, 2F, 18F, 2F), PartPose.ZERO);
        root.addOrReplaceChild("grip", CubeListBuilder.create().texOffs(104, 107)
                .addBox(-3F, -26F, -4F, 6F, 5F, 8F), PartPose.ZERO);
        root.addOrReplaceChild("bench_front", CubeListBuilder.create().texOffs(132, 107)
                .addBox(-13F, 17F, -14F, 26F, 3F, 5F), PartPose.ZERO);
        root.addOrReplaceChild("bench_back", CubeListBuilder.create().texOffs(194, 107)
                .addBox(-13F, 17F, 9F, 26F, 3F, 5F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 256, 256);
    }
}
