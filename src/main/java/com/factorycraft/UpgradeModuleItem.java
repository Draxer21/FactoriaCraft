package com.factorycraft;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** An installable machine upgrade (see {@link Upgrade}). */
public final class UpgradeModuleItem extends Item {
    private final Upgrade upgrade;

    public UpgradeModuleItem(Properties properties, Upgrade upgrade) {
        super(properties.stacksTo(16));
        this.upgrade = upgrade;
    }

    public Upgrade upgrade() {
        return upgrade;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable(upgrade.translationKey() + ".desc").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("upgrade.factorycraft.how").withStyle(ChatFormatting.DARK_GRAY));
    }
}
