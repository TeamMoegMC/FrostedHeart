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

package com.teammoeg.frostedheart.mixin.minecraft.temperature;

import java.util.function.Consumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.teammoeg.frostedheart.content.climate.recipe.DismantleInnerRecipe;
import com.teammoeg.frostedheart.util.FHUtils;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
/**
 * Gives lining when cloth destroyed
 * <p>
 * */
@Mixin(ItemStack.class)
public class ItemStackMixin_GiveLining {
    @Inject(at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V",
            ordinal = 0), method = "hurtAndBreak")
    public void FH$InnerItemBreak(int amount, LivingEntity entityIn, Consumer onBroken, CallbackInfo cbi) {
        ItemStack item = DismantleInnerRecipe.tryDismantle((ItemStack) (Object) this);
        if (!item.isEmpty() && entityIn instanceof Player)
            FHUtils.giveItem((Player) entityIn, item);
    }
}
