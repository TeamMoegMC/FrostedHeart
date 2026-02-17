package com.teammoeg.frostedheart.block.gunpowderbarrel;

import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.bootstrap.common.FHEntityTypes;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

public class GunpowderBarrelEntity extends ThrowableItemProjectile {
    @Setter
    protected GunpowderBarrelBlock block;

    public GunpowderBarrelEntity(EntityType<? extends ThrowableItemProjectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.block = FHBlocks.GUNPOWDER_BARREL.get();
    }

    public GunpowderBarrelEntity(Level pLevel, LivingEntity shooter) {
        super(FHEntityTypes.GUNPOWDER_BARREL_ENTITY.get(), shooter, pLevel);
        this.block = FHBlocks.GUNPOWDER_BARREL.get();
    }

    @Override
    public void tick() {
        super.tick();
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
            GunpowderBarrelBlock.explode(level(), new BlockPos((int)loc.x, (int)loc.y, (int)loc.z), getItem(), getOwner());
            this.level().broadcastEntityEvent(this, (byte)3);
            this.discard();
        }
    }

    @Override
    public void handleEntityEvent(byte pId) {
        if (pId == 3) {
            level().addParticle(ParticleTypes.EXPLOSION, getX(), getY(), getZ(), 1.0D, 0.0D, 0.0D);
            level().addParticle(ParticleTypes.EXPLOSION_EMITTER, getX(), getY(), getZ(), 1.0D, 0.0D, 0.0D);
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
