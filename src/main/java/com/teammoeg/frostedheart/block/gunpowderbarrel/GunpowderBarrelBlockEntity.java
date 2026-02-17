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

@Getter
public class GunpowderBarrelBlockEntity extends CBlockEntity implements CTickableBlockEntity {
    int range = 1;
    int fortuneLevel = 0;

    int litTime = 0;
    boolean lit = false;
    LivingEntity owner;

    public GunpowderBarrelBlockEntity(BlockPos pos, BlockState state) {
        super(FHBlockEntityTypes.GUNPOWDER_BARREL.get(), pos, state);
    }

    @Override
    public void tick() {
        if (lit) {
            litTime++;
            if (litTime >= 100) {
                if (getBlock().getBlock() instanceof GunpowderBarrelBlock barrel) {
                    GunpowderBarrelBlock.explode(level, getBlockPos(), range, fortuneLevel, owner);
                }
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
        this.range = nbt.getInt(GunpowderBarrelBlock.RANGE_KEY);
        this.fortuneLevel = nbt.getInt(GunpowderBarrelBlock.FORTUNE_LEVEL_KEY);
        this.litTime = nbt.getInt("litTime");
        this.lit = nbt.getBoolean("lit");
    }

    @Override
    public void writeCustomNBT(CompoundTag nbt, boolean descPacket) {
        nbt.putInt(GunpowderBarrelBlock.RANGE_KEY, this.range);
        nbt.putInt(GunpowderBarrelBlock.FORTUNE_LEVEL_KEY, this.fortuneLevel);
        nbt.putInt("litTIme", this.litTime);
        nbt.putBoolean("lit", this.lit);
    }
}
