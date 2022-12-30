/*
 * Copyright (c) 2022 TeamMoeg
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
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.cannolicatfish.rankine.init.RankineTags;
import com.teammoeg.frostedheart.util.BreedUtil;

import net.minecraft.entity.AgeableEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.passive.FoxEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.passive.RabbitEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.passive.horse.AbstractHorseEntity;
import net.minecraft.entity.passive.horse.LlamaEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.world.World;

@Mixin({AnimalEntity.class})
public abstract class AnimalEntityMixin extends AgeableEntity {

    protected AnimalEntityMixin(EntityType<? extends AgeableEntity> type, World worldIn) {
        super(type, worldIn);
    }

    @ModifyConstant(method = "spawnBabyAnimal", constant = @Constant(intValue = 6000))
    public int getBreedCooldown(int orig) {
        return 28800;
    }

	@Inject(at = @At("HEAD"), method = "isBreedingItem",cancellable=true)
    public void isBreedingItem(ItemStack itemStack,CallbackInfoReturnable<Boolean> cbi) {
        EntityType<?> type = getType();
        boolean f=BreedUtil.isBreedingItem(type, itemStack);
        if(f)
        	cbi.setReturnValue(true);
     }
}
