package com.teammoeg.frostedheart.block.gunpowderbarrel;

import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.bootstrap.common.FHEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

public class GunpowderBarrelEntity extends ThrowableItemProjectile {

    public GunpowderBarrelEntity(EntityType<? extends ThrowableItemProjectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public GunpowderBarrelEntity(Level level, LivingEntity owner) {
        super(FHEntityTypes.GUNPOWDER_BARREL_ENTITY.get(), owner, level);
    }

    public GunpowderBarrelEntity(Level pLevel) {
        this(FHEntityTypes.GUNPOWDER_BARREL_ENTITY.get(), pLevel);
        double d0 = pLevel.random.nextDouble() * (double)((float)Math.PI * 2F);
        this.setDeltaMovement(-Math.sin(d0) * 0.01D, 0, -Math.cos(d0) * 0.01D);
    }

    public static GunpowderBarrelEntity fall(Level level, BlockPos pos, int range, int fortuneLevel, boolean destroyBlock, @Nullable Entity owner) {
        var barrel = new GunpowderBarrelEntity(level);
        barrel.setOwner(owner);
        barrel.setPos(pos.getX()+0.5, pos.getY(), pos.getZ()+0.5);
        barrel.setItem(GunpowderBarrelItem.create(range, fortuneLevel, true, destroyBlock));
        level.removeBlock(pos, false);
        level.addFreshEntity(barrel);
        return barrel;
    }

    public static FallingBlockEntity vanillaFall(Level level, BlockPos pos, BlockState state) {
        var data = new CompoundTag();
        if (level.getBlockEntity(pos) instanceof GunpowderBarrelBlockEntity be) {
            be.writeCustomNBT(data, false);
        }
        var fallingBlock = FallingBlockEntity.fall(level, pos, state);
        fallingBlock.blockData = data;
        fallingBlock.disableDrop();
        return fallingBlock;
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
            GunpowderBarrelBlock.explode(level(), new BlockPos((int)Math.floor(loc.x), (int)Math.floor(loc.y), (int)Math.floor(loc.z)), getItem(), false, getOwner());
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
