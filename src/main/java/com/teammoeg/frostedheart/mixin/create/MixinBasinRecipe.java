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

package com.teammoeg.frostedheart.mixin.create;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.piston.LinearActuatorBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder;
import com.simibubi.create.foundation.item.SmartInventory;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import com.teammoeg.frostedheart.compat.create.ContraptionCostUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({BasinRecipe.class})
public abstract class MixinBasinRecipe extends ProcessingRecipe<SmartInventory> {

    public MixinBasinRecipe(IRecipeTypeInfo typeInfo, ProcessingRecipeBuilder.ProcessingRecipeParams params) {
        super(typeInfo, params);
    }

    @Inject(at = @At("HEAD"), method = "apply(Lcom/simibubi/create/content/processing/basin/BasinBlockEntity;Lnet/minecraft/world/item/crafting/Recipe;)Z", remap = false,cancellable = true)
    private static void FixApply(BasinBlockEntity basin, Recipe<?> recipe, CallbackInfoReturnable<Boolean> cir) {
/*        basin.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack stack = handler.getStackInSlot(i);
                if (stack.isEmpty()) continue;
                var resourceLocation = ForgeRegistries.ITEMS.getKey(stack.getItem());
                if (resourceLocation != null && "tetra".equals(resourceLocation.getNamespace())) {
                    cir.setReturnValue(false);
                }
            }});*/
        var optionalHandler = basin.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve();

        if (optionalHandler.isEmpty()) {
            return;
        }
        IItemHandler handler = optionalHandler.get();
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            var resourceLocation = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (resourceLocation != null && "tetra".equals(resourceLocation.getNamespace())) {
                cir.setReturnValue(false);
                return;
            }
        }
    }
}

