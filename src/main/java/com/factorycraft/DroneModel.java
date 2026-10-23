package com.factorycraft;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;

/** Quadcopter: body, cross arms and four spinning rotors. Mob-style space (y down, feet at y = 24). */
public final class DroneModel extends EntityModel<DroneRenderer.DroneRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(FactoryCraft.MOD_ID, "drone"), "main");
    private static final String[] ROTORS = {"rotor_0", "rotor_1", "rotor_2", "rotor_3"};

    private final ModelPart[] rotors;

    public DroneModel(ModelPart root) {
        super(root);
        this.rotors = Headlights.parts(root, ROTORS);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(36, 0)
                .addBox(-4, 18, -4, 8, 3, 8), PartPose.ZERO);
        root.addOrReplaceChild("arm_x", CubeListBuilder.create().texOffs(0, 17)
                .addBox(-8, 19, -1, 16, 1, 2), PartPose.ZERO);
        root.addOrReplaceChild("arm_z", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-1, 19, -8, 2, 1, 16), PartPose.ZERO);
        root.addOrReplaceChild("camera", CubeListBuilder.create().texOffs(82, 0)
                .addBox(-1.5F, 21, -4, 3, 2, 2), PartPose.ZERO);
        root.addOrReplaceChild("leg_left", CubeListBuilder.create().texOffs(68, 0)
                .addBox(-4, 21, -3, 1, 3, 6), PartPose.ZERO);
        root.addOrReplaceChild("leg_right", CubeListBuilder.create().texOffs(68, 0)
                .addBox(3, 21, -3, 1, 3, 6), PartPose.ZERO);
        int[][] corners = {{-7, -7}, {7, -7}, {-7, 7}, {7, 7}};
        for (int i = 0; i < corners.length; i++) {
            root.addOrReplaceChild("motor_" + i, CubeListBuilder.create().texOffs(92, 0)
                    .addBox(-1, 17, -1, 2, 2, 2), PartPose.offset(corners[i][0], 0, corners[i][1]));
            root.addOrReplaceChild("rotor_" + i, CubeListBuilder.create().texOffs(36, 17)
                    .addBox(-4, -0.5F, -0.5F, 8, 1, 1), PartPose.offset(corners[i][0], 16.5F, corners[i][1]));
        }
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(DroneRenderer.DroneRenderState state) {
        super.setupAnim(state);
        float spin = state.ageInTicks * 1.6F;
        for (int i = 0; i < rotors.length; i++) rotors[i].yRot = (i % 2 == 0 ? spin : -spin);
    }
}
