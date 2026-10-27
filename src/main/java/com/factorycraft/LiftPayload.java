package com.factorycraft;

import net.minecraft.core.BlockPos;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client → server: the player on a lift pad jumped (up) or sneaked (down). */
public record LiftPayload(boolean up) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<LiftPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(FactoryCraft.MOD_ID, "lift"));
    public static final StreamCodec<ByteBuf, LiftPayload> STREAM_CODEC =
            ByteBufCodecs.BOOL.map(LiftPayload::new, LiftPayload::up);

    public static void handle(LiftPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        BlockPos pad = player.blockPosition().below();
        if (!player.level().getBlockState(pad).is(Defense.LIFT_PAD.value())) return;
        BlockPos target = LiftPadBlock.next(player.level(), pad, payload.up());
        if (target == null) return;
        player.teleportTo(target.getX() + 0.5, target.getY() + 1.0, target.getZ() + 0.5);
        player.level().playSound(null, target, SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 0.4F, 1.6F);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
