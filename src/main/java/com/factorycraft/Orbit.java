package com.factorycraft;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;

/** The satellites this world has put in orbit (kept with the overworld's data). */
public record Orbit(int solar, int survey) {
    public static final Orbit EMPTY = new Orbit(0, 0);
    static final com.mojang.serialization.MapCodec<Orbit> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.fieldOf("solar").forGetter(Orbit::solar),
            Codec.INT.fieldOf("survey").forGetter(Orbit::survey)).apply(i, Orbit::new));

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Orbit>> TYPE = AirQuality.ATTACHMENTS.register("orbit",
            () -> AttachmentType.builder(() -> EMPTY).serialize(CODEC).build());

    static void init() {}

    public static Orbit get(Level level) {
        MinecraftServer server = level.getServer();
        if (server == null) return EMPTY;
        return server.overworld().getData(TYPE);
    }

    public static void launch(Level level, boolean survey) {
        MinecraftServer server = level.getServer();
        if (server == null) return;
        Orbit now = get(level);
        server.overworld().setData(TYPE, survey ? new Orbit(now.solar(), now.survey() + 1) : new Orbit(now.solar() + 1, now.survey()));
    }
}
