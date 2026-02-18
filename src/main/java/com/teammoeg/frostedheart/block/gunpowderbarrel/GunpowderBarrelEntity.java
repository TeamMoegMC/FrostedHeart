package com.teammoeg.frostedheart.block.gunpowderbarrel;

import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.bootstrap.common.FHEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

public class GunpowderBarrelEntity extends ThrowableItemProjectile {

    public GunpowderBarrelEntity(EntityType<? extends ThrowableItemProjectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public GunpowderBarrelEntity(Level level, LivingEntity owner) {
        super(FHEntityTypes.GUNPOWDER_BARREL_ENTITY.get(), owner, level);
    }

    public GunpowderBarrelEntity(Level pLevel) {
        super(FHEntityTypes.GUNPOWDER_BARREL_ENTITY.get(), pLevel);
    }

    @Override
    public void tick() {
        super.tick();
        // 轨迹粒子
        if (level().isClientSide()) {
            level().addParticle(ParticleTypes.FLAME, true, getX(), getY(), getZ(), 0, 0, 0);
            level().addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, true, getX(), getY(), getZ(), 0, 0, 0);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide()) {
            var loc = result.getLocation();
            GunpowderBarrelBlock.explode(level(), new BlockPos((int)loc.x, (int)loc.y, (int)loc.z), getItem(), getOwner(), false);
            this.level().broadcastEntityEvent(this, (byte)3);
            this.discard();
        }
    }

    @Override
    protected Item getDefaultItem() {
        return FHBlocks.GUNPOWDER_BARREL.asItem();
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
    }

    @Override
    protected float getGravity() {
        return 0.1F;
    }
}
