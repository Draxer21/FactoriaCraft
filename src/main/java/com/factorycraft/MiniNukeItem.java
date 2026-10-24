package com.factorycraft;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/** Mini-nuke: throw it; a big blast plus a radioactive cloud where it lands. */
public final class MiniNukeItem extends Item implements Radiation.Source {
    public MiniNukeItem(Properties properties) {
        super(properties);
    }

    @Override
    public float radiation() {
        return 3.0F;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.8F, 0.5F);
        if (!level.isClientSide()) {
            MiniNukeEntity nuke = new MiniNukeEntity(level, player, stack);
            nuke.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.1F, 1.0F);
            level.addFreshEntity(nuke);
        }
        player.getCooldowns().addCooldown(stack, 40);
        if (!player.isCreative()) stack.shrink(1);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Radiation.tooltip(radiation()));
        tooltip.accept(Component.translatable("tooltip.factorycraft.mini_nuke.help").withStyle(ChatFormatting.GRAY));
    }
}
