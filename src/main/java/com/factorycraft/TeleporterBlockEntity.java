package com.factorycraft;

import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Teleporter: link two pads with the wrench (click one, then the other; they work
 * both ways, even between dimensions). Right-click a pad to jump to the other one.
 * Costs 2000 power plus 2 per block of distance (50 000 between dimensions), taken
 * from the network of the pad you leave from.
 */
public final class TeleporterBlockEntity extends MachineBlockEntity implements Energy.Consumer {
    private static final int BASE_COST = 2000;
    private static final int CROSS_DIMENSION_COST = 50_000;
    private final Energy.Link link = new Energy.Link(this);
    private @Nullable BlockPos target;
    private @Nullable ResourceKey<Level> targetLevel;
    private int cooldown;

    public TeleporterBlockEntity(BlockPos pos, BlockState state) {
        super(Endgame.TELEPORTER_BLOCK_ENTITY.value(), pos, state, 0);
    }

    @Override protected boolean isInputSlot(int slot) { return false; }
    @Override protected boolean isOutputSlot(int slot) { return false; }
    @Override protected boolean accepts(int slot, ItemStack stack) { return false; }
    @Override public Energy.Link energyLink() { return link; }

    @Override
    protected boolean work(Level level) {
        if (cooldown > 0) cooldown--;
        return target != null;
    }

    void link(BlockPos to, ResourceKey<Level> dimension) {
        target = to.immutable();
        targetLevel = dimension;
        setChanged();
    }

    public boolean linked() {
        return target != null;
    }

    int cost(ServerLevel destination) {
        if (level == null || target == null) return 0;
        if (destination != level) return CROSS_DIMENSION_COST;
        return BASE_COST + (int) (2 * Math.sqrt(target.distSqr(worldPosition)));
    }

    /** Sends {@code entity} to the linked pad. Returns a message for players. */
    public Component teleport(Entity entity) {
        if (!(level instanceof ServerLevel here) || target == null || targetLevel == null) return Component.translatable("message.factorycraft.teleporter.unlinked");
        if (cooldown > 0) return Component.translatable("message.factorycraft.teleporter.charging");
        ServerLevel there = here.getServer().getLevel(targetLevel);
        if (there == null) return Component.translatable("message.factorycraft.teleporter.lost");
        there.getChunkAt(target); // load the destination
        if (!(there.getBlockEntity(target) instanceof TeleporterBlockEntity other)) {
            target = null;
            setChanged();
            return Component.translatable("message.factorycraft.teleporter.lost");
        }
        int cost = cost(there);
        if (link.draw(here, cost) < cost) return Component.translatable("message.factorycraft.teleporter.power", cost);
        here.sendParticles(ParticleTypes.PORTAL, entity.getX(), entity.getY() + 1, entity.getZ(), 60, 0.4, 0.8, 0.4, 0.3);
        here.playSound(null, worldPosition, SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 1.0F, 0.8F);
        entity.teleportTo(there, target.getX() + 0.5, target.getY() + 1.0, target.getZ() + 0.5, Set.of(), entity.getYRot(), entity.getXRot(), true);
        there.sendParticles(ParticleTypes.REVERSE_PORTAL, target.getX() + 0.5, target.getY() + 1.5, target.getZ() + 0.5, 60, 0.4, 0.8, 0.4, 0.1);
        there.playSound(null, target, SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 1.0F, 1.2F);
        cooldown = 20;
        other.cooldown = 20;
        return Component.translatable("message.factorycraft.teleporter.done", cost);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        int[] t = input.getIntArray("Target").orElse(new int[0]);
        target = t.length == 3 ? new BlockPos(t[0], t[1], t[2]) : null;
        targetLevel = input.read("TargetLevel", Level.RESOURCE_KEY_CODEC).orElse(null);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (target != null) output.putIntArray("Target", new int[] {target.getX(), target.getY(), target.getZ()});
        if (targetLevel != null) output.store("TargetLevel", Level.RESOURCE_KEY_CODEC, targetLevel);
    }
}
