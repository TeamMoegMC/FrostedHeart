/*
 * Copyright (c) 2026 TeamMoeg
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
 *
 */

package com.teammoeg.frostedheart.mixin.minecraft.campfire;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.teammoeg.frostedheart.util.mixin.ICampfireExtra;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
/**
 * Add time limit for campfire
 * <p>
 * */
@Mixin(CampfireBlockEntity.class)
public abstract class CampfireTileEntityMixin_TimeLimit extends BlockEntity implements ICampfireExtra {
    public CampfireTileEntityMixin_TimeLimit(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
		super(pType, pPos, pBlockState);
	}

	public int lifeTime = 0;

	IItemHandler campfireItemHandler=new IItemHandler() {

		@Override
		public int getSlots() {
			return 1;
		}

		@Override
		public @NotNull ItemStack getStackInSlot(int slot) {
			return ItemStack.EMPTY;
		}

		@Override
		public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
			int rawBurnTime = ForgeHooks.getBurnTime(stack,RecipeType.CAMPFIRE_COOKING);
			if(rawBurnTime<=0)return stack;
			int maxcs = (19200 - getLifeTime()) / rawBurnTime / 3;
			if(maxcs<=0)return stack;
            int rcs = Math.min(maxcs, stack.getCount());
            //int burnTime = rawBurnTime * 3 * rcs;
            if(!simulate) {
            	int burnTime = rawBurnTime * 3 * rcs;
            	addLifeTime(burnTime);
            }
            if(stack.getCount()==rcs)
            	return ItemStack.EMPTY;
			return stack.copyWithCount(stack.getCount()-rcs);
		}

		@Override
		public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
			return ItemStack.EMPTY;
		}

		@Override
		public int getSlotLimit(int slot) {
			return 64;
		}

		@Override
		public boolean isItemValid(int slot, @NotNull ItemStack stack) {
			return ForgeHooks.getBurnTime(stack,RecipeType.CAMPFIRE_COOKING)>0;
		}
		
	};
	LazyOptional<IItemHandler> cap=LazyOptional.of(()->campfireItemHandler);
    @Override
	public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
		if(cap==ForgeCapabilities.ITEM_HANDLER){
			return this.cap.cast();
		}
		return super.getCapability(cap, side);
	}

	@Override
    public void addLifeTime(int add) {
        lifeTime += add;
        this.setChanged();
    }

    public void extinguishCampfire() {
        this.level.playSound(null, worldPosition, SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.BLOCKS, 1.0F, 1.0F);
        this.level.setBlockAndUpdate(this.worldPosition, this.getBlockState().setValue(CampfireBlock.LIT, false));
    }

    @Override
    public int getLifeTime() {
        return lifeTime;
    }

    @Inject(at = @At("RETURN"), method = "<init>")
    private void init(CallbackInfo ci) {
        lifeTime = 0;
    }

    @Inject(at = @At("TAIL"), method = "load")
    public void readAdditional(CompoundTag nbt, CallbackInfo ci) {
        if (nbt.contains("LifeTime", 3)) {
            setLifeTime(nbt.getInt("LifeTime"));
        }
    }

    @Override
    public void setLifeTime(int set) {
        lifeTime = set;
        this.setChanged();
    }

    @Inject(at = @At("RETURN"), method = "cookTick")
    private static void fh$cookTick(Level pLevel, BlockPos pPos, BlockState pState, CampfireBlockEntity pBlockEntity,CallbackInfo ci) {
    	CampfireTileEntityMixin_TimeLimit mxi=(CampfireTileEntityMixin_TimeLimit)(BlockEntity)pBlockEntity;
        if (mxi.lifeTime > 0) {
        	mxi.lifeTime--;
        	mxi.setChanged();
        } else {
        	mxi.lifeTime = 0;
        	mxi.extinguishCampfire();
        }
    }

    @Inject(at = @At("HEAD"), method = "saveAdditional")
    public void writeAdditional(CompoundTag compound, CallbackInfo cir) {
        compound.putInt("LifeTime", lifeTime);
    }
}