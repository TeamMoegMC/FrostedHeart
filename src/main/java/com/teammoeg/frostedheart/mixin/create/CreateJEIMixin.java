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

package com.teammoeg.frostedheart.mixin.create;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.simibubi.create.compat.jei.CreateJEI;
import com.simibubi.create.compat.jei.category.CreateRecipeCategory;

import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapedRecipe;
import net.minecraft.item.crafting.ShapelessRecipe;
import net.minecraft.util.NonNullList;

@Mixin(targets = "com.simibubi.create.compat.jei.CreateJEI$CategoryBuilder", remap = false)
public abstract class CreateJEIMixin {
    @Shadow(remap = false)
    private CreateRecipeCategory category;
    private static final List<String> hidden = Arrays.asList("automatic_packing", "automatic_shapeless");

    private static final IRecipe<?> fh$convert(ShapelessRecipe r) {
        NonNullList<Ingredient> i = r.getIngredients();
        NonNullList<Ingredient> outcopy = NonNullList.create();
        outcopy.addAll(i);
        if (outcopy.size() > 3)
            while (outcopy.size() % 3 != 0)
                outcopy.add(Ingredient.EMPTY);
        return new ShapedRecipe(r.getId(), r.getGroup(), Math.min(i.size(), 3), Math.max(1, (outcopy.size() + 2) / 3), outcopy, r.getRecipeOutput());
    }

    /*
     * Increate performance.
     * */
    @Inject(at = @At("HEAD"), method = "build", remap = false, cancellable = true)
    private void build(CallbackInfoReturnable<CreateRecipeCategory> cat) {
        if (hidden.contains(category.getUid().getPath())) cat.setReturnValue(category);
        if (category.getUid().getPath().equals("automatic_shaped")) {
            try {
                this.getClass().getMethod("recipeList", Supplier.class, Function.class).invoke(this, (Supplier<List<IRecipe<?>>>) () -> CreateJEI.findRecipes(r -> r.getSerializer() == IRecipeSerializer.CRAFTING_SHAPELESS), (Function<ShapelessRecipe, IRecipe<?>>) CreateJEIMixin::fh$convert);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
                    | NoSuchMethodException | SecurityException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}
