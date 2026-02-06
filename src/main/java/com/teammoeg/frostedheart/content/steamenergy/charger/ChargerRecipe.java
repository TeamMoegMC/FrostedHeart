/*
 * Copyright (c) 2026 TeamMoeg
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

package com.teammoeg.frostedheart.content.steamenergy.charger;

import blusunrize.immersiveengineering.api.crafting.IERecipeSerializer;
import blusunrize.immersiveengineering.api.crafting.IERecipeTypes.TypeWithClass;
import blusunrize.immersiveengineering.api.crafting.IESerializableRecipe;
import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;
import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.common.crafting.conditions.ICondition.IContext;
import net.minecraftforge.common.util.Lazy;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nullable;

public class ChargerRecipe extends IESerializableRecipe {
    public static RegistryObject<RecipeType<ChargerRecipe>> TYPE;
    public static Lazy<TypeWithClass<ChargerRecipe>> IEType = Lazy.of(() -> new TypeWithClass<>(TYPE, ChargerRecipe.class));
    public static RegistryObject<IERecipeSerializer<ChargerRecipe>> SERIALIZER;
    public final IngredientWithSize input;
    public final ItemStack output;
    public final float cost;

    public ChargerRecipe(ResourceLocation id, ItemStack output, IngredientWithSize input, float cost2) {
        super(Lazy.of(() -> output), IEType.get(), id);
        this.output = output;
        this.input = input;
        this.cost = cost2;
    }

    @Override
    protected IERecipeSerializer<ChargerRecipe> getIESerializer() {
        return SERIALIZER.get();
    }

    @Override
    public ItemStack getResultItem(RegistryAccess access) {
        return this.output;
    }

    public static class Serializer extends IERecipeSerializer<ChargerRecipe> {
        @Override
        public ItemStack getIcon() {
            return new ItemStack(FHBlocks.CHARGER.get());
        }

        @Nullable
        @Override
        public ChargerRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
            ItemStack output = buffer.readItem();
            IngredientWithSize input = IngredientWithSize.read(buffer);
            float cost = buffer.readFloat();
            return new ChargerRecipe(recipeId, output, input, cost);
        }

        @Override
        public ChargerRecipe readFromJson(ResourceLocation recipeId, JsonObject json, IContext ctx) {
            ItemStack output = readOutput(json.get("result")).get();
            IngredientWithSize input = IngredientWithSize.deserialize(json.get("input"));
            float cost = GsonHelper.getAsInt(json, "cost");
            return new ChargerRecipe(recipeId, output, input, cost);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, ChargerRecipe recipe) {
            buffer.writeItem(recipe.output);
            recipe.input.write(buffer);
            buffer.writeFloat(recipe.cost);
        }
    }

}
