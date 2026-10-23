package com.factorycraft;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.BoatRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector3f;

/**
 * Client-side headlight visuals: lamps drawn at full brightness plus a soft,
 * additive light cone from each headlight. Purely cosmetic and version-safe
 * (no mixins); the actual world lighting comes from {@link VehicleLights}.
 */
public final class Headlights {
    /** Beam length and end radius, in model units (blocks before the renderer's scale). */
    private static final float BEAM_LENGTH = 5.0F;
    private static final float OUTER_END_RADIUS = 1.15F;
    private static final float INNER_END_RADIUS = 0.45F;
    private static final float START_RADIUS = 0.06F;
    /** Slight downward dip of the beam (model y points down here). */
    private static final float BEAM_DIP = 0.07F;
    private static final int SEGMENTS = 16;
    /** Warm white, like halogen work lights. */
    private static final int RED = 255, GREEN = 238, BLUE = 190;
    private static final int OUTER_ALPHA = 46;
    private static final int INNER_ALPHA = 64;

    private final ModelPart[] lamps;
    private final float[][] beamOrigins;
    private final Identifier texture;

    /**
     * @param root        an independently baked copy of the vehicle model
     * @param lampParts   names of the lamp parts (headlights first)
     * @param beamOrigins headlight front-face centres in model px
     */
    public Headlights(ModelPart root, String[] lampParts, float[][] beamOrigins, Identifier texture) {
        this.lamps = parts(root, lampParts);
        this.beamOrigins = beamOrigins;
        this.texture = texture;
    }

    public static ModelPart[] parts(ModelPart root, String[] names) {
        ModelPart[] parts = new ModelPart[names.length];
        for (int i = 0; i < names.length; i++) parts[i] = root.getChild(names[i]);
        return parts;
    }

    /** Same rule the server uses to place light blocks: someone driving and it's dark. */
    public static boolean areOn(Entity vehicle) {
        if (!(vehicle instanceof VehicleLights.Holder holder)) return false;
        boolean driven = vehicle.getFirstPassenger() instanceof Player;
        VehicleEngine engine = VehicleEngine.of(vehicle);
        if (engine == null) return driven && holder.lights().isDark(vehicle.level());
        return engine.lightsWanted(driven, holder.lights().isDark(vehicle.level()));
    }

    /**
     * Call from the renderer's {@code submitTypeAdditions}, i.e. inside the boat pose
     * (model space: +x forward, y down, units of 16 px).
     *
     * @param maxLamps how many lamp parts to light (the rest stay hidden this frame)
     */
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int maxLamps) {
        int count = Math.min(maxLamps, lamps.length);
        for (int i = 0; i < count; i++) {
            collector.submitModelPart(lamps[i], poseStack, RenderTypes.entityCutout(texture),
                    LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, null);
        }
        collector.submitCustomGeometry(poseStack, RenderTypes.lightning(), (pose, buffer) -> {
            for (float[] origin : beamOrigins) {
                Vector3f start = new Vector3f(origin[0] / 16.0F, origin[1] / 16.0F, origin[2] / 16.0F);
                cone(pose, buffer, start, OUTER_END_RADIUS, OUTER_ALPHA);
                cone(pose, buffer, start, INNER_END_RADIUS, INNER_ALPHA);
            }
        });
    }

    private static void cone(PoseStack.Pose pose, VertexConsumer buffer, Vector3f start, float endRadius, int alpha) {
        Vector3f dir = new Vector3f(1.0F, BEAM_DIP, 0.0F).normalize();
        Vector3f end = new Vector3f(dir).mul(BEAM_LENGTH).add(start);
        // Two axes perpendicular to the beam.
        Vector3f side = new Vector3f(0.0F, 0.0F, 1.0F);
        Vector3f up = new Vector3f(dir).cross(side).normalize();

        for (int i = 0; i < SEGMENTS; i++) {
            float a0 = Mth.TWO_PI * i / SEGMENTS;
            float a1 = Mth.TWO_PI * (i + 1) / SEGMENTS;
            Vector3f n0 = ring(up, side, a0);
            Vector3f n1 = ring(up, side, a1);
            Vector3f s0 = new Vector3f(n0).mul(START_RADIUS).add(start);
            Vector3f s1 = new Vector3f(n1).mul(START_RADIUS).add(start);
            Vector3f e0 = new Vector3f(n0).mul(endRadius).add(end);
            Vector3f e1 = new Vector3f(n1).mul(endRadius).add(end);
            // Both windings so the beam is visible from inside and outside.
            quad(pose, buffer, s0, s1, e1, e0, alpha);
            quad(pose, buffer, s0, e0, e1, s1, alpha);
        }
    }

    private static Vector3f ring(Vector3f up, Vector3f side, float angle) {
        return new Vector3f(up).mul(Mth.cos(angle)).add(new Vector3f(side).mul(Mth.sin(angle)));
    }

    /** a and b are at the lamp (bright); c and d at the far end (fully faded). */
    private static void quad(PoseStack.Pose pose, VertexConsumer buffer,
                             Vector3f a, Vector3f b, Vector3f c, Vector3f d, int alpha) {
        vertex(pose, buffer, a, isNear(a, b, c, d, a) ? alpha : 0);
        vertex(pose, buffer, b, isNear(a, b, c, d, b) ? alpha : 0);
        vertex(pose, buffer, c, isNear(a, b, c, d, c) ? alpha : 0);
        vertex(pose, buffer, d, isNear(a, b, c, d, d) ? alpha : 0);
    }

    /** Vertices with the smallest x of the quad are at the lamp end. */
    private static boolean isNear(Vector3f a, Vector3f b, Vector3f c, Vector3f d, Vector3f v) {
        float min = Math.min(Math.min(a.x, b.x), Math.min(c.x, d.x));
        return v.x < min + 0.5F;
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer buffer, Vector3f p, int alpha) {
        buffer.addVertex(pose, p.x, p.y, p.z).setColor(RED, GREEN, BLUE, alpha);
    }

    /** Render state for vehicles with headlights. */
    public static class LitBoatRenderState extends BoatRenderState {
        public boolean lightsOn;
        /** Blocks driven, for track treads and wheels. */
        public float travelled;
    }
}
