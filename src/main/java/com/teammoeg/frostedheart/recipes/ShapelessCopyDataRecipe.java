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

package com.teammoeg.frostedheart.recipes;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.teammoeg.frostedheart.util.RegistryUtils;
import com.teammoeg.frostedheart.util.io.SerializeUtil;

import blusunrize.immersiveengineering.api.crafting.IERecipeSerializer;
import blusunrize.immersiveengineering.api.crafting.IERecipeTypes.TypeWithClass;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.GsonHelper;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.crafting.conditions.ICondition.IContext;
import net.minecraftforge.common.util.Lazy;
import net.minecraftforge.registries.RegistryObject;

public class ShapelessCopyDataRecipe extends ShapelessRecipe{
    public static class Serializer extends IERecipeSerializer<ShapelessCopyDataRecipe> {
        private static NonNullList<Ingredient> readIngredients(JsonArray ingredientArray) {
            NonNullList<Ingredient> nonnulllist = NonNullList.create();

            for (int i = 0; i < ingredientArray.size(); ++i) {
                Ingredient ingredient = Ingredient.fromJson(ingredientArray.get(i));
                if (!ingredient.isEmpty()) {
                    nonnulllist.add(ingredient);
                }
            }

            return nonnulllist;
        }

        @Override
        public ItemStack getIcon() {
            return new ItemStack(Blocks.CRAFTING_TABLE);
        }

        public ShapelessCopyDataRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf pBuffer) {

            String s = pBuffer.readUtf();
            CraftingBookCategory craftingbookcategory = pBuffer.readEnum(CraftingBookCategory.class);
            int i = pBuffer.readVarInt();
            NonNullList<Ingredient> nonnulllist = NonNullList.withSize(i, Ingredient.EMPTY);

            for(int j = 0; j < nonnulllist.size(); ++j) {
               nonnulllist.set(j, Ingredient.fromNetwork(pBuffer));
            }

            ItemStack itemstack = pBuffer.readItem();
            return new ShapelessCopyDataRecipe(recipeId, itemstack, nonnulllist);
        }

        public ShapelessCopyDataRecipe readFromJson(ResourceLocation recipeId, JsonObject json,IContext ctx) {
            NonNullList<Ingredient> nonnulllist = readIngredients(GsonHelper.getAsJsonArray(json, "ingredients"));
            if (nonnulllist.isEmpty()) {
                throw new JsonParseException("No ingredients for shapeless recipe");
            } else if (nonnulllist.size() > 9) {
                throw new JsonParseException("Too many ingredients for shapeless recipe the max is 9");
            } else {
                ItemStack itemstack = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
                return new ShapelessCopyDataRecipe(recipeId, itemstack, nonnulllist);
            }
        }

        public void toNetwork(FriendlyByteBuf buffer, ShapelessCopyDataRecipe recipe) {
        	((RecipeSerializer)recipe.getSuperSerializer()).toNetwork(buffer,recipe);
        }
    }
    public static RegistryObject<IERecipeSerializer<ShapelessCopyDataRecipe>> SERIALIZER;

    public final Ingredient tool;

    public ShapelessCopyDataRecipe(ResourceLocation idIn, ItemStack out, NonNullList<Ingredient> materials) {
        super(idIn, "", CraftingBookCategory.MISC, out, materials);
        tool = materials.get(0);
    }

    /**
     * Returns an Item that is the result of this recipe
     */
    public ItemStack assemble(CraftingContainer inv,RegistryAccess registry) {
        for (int j = 0; j < inv.getContainerSize(); ++j) {
            ItemStack in = inv.getItem(j);
            if (tool.test(in)) {
                ItemStack out = super.assemble(inv,registry);
                out.setTag(in.getTag());
                return out;
            }
        }

        return ItemStack.EMPTY;
    }

    @Override
    public ResourceLocation getId() {
        return getId();
    }

    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER.get();
    }
    public RecipeSerializer<?> getSuperSerializer() {
        return super.getSerializer();
    }
    /*@Override
    public void serializeRecipeData(JsonObject json) {

        JsonArray jsonarray = new JsonArray();

        for (Ingredient ingredient : this.getIngredients()) {
            jsonarray.add(ingredient.toJson());
        }

        json.add("ingredients", jsonarray);
        JsonObject jsonobject = new JsonObject();
        jsonobject.addProperty("item", RegistryUtils.getRegistryName(this.getResultItem(null).getItem()).toString());
        if (this.getResultItem(null).getCount() > 1) {
            jsonobject.addProperty("count", this.getResultItem(null).getCount());
        }

        json.add("result", jsonobject);
    }*/

}
