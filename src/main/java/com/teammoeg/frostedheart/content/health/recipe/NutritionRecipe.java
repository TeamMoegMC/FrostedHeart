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

package com.teammoeg.frostedheart.content.health.recipe;

import com.google.gson.JsonObject;

import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Generated static mapping from an item ingredient to legacy raw nutrition values.
 *
 * <p>This recipe is data storage, not a public nutrition model. Its four values intentionally
 * remain on the generated-data scale so existing JSON and spreadsheet output stay unchanged.
 * {@code FoodNutritionResolver} is responsible for selecting one matching recipe and converting
 * these raw values to a {@code FoodNutritionProfile} percentage profile.</p>
 */
public class NutritionRecipe implements Recipe<Inventory> {
    private final float fat,carbohydrate,protein,vegetable;
    protected final ResourceLocation id;
    protected final Ingredient ingredient;

    public static RegistryObject<RecipeSerializer<NutritionRecipe>> SERIALIZER;
    public static RegistryObject<RecipeType<NutritionRecipe>> TYPE;

    public static class Serializer implements RecipeSerializer<NutritionRecipe> {

        @Override
        public NutritionRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            JsonObject group = GsonHelper.getAsJsonObject(json, "group", new JsonObject());
            Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(GsonHelper.getAsString(json, "item", "")));
            float fat = GsonHelper.getAsFloat(group, "fat", 0);
            float carbohydrate = GsonHelper.getAsFloat(group, "carbohydrate", 0);
            float protein = GsonHelper.getAsFloat(group, "protein", 0);
            float vegetable = GsonHelper.getAsFloat(group, "vegetable", 0);
            return new NutritionRecipe(recipeId,fat,carbohydrate,protein,vegetable,Ingredient.of(item));
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, NutritionRecipe recipe) {
            recipe.getIngredient().toNetwork(buffer);
            buffer.writeFloat(recipe.fat);
            buffer.writeFloat(recipe.carbohydrate);
            buffer.writeFloat(recipe.protein);
            buffer.writeFloat(recipe.vegetable);
        }

        @javax.annotation.Nullable
        @Override
        public NutritionRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf byteBuf) {
            Ingredient ingredient = Ingredient.fromNetwork(byteBuf);
            float fat = byteBuf.readFloat();
            float carbohydrate = byteBuf.readFloat();
            float protein = byteBuf.readFloat();
            float vegetable = byteBuf.readFloat();
            return new NutritionRecipe(recipeId,fat,carbohydrate,protein,vegetable,  ingredient);
        }

    }

    public NutritionRecipe(ResourceLocation id, float fat, float carbohydrate, float protein, float vegetable, Ingredient ingredient) {
        super();
        this.fat = fat;
        this.carbohydrate = carbohydrate;
        this.protein = protein;
        this.vegetable = vegetable;
        this.id = id;
        this.ingredient = ingredient;
    }

    public Ingredient getIngredient() {
        return ingredient;
    }


    public boolean conform(ItemStack conformStack) {
        return ingredient.test(conformStack);
    }

    public boolean conform(Item conformItem) {
        if (conformItem == null) {
            return false;
        } else if (conformItem == Items.AIR) {
            return false;
        }
        for (ItemStack stack : ingredient.getItems()) {
            if (stack.getItem() == conformItem) {
                return true;
            }
        }
        return false;
    }
    @Override
    public boolean matches(Inventory iInventory, Level world) {
        return false;
    }

    @Override
    public ItemStack assemble(Inventory inventory, RegistryAccess registryAccess) {
        return null;
    }

    @Override
    public boolean canCraftInDimensions(int p_43999_, int p_44000_) {
        return false;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        return ItemStack.EMPTY;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return TYPE.get();
    }

    /** @return raw fat value before the resolver's {@code /400} conversion */
    public float rawFat() {
        return fat;
    }

    /** @return raw carbohydrate value before the resolver's {@code /400} conversion */
    public float rawCarbohydrate() {
        return carbohydrate;
    }

    /** @return raw protein value before the resolver's {@code /400} conversion */
    public float rawProtein() {
        return protein;
    }

    /** @return raw vegetable value before the resolver's {@code /400} conversion */
    public float rawVegetable() {
        return vegetable;
    }
}
