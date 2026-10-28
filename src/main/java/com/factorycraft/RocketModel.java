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

/** Two-stage rocket with fins and a gold payload fairing. y down, base at y = 24. */
public final class RocketModel extends EntityModel<EntityRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(FactoryCraft.MOD_ID, "rocket"), "main");

    public RocketModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("engine", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-6F, 16F, -6F, 12F, 8F, 12F), PartPose.ZERO);
        root.addOrReplaceChild("nozzle", CubeListBuilder.create().texOffs(48, 0)
                .addBox(-4F, 20F, -4F, 8F, 4F, 8F), PartPose.ZERO);
        root.addOrReplaceChild("stage1", CubeListBuilder.create().texOffs(80, 0)
                .addBox(-8F, -24F, -8F, 16F, 40F, 16F), PartPose.ZERO);
        root.addOrReplaceChild("band1", CubeListBuilder.create().texOffs(144, 0)
                .addBox(-8.3F, -6F, -8.3F, 16.6F, 3F, 16.6F), PartPose.ZERO);
        root.addOrReplaceChild("band2", CubeListBuilder.create().texOffs(0, 56)
                .addBox(-8.3F, 10F, -8.3F, 16.6F, 2F, 16.6F), PartPose.ZERO);
        root.addOrReplaceChild("stage2", CubeListBuilder.create().texOffs(68, 56)
                .addBox(-7F, -56F, -7F, 14F, 32F, 14F), PartPose.ZERO);
        root.addOrReplaceChild("window", CubeListBuilder.create().texOffs(124, 56)
                .addBox(-2F, -50F, -7.3F, 4F, 4F, 1F), PartPose.ZERO);
        root.addOrReplaceChild("band3", CubeListBuilder.create().texOffs(134, 56)
                .addBox(-7.3F, -30F, -7.3F, 14.6F, 3F, 14.6F), PartPose.ZERO);
        root.addOrReplaceChild("fairing", CubeListBuilder.create().texOffs(194, 56)
                .addBox(-6F, -70F, -6F, 12F, 14F, 12F), PartPose.ZERO);
        root.addOrReplaceChild("nose", CubeListBuilder.create().texOffs(0, 102)
                .addBox(-4F, -80F, -4F, 8F, 10F, 8F), PartPose.ZERO);
        root.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(32, 102)
                .addBox(-1.5F, -86F, -1.5F, 3F, 6F, 3F), PartPose.ZERO);
        root.addOrReplaceChild("fin_n", CubeListBuilder.create().texOffs(44, 102)
                .addBox(-1F, 4F, -14F, 2F, 20F, 6F), PartPose.ZERO);
        root.addOrReplaceChild("fin_s", CubeListBuilder.create().texOffs(60, 102)
                .addBox(-1F, 4F, 8F, 2F, 20F, 6F), PartPose.ZERO);
        root.addOrReplaceChild("fin_e", CubeListBuilder.create().texOffs(76, 102)
                .addBox(8F, 4F, -1F, 6F, 20F, 2F), PartPose.ZERO);
        root.addOrReplaceChild("fin_w", CubeListBuilder.create().texOffs(92, 102)
                .addBox(-14F, 4F, -1F, 6F, 20F, 2F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 256, 256);
    }
}
