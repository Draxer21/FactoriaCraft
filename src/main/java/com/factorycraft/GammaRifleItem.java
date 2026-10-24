package com.factorycraft;

import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Gamma rifle: fires a beam of radiation that hits the first creature in its
 * path (48 blocks) for heavy damage and withers it. Runs on stored energy:
 * charge it by right-clicking a generator or reactor, or in a solar
 * generator's slot. 1 500 E per shot, 40 shots per full charge.
 */
public final class GammaRifleItem extends Item implements ItemEnergy.Chargeable {
    public static final int CAPACITY = 60_000;
    private static final int SHOT = 1_500;
    private static final double RANGE = 48.0;
    private static final float DAMAGE = 14.0F;

    public GammaRifleItem(Properties properties) {
        super(properties);
    }

    @Override
    public int energyCapacity() {
        return CAPACITY;
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        return ItemEnergy.chargeFromBlock(stack, context);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        if (player.getCooldowns().isOnCooldown(stack)) return InteractionResult.FAIL;
        if (!player.isCreative() && !ItemEnergy.use(stack, SHOT)) {
            player.sendOverlayMessage(Component.translatable("message.factorycraft.no_charge"));
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6F, 1.6F);
            return InteractionResult.FAIL;
        }
        fire(server, player);
        player.getCooldowns().addCooldown(stack, 10);
        return InteractionResult.SUCCESS_SERVER;
    }

    private static void fire(ServerLevel level, Player player) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().scale(RANGE));
        BlockHitResult block = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (block.getType() != HitResult.Type.MISS) end = block.getLocation();

        LivingEntity target = null;
        Vec3 targetHit = null;
        double best = Double.MAX_VALUE;
        AABB box = new AABB(start, end).inflate(1.0);
        for (Entity entity : level.getEntities(player, box, e -> e instanceof LivingEntity && e.isAlive() && e.isPickable())) {
            Optional<Vec3> hit = entity.getBoundingBox().inflate(0.3).clip(start, end);
            if (hit.isPresent() && start.distanceToSqr(hit.get()) < best) {
                best = start.distanceToSqr(hit.get());
                target = (LivingEntity) entity;
                targetHit = hit.get();
            }
        }
        if (target != null) {
            end = targetHit;
            target.hurtServer(level, level.damageSources().playerAttack(player), DAMAGE);
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 1));
            level.sendParticles(ParticleTypes.EXPLOSION, end.x, end.y, end.z, 1, 0.0, 0.0, 0.0, 0.0);
        }

        // The beam.
        Vec3 step = end.subtract(start);
        double length = step.length();
        Vec3 dir = step.normalize();
        for (double d = 1.0; d < length; d += 0.5) {
            Vec3 p = start.add(dir.scale(d)).add(0, -0.15, 0);
            level.sendParticles(ParticleTypes.GLOW, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.0);
            if (((int) (d * 2)) % 3 == 0) level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.0);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.0F, 2.0F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 0.8F, 1.4F);
    }

    // ---------------------------------------------------------------- display

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * ItemEnergy.get(stack) / CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x6CFF4A;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.factorycraft.charge", ItemEnergy.percent(stack)).withStyle(ChatFormatting.GREEN));
        tooltip.accept(Component.translatable("tooltip.factorycraft.gamma_rifle.help").withStyle(ChatFormatting.GRAY));
    }
}
