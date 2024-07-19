/*
 * Copyright (c) 2021-2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.climate.heatdevice.generator;

import javax.annotation.Nullable;

import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.FHMultiblocks;

import blusunrize.immersiveengineering.api.crafting.IERecipeSerializer;
import blusunrize.immersiveengineering.api.crafting.IESerializableRecipe;
import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.RegistryObject;

public class GeneratorRecipe extends IESerializableRecipe {
    public static class Serializer extends IERecipeSerializer<GeneratorRecipe> {
        @Override
        public ItemStack getIcon() {
            return new ItemStack(FHMultiblocks.generator);
        }

        @Nullable
        @Override
        public GeneratorRecipe read(ResourceLocation recipeId, PacketBuffer buffer) {
            ItemStack output = buffer.readItemStack();
            IngredientWithSize input = IngredientWithSize.read(buffer);
            int time = buffer.readInt();
            return new GeneratorRecipe(recipeId, output, input, time);
        }

        @Override
        public GeneratorRecipe readFromJson(ResourceLocation recipeId, JsonObject json) {
            ItemStack output = readOutput(json.get("result"));
            IngredientWithSize input = IngredientWithSize.deserialize(json.get("input"));
            int time = JSONUtils.getInt(json, "time");
            return new GeneratorRecipe(recipeId, output, input, time);
        }

        @Override
        public void write(PacketBuffer buffer, GeneratorRecipe recipe) {
            buffer.writeItemStack(recipe.output);
            recipe.input.write(buffer);
            buffer.writeInt(recipe.time);
        }
    }
    public static IRecipeType<GeneratorRecipe> TYPE;

    public static RegistryObject<IERecipeSerializer<GeneratorRecipe>> SERIALIZER;
    public final IngredientWithSize input;

    public final ItemStack output;

    public final int time;


/*
    public static List<ItemStack> listAll() {
        ArrayList<ItemStack> all = new ArrayList<>();
        recipeList.values().stream().map(e -> e.input.getMatchingStacks()).forEach(e -> {
            for (ItemStack i : e) if (!all.contains(i)) all.add(i);
        });
        return all;
    }

    public static List<ItemStack> listOut() {
        ArrayList<ItemStack> all = new ArrayList<>();
        recipeList.values().stream().map(e -> e.output).forEach(i -> {
            if (!all.contains(i)) all.add(i);
        });
        return all;
    }*/

    public GeneratorRecipe(ResourceLocation id, ItemStack output, IngredientWithSize input, int time) {
        super(output, TYPE, id);
        this.output = output;
        this.input = input;
        this.time = time;
    }

    @Override
    protected IERecipeSerializer<GeneratorRecipe> getIESerializer() {
        return SERIALIZER.get();
    }

    @Override
    public ItemStack getRecipeOutput() {
        return this.output;
    }
}
