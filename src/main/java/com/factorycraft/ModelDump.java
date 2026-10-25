package com.factorycraft;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Vector3f;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * CI tool: writes every vehicle model's cubes in the vehicle's own frame (blocks: forward, left, up),
 * so the segmented collision boxes can be fitted to the real geometry.
 */
final class ModelDump {
    private ModelDump() {
    }

    static void run() {
        Object[][] models = {
                {"excavator", ExcavatorModel.LAYER_LOCATION, 1.75F},
                {"tunneler", TunnelerModel.LAYER_LOCATION, 1.65F},
                {"dump_truck", DumpTruckModel.LAYER_LOCATION, 1.5F},
                {"cargo_wagon", CargoWagonModel.LAYER_LOCATION, 1.5F},
                {"paver", PaverModel.LAYER_LOCATION, 1.5F},
                {"harvester", HarvesterModel.LAYER_LOCATION, 1.5F},
                {"crane", CraneModel.LAYER_LOCATION, 1.5F},
                {"tower_crane", TowerCraneModel.LAYER_LOCATION, 1.5F},
                {"helicopter", HelicopterModel.LAYER_LOCATION, 1.5F},
                {"submarine", SubmarineModel.LAYER_LOCATION, 1.5F},
                {"locomotive", LocomotiveModel.LAYER_LOCATION, 1.5F},
                {"forester", ForesterModel.LAYER_LOCATION, 1.5F},
                {"bulldozer", BulldozerModel.LAYER_LOCATION, 1.5F},
                {"tanker", TankerModel.LAYER_LOCATION, 1.5F},
        };
        StringBuilder out = new StringBuilder();
        for (Object[] m : models) {
            String name = (String) m[0];
            float s = (Float) m[2];
            try {
                ModelPart root = Minecraft.getInstance().getEntityModels().bakeLayer((ModelLayerLocation) m[1]);
                root.visit(new PoseStack(), (pose, path, index, cube) -> {
                    float[] lo = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE};
                    float[] hi = {-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
                    for (int i = 0; i < 8; i++) {
                        Vector3f v = new Vector3f(
                                ((i & 1) == 0 ? cube.minX : cube.maxX) / 16.0F,
                                ((i & 2) == 0 ? cube.minY : cube.maxY) / 16.0F,
                                ((i & 4) == 0 ? cube.minZ : cube.maxZ) / 16.0F);
                        pose.pose().transformPosition(v);
                        float[] local = {s * v.x, s * v.z, s * (0.375F - v.y)};
                        for (int k = 0; k < 3; k++) {
                            lo[k] = Math.min(lo[k], local[k]);
                            hi[k] = Math.max(hi[k], local[k]);
                        }
                    }
                    out.append(String.format(Locale.ROOT, "%s %s %.2f %.2f %.2f %.2f %.2f %.2f%n",
                            name, path.isEmpty() ? "root" : path, lo[0], hi[0], lo[1], hi[1], lo[2], hi[2]));
                });
            } catch (RuntimeException e) {
                out.append(name).append(" ERROR ").append(e).append('\n');
            }
        }
        try {
            Files.writeString(Path.of("models_dump.txt"), out);
        } catch (IOException e) {
            com.mojang.logging.LogUtils.getLogger().warn("model dump failed", e);
        }
    }
}
