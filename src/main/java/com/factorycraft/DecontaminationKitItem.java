package com.factorycraft;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Decontamination kit: right-click contaminated soil to clean a 5x5 area back to grass. */
public final class DecontaminationKitItem extends Item {
    public DecontaminationKitItem(Properties properties) {
        super(properties.durability(32));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos center = context.getClickedPos();
        if (!level.getBlockState(center).is(Nuclear.CONTAMINATED_SOIL.value())) return InteractionResult.PASS;
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        int cleaned = 0;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-2, -1, -2), center.offset(2, 1, 2))) {
            if (level.getBlockState(pos).is(Nuclear.CONTAMINATED_SOIL.value())) {
                level.setBlock(pos, Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
                server.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 2, 0.3, 0.1, 0.3, 0);
                cleaned++;
            }
        }
        level.playSound(null, center, SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 0.8F, 1.4F);
        if (context.getPlayer() != null && cleaned > 0) {
            context.getItemInHand().hurtAndBreak(1, context.getPlayer(),
                    context.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        }
        return InteractionResult.SUCCESS_SERVER;
    }
}
