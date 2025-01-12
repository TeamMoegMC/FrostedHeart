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

package com.teammoeg.frostedheart.content.research.recipe;

import com.google.gson.JsonObject;

import blusunrize.immersiveengineering.api.crafting.IERecipeSerializer;
import blusunrize.immersiveengineering.api.crafting.IESerializableRecipe;
import blusunrize.immersiveengineering.api.crafting.IERecipeTypes.TypeWithClass;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.crafting.conditions.ICondition.IContext;
import net.minecraftforge.common.util.Lazy;
import net.minecraftforge.registries.RegistryObject;

public class ResearchPaperRecipe extends IESerializableRecipe {
    public static class Serializer extends IERecipeSerializer<ResearchPaperRecipe> {


        @Override
        public ItemStack getIcon() {
            return new ItemStack(Items.PAPER);
        }

        @Override
        public ResearchPaperRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
            return new ResearchPaperRecipe(recipeId, Ingredient.fromNetwork(buffer), buffer.readVarInt());
        }

        @Override
        public ResearchPaperRecipe readFromJson(ResourceLocation arg0, JsonObject arg1,IContext ctx) {
            return new ResearchPaperRecipe(arg0, Ingredient.fromJson(arg1.get("item")), arg1.get("level").getAsInt());
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, ResearchPaperRecipe recipe) {
            recipe.paper.toNetwork(buffer);
            buffer.writeVarInt(recipe.maxlevel);
        }

    }
    public static RegistryObject<RecipeType<ResearchPaperRecipe>> TYPE;
    public static Lazy<TypeWithClass<ResearchPaperRecipe>> IEType=Lazy.of(()->new TypeWithClass<>(TYPE, ResearchPaperRecipe.class));
    public static RegistryObject<IERecipeSerializer<ResearchPaperRecipe>> SERIALIZER;
    public Ingredient paper;

    public int maxlevel;

    public ResearchPaperRecipe(ResourceLocation id, Ingredient paper, int maxlevel) {
        super(Lazy.of(()->ItemStack.EMPTY), IEType.get(), id);
        this.paper = paper;
        this.maxlevel = maxlevel;
    }

    @Override
    protected IERecipeSerializer<ResearchPaperRecipe> getIESerializer() {
        return SERIALIZER.get();
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registry) {
        return ItemStack.EMPTY;
    }
}
