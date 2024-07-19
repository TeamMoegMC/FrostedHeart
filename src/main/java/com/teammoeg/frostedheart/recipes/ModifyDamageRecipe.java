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

import javax.annotation.Nullable;

import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.util.io.JsonHelper;

import blusunrize.immersiveengineering.api.crafting.IERecipeSerializer;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapelessRecipe;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.RegistryObject;

public class ModifyDamageRecipe extends ShapelessRecipe {
    public static class Serializer extends IERecipeSerializer<ModifyDamageRecipe> {
        @Override
        public ItemStack getIcon() {
            return new ItemStack(Items.CRAFTING_TABLE);
        }

        @Nullable
        @Override
        public ModifyDamageRecipe read(ResourceLocation recipeId, PacketBuffer buffer) {
            Ingredient input = Ingredient.read(buffer);
            Ingredient input2 = Ingredient.read(buffer);
            int dura = buffer.readVarInt();
            return new ModifyDamageRecipe(recipeId, input, input2, dura);
        }

        @Override
        public ModifyDamageRecipe readFromJson(ResourceLocation recipeId, JsonObject json) {
            Ingredient input = Ingredient.deserialize(json.get("tool"));
            Ingredient input2 = Ingredient.deserialize(json.get("item"));
            int dura = JsonHelper.getIntOrDefault(json, "modify", 1);
            return new ModifyDamageRecipe(recipeId, input, input2, dura);
        }

        @Override
        public void write(PacketBuffer buffer, ModifyDamageRecipe recipe) {
            recipe.tool.write(buffer);
            recipe.repair.write(buffer);

            buffer.writeVarInt(recipe.modify);
        }
    }
    public static RegistryObject<IERecipeSerializer<ModifyDamageRecipe>> SERIALIZER;
    public final Ingredient tool;
    public final Ingredient repair;

    public final int modify;

    public ModifyDamageRecipe(ResourceLocation idIn, Ingredient torepair, Ingredient material, int mod) {
        super(idIn, "", torepair.getMatchingStacks()[0], NonNullList.from(Ingredient.EMPTY, torepair, material));
        repair = material;
        tool = torepair;
        modify = mod;
    }

    /**
     * Used to determine if this recipe can fit in a grid of the given width/height
     */
    public boolean canFit(int width, int height) {
        return width * height >= 2;
    }

    /**
     * Returns an Item that is the result of this recipe
     */
    public ItemStack getCraftingResult(CraftingInventory inv) {
        for (int j = 0; j < inv.getSizeInventory(); ++j) {
            ItemStack in = inv.getStackInSlot(j);
            if (tool.test(in)) {
                in = in.copy();
                in.setDamage(in.getDamage() - modify);
                return in;
            }
        }

        return ItemStack.EMPTY;
    }

    public IRecipeSerializer<?> getSerializer() {
        return SERIALIZER.get();
    }

    public boolean matches(CraftingInventory inv, World worldIn) {
        boolean hasArmor = false;
        boolean hasItem = false;
        for (int i = 0; i < inv.getSizeInventory(); ++i) {
            ItemStack itemstack = inv.getStackInSlot(i);
            if (itemstack == null || itemstack.isEmpty()) {
                continue;
            }
            if (repair.test(itemstack)) {
                if (hasItem)
                    return false;
                hasItem = true;
            } else {
                if (hasArmor)
                    return false;
                if (tool.test(itemstack)) {
                    int newdmg = itemstack.getDamage() - modify;
                    if (newdmg < 0 || newdmg > itemstack.getMaxDamage())
                        return false;

                    hasArmor = true;
                }
            }
        }
        return hasArmor && hasItem;
    }

}
