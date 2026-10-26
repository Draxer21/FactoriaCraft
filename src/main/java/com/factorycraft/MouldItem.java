package com.factorycraft;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** A steel mould for the tool forge: decides which tool it makes. Never used up. */
public final class MouldItem extends Item {
    private final ToolForge.Kind kind;

    public MouldItem(Properties properties, ToolForge.Kind kind) {
        super(properties.stacksTo(1));
        this.kind = kind;
    }

    public ToolForge.Kind kind() {
        return kind;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.factorycraft.mould", kind.materials,
                Component.translatable(kind.electric ? "item.factorycraft.circuit_board" : "item.factorycraft.iron_gear"))
                .withStyle(ChatFormatting.GRAY));
    }
}
