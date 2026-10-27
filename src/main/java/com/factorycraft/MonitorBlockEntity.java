package com.factorycraft;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Security monitor: shows what its cameras see. Link cameras with the wrench
 * (click the camera, then the monitor; up to 8). Right-click to watch: jump
 * switches camera, sneak goes back. Sneak + right-click clears the list.
 */
public final class MonitorBlockEntity extends MachineBlockEntity {
    public static final int MAX_CAMERAS = 8;
    private final List<BlockPos> cameras = new ArrayList<>();

    public MonitorBlockEntity(BlockPos pos, BlockState state) {
        super(Defense.MONITOR_BLOCK_ENTITY.value(), pos, state, 0);
    }

    @Override protected boolean isInputSlot(int slot) { return false; }
    @Override protected boolean isOutputSlot(int slot) { return false; }
    @Override protected boolean accepts(int slot, ItemStack stack) { return false; }

    @Override
    protected boolean work(Level level) {
        return !cameras.isEmpty();
    }

    public List<BlockPos> cameras() {
        return cameras;
    }

    public void link(Player player, BlockPos camera) {
        if (cameras.contains(camera)) return;
        if (cameras.size() >= MAX_CAMERAS) {
            player.sendOverlayMessage(Component.translatable("message.factorycraft.monitor.full", MAX_CAMERAS));
            return;
        }
        cameras.add(camera.immutable());
        setChanged();
        player.sendOverlayMessage(Component.translatable("message.factorycraft.monitor.linked", cameras.size()));
    }

    public void clear(Player player) {
        cameras.clear();
        setChanged();
        player.sendOverlayMessage(Component.translatable("message.factorycraft.monitor.cleared"));
    }

    /** Sends the cameras that still exist to the player's screen. */
    public void watch(ServerPlayer player) {
        cameras.removeIf(pos -> level != null && level.isLoaded(pos) && !(level.getBlockEntity(pos) instanceof SecurityCameraBlockEntity));
        List<CameraViewPayload.View> views = new ArrayList<>();
        for (BlockPos pos : cameras) {
            if (level == null || !level.isLoaded(pos)) continue;
            BlockState state = level.getBlockState(pos);
            if (!state.hasProperty(MachineBlock.FACING)) continue;
            views.add(new CameraViewPayload.View(pos, state.getValue(MachineBlock.FACING).toYRot()));
        }
        if (views.isEmpty()) {
            player.sendOverlayMessage(Component.translatable("message.factorycraft.monitor.none"));
            return;
        }
        PacketDistributor.sendToPlayer(player, new CameraViewPayload(views));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        cameras.clear();
        int n = input.getIntOr("Cameras", 0);
        for (int i = 0; i < n && i < MAX_CAMERAS; i++) {
            int[] p = input.getIntArray("Camera" + i).orElse(new int[0]);
            if (p.length == 3) cameras.add(new BlockPos(p[0], p[1], p[2]));
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Cameras", cameras.size());
        for (int i = 0; i < cameras.size(); i++) {
            BlockPos p = cameras.get(i);
            output.putIntArray("Camera" + i, new int[] {p.getX(), p.getY(), p.getZ()});
        }
    }
}
