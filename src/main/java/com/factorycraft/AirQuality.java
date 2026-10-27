package com.factorycraft;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Air quality per chunk (0 = clean, 1000 = choking), saved with the world.
 * Smoke from dirty generators and incinerators, reactor leaks and nuclear blasts
 * raise it; it clears by itself (about 700 points an hour) and faster under air
 * purifiers and water treatment plants.
 * <ul>
 * <li>150+: a brown haze closes in (thicker the worse it gets).</li>
 * <li>300+: animals leave the area and none are born there.</li>
 * <li>400+ and raining: acid rain kills leaves and crops, scorches grass and
 * stings players without a helmet.</li>
 * <li>0 (spotless): greenhouses work 50% faster.</li>
 * </ul>
 */
@EventBusSubscriber(modid = FactoryCraft.MOD_ID)
public final class AirQuality {
    private AirQuality() {}

    public static final int MAX = 1000;
    public static final int HAZE = 150;
    public static final int FAUNA = 300;
    public static final int ACID = 400;
    /** Ticks for one point to clear. */
    private static final int DECAY_TICKS = 100;

    /** The stored value and when it was last written (decay is worked out on read). */
    public record Air(int value, long stamp) {
        static final Air CLEAN = new Air(0, 0);
        static final com.mojang.serialization.MapCodec<Air> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.INT.fieldOf("value").forGetter(Air::value),
                Codec.LONG.fieldOf("stamp").forGetter(Air::stamp)).apply(i, Air::new));

        int now(long time) {
            if (value <= 0) return 0;
            return Math.max(0, value - (int) ((time - stamp) / DECAY_TICKS));
        }
    }

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, FactoryCraft.MOD_ID);
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Air>> AIR = ATTACHMENTS.register("air_quality",
            () -> AttachmentType.builder(() -> Air.CLEAN).serialize(Air.CODEC).build());

    // ---------------------------------------------------------------- reading and writing

    public static int get(Level level, BlockPos pos) {
        if (!level.isLoaded(pos)) return 0;
        LevelChunk chunk = level.getChunkAt(pos);
        return chunk.getData(AIR).now(level.getGameTime());
    }

    /** What a player standing at {@code pos} breathes: own chunk plus some of the neighbours'. */
    public static int felt(Level level, BlockPos pos) {
        int own = get(level, pos);
        int around = 0;
        for (int[] d : new int[][] {{16, 0}, {-16, 0}, {0, 16}, {0, -16}}) around += get(level, pos.offset(d[0], 0, d[1]));
        return Math.min(MAX, Math.max(own, own / 2 + around / 8));
    }

    public static void add(Level level, BlockPos pos, int amount) {
        if (!FactoryConfig.pollution() || !level.isLoaded(pos) || level.isClientSide()) return;
        LevelChunk chunk = level.getChunkAt(pos);
        long time = level.getGameTime();
        int value = Math.max(0, Math.min(MAX, chunk.getData(AIR).now(time) + amount));
        chunk.setData(AIR, new Air(value, time));
        chunk.markUnsaved();
    }

    /** Adds to a chunk and half as much to the ones around it. */
    public static void spread(Level level, BlockPos pos, int amount) {
        add(level, pos, amount);
        for (int[] d : new int[][] {{16, 0}, {-16, 0}, {0, 16}, {0, -16}}) add(level, pos.offset(d[0], 0, d[1]), amount / 2);
    }

    // ---------------------------------------------------------------- effects

    /** Sends each player the air they breathe every 2 seconds (for the haze). */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 40 != 0) return;
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            Level level = player.level();
            int air = felt(level, player.blockPosition());
            boolean acid = air >= ACID && level.isRainingAt(player.blockPosition().above());
            PacketDistributor.sendToPlayer(player, new PollutionPayload(air, acid));
            if (acid && player.getItemBySlot(EquipmentSlot.HEAD).isEmpty() && level instanceof ServerLevel server && player.tickCount % 80 < 40) {
                player.hurtServer(server, server.damageSources().magic(), 1.0F);
            }
            if (acid && level instanceof ServerLevel server) acidRain(server, player.blockPosition());
        }
    }

    /** A few drops of acid rain on the ground around a player. */
    private static void acidRain(ServerLevel level, BlockPos center) {
        RandomSource random = level.getRandom();
        for (int i = 0; i < 6; i++) {
            int x = center.getX() + random.nextInt(49) - 24;
            int z = center.getZ() + random.nextInt(49) - 24;
            BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(x, 0, z)).below();
            if (!level.isRainingAt(top.above()) || get(level, top) < ACID) continue;
            BlockState state = level.getBlockState(top);
            if (state.is(BlockTags.LEAVES)) level.destroyBlock(top, false);
            else if (state.is(Blocks.GRASS_BLOCK)) level.setBlock(top, Blocks.COARSE_DIRT.defaultBlockState(), Block.UPDATE_ALL);
            else if (state.getBlock() instanceof CropBlock crop && crop.getAge(state) > 0) level.setBlock(top, crop.getStateForAge(crop.getAge(state) - 1), Block.UPDATE_ALL);
            else if (state.is(BlockTags.FLOWERS)) level.destroyBlock(top, false);
            level.sendParticles(ParticleTypes.SMOKE, top.getX() + 0.5, top.getY() + 1.1, top.getZ() + 0.5, 3, 0.3, 0.1, 0.3, 0.01);
        }
    }

    /** Animals walk away from foul air. */
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Animal animal) || animal.level().isClientSide() || animal.tickCount % 100 != 37) return;
        if (animal.isLeashed() || animal.isVehicle() || animal.isPassenger()) return;
        Level level = animal.level();
        BlockPos pos = animal.blockPosition();
        if (get(level, pos) < FAUNA) return;
        BlockPos best = null;
        int bestAir = get(level, pos);
        for (int[] d : new int[][] {{24, 0}, {-24, 0}, {0, 24}, {0, -24}, {24, 24}, {-24, -24}, {24, -24}, {-24, 24}}) {
            BlockPos p = pos.offset(d[0], 0, d[1]);
            int air = get(level, p);
            if (air < bestAir) {
                bestAir = air;
                best = p;
            }
        }
        if (best != null) animal.getNavigation().moveTo(best.getX() + 0.5, pos.getY(), best.getZ() + 0.5, 1.3);
    }

    @SubscribeEvent
    public static void onSpawn(FinalizeSpawnEvent event) {
        if (!(event.getEntity() instanceof Animal)) return;
        EntitySpawnReason reason = event.getSpawnType();
        if (reason != EntitySpawnReason.NATURAL && reason != EntitySpawnReason.CHUNK_GENERATION && reason != EntitySpawnReason.BREEDING) return;
        if (get(event.getEntity().level(), event.getEntity().blockPosition()) >= FAUNA) event.setSpawnCancelled(true);
    }

    static ChunkPos chunk(BlockPos pos) {
        return ChunkPos.containing(pos);
    }

    static List<Integer> sample(Level level, BlockPos pos) {
        return List.of(get(level, pos), felt(level, pos));
    }
}
