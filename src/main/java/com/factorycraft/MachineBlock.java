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
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.@org.jspecify.annotations.Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof Player player && level.getBlockEntity(pos) instanceof Owned owned) owned.setOwner(player.getUUID());
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        // Vending machine: customers pay with what they hold.
        if (level.getBlockEntity(pos) instanceof VendingMachineBlockEntity vending && !vending.isOwner(player)) {
            if (!level.isClientSide()) player.sendOverlayMessage(vending.buy(player, hand));
            return InteractionResult.SUCCESS;
        }
        // Contract board: deliver goods.
        if (level.getBlockEntity(pos) instanceof ContractBoardBlockEntity board && !stack.isEmpty()) {
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            if (board.deliver(player, hand)) return InteractionResult.SUCCESS_SERVER;
            board.read(player);
            return InteractionResult.SUCCESS_SERVER;
        }
        // Zone controller: the owner binds access cards.
        if (level.getBlockEntity(pos) instanceof ZoneControllerBlockEntity zone && stack.is(Economy.ACCESS_CARD.value())) {
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            if (!zone.isOwner(player)) return InteractionResult.FAIL;
            zone.bind(player, stack);
            return InteractionResult.SUCCESS_SERVER;
        }
        // Teleporters: wrench one pad, then the other.
        if (stack.getItem() instanceof WrenchItem && level.getBlockEntity(pos) instanceof TeleporterBlockEntity pad) {
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            net.minecraft.nbt.CompoundTag tag = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                    net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
            int[] from = tag.getIntArray("TeleFrom").orElse(new int[0]);
            var fromLevel = tag.read("TeleFromLevel", Level.RESOURCE_KEY_CODEC);
            BlockPos source = from.length == 3 ? new BlockPos(from[0], from[1], from[2]) : null;
            net.minecraft.server.level.ServerLevel sourceLevel = source != null && fromLevel.isPresent() && level.getServer() != null
                    ? level.getServer().getLevel(fromLevel.get()) : null;
            if (sourceLevel != null && !(source.equals(pos) && sourceLevel == level)
                    && sourceLevel.getBlockEntity(source) instanceof TeleporterBlockEntity first) {
                first.link(pos, level.dimension());
                pad.link(source, sourceLevel.dimension());
                tag.remove("TeleFrom");
                tag.remove("TeleFromLevel");
                player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.factorycraft.teleporter.linked"));
            } else {
                tag.putIntArray("TeleFrom", new int[] {pos.getX(), pos.getY(), pos.getZ()});
                tag.store("TeleFromLevel", Level.RESOURCE_KEY_CODEC, level.dimension());
                player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.factorycraft.teleporter.pick"));
            }
            stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
            return InteractionResult.SUCCESS_SERVER;
        }
        // Security cameras: wrench the camera, then the monitor.
        if (stack.getItem() instanceof WrenchItem
                && (level.getBlockEntity(pos) instanceof SecurityCameraBlockEntity || level.getBlockEntity(pos) instanceof MonitorBlockEntity)) {
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            net.minecraft.nbt.CompoundTag tag = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                    net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
            if (level.getBlockEntity(pos) instanceof SecurityCameraBlockEntity) {
                tag.putIntArray("CameraFrom", new int[] {pos.getX(), pos.getY(), pos.getZ()});
                player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.factorycraft.monitor.pick"));
            } else if (level.getBlockEntity(pos) instanceof MonitorBlockEntity monitor) {
                int[] from = tag.getIntArray("CameraFrom").orElse(new int[0]);
                if (from.length == 3) {
                    monitor.link(player, new BlockPos(from[0], from[1], from[2]));
                    tag.remove("CameraFrom");
                } else {
                    player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.factorycraft.monitor.how"));
                }
            }
            stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
            return InteractionResult.SUCCESS_SERVER;
        }
        // Drone ports: dock delivery drones, link with the wrench.
        if (level.getBlockEntity(pos) instanceof DronePortBlockEntity port) {
            if (stack.is(Logistics.DELIVERY_DRONE_ITEM.value())) {
                if (level.isClientSide()) return InteractionResult.SUCCESS;
                if (!port.dock()) return InteractionResult.FAIL;
                if (!player.isCreative()) stack.shrink(1);
                player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.factorycraft.drone_port.docked", port.drones()));
                return InteractionResult.SUCCESS_SERVER;
            }
            if (stack.getItem() instanceof WrenchItem) {
                if (level.isClientSide()) return InteractionResult.SUCCESS;
                net.minecraft.nbt.CompoundTag tag = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                        net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
                int[] from = tag.getIntArray("PortFrom").orElse(new int[0]);
                BlockPos source = from.length == 3 ? new BlockPos(from[0], from[1], from[2]) : null;
                if (source != null && !source.equals(pos) && level.getBlockEntity(source) instanceof DronePortBlockEntity sender) {
                    sender.link(player, pos);
                    tag.remove("PortFrom");
                } else {
                    tag.putIntArray("PortFrom", new int[] {pos.getX(), pos.getY(), pos.getZ()});
                    player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.factorycraft.drone_port.pick"));
                }
                stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
                return InteractionResult.SUCCESS_SERVER;
            }
        }
        // Buckets fill and empty tanks and fluid machines.
        if (stack.getItem() instanceof net.minecraft.world.item.BucketItem
                && level.getBlockEntity(pos) instanceof FluidStorage.Holder holder) {
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            if (!holder.fluids().useBucket(player, hand)) return InteractionResult.FAIL;
            player.sendOverlayMessage(holder.fluids().describe(0));
            return InteractionResult.SUCCESS_SERVER;
        }
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
        if (level.getBlockEntity(pos) instanceof TeleporterBlockEntity pad) {
            if (!level.isClientSide()) player.sendOverlayMessage(pad.teleport(player));
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown() && level.getBlockEntity(pos) instanceof LaunchPadBlockEntity launchPad) {
            if (!level.isClientSide()) player.sendOverlayMessage(launchPad.start(level));
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof StatsTerminalBlockEntity) {
            if (player instanceof net.minecraft.server.level.ServerPlayer server) {
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(server, ProductionStats.snapshot(level, 12));
            }
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof ContractBoardBlockEntity board) {
            if (!level.isClientSide()) board.read(player);
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof ZoneControllerBlockEntity zone) {
            if (!level.isClientSide()) {
                if (zone.isOwner(player) && player.isShiftKeyDown()) zone.cycle(player);
                else player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable(
                        zone.isOwner(player) ? "message.factorycraft.zone.yours" : "message.factorycraft.zone.denied"));
            }
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof MonitorBlockEntity monitor) {
            if (!level.isClientSide()) {
                if (player.isShiftKeyDown()) monitor.clear(player);
                else if (player instanceof net.minecraft.server.level.ServerPlayer server) monitor.watch(server);
            }
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown() && level.getBlockEntity(pos) instanceof GarageBlockEntity garage) {
            if (!level.isClientSide()) garage.park(player);
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown() && level.getBlockEntity(pos) instanceof DronePortBlockEntity port) {
            if (!level.isClientSide() && port.undock()) {
                ItemStack drone = new ItemStack(Logistics.DELIVERY_DRONE_ITEM.value());
                if (!player.getInventory().add(drone)) player.drop(drone, false);
            }
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof CableStationBlockEntity station) {
            if (!level.isClientSide()) player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable(
                    station.linked() ? "message.factorycraft.cable.station_linked" : "message.factorycraft.cable.station_free"));
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof FluidTankBlockEntity || level.getBlockEntity(pos) instanceof SewerGrateBlockEntity) {
            if (!level.isClientSide()) player.sendOverlayMessage(((FluidStorage.Holder) level.getBlockEntity(pos)).fluids().describe(0));
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown() && level.getBlockEntity(pos) instanceof AreaMachine area) {
            if (!level.isClientSide()) area.cycleRadius(player);
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof MenuProvider menu)) return InteractionResult.PASS;
        if (!level.isClientSide()) player.openMenu(menu);
        return InteractionResult.SUCCESS;
    }
}
