/*
 * Copyright (c) 2021 TeamMoeg
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
 */

package com.teammoeg.frostedheart.content.temperature.handstoves;

import javax.annotation.Nullable;

import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.FHItems;
import com.teammoeg.frostedheart.climate.data.JsonHelper;

import blusunrize.immersiveengineering.api.crafting.IERecipeSerializer;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.SpecialRecipe;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.RegistryObject;

public class FuelingRecipe extends SpecialRecipe {
    public static RegistryObject<IERecipeSerializer<FuelingRecipe>> SERIALIZER;

    protected FuelingRecipe(ResourceLocation id, Ingredient t, int d) {
        super(id);
        type = t;
        fuel = d;
    }

    Ingredient type;
    int fuel;

    public Ingredient getIngredient() {
        return type;
    }

    public int getFuel() {
        return fuel;
    }

    /**
     * Used to check if a recipe matches current crafting inventory
     */
    public boolean matches(CraftingInventory inv, World worldIn) {
        boolean hasArmor = false;
        boolean hasItem = false;
        for (int i = 0; i < inv.getSizeInventory(); ++i) {
            ItemStack itemstack = inv.getStackInSlot(i);
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

    /**
     * Returns an Item that is the result of this recipe
     */
    public ItemStack getCraftingResult(CraftingInventory inv) {
        ItemStack buffstack = ItemStack.EMPTY;
        ItemStack armoritem = ItemStack.EMPTY;
        for (int i = 0; i < inv.getSizeInventory(); ++i) {
            ItemStack itemstack = inv.getStackInSlot(i);
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

    /**
     * Used to determine if this recipe can fit in a grid of the given width/height
     */
    public boolean canFit(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public IRecipeSerializer<?> getSerializer() {
        return SERIALIZER.get();
    }

    public static class Serializer extends IERecipeSerializer<FuelingRecipe> {
        @Override
        public ItemStack getIcon() {
            return new ItemStack(FHItems.buff_coat);
        }

        @Override
        public FuelingRecipe readFromJson(ResourceLocation recipeId, JsonObject json) {
            Ingredient input = Ingredient.deserialize(json.get("input"));
            int fuel = JsonHelper.getIntOrDefault(json, "fuel", 400);
            return new FuelingRecipe(recipeId, input, fuel);
        }

        @Nullable
        @Override
        public FuelingRecipe read(ResourceLocation recipeId, PacketBuffer buffer) {
            Ingredient input = Ingredient.read(buffer);
            int f = buffer.readVarInt();
            return new FuelingRecipe(recipeId, input, f);
        }

        @Override
        public void write(PacketBuffer buffer, FuelingRecipe recipe) {
            recipe.type.write(buffer);
            buffer.writeVarInt(recipe.fuel);
        }
    }
}