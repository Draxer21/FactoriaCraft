package com.factorycraft;

import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

/** Radioactive ore or storage block, as an item. */
public class RadioactiveBlockItem extends BlockItem implements Radiation.Source {
    private final float radiation;

    public RadioactiveBlockItem(Block block, Properties properties, float radiation) {
        super(block, properties);
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
