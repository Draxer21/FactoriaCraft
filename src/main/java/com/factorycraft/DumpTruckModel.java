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

/** Model space: +x is the front of the truck, y points down, ground at y = 6. */
public final class DumpTruckModel extends EntityModel<BoatRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(FactoryCraft.MOD_ID, "dump_truck"), "main");

    /** Lamp parts drawn full-bright by the renderer when the headlights are on. */
    public static final String[] LAMP_PARTS = {"headlight_left", "headlight_right", "tail_light_left", "tail_light_right"};
    /** Beam origins (model px): front face centre of each headlight. */
    public static final float[][] BEAM_ORIGINS = {{32.0F, -9.0F, -8.5F}, {32.0F, -9.0F, 8.5F}};

    private static final String[] ROLLERS = {"roller_l0", "roller_l1", "roller_l2", "roller_l3",
            "roller_r0", "roller_r1", "roller_r2", "roller_r3"};

    private final ModelPart bed;
    private final ModelPart tailgate;
    private final ModelPart load;
    private final ModelPart[] wheels;
    private final ModelPart[] lamps;

    public DumpTruckModel(ModelPart root) {
        super(root);
        this.bed = root.getChild("bed");
        this.tailgate = bed.getChild("tailgate");
        this.load = bed.getChild("load");
        this.wheels = Headlights.parts(root, ROLLERS);
        this.lamps = Headlights.parts(root, LAMP_PARTS);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Frame and crawler tracks, with road wheels on the outside that spin when driving.
        root.addOrReplaceChild("chassis", CubeListBuilder.create().texOffs(52, 101)
                .addBox(-28, -5, -11, 56, 3, 22), PartPose.ZERO);
        root.addOrReplaceChild("left_track", CubeListBuilder.create().texOffs(0, 127)
                .addBox(-27, -3, -16, 54, 9, 5), PartPose.ZERO);
        root.addOrReplaceChild("right_track", CubeListBuilder.create().texOffs(0, 127)
                .addBox(-27, -3, 11, 54, 9, 5), PartPose.ZERO);
        root.addOrReplaceChild("roller_l0", CubeListBuilder.create().texOffs(210, 127)
                .addBox(-2.5F, -2.5F, -0.7F, 5, 5, 0.7F), PartPose.offset(-19, 1.5F, -16));
        root.addOrReplaceChild("roller_r0", CubeListBuilder.create().texOffs(210, 127)
                .addBox(-2.5F, -2.5F, 0, 5, 5, 0.7F), PartPose.offset(-19, 1.5F, 16));
        root.addOrReplaceChild("roller_l1", CubeListBuilder.create().texOffs(210, 127)
                .addBox(-2.5F, -2.5F, -0.7F, 5, 5, 0.7F), PartPose.offset(-7, 1.5F, -16));
        root.addOrReplaceChild("roller_r1", CubeListBuilder.create().texOffs(210, 127)
                .addBox(-2.5F, -2.5F, 0, 5, 5, 0.7F), PartPose.offset(-7, 1.5F, 16));
        root.addOrReplaceChild("roller_l2", CubeListBuilder.create().texOffs(210, 127)
                .addBox(-2.5F, -2.5F, -0.7F, 5, 5, 0.7F), PartPose.offset(5, 1.5F, -16));
        root.addOrReplaceChild("roller_r2", CubeListBuilder.create().texOffs(210, 127)
                .addBox(-2.5F, -2.5F, 0, 5, 5, 0.7F), PartPose.offset(5, 1.5F, 16));
        root.addOrReplaceChild("roller_l3", CubeListBuilder.create().texOffs(210, 127)
                .addBox(-2.5F, -2.5F, -0.7F, 5, 5, 0.7F), PartPose.offset(17, 1.5F, -16));
        root.addOrReplaceChild("roller_r3", CubeListBuilder.create().texOffs(210, 127)
                .addBox(-2.5F, -2.5F, 0, 5, 5, 0.7F), PartPose.offset(17, 1.5F, 16));

        // Cab: solid lower body (the seat sits on it), open sides, tall roof.
        root.addOrReplaceChild("cab_lower", CubeListBuilder.create().texOffs(120, 40)
                .addBox(12, -14, -12, 16, 9, 24), PartPose.ZERO);
        root.addOrReplaceChild("cab_back", CubeListBuilder.create().texOffs(0, 0)
                .addBox(12, -30, -12, 2, 16, 24), PartPose.ZERO);
        root.addOrReplaceChild("cab_roof", CubeListBuilder.create().texOffs(128, 73)
                .addBox(12, -32, -12, 16, 2, 24), PartPose.ZERO);
        root.addOrReplaceChild("cab_post_left", CubeListBuilder.create().texOffs(216, 101)
                .addBox(26, -30, -12, 2, 16, 2), PartPose.ZERO);
        root.addOrReplaceChild("cab_post_right", CubeListBuilder.create().texOffs(216, 101)
                .addBox(26, -30, 10, 2, 16, 2), PartPose.ZERO);
        root.addOrReplaceChild("steering", CubeListBuilder.create().texOffs(196, 127)
                .addBox(23, -18, -3, 1, 4, 6), PartPose.ZERO);
        root.addOrReplaceChild("hood", CubeListBuilder.create().texOffs(200, 40)
                .addBox(28, -13, -11, 3, 8, 22), PartPose.ZERO);
        root.addOrReplaceChild("bumper", CubeListBuilder.create().texOffs(0, 101)
                .addBox(31, -6, -12, 2, 2, 24), PartPose.ZERO);
        root.addOrReplaceChild("headlight_left", CubeListBuilder.create().texOffs(222, 127)
                .addBox(31, -10, -10, 1, 2, 3), PartPose.ZERO);
        root.addOrReplaceChild("headlight_right", CubeListBuilder.create().texOffs(222, 127)
                .addBox(31, -10, 7, 1, 2, 3), PartPose.ZERO);
        root.addOrReplaceChild("exhaust", CubeListBuilder.create().texOffs(208, 101)
                .addBox(10, -31, 10, 2, 18, 2), PartPose.ZERO);
        root.addOrReplaceChild("tail_light_left", CubeListBuilder.create().texOffs(230, 127)
                .addBox(-29, -5, -11, 1, 2, 3), PartPose.ZERO);
        root.addOrReplaceChild("tail_light_right", CubeListBuilder.create().texOffs(230, 127)
                .addBox(-29, -5, 8, 1, 2, 3), PartPose.ZERO);

        // Tipping bed, hinged at the rear bottom of the frame.
        PartDefinition bed = root.addOrReplaceChild("bed", CubeListBuilder.create().texOffs(0, 73)
                .addBox(0, -2, -13, 38, 2, 26), PartPose.offset(-28, -5, 0));
        bed.addOrReplaceChild("bed_left", CubeListBuilder.create().texOffs(118, 127)
                .addBox(0, -12, -13, 38, 10, 1), PartPose.ZERO);
        bed.addOrReplaceChild("bed_right", CubeListBuilder.create().texOffs(118, 127)
                .addBox(0, -12, 12, 38, 10, 1), PartPose.ZERO);
        bed.addOrReplaceChild("bed_front", CubeListBuilder.create().texOffs(52, 0)
                .addBox(37, -15, -13, 1, 13, 26), PartPose.ZERO);
        // Tailgate hangs from a hinge on top and swings out while dumping.
        bed.addOrReplaceChild("tailgate", CubeListBuilder.create().texOffs(106, 0)
                .addBox(-1, 0, -12, 1, 10, 24), PartPose.offset(0, -12, 0));
        // Cargo heap, scaled up from the bed floor by how full the truck is.
        bed.addOrReplaceChild("load", CubeListBuilder.create().texOffs(0, 40)
                .addBox(0, -9, -12, 36, 9, 24), PartPose.offset(1, -2, 0));

        return LayerDefinition.create(mesh, 256, 256);
    }

    @Override
    public void setupAnim(BoatRenderState state) {
        super.setupAnim(state);
        float dump = 0.0F;
        float fill = 0.0F;
        float wheelRot = 0.0F;
        boolean lit = false;
        if (state instanceof DumpTruckRenderer.TruckRenderState truck) {
            dump = truck.dump;
            fill = truck.fill;
            wheelRot = truck.wheelRot;
            lit = truck.lightsOn;
        }
        bed.zRot = -dump * 0.75F;
        tailgate.zRot = dump * 1.1F;
        load.visible = fill > 0.0F;
        load.yScale = Math.max(fill, 0.05F);
        for (ModelPart wheel : wheels) wheel.zRot = wheelRot;
        for (ModelPart lamp : lamps) lamp.visible = !lit;
    }
}
