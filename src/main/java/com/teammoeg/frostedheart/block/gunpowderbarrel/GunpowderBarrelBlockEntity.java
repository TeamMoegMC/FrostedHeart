package com.teammoeg.frostedheart.block.gunpowderbarrel;

import com.teammoeg.chorda.block.entity.CBlockEntity;
import com.teammoeg.chorda.block.entity.CTickableBlockEntity;
import com.teammoeg.frostedheart.bootstrap.common.FHBlockEntityTypes;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Getter
public class GunpowderBarrelBlockEntity extends CBlockEntity implements CTickableBlockEntity {
    int range = 1;
    int fortuneLevel = 0;
    boolean willFall = false;
    boolean destroyBlock = true;

    int litTime = 0;
    boolean lit = false;
    @Nullable
    private UUID ownerUUID;
    @Nullable
    private Entity owner;

    public GunpowderBarrelBlockEntity(BlockPos pos, BlockState state) {
        super(FHBlockEntityTypes.GUNPOWDER_BARREL.get(), pos, state);
    }

    @Override
    public void tick() {
        if (!(level instanceof ServerLevel)) return;
        if (lit) {
            if (willFall) {
                GunpowderBarrelEntity.vanillaFall(level, getBlockPos(), getBlockState());
                return;
            }
            litTime++;
            if (litTime >= 100) {
                GunpowderBarrelBlock.explode(level, getBlockPos(), range, fortuneLevel, destroyBlock, true, getOwner());
            }
        }
    }

    public void lit() {
        lit = true;
        if (level != null) {
            level.playSound(null, getBlockPos(), SoundEvents.TNT_PRIMED, SoundSource.BLOCKS);
        }
    }

    public void setOwner(Entity owner) {
        if (owner != null) {
            this.ownerUUID = owner.getUUID();
            this.owner = owner;
        }
    }

    @Nullable
    public Entity getOwner() {
        if (this.owner != null && !this.owner.isRemoved()) {
            return this.owner;
        } else if (this.ownerUUID != null && level instanceof ServerLevel sl) {
            this.owner = sl.getEntity(this.ownerUUID);
            return this.owner;
        } else {
            return null;
        }
    }

    @Override
    public void readCustomNBT(CompoundTag nbt, boolean descPacket) {
        this.range = nbt.getInt(GunpowderBarrelBlock.RANGE);
        this.fortuneLevel = nbt.getInt(GunpowderBarrelBlock.FORTUNE);
        this.willFall = nbt.getBoolean(GunpowderBarrelBlock.WILL_FALL);
        this.destroyBlock = !nbt.getBoolean(GunpowderBarrelBlock.SAFE_EXPLODE);
        this.litTime = nbt.getInt("litTime");
        this.lit = nbt.getBoolean("lit");
        if (nbt.hasUUID("owner")) {
            this.ownerUUID = nbt.getUUID("owner");
        }
    }

    @Override
    public void writeCustomNBT(CompoundTag nbt, boolean descPacket) {
        nbt.putInt(GunpowderBarrelBlock.RANGE, this.range);
        nbt.putInt(GunpowderBarrelBlock.FORTUNE, this.fortuneLevel);
        nbt.putBoolean(GunpowderBarrelBlock.WILL_FALL, this.willFall);
        nbt.putBoolean(GunpowderBarrelBlock.SAFE_EXPLODE, !this.destroyBlock);
        nbt.putInt("litTIme", this.litTime);
        nbt.putBoolean("lit", this.lit);
        if (ownerUUID != null) {
            nbt.putUUID("owner", ownerUUID);
        }
    }
}
