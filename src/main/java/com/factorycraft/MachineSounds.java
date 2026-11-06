package com.factorycraft;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Working sounds of stationary machines (client only). Every half second the
 * machines near the player that are running (lit) get a looping sound that
 * fades in, and fades out when they stop. Only the closest few play at once,
 * so a big factory doesn't use up every sound channel. Players can turn them
 * down with the "Blocks" volume slider.
 */
@EventBusSubscriber(modid = FactoryCraft.MOD_ID, value = Dist.CLIENT)
public final class MachineSounds {
    private static final int MAX_PLAYING = 10;
    private static final double RANGE = 20.0;

    private record Voice(Supplier<SoundEvent> sound, float volume) {}

    private static final Map<Class<? extends BlockEntity>, Voice> VOICES = new HashMap<>();
    private static final Map<BlockPos, Loop> PLAYING = new HashMap<>();
    private static int timer;

    static {
        voice(FactoryCraft.CRUSHER_LOOP::value, 0.6F, CrusherBlockEntity.class, WellDrillBlockEntity.class, QuarryBlockEntity.class);
        voice(FactoryCraft.CENTRIFUGE_LOOP::value, 0.45F, CentrifugeBlockEntity.class);
        voice(FactoryCraft.PUMP_LOOP::value, 0.5F, PumpBlockEntity.class, PumpjackBlockEntity.class, DrainBlockEntity.class,
                DesalinatorBlockEntity.class, TreatmentPlantBlockEntity.class);
        voice(FactoryCraft.GENERATOR_LOOP::value, 0.5F, GeneratorBlockEntity.class, RefineryBlockEntity.class);
        voice(FactoryCraft.REACTOR_LOOP::value, 0.7F, NuclearReactorBlockEntity.class, AntimatterReactorBlockEntity.class);
        voice(FactoryCraft.ACCELERATOR_LOOP::value, 0.6F, AcceleratorBlockEntity.class);
        voice(FactoryCraft.COMPUTER_LOOP::value, 0.35F, ResearchStationBlockEntity.class);
        voice(FactoryCraft.ALARM_LOOP::value, 1.2F, AlarmBlockEntity.class);
        voice(FactoryCraft.PRESS_LOOP::value, 0.5F, PressBlockEntity.class);
        voice(FactoryCraft.MACHINE_HUM::value, 0.4F, AlloySmelterBlockEntity.class, PackagerBlockEntity.class, TanneryBlockEntity.class,
                MixerBlockEntity.class, ToolForgeBlockEntity.class, RecyclerBlockEntity.class, OreWasherBlockEntity.class,
                FermenterBlockEntity.class, AssemblerBlockEntity.class, LevelerBlockEntity.class, FillerBlockEntity.class, BuilderBlockEntity.class);
    }

    @SafeVarargs
    private static void voice(Supplier<SoundEvent> sound, float volume, Class<? extends BlockEntity>... types) {
        for (Class<? extends BlockEntity> type : types) VOICES.put(type, new Voice(sound, volume));
    }

    private MachineSounds() {}

    private static boolean running(Level level, BlockPos pos, Class<?> type) {
        BlockState state = level.getBlockState(pos);
        BlockEntity be = level.getBlockEntity(pos);
        return be != null && be.getClass() == type && state.hasProperty(MachineBlock.LIT) && state.getValue(MachineBlock.LIT);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            PLAYING.clear();
            return;
        }
        PLAYING.values().removeIf(Loop::isStopped);
        if (++timer % 10 != 0) return;

        Level level = mc.level;
        BlockPos center = mc.player.blockPosition();
        List<BlockEntity> found = new ArrayList<>();
        int cx = center.getX() >> 4;
        int cz = center.getZ() >> 4;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (!level.hasChunk(cx + dx, cz + dz)) continue;
                LevelChunk chunk = level.getChunk(cx + dx, cz + dz);
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (!VOICES.containsKey(be.getClass())) continue;
                    if (be.getBlockPos().distSqr(center) > RANGE * RANGE) continue;
                    if (running(level, be.getBlockPos(), be.getClass())) found.add(be);
                }
            }
        }
        found.sort(Comparator.comparingDouble(be -> be.getBlockPos().distSqr(center)));
        for (int i = 0; i < found.size() && PLAYING.size() < MAX_PLAYING; i++) {
            BlockEntity be = found.get(i);
            BlockPos pos = be.getBlockPos().immutable();
            if (PLAYING.containsKey(pos)) continue;
            Voice voice = VOICES.get(be.getClass());
            Loop loop = new Loop(voice.sound().get(), pos, be.getClass(), voice.volume());
            PLAYING.put(pos, loop);
            mc.getSoundManager().play(loop);
        }
    }

    /** One machine's loop: fades in while the machine runs, fades out and ends when it stops. */
    private static final class Loop extends AbstractTickableSoundInstance {
        private final BlockPos pos;
        private final Class<?> type;
        private final float full;
        private float level;

        Loop(SoundEvent event, BlockPos pos, Class<?> type, float full) {
            super(event, SoundSource.BLOCKS, RandomSource.create(pos.asLong()));
            this.pos = pos;
            this.type = type;
            this.full = full;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.0F;
            this.x = pos.getX() + 0.5;
            this.y = pos.getY() + 0.5;
            this.z = pos.getZ() + 0.5;
            this.pitch = 0.95F + RandomSource.create(pos.asLong()).nextFloat() * 0.1F;
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        @Override
        public void tick() {
            Minecraft mc = Minecraft.getInstance();
            boolean on = mc.level != null && mc.player != null && running(mc.level, pos, type)
                    && pos.distSqr(mc.player.blockPosition()) <= (RANGE + 4) * (RANGE + 4);
            level = Mth.approach(level, on ? full : 0.0F, full / 10.0F);
            volume = level;
            if (!on && level <= 0.0F) stop();
        }
    }
}
