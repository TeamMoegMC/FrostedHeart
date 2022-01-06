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

package com.teammoeg.frostedheart.content.recipes;

import javax.annotation.Nullable;

import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.FHContent.FHItems;

import blusunrize.immersiveengineering.api.crafting.IERecipeSerializer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;

public class RecipeInnerDismantleSerializer extends IERecipeSerializer<RecipeInnerDismantle> {
    @Override
    public ItemStack getIcon() {
        return new ItemStack(FHItems.buff_coat);
    }

    @Override
    public RecipeInnerDismantle readFromJson(ResourceLocation recipeId, JsonObject json) {
        return new RecipeInnerDismantle(recipeId);
    }

    @Nullable
    @Override
    public RecipeInnerDismantle read(ResourceLocation recipeId, PacketBuffer buffer) {
        return new RecipeInnerDismantle(recipeId);
    }

    @Override
    public void write(PacketBuffer buffer, RecipeInnerDismantle recipe) {
    }
}
