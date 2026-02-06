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
import com.teammoeg.frostedheart.content.health.capability.ImmutableNutrition;
import com.teammoeg.frostedheart.content.health.capability.Nutrition;
import com.teammoeg.frostedheart.content.health.event.GatherFoodNutritionEvent;

import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;

public class NutritionRecipe implements Recipe<Inventory> {
    public final float fat,carbohydrate,protein,vegetable;
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

    public static NutritionRecipe getRecipe(Level level, ItemStack itemStack) {
        if (level != null) {
            
	        for (NutritionRecipe recipe : level.getRecipeManager().getAllRecipesFor(TYPE.get())) {
	            if (recipe.conform(itemStack)) {
	                return recipe;
	            }
	        }
        }
        return null;
    }

    public static Nutrition getRecipeFromItem(Player player, ItemStack itemStack) {
    	NutritionRecipe rcp=getRecipe(player.level(),itemStack);
    	Nutrition value=Nutrition.ZERO;
        if (rcp != null) {
        	value=rcp.getNutrition();
        }
        return postEvent(value, player.level(), itemStack, player);

    }
    public static Nutrition getRecipeFromItem(Level level, ItemStack itemStack) {
    	NutritionRecipe rcp=getRecipe(level,itemStack);
    	Nutrition value=Nutrition.ZERO;
        if (rcp != null) {
        	value=rcp.getNutrition();
        }
        
        return postEvent(value, level, itemStack, null);

    }
    private static Nutrition postEvent(Nutrition value,Level level,ItemStack itemStack,Player player) {
    	GatherFoodNutritionEvent event=new GatherFoodNutritionEvent(value, level, itemStack, player);
    	if(MinecraftForge.EVENT_BUS.post(event))
    		return null;
    	if(event.isModified()) {
    		return event.getForModify();
    	}
    	return value==Nutrition.ZERO?null:event.getOriginalValue();
    }
    public ImmutableNutrition getNutrition() {
        return new ImmutableNutrition(fat,carbohydrate,protein,vegetable);
    }
}
