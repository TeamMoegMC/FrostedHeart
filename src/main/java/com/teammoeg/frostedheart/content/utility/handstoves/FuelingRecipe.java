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

package com.teammoeg.frostedheart.content.utility.handstoves;

import javax.annotation.Nullable;

import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.bootstrap.common.FHItems;
import com.teammoeg.chorda.util.io.JsonHelper;

import blusunrize.immersiveengineering.api.crafting.IERecipeSerializer;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.crafting.conditions.ICondition.IContext;
import net.minecraftforge.registries.RegistryObject;

public class FuelingRecipe extends CustomRecipe {
    public static class Serializer extends IERecipeSerializer<FuelingRecipe> {
        @Override
        public ItemStack getIcon() {
            return new ItemStack(FHItems.hand_stove.get());
        }

        @Nullable
        @Override
        public FuelingRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
            Ingredient input = Ingredient.fromNetwork(buffer);
            int f = buffer.readVarInt();
            return new FuelingRecipe(recipeId, input, f);
        }

        @Override
        public FuelingRecipe readFromJson(ResourceLocation recipeId, JsonObject json,IContext ctx) {
            Ingredient input = Ingredient.fromJson(json.get("input"));
            int fuel = JsonHelper.getIntOrDefault(json, "fuel", 400);
            return new FuelingRecipe(recipeId, input, fuel);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, FuelingRecipe recipe) {
            recipe.type.toNetwork(buffer);
            buffer.writeVarInt(recipe.fuel);
        }
    }

    public static RegistryObject<IERecipeSerializer<FuelingRecipe>> SERIALIZER;

    Ingredient type;
    int fuel;

    protected FuelingRecipe(ResourceLocation id, Ingredient t, int d) {
        super(id,CraftingBookCategory.EQUIPMENT);
        type = t;
        fuel = d;
    }

    /**
     * Used to determine if this recipe can fit in a grid of the given width/height
     */
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    /**
     * Returns an Item that is the result of this recipe
     */
    public ItemStack assemble(CraftingContainer inv, RegistryAccess pRegistryAccess) {
        ItemStack buffstack = ItemStack.EMPTY;
        ItemStack armoritem = ItemStack.EMPTY;
        for (int i = 0; i < inv.getContainerSize(); ++i) {
            ItemStack itemstack = inv.getItem(i);
            if (itemstack != null && !itemstack.isEmpty()) {
                if (type.test(itemstack)) {
                    if (!buffstack.isEmpty()) return ItemStack.EMPTY;
                    buffstack = itemstack;
                } else {
                    if (!armoritem.isEmpty()) return ItemStack.EMPTY;
                    if (itemstack.getItem() instanceof CoalHandStove)
                        armoritem = itemstack;
                }
            }
        }

        if (!armoritem.isEmpty() && !buffstack.isEmpty() && CoalHandStove.getAshAmount(armoritem) < 800 && CoalHandStove.getFuelAmount(armoritem) + fuel <= CoalHandStove.max_fuel) {
            ItemStack ret = armoritem.copy();
            CoalHandStove.setFuelAmount(ret, CoalHandStove.getFuelAmount(ret) + fuel);
            return ret;
        }
        return ItemStack.EMPTY;
    }

    public int getFuel() {
        return fuel;
    }

    public Ingredient getIngredient() {
        return type;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER.get();
    }

    /**
     * Used to check if a recipe matches current crafting inventory
     */
    public boolean matches(CraftingContainer inv, Level worldIn) {
        boolean hasArmor = false;
        boolean hasItem = false;
        for (int i = 0; i < inv.getContainerSize(); ++i) {
            ItemStack itemstack = inv.getItem(i);
            if (itemstack == null || itemstack.isEmpty()) {
                continue;
            }
            if (type.test(itemstack)) {
                if (hasItem)
                    return false;
                hasItem = true;
            } else {
                if (hasArmor)
                    return false;
                if (itemstack.getItem() instanceof CoalHandStove)
                    hasArmor = true;
            }
        }
        return hasArmor && hasItem;
    }
}