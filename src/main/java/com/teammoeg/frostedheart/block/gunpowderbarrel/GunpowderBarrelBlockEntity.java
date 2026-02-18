package com.teammoeg.frostedheart.block.gunpowderbarrel;

import com.teammoeg.chorda.block.entity.CBlockEntity;
import com.teammoeg.chorda.block.entity.CTickableBlockEntity;
import com.teammoeg.frostedheart.bootstrap.common.FHBlockEntityTypes;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

@Getter
public class GunpowderBarrelBlockEntity extends CBlockEntity implements CTickableBlockEntity {
    int range = 1;
    int fortuneLevel = 0;
    boolean willFall = false;

    int litTime = 0;
    boolean lit = false;
    @Nullable
    LivingEntity owner;

    public GunpowderBarrelBlockEntity(BlockPos pos, BlockState state) {
        super(FHBlockEntityTypes.GUNPOWDER_BARREL.get(), pos, state);
    }

    @Override
    public void tick() {
        if (level == null) return;
        if (lit) {
            if (willFall) {
                var barrel = new GunpowderBarrelEntity(level);
                barrel.setOwner(owner);
                barrel.setPos(getBlockPos().getCenter());
                barrel.setItem(GunpowderBarrelItem.create(range, fortuneLevel, willFall));
                level.removeBlock(getBlockPos(), false);
                level.addFreshEntity(barrel);
                return;
            }
            litTime++;
            if (litTime >= 100) {
                GunpowderBarrelBlock.explode(level, getBlockPos(), range, fortuneLevel, owner, true);
            }
        }
    }

    public void lit() {
        lit = true;
        if (level != null) {
            level.playSound(null, getBlockPos(), SoundEvents.TNT_PRIMED, SoundSource.BLOCKS);
        }
    }

    @Override
    public void readCustomNBT(CompoundTag nbt, boolean descPacket) {
        this.range = nbt.getInt(GunpowderBarrelBlock.RANGE);
        this.fortuneLevel = nbt.getInt(GunpowderBarrelBlock.FORTUNE);
        this.willFall = nbt.getBoolean(GunpowderBarrelBlock.WILL_FALL);
        this.litTime = nbt.getInt("litTime");
        this.lit = nbt.getBoolean("lit");
    }

    @Override
    public void writeCustomNBT(CompoundTag nbt, boolean descPacket) {
        nbt.putInt(GunpowderBarrelBlock.RANGE, this.range);
        nbt.putInt(GunpowderBarrelBlock.FORTUNE, this.fortuneLevel);
        nbt.putBoolean(GunpowderBarrelBlock.WILL_FALL, this.willFall);
        nbt.putInt("litTIme", this.litTime);
        nbt.putBoolean("lit", this.lit);
    }
}
