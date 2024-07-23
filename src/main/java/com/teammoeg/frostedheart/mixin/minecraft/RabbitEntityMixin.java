/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.frostedheart.mixin.minecraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.teammoeg.frostedheart.util.mixin.IFeedStore;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

@Mixin(PigEntity.class)
public abstract class RabbitEntityMixin extends AnimalEntity implements IFeedStore {
    byte feeded = 0;


    protected RabbitEntityMixin(EntityType<? extends AnimalEntity> type, World worldIn) {
        super(type, worldIn);
    }


    @Override
    public boolean consumeFeed() {
        if (feeded > 0) {
            feeded--;
            return true;
        }
        return false;
    }

    @Inject(at = @At("HEAD"), method = "writeAdditional")
    public void fh$readAdditional(CompoundNBT compound, CallbackInfo cbi) {
        feeded = compound.getByte("feed_stored");
    }


    @Inject(at = @At("HEAD"), method = "writeAdditional")
    public void fh$writeAdditional(CompoundNBT compound, CallbackInfo cbi) {
        compound.putByte("feed_stored", feeded);

    }

    @Override
    public ActionResultType mobInteract(PlayerEntity playerIn, Hand hand) {
        ItemStack itemstack = playerIn.getItemInHand(hand);

        if (!this.isBaby() && !itemstack.isEmpty() && isFood(itemstack)) {
            if (feeded < 2) {
                feeded++;
                if (!this.level.isClientSide)
                    this.usePlayerItem(playerIn, itemstack);
                return ActionResultType.sidedSuccess(this.level.isClientSide);
            }
        }
        return super.mobInteract(playerIn, hand);
    }
}
