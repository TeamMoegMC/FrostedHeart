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

import blusunrize.immersiveengineering.api.crafting.IERecipeSerializer;
import blusunrize.immersiveengineering.api.crafting.IERecipeTypes.TypeWithClass;
import blusunrize.immersiveengineering.api.crafting.IESerializableRecipe;
import com.google.gson.JsonObject;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.common.crafting.conditions.ICondition.IContext;
import net.minecraftforge.common.util.Lazy;
import net.minecraftforge.registries.RegistryObject;

public class InspireRecipe extends IESerializableRecipe {
    public static RegistryObject<RecipeType<InspireRecipe>> TYPE;
    public static Lazy<TypeWithClass<InspireRecipe>> IEType = Lazy.of(() -> new TypeWithClass<>(TYPE, InspireRecipe.class));
    public static RegistryObject<IERecipeSerializer<InspireRecipe>> SERIALIZER;
    public Ingredient item;
    public int inspire;

    public InspireRecipe(ResourceLocation id, Ingredient item, int inspire) {
        super(Lazy.of(() -> ItemStack.EMPTY), IEType.get(), id);
        this.item = item;
        this.inspire = inspire;
    }

    @Override
    protected IERecipeSerializer<InspireRecipe> getIESerializer() {
        return SERIALIZER.get();
    }

    @Override
    public ItemStack getResultItem(RegistryAccess pRegistryAccess) {
        return ItemStack.EMPTY;
    }

    public static class Serializer extends IERecipeSerializer<InspireRecipe> {


        @Override
        public ItemStack getIcon() {
            return new ItemStack(Items.PAPER);
        }

        @Override
        public InspireRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
            return new InspireRecipe(recipeId, Ingredient.fromNetwork(buffer), buffer.readInt());
        }

        @Override
        public InspireRecipe readFromJson(ResourceLocation arg0, JsonObject arg1, IContext ctx) {
            return new InspireRecipe(arg0, Ingredient.fromJson(arg1.get("item")), arg1.get("amount").getAsInt());
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, InspireRecipe recipe) {
            recipe.item.toNetwork(buffer);
            buffer.writeInt(recipe.inspire);
        }

    }
}
