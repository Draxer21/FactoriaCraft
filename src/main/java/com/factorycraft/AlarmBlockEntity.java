package com.factorycraft;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * Alarm: goes off when a hostile mob comes within 16 blocks, or another player
 * (not its owner) within 10. While it sounds it lights up and gives a redstone
 * signal on every side, to shut shutters, open doors or switch on lamps.
 */
public final class AlarmBlockEntity extends MachineBlockEntity implements Owned {
    private static final double MOB_RANGE = 16.0;
    private static final double PLAYER_RANGE = 10.0;
    private @Nullable UUID owner;
    private int timer;
    private int hold;

    public AlarmBlockEntity(BlockPos pos, BlockState state) {
        super(Defense.ALARM_BLOCK_ENTITY.value(), pos, state, 0);
    }

    @Override protected boolean isInputSlot(int slot) { return false; }
    @Override protected boolean isOutputSlot(int slot) { return false; }
    @Override protected boolean accepts(int slot, ItemStack stack) { return false; }
    @Override public void setOwner(UUID owner) { this.owner = owner; setChanged(); }
    @Override public @Nullable UUID owner() { return owner; }

    boolean intruder(Level level) {
        AABB area = new AABB(worldPosition).inflate(MOB_RANGE);
        if (!level.getEntitiesOfClass(Mob.class, area, mob -> mob instanceof Enemy && mob.isAlive()).isEmpty()) return true;
        AABB near = new AABB(worldPosition).inflate(PLAYER_RANGE);
        return !level.getEntitiesOfClass(Player.class, near,
                p -> p.isAlive() && !p.isSpectator() && owner != null && !owner.equals(p.getUUID())).isEmpty();
    }

    @Override
    protected boolean work(Level level) {
        if (!(level instanceof ServerLevel server)) return false;
        if (++timer % 10 == 0) {
            if (intruder(level)) hold = 60;
            else if (hold > 0) hold -= 10;
        }
        boolean on = hold > 0;
        if (on && timer % 10 == 0) {
            float pitch = (timer / 10) % 2 == 0 ? 1.4F : 1.0F;
            server.playSound(null, worldPosition, SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.BLOCKS, 2.0F, pitch);
        }
        return on;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        owner = input.read("Owner", UUIDUtil.CODEC).orElse(null);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (owner != null) output.store("Owner", UUIDUtil.CODEC, owner);
    }
}
