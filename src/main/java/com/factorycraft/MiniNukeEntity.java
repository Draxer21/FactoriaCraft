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
            level.explode(this, getX(), getY(), getZ(), MiniNukeItem.power(yield), true,
                    NuclearConfig.weaponsBreakBlocks() ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE);
            NuclearReactorBlockEntity.fallout(level, getX(), getY(), getZ(), 1.0F + 2.0F * yield, 300 + 200 * yield);
            discard();
        }
    }
}
