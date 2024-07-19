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

package com.teammoeg.frostedheart.recipes;

import java.util.Collections;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.common.base.Optional;
import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.FHItems;
import com.teammoeg.frostedheart.util.RegistryUtils;
import com.teammoeg.frostedheart.util.io.JsonHelper;

import blusunrize.immersiveengineering.api.crafting.IERecipeSerializer;
import blusunrize.immersiveengineering.common.util.ItemNBTHelper;
import net.minecraft.entity.MobEntity;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.SpecialRecipe;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.RegistryObject;

public class InstallInnerRecipe extends SpecialRecipe {
    public static class Serializer extends IERecipeSerializer<InstallInnerRecipe> {
        @Override
        public ItemStack getIcon() {
            return new ItemStack(FHItems.buff_coat.get());
        }

        @Nullable
        @Override
        public InstallInnerRecipe read(ResourceLocation recipeId, PacketBuffer buffer) {
            Ingredient input = Ingredient.read(buffer);
            int dura = buffer.readVarInt();
            return new InstallInnerRecipe(recipeId, input, dura);
        }

        @Override
        public InstallInnerRecipe readFromJson(ResourceLocation recipeId, JsonObject json) {
            Ingredient input = Ingredient.deserialize(json.get("input"));
            int dura = JsonHelper.getIntOrDefault(json, "durable", 100);
            return new InstallInnerRecipe(recipeId, input, dura);
        }

        @Override
        public void write(PacketBuffer buffer, InstallInnerRecipe recipe) {
            recipe.type.write(buffer);
            buffer.writeVarInt(recipe.durability);
        }
    }

    public static RegistryObject<IERecipeSerializer<InstallInnerRecipe>> SERIALIZER;

    public static Map<ResourceLocation, InstallInnerRecipe> recipeList = Collections.emptyMap();

    Ingredient type;

    int durability;

    protected InstallInnerRecipe(ResourceLocation id, Ingredient t, int d) {
        super(id);
        type = t;
        durability = d;
    }
    /**
     * Used to determine if this recipe can fit in a grid of the given width/height
     */
    public boolean canFit(int width, int height) {
        return width * height >= 2;
    }

    public ResourceLocation getBuffType() {
        return Optional.fromNullable(type.getMatchingStacks()[0]).transform(e -> RegistryUtils.getRegistryName(e.getItem()))
                .or(new ResourceLocation("minecraft", "air"));
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
                    if (!buffstack.isEmpty())
                        return ItemStack.EMPTY;
                    buffstack = itemstack;
                } else {
                    if (!armoritem.isEmpty())
                        return ItemStack.EMPTY;
                    EquipmentSlotType type = MobEntity.getSlotForItemStack(itemstack);
                    if (type != null && type != EquipmentSlotType.MAINHAND && type != EquipmentSlotType.OFFHAND)
                        if (itemstack.hasTag()) {
                            if (!itemstack.getTag().getString("inner_cover").isEmpty()) return ItemStack.EMPTY;
                        }
                    armoritem = itemstack;
                }
            }
        }

        if (!armoritem.isEmpty() && !buffstack.isEmpty()) {
            ItemStack ret = armoritem.copy();
            ret.setCount(1);
            ItemNBTHelper.putString(ret, "inner_cover", RegistryUtils.getRegistryName(buffstack.getItem()).toString());
            CompoundNBT nbt = buffstack.getTag();
            ret.getTag().put("inner_cover_tag", nbt != null ? nbt : new CompoundNBT());
            return ret;
        }
        return ItemStack.EMPTY;
    }

    public int getDurability() {
        return durability;
    }

    public Ingredient getIngredient() {
        return type;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInventory inv) {
        return NonNullList.withSize(inv.getSizeInventory(), ItemStack.EMPTY);
    }

    @Override
    public IRecipeSerializer<?> getSerializer() {
        return SERIALIZER.get();
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
                EquipmentSlotType type = MobEntity.getSlotForItemStack(itemstack);
                if (type != null && type != EquipmentSlotType.MAINHAND && type != EquipmentSlotType.OFFHAND)
                    if (itemstack.hasTag()) {
                        if (!itemstack.getTag().getString("inner_cover").isEmpty()) return false;
                    }
                hasArmor = true;
            }
        }
        return hasArmor && hasItem;
    }

    public boolean matches(ItemStack itemstack) {
        EquipmentSlotType type = MobEntity.getSlotForItemStack(itemstack);
        return type != null && type != EquipmentSlotType.MAINHAND && type != EquipmentSlotType.OFFHAND;
    }

}