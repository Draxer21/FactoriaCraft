package com.factorycraft;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/**
 * Moving track treads: rows of cleats along the top and bottom runs of each
 * crawler track that slide as the machine drives (top run forward, bottom run
 * backward relative to the hull), so the tracks visibly turn.
 */
public final class TrackTreads {
    private static final float CLEAT = 1.5F;

    private final ModelPart[][] top;
    private final ModelPart[][] bottom;
    private final float x0;
    private final float length;
    private final float spacing;
    private final float pxPerBlock;

    private static int count(float x0, float x1) {
        return Math.max(4, Math.round((x1 - x0) / 6.0F));
    }

    /** Adds cleat parts for each track ({z0, z1} pairs) running from x0 to x1, with the track's own texture. */
    public static void add(PartDefinition root, int u, int v, float x0, float x1, float yTop, float yBottom, float[]... tracks) {
        int n = count(x0, x1);
        for (int side = 0; side < tracks.length; side++) {
            float z0 = tracks[side][0] - 0.3F;
            float depth = tracks[side][1] - tracks[side][0] + 0.6F;
            for (int i = 0; i < n; i++) {
                root.addOrReplaceChild("tread_" + side + "_t" + i, CubeListBuilder.create().texOffs(u, v)
                        .addBox(0, yTop - 0.5F, z0, CLEAT, 1.0F, depth), PartPose.offset(x0, 0, 0));
                root.addOrReplaceChild("tread_" + side + "_b" + i, CubeListBuilder.create().texOffs(u, v)
                        .addBox(0, yBottom - 0.5F, z0, CLEAT, 1.0F, depth), PartPose.offset(x0, 0, 0));
            }
        }
    }

    /** {@code scale} is the renderer's scale, to turn blocks driven into model pixels. */
    public TrackTreads(ModelPart root, float x0, float x1, int sides, float scale) {
        int n = count(x0, x1);
        this.top = new ModelPart[sides][n];
        this.bottom = new ModelPart[sides][n];
        for (int side = 0; side < sides; side++) {
            for (int i = 0; i < n; i++) {
                top[side][i] = root.getChild("tread_" + side + "_t" + i);
                bottom[side][i] = root.getChild("tread_" + side + "_b" + i);
            }
        }
        this.x0 = x0;
        this.length = x1 - x0 - CLEAT;
        this.spacing = (x1 - x0) / n;
        this.pxPerBlock = 16.0F / scale;
    }

    public void animate(float travelledBlocks) {
        float shift = travelledBlocks * pxPerBlock;
        for (int side = 0; side < top.length; side++) {
            for (int i = 0; i < top[side].length; i++) {
                top[side][i].x = x0 + wrap(i * spacing + shift);
                bottom[side][i].x = x0 + wrap(i * spacing - shift);
            }
        }
    }

    private float wrap(float x) {
        float span = length + spacing - CLEAT;
        float w = x % span;
        if (w < 0) w += span;
        return Math.min(w, length);
    }

    // ---------------------------------------------------------------- distance driven (client)

    private static final Map<Entity, float[]> DISTANCE = new WeakHashMap<>();

    /** Blocks driven forward by {@code vehicle} (negative when reversing), smoothed over the frame. */
    public static float travelled(Entity vehicle, float partialTicks) {
        float[] d = DISTANCE.computeIfAbsent(vehicle, e -> new float[] {-1, 0, 0});
        if (d[0] != vehicle.tickCount) {
            d[0] = vehicle.tickCount;
            d[1] = d[2];
            float yaw = vehicle.getYRot() * Mth.DEG_TO_RAD;
            d[2] += (float) ((vehicle.getX() - vehicle.xo) * -Mth.sin(yaw) + (vehicle.getZ() - vehicle.zo) * Mth.cos(yaw));
        }
        return Mth.lerp(partialTicks, d[1], d[2]);
    }
}
