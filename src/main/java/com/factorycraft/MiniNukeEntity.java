package com.factorycraft;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

/** The thrown mini-nuke. */
public final class MiniNukeEntity extends ThrowableItemProjectile {
    public MiniNukeEntity(EntityType<? extends MiniNukeEntity> type, Level level) {
        super(type, level);
    }

    public MiniNukeEntity(Level level, LivingEntity owner, ItemStack stack) {
        super(Nuclear.MINI_NUKE_ENTITY.value(), owner, level, stack);
    }

    @Override
    protected Item getDefaultItem() {
        return Nuclear.MINI_NUKE.value();
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level() instanceof ServerLevel level) {
            int yield = MiniNukeItem.coreGrade(getItem());
            float power = MiniNukeItem.power(yield);
            Level.ExplosionInteraction interaction =
                    FactoryConfig.weaponsBreakBlocks() ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE;
            level.explode(this, getX(), getY(), getZ(), power, true, interaction);
            // A ring of secondary blasts fills out the crater: one big explosion alone leaves it ragged.
            int ring = 4 + 2 * yield;
            double reach = power * 0.45;
            for (int i = 0; i < ring; i++) {
                double angle = Math.PI * 2.0 * i / ring;
                level.explode(this, getX() + Math.cos(angle) * reach, getY() - power * 0.1, getZ() + Math.sin(angle) * reach,
                        power * 0.45F, false, interaction);
            }
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER,
                    getX(), getY() + power * 0.3, getZ(), 6, power * 0.3, power * 0.3, power * 0.3, 0.0);
            NuclearReactorBlockEntity.fallout(level, getX(), getY(), getZ(), 1.0F + 2.0F * yield, 300 + 200 * yield);
            discard();
        }
    }
}
