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

package com.teammoeg.frostedheart.content.incubator;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.chorda.util.RegistryUtils;
import com.teammoeg.chorda.util.io.SerializeUtil;

import blusunrize.immersiveengineering.api.ApiUtils;
import blusunrize.immersiveengineering.api.crafting.IERecipeSerializer;
import blusunrize.immersiveengineering.api.crafting.IESerializableRecipe;
import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;
import blusunrize.immersiveengineering.api.crafting.IERecipeTypes.TypeWithClass;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.ItemLike;
import net.minecraft.util.GsonHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.crafting.conditions.ICondition.IContext;
import net.minecraftforge.common.util.Lazy;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.RegistryObject;

public class IncubateRecipe extends IESerializableRecipe {
    public static class Serializer extends IERecipeSerializer<IncubateRecipe> {
        @Override
        public ItemStack getIcon() {
            return new ItemStack(FHBlocks.INCUBATOR.get());
        }

        @Nullable
        @Override
        public IncubateRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
            return new IncubateRecipe(recipeId, IngredientWithSize.read(buffer), SerializeUtil.readOptional(buffer, IngredientWithSize::read).orElse(null), buffer.readItem(), buffer.readFluidStack(), buffer.readBoolean(), buffer.readVarInt(), buffer.readVarInt());
        }

        @Override
        public IncubateRecipe readFromJson(ResourceLocation recipeId, JsonObject json, IContext context) {
            IngredientWithSize input = IngredientWithSize.deserialize(GsonHelper.getAsJsonObject(json, "input"));
            ItemStack output = ItemStack.EMPTY;
            if (json.has("output"))
                output = readOutput(json.get("output")).get();
            FluidStack output_fluid = FluidStack.EMPTY;
            if (json.has("fluid"))
                output_fluid = ApiUtils.jsonDeserializeFluidStack(json.get("fluid").getAsJsonObject());
            IngredientWithSize seed = null;
            if (json.has("catalyst"))
                seed = IngredientWithSize.deserialize(json.get("catalyst"));
            boolean use_catalyst = false;
            if (json.has("consume_catalyst"))
                use_catalyst = json.get("consume_catalyst").getAsBoolean();
            int water = 0;
            if (json.has("water"))
                water = json.get("water").getAsInt();
            int time = 100;
            if (json.has("time"))
                time = json.get("time").getAsInt();

            return new IncubateRecipe(recipeId, input, seed, output, output_fluid, use_catalyst, water, time);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, IncubateRecipe recipe) {
            recipe.input.write(buffer);
            SerializeUtil.writeOptional(buffer, recipe.catalyst, IngredientWithSize::write);

            buffer.writeItem(recipe.output);
            buffer.writeFluidStack(recipe.output_fluid);
            buffer.writeBoolean(recipe.consume_catalyst);
            buffer.writeVarInt(recipe.water);
            buffer.writeVarInt(recipe.time);
        }
    }
    public static RegistryObject<RecipeType<IncubateRecipe>> TYPE;
    public static Lazy<TypeWithClass<IncubateRecipe>> IEType=Lazy.of(()->new TypeWithClass<>(TYPE, IncubateRecipe.class));
    public static RegistryObject<Serializer> SERIALIZER;
    public IngredientWithSize input;
    public IngredientWithSize catalyst;
    public ItemStack output;
    public FluidStack output_fluid;
    public boolean consume_catalyst;
    public final boolean isFood;


    public int water;

    public int time;



    public IncubateRecipe() {
        super(Lazy.of(()->ItemStack.EMPTY), IEType.get(), IncubatorTileEntity.food);
        isFood = true;
        List<ItemLike> items = new ArrayList<>();
        for (Item i : RegistryUtils.getItems()) {
            if (i.isEdible())
                items.add(i);
        }

        this.input = new IngredientWithSize(Ingredient.of(items.toArray(new ItemLike[0])), 1);
        this.catalyst = IngredientWithSize.of(new ItemStack(Items.ROTTEN_FLESH));
        this.output = ItemStack.EMPTY;
        this.output_fluid = new FluidStack(IncubatorTileEntity.getProtein(), 25);
        this.consume_catalyst = true;
        this.water = 20;
        this.time = 20;
    }

    public IncubateRecipe(ResourceLocation id, IngredientWithSize input,
                          IngredientWithSize catalyst, ItemStack output, FluidStack output_fluid, boolean consume_catalyst, int water,
                          int time) {
        super(Lazy.of(()->output), IEType.get(), id);
        this.input = input;
        this.catalyst = catalyst;
        this.output = output;
        this.output_fluid = output_fluid;
        this.consume_catalyst = consume_catalyst;
        this.water = water;
        this.time = time;
        isFood = false;
    }

    @Override
    protected IERecipeSerializer<IncubateRecipe> getIESerializer() {
        return SERIALIZER.get();
    }

    @Override
    public ItemStack getResultItem(RegistryAccess ra) {
        return this.output;
    }
}
