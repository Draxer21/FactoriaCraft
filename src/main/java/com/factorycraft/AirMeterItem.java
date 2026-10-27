package com.factorycraft;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** Air quality meter: right-click to read the pollution of the chunk you stand in (0-1000) and what it means. */
public final class AirMeterItem extends Item {
    public AirMeterItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    static String band(int air) {
        if (air == 0) return "clean";
        if (air < AirQuality.HAZE) return "fair";
        if (air < AirQuality.FAUNA) return "hazy";
        if (air < AirQuality.ACID) return "bad";
        return "toxic";
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            int air = AirQuality.get(level, player.blockPosition());
            player.sendOverlayMessage(Component.translatable("message.factorycraft.air_meter", air,
                    Component.translatable("message.factorycraft.air_meter." + band(air))));
        }
        return InteractionResult.SUCCESS;
    }
}
