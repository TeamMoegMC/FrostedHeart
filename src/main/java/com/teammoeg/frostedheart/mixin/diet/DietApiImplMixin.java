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

package com.teammoeg.frostedheart.mixin.diet;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.teammoeg.frostedheart.recipes.DietValueRecipe;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import top.theillusivec4.diet.api.DietApi;
import top.theillusivec4.diet.api.IDietResult;
import top.theillusivec4.diet.common.impl.DietApiImpl;
import top.theillusivec4.diet.common.util.DietResult;

@Mixin(DietApiImpl.class)
public class DietApiImplMixin extends DietApi {
    @Inject(at = @At("HEAD"), require = 1, method = "get(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/item/ItemStack;)Ltop/theillusivec4/diet/api/IDietResult;", cancellable = true, remap = false)
    public void get(PlayerEntity player, ItemStack input, CallbackInfoReturnable<IDietResult> result) {
        DietValueRecipe fvr = DietValueRecipe.recipeList.get(input.getItem());
        if (fvr != null) {
            result.setReturnValue(new DietResult(fvr.getValues()));
        }
    }

    @Inject(at = @At("HEAD"), require = 1, method = "get(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/item/ItemStack;IF)Ltop/theillusivec4/diet/api/IDietResult;", cancellable = true, remap = false)
    public void get(PlayerEntity player, ItemStack input, int heal, float sat,
                    CallbackInfoReturnable<IDietResult> result) {
        DietValueRecipe fvr = DietValueRecipe.recipeList.get(input.getItem());
        if (fvr != null) {
            result.setReturnValue(new DietResult(fvr.getValues()));
        }
    }

}
