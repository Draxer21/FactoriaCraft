package com.factorycraft;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Base for processing machines. The front (output side) faces away from the
 * player who placed it, so a belt running into its back and another leaving
 * its front form a straight production line.
 */
public abstract class MachineBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    protected MachineBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        // Machine upgrade modules and the wrench.
        if (level.getBlockEntity(pos) instanceof MachineBlockEntity machine) {
            if (stack.getItem() instanceof MachineUpgradeItem module) {
                if (level.isClientSide()) return InteractionResult.SUCCESS;
                if (!machine.supportsUpgrades()) {
                    player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.factorycraft.machine_upgrade.unsupported"));
                    return InteractionResult.FAIL;
                }
                if (!machine.installUpgrade(module.type())) {
                    player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.factorycraft.machine_upgrade.full",
                            MachineBlockEntity.MAX_UPGRADES));
                    return InteractionResult.FAIL;
                }
                if (!player.isCreative()) stack.shrink(1);
                player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.factorycraft.machine_upgrade.installed",
                        machine.upgradeCount(), MachineBlockEntity.MAX_UPGRADES));
                level.playSound(null, pos, net.minecraft.sounds.SoundEvents.SMITHING_TABLE_USE, net.minecraft.sounds.SoundSource.BLOCKS, 0.8F, 1.2F);
                return InteractionResult.SUCCESS_SERVER;
            }
            if (stack.getItem() instanceof WrenchItem && machine instanceof IndustrialChestBlockEntity chest) {
                if (level.isClientSide()) return InteractionResult.SUCCESS;
                boolean on = chest.toggleFilter();
                player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable(
                        on ? "message.factorycraft.chest.filter_on" : "message.factorycraft.chest.filter_off"));
                return InteractionResult.SUCCESS_SERVER;
            }
            if (stack.getItem() instanceof WrenchItem && machine.upgradeCount() > 0) {
                if (level.isClientSide()) return InteractionResult.SUCCESS;
                for (ItemStack out : machine.removeUpgrades()) {
                    if (!player.getInventory().add(out)) player.drop(out, false);
                }
                player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.factorycraft.machine_upgrade.removed"));
                return InteractionResult.SUCCESS_SERVER;
            }
        }
        // Water buckets cool the reactor.
        if (stack.is(net.minecraft.world.item.Items.WATER_BUCKET)
                && level.getBlockEntity(pos) instanceof NuclearReactorBlockEntity reactor) {
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            if (!reactor.acceptWater()) return InteractionResult.FAIL;
            if (!player.isCreative()) player.setItemInHand(hand, new ItemStack(net.minecraft.world.item.Items.BUCKET));
            level.playSound(null, pos, net.minecraft.sounds.SoundEvents.BUCKET_EMPTY, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
            return InteractionResult.SUCCESS_SERVER;
        }
        // Otherwise always open the machine's screen (handled below), whatever is in hand.
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof PumpBlockEntity pump) {
            if (!level.isClientSide()) pump.click(player);
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof MenuProvider menu)) return InteractionResult.PASS;
        if (!level.isClientSide()) player.openMenu(menu);
        return InteractionResult.SUCCESS;
    }
}
