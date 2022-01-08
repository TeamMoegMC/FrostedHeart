/*
 * Copyright (c) 2021 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 */

package com.teammoeg.frostedheart.mixin.minecraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.teammoeg.frostedheart.util.ICampfireExtra;

import net.minecraft.block.BlockState;
import net.minecraft.block.CampfireBlock;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.CampfireTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;

@Mixin(CampfireTileEntity.class)
public abstract class CampfireTileEntityMixin extends TileEntity implements ICampfireExtra {
    public int lifeTime;

    @Override
    public int getLifeTime() {
        return lifeTime;
    }

    @Override
    public void addLifeTime(int add) {
        lifeTime += add;
    }

    @Override
    public void setLifeTime(int set) {
        lifeTime = set;
    }

    public CampfireTileEntityMixin(TileEntityType<?> tileEntityTypeIn) {
        super(tileEntityTypeIn.CAMPFIRE);
    }

    public void extinguishCampfire() {
        this.world.playSound(null, pos, SoundEvents.ENTITY_GENERIC_EXTINGUISH_FIRE, SoundCategory.BLOCKS, 1.0F, 1.0F);
        this.world.setBlockState(this.pos, this.getBlockState().with(CampfireBlock.LIT, false));
    }
    @Inject(at = @At("RETURN"), method = "<init>()V")
    private void init(CallbackInfo ci)
    {
        lifeTime = 0;
    }
    @Inject(at = @At("RETURN"), method = "tick")
    public void tick(CallbackInfo ci) {
        if (!this.world.isRemote) {
            if (CampfireBlock.isLit(world.getBlockState(getPos())))
                if (lifeTime > 0)
                    lifeTime--;
                else {
                    lifeTime = 0;
                    extinguishCampfire();
                }
        }
    }

    @Inject(at = @At("TAIL"), method = "read")
    public void readAdditional(BlockState state, CompoundNBT nbt, CallbackInfo ci) {
        if (nbt.contains("LifeTime", 3)) {
            setLifeTime(nbt.getInt("LifeTime"));
        }
    }

    @Inject(at = @At("HEAD"), method = "write")
    public void writeAdditional(CompoundNBT compound, CallbackInfoReturnable<CompoundNBT> cir) {
        compound.putInt("LifeTime", lifeTime);
    }
}