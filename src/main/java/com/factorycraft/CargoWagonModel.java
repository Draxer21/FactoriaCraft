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

/** Model space: +x is the front (tow bar), y points down, ground at y = 6. */
public final class CargoWagonModel extends EntityModel<BoatRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(FactoryCraft.MOD_ID, "cargo_wagon"), "main");

    private final ModelPart load;

    public CargoWagonModel(ModelPart root) {
        super(root);
        this.load = root.getChild("load");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("left_track", CubeListBuilder.create().texOffs(0, 69)
                .addBox(-15, -2, -11, 30, 8, 3), PartPose.ZERO);
        root.addOrReplaceChild("right_track", CubeListBuilder.create().texOffs(0, 69)
                .addBox(-15, -2, 8, 30, 8, 3), PartPose.ZERO);
        root.addOrReplaceChild("chassis", CubeListBuilder.create().texOffs(0, 50)
                .addBox(-15, -4, -8, 30, 3, 16), PartPose.ZERO);
        root.addOrReplaceChild("bin_floor", CubeListBuilder.create().texOffs(0, 28)
                .addBox(-14, -6, -10, 28, 2, 20), PartPose.ZERO);
        root.addOrReplaceChild("bin_left", CubeListBuilder.create().texOffs(66, 69)
                .addBox(-14, -16, -10, 28, 10, 1), PartPose.ZERO);
        root.addOrReplaceChild("bin_right", CubeListBuilder.create().texOffs(66, 69)
                .addBox(-14, -16, 9, 28, 10, 1), PartPose.ZERO);
        root.addOrReplaceChild("bin_front", CubeListBuilder.create().texOffs(0, 0)
                .addBox(13, -16, -9, 1, 10, 18), PartPose.ZERO);
        root.addOrReplaceChild("bin_back", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-14, -16, -9, 1, 10, 18), PartPose.ZERO);
        root.addOrReplaceChild("tow_bar", CubeListBuilder.create().texOffs(10, 80)
                .addBox(14, -4, -1, 6, 2, 2), PartPose.ZERO);
        root.addOrReplaceChild("rear_hitch", CubeListBuilder.create().texOffs(0, 80)
                .addBox(-17, -4, -1.5F, 2, 2, 3), PartPose.ZERO);
        // Cargo heap, scaled up from the bin floor by how full the wagon is.
        root.addOrReplaceChild("load", CubeListBuilder.create().texOffs(38, 0)
                .addBox(0, -9, -8.5F, 26, 9, 17), PartPose.offset(-13, -6, 0));

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(BoatRenderState state) {
        super.setupAnim(state);
        float fill = state instanceof CargoWagonRenderer.WagonRenderState wagon ? wagon.fill : 0.0F;
        load.visible = fill > 0.0F;
        load.yScale = Math.max(fill, 0.05F);
    }
}
