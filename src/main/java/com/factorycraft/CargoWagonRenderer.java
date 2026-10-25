package com.factorycraft;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.AbstractBoatRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.BoatRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import org.joml.Quaternionf;

public final class CargoWagonRenderer extends AbstractBoatRenderer {
    private static final float SCALE = 1.5F;
    /** The wagon tilts around its tow-bar height, matching the towing code. */
    private static final float PIVOT_HEIGHT = 0.5F;

    private final EntityModel<BoatRenderState> model;

    public CargoWagonRenderer(EntityRendererProvider.Context context) {
        this(context, "cargo_wagon");
    }

    public CargoWagonRenderer(EntityRendererProvider.Context context, String texture) {
        super(context, Identifier.fromNamespaceAndPath(FactoryCraft.MOD_ID, "textures/entity/" + texture + ".png"));
        this.model = new CargoWagonModel(context.bakeLayer(CargoWagonModel.LAYER_LOCATION));
        this.shadowRadius = 1.2F;
    }

    public static final class WagonRenderState extends BoatRenderState {
        public float pitch;
        public float fill;
        public float travelled;
    }

    @Override
    protected EntityModel<BoatRenderState> model() {
        return model;
    }

    @Override
    public BoatRenderState createRenderState() {
        return new WagonRenderState();
    }

    @Override
    public void extractRenderState(AbstractBoat entity, BoatRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        if (state instanceof WagonRenderState wagon && entity instanceof CargoWagonEntity cargo) {
            wagon.pitch = entity.getXRot(partialTicks);
            wagon.fill = cargo.getFill();
            wagon.travelled = TrackTreads.travelled(entity, partialTicks);
        }
    }

    @Override
    public void submit(BoatRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        float pitch = state instanceof WagonRenderState wagon ? wagon.pitch : 0.0F;
        if (pitch != 0.0F) {
            float yaw = state.yRot * Mth.DEG_TO_RAD;
            float forwardX = -Mth.sin(yaw);
            float forwardZ = Mth.cos(yaw);
            poseStack.translate(0.0F, PIVOT_HEIGHT, 0.0F);
            poseStack.mulPose(new Quaternionf().rotationAxis(pitch * Mth.DEG_TO_RAD, forwardZ, 0.0F, -forwardX));
            poseStack.translate(0.0F, -PIVOT_HEIGHT, 0.0F);
        }
        poseStack.scale(SCALE, SCALE, SCALE);
        try {
            super.submit(state, poseStack, collector, camera);
        } finally {
            poseStack.popPose();
        }
    }
}
