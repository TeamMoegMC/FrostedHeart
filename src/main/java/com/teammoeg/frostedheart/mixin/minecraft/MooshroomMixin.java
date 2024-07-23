/*
 * Copyright (c) 2022-2024 TeamMoeg
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.teammoeg.frostedheart.util.TranslateUtils;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.passive.MooshroomEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

@Mixin(MooshroomEntity.class)
public abstract class MooshroomMixin extends AnimalEntity {

    public MooshroomMixin(EntityType<? extends CowEntity> p_i48567_1_, World p_i48567_2_) {
        super(p_i48567_1_, p_i48567_2_);
    }

    @Inject(at = @At("HEAD"), method = "getEntityInteractionResult", cancellable = true)
    public void fhmo$getEntityInteractionResult(PlayerEntity playerIn, Hand hand, CallbackInfoReturnable<ActionResultType> cbi) {
        ItemStack itemstack = playerIn.getItemInHand(hand);
        if (itemstack.getItem() == Items.BOWL && !this.isBaby()) {
            CowEntityMixin ot = (CowEntityMixin) (Object) this;

            if (ot.milk <= 0) {
                if (!level.isClientSide) {
                    if (ot.feeded <= 0)
                        playerIn.displayClientMessage(TranslateUtils.translateMessage("cow.nomilk.hungry"), true);
                    else
                        playerIn.displayClientMessage(TranslateUtils.translateMessage("cow.nomilk.digest"), true);
                }
                cbi.setReturnValue(ActionResultType.PASS);
            }
            ot.milk--;
        }
    }

}
