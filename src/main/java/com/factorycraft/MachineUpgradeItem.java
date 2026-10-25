package com.factorycraft;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** Upgrade module for processing machines (right-click the machine to install). */
public final class MachineUpgradeItem extends Item {
    private final MachineBlockEntity.MachineUpgrade type;

    public MachineUpgradeItem(Properties properties, MachineBlockEntity.MachineUpgrade type) {
        super(properties.stacksTo(16));
        this.type = type;
    }

    public MachineBlockEntity.MachineUpgrade type() {
        return type;
    }

    public static Item item(MachineBlockEntity.MachineUpgrade type) {
        return switch (type) {
            case SPEED -> FactoryCraft.MACHINE_UPGRADE_SPEED.value();
            case EFFICIENCY -> FactoryCraft.MACHINE_UPGRADE_EFFICIENCY.value();
            case OUTPUT -> FactoryCraft.MACHINE_UPGRADE_OUTPUT.value();
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("upgrade.factorycraft.machine." + type.name().toLowerCase() + ".desc").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.factorycraft.machine_upgrade.help").withStyle(ChatFormatting.DARK_GRAY));
    }
}
