package com.factorycraft;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** Round model parts out of boxes: a regular polygon prism is the union of rotated slabs. */
final class RoundParts {
    private RoundParts() {
    }

    /**
     * Adds a {@code sides}-sided prism along the part's z axis, centred on the part's origin.
     * {@code radius} is the flat-to-centre distance (half the old square's width) and
     * {@code length} its extent along z. Each slab is a hair shorter than the last so the
     * end caps don't flicker.
     */
    static void prismZ(PartDefinition part, String name, int u, int v, float radius, float length, int sides) {
        float width = 2.0F * radius * (float) Math.tan(Math.PI / sides);
        int slabs = sides / 2;
        for (int i = 0; i < slabs; i++) {
            float inset = 0.03F * i;
            part.addOrReplaceChild(name + "_" + i, CubeListBuilder.create().texOffs(u, v)
                            .addBox(-radius, -width / 2.0F, -length / 2.0F + inset, 2.0F * radius, width, length - 2.0F * inset),
                    PartPose.rotation(0.0F, 0.0F, (float) (Math.PI * 2.0 * i / sides)));
        }
    }
}
