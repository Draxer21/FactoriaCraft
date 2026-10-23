package com.factorycraft;

import net.minecraft.client.renderer.entity.EntityRendererProvider;

/** Tower crane: same renderer as the crawler crane (machine + hanging load), different model. */
public final class TowerCraneRenderer extends CraneRenderer {
    public TowerCraneRenderer(EntityRendererProvider.Context context) {
        super(context, "tower_crane", TowerCraneModel.LAYER_LOCATION, TowerCraneModel::new,
                TowerCraneModel.LAMP_PARTS, TowerCraneModel.BEAM_ORIGINS);
    }
}
