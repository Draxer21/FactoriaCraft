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

public final class TunnelerModel extends EntityModel<BoatRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(FactoryCraft.MOD_ID, "tunneler"), "main");

    private final ModelPart drillHead;
    /** Cab frame parts hidden from the pilot's own first-person view. */
    private final ModelPart[] cabFrame;
    private final ModelPart[] lamps;

    /** Lamp parts drawn full-bright by the renderer when the headlights are on. */
    public static final String[] LAMP_PARTS = {"headlight_left", "headlight_right", "lights"};
    /** Beam origins (model px): front face centre of each headlight. */
    public static final float[][] BEAM_ORIGINS = {{5.0F, -10.0F, -8.5F}, {5.0F, -10.0F, 8.5F}};

    public TunnelerModel(ModelPart root) {
        super(root);
        this.drillHead = root.getChild("drill_head");
        this.cabFrame = new ModelPart[] {
                root.getChild("cab_back"), root.getChild("cab_roof_rear"),
                root.getChild("cab_post_left"), root.getChild("cab_post_right"),
                root.getChild("cab_front_left"), root.getChild("cab_front_right"),
                root.getChild("lights")
        };
        this.lamps = Headlights.parts(root, LAMP_PARTS);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Heavy twin tracks and rollers for a compact underground machine.
        root.addOrReplaceChild("left_track", CubeListBuilder.create().texOffs(24, 51)
                .addBox(-15, -1, -12, 30, 6, 8), PartPose.ZERO);
        root.addOrReplaceChild("right_track", CubeListBuilder.create().texOffs(24, 51)
                .addBox(-15, -1, 4, 30, 6, 8), PartPose.ZERO);
        for (int x = -12; x <= 12; x += 4) {
            root.addOrReplaceChild("left_roller_" + x, CubeListBuilder.create().texOffs(52, 67)
                    .addBox(x - 1.5F, -1, -12.4F, 3, 3, 0.8F), PartPose.ZERO);
            root.addOrReplaceChild("right_roller_" + x, CubeListBuilder.create().texOffs(52, 67)
                    .addBox(x - 1.5F, -1, 11.6F, 3, 3, 0.8F), PartPose.ZERO);
        }

        root.addOrReplaceChild("lower_hull", CubeListBuilder.create().texOffs(0, 28)
                .addBox(-12, -7, -9, 24, 5, 18), PartPose.ZERO);
        root.addOrReplaceChild("engine_housing", CubeListBuilder.create().texOffs(86, 0)
                .addBox(-13, -15, -8, 15, 8, 16), PartPose.ZERO);
        root.addOrReplaceChild("rear_deck", CubeListBuilder.create().texOffs(84, 28)
                .addBox(-15, -10, -10, 7, 3, 20), PartPose.ZERO);

        // Open operator station sized for a seated player: low seat back so the
        // third-person camera sees over it, and a roof above head height.
        root.addOrReplaceChild("cab_floor", CubeListBuilder.create().texOffs(100, 51)
                .addBox(-9, -13, -6, 11, 2, 12), PartPose.ZERO);
        root.addOrReplaceChild("seat", CubeListBuilder.create().texOffs(186, 51)
                .addBox(-7, -16, -2.5F, 4, 4, 5), PartPose.ZERO);
        root.addOrReplaceChild("cab_back", CubeListBuilder.create().texOffs(166, 28)
                .addBox(-10, -20, -6, 2, 7, 12), PartPose.ZERO);
        root.addOrReplaceChild("cab_post_left", CubeListBuilder.create().texOffs(194, 28)
                .addBox(-9, -30, -6, 1, 18, 1), PartPose.ZERO);
        root.addOrReplaceChild("cab_post_right", CubeListBuilder.create().texOffs(194, 28)
                .addBox(-9, -30, 5, 1, 18, 1), PartPose.ZERO);
        root.addOrReplaceChild("cab_front_left", CubeListBuilder.create().texOffs(194, 28)
                .addBox(1, -30, -6, 1, 18, 1), PartPose.ZERO);
        root.addOrReplaceChild("cab_front_right", CubeListBuilder.create().texOffs(194, 28)
                .addBox(1, -30, 5, 1, 18, 1), PartPose.ZERO);
        root.addOrReplaceChild("cab_roof_rear", CubeListBuilder.create().texOffs(198, 28)
                .addBox(-10, -32, -7, 13, 2, 14), PartPose.ZERO);
        root.addOrReplaceChild("console", CubeListBuilder.create().texOffs(146, 51)
                .addBox(-1, -15, -4, 2, 3, 8), PartPose.ZERO);
        root.addOrReplaceChild("lights", CubeListBuilder.create().texOffs(42, 67)
                .addBox(-12, -17, -7, 2, 2, 3)
                .addBox(-12, -17, 4, 2, 2, 3), PartPose.ZERO);

        // Drill housing and stepped conical boring head, with rotating auger flights.
        PartDefinition drill = root.addOrReplaceChild("drill_head", CubeListBuilder.create().texOffs(148, 0)
                .addBox(0, -6, -6, 14, 12, 12)
                .texOffs(50, 0).addBox(12, -7, -7, 4, 14, 14)
                .texOffs(200, 0).addBox(16, -6, -6, 4, 12, 12)
                .texOffs(138, 28).addBox(20, -5, -5, 4, 10, 10)
                .texOffs(0, 51).addBox(24, -4, -4, 4, 8, 8)
                .texOffs(166, 51).addBox(28, -2.5F, -2.5F, 5, 5, 5),
                PartPose.offset(1, -8, 0));
        drill.addOrReplaceChild("auger_upper_flight", CubeListBuilder.create().texOffs(204, 51)
                .addBox(2, -8, -1.5F, 19, 2, 3), PartPose.ZERO);
        drill.addOrReplaceChild("auger_lower_flight", CubeListBuilder.create().texOffs(204, 51)
                .addBox(2, 6, -1.5F, 19, 2, 3), PartPose.ZERO);
        drill.addOrReplaceChild("auger_left_flight", CubeListBuilder.create().texOffs(0, 67)
                .addBox(2, -1.5F, -8, 19, 3, 2), PartPose.ZERO);
        drill.addOrReplaceChild("auger_right_flight", CubeListBuilder.create().texOffs(0, 67)
                .addBox(2, -1.5F, 6, 19, 3, 2), PartPose.ZERO);
        root.addOrReplaceChild("drill_mount", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-1, -12, -10, 5, 8, 20), PartPose.ZERO);
        // Headlights on the drill mount, either side of the drill housing.
        root.addOrReplaceChild("headlight_left", CubeListBuilder.create().texOffs(60, 67)
                .addBox(4, -11, -9.5F, 1, 2, 2), PartPose.ZERO);
        root.addOrReplaceChild("headlight_right", CubeListBuilder.create().texOffs(60, 67)
                .addBox(4, -11, 7.5F, 1, 2, 2), PartPose.ZERO);

        return LayerDefinition.create(mesh, 256, 128);
    }

    @Override
    public void setupAnim(BoatRenderState state) {
        super.setupAnim(state);
        this.drillHead.xRot = state.ageInTicks * 0.75F;
        // The pilot can't tilt their camera with the machine, so in first person the
        // cab frame would cut through their view when sloped. Other players still see it.
        boolean hide = state instanceof TunnelerRenderer.TunnelerRenderState tunneler && tunneler.pilotView;
        // When lit, the renderer draws the lamps in a separate full-bright pass.
        boolean lit = state instanceof Headlights.LitBoatRenderState lamp && lamp.lightsOn;
        for (ModelPart part : lamps) {
            part.visible = !lit;
        }
        for (ModelPart part : cabFrame) {
            // "lights" is both a cab part and a lamp: hidden if either rule says so.
            part.visible = !hide && (part.visible || !isLamp(part));
        }
    }

    private boolean isLamp(ModelPart part) {
        for (ModelPart lamp : lamps) {
            if (lamp == part) return true;
        }
        return false;
    }
}
