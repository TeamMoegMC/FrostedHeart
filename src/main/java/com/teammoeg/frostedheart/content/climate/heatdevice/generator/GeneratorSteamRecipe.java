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

package com.teammoeg.frostedheart.content.climate.heatdevice.generator;

import java.util.Collections;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.bootstrap.common.FHMultiblocks;

import blusunrize.immersiveengineering.api.crafting.FluidTagInput;
import blusunrize.immersiveengineering.api.crafting.IERecipeSerializer;
import blusunrize.immersiveengineering.api.crafting.IERecipeTypes.TypeWithClass;
import blusunrize.immersiveengineering.api.crafting.IESerializableRecipe;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.common.crafting.conditions.ICondition.IContext;
import net.minecraftforge.common.util.Lazy;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.RegistryObject;

public class GeneratorSteamRecipe extends IESerializableRecipe {
    public static RegistryObject<RecipeType<GeneratorSteamRecipe>> TYPE;
    public static RegistryObject<IERecipeSerializer<GeneratorSteamRecipe>> SERIALIZER;
    public static Lazy<TypeWithClass<GeneratorSteamRecipe>> IEType = Lazy.of(() -> new TypeWithClass<>(TYPE, GeneratorSteamRecipe.class));
    // Initialized by reload listener
    public static Map<ResourceLocation, GeneratorSteamRecipe> recipeList = Collections.emptyMap();
    public final FluidTagInput input;
    public final float power;
    public final float level;


    public GeneratorSteamRecipe(ResourceLocation id, FluidTagInput input,
                                float power, float tempMod) {
        super(Lazy.of(() -> ItemStack.EMPTY), IEType.get(), id);
        this.input = input;
        this.power = power;
        this.level = tempMod;
    }

    public static GeneratorSteamRecipe findRecipe(FluidStack input) {
        for (GeneratorSteamRecipe recipe : recipeList.values())
            if (recipe.input.testIgnoringAmount(input))
                return recipe;
        return null;
    }

    @Override
    protected IERecipeSerializer<GeneratorSteamRecipe> getIESerializer() {
        return SERIALIZER.get();
    }

    @Override
    public ItemStack getResultItem(RegistryAccess pRegistryAccess) {
        return super.outputDummy.get();
    }

    public static class Serializer extends IERecipeSerializer<GeneratorSteamRecipe> {
        @Override
        public ItemStack getIcon() {
            return new ItemStack(FHMultiblocks.Registration.GENERATOR_T1.block().get());
        }

        @Nullable
        @Override
        public GeneratorSteamRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
            FluidTagInput input = FluidTagInput.read(buffer);
            float power = buffer.readFloat();
            float tempMod = buffer.readFloat();
            return new GeneratorSteamRecipe(recipeId, input, power, tempMod);
        }

        @Override
        public GeneratorSteamRecipe readFromJson(ResourceLocation recipeId, JsonObject json, IContext ctx) {
            FluidTagInput input = FluidTagInput.deserialize(GsonHelper.getAsJsonObject(json, "input"));
            float power = GsonHelper.getAsFloat(json, "energy");
            float tempMod = GsonHelper.getAsFloat(json, "level");
            return new GeneratorSteamRecipe(recipeId, input, power, tempMod);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, GeneratorSteamRecipe recipe) {
            recipe.input.write(buffer);
            buffer.writeFloat(recipe.power);
            buffer.writeFloat(recipe.level);
        }
    }
}
