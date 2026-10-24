package com.factorycraft;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** An item that irradiates whoever carries it without lead armour (see {@link Radiation}). */
public class RadioactiveItem extends Item implements Radiation.Source {
    private final float radiation;

    public RadioactiveItem(Properties properties, float radiation) {
        super(properties);
        this.radiation = radiation;
    }

    @Override
    public float radiation() {
        return radiation;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Radiation.tooltip(radiation));
    }
}
