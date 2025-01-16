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

package com.teammoeg.frostedheart.content.climate.recipe;

import java.util.Collections;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.common.base.Optional;
import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.bootstrap.common.FHItems;
import com.teammoeg.chorda.util.RegistryUtils;
import com.teammoeg.chorda.util.io.JsonHelper;

import blusunrize.immersiveengineering.api.crafting.IERecipeSerializer;
import blusunrize.immersiveengineering.common.util.ItemNBTHelper;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.crafting.conditions.ICondition.IContext;
import net.minecraftforge.registries.RegistryObject;

public class InstallInnerRecipe extends CustomRecipe {
    public static class Serializer extends IERecipeSerializer<InstallInnerRecipe> {
        @Override
        public ItemStack getIcon() {
            return new ItemStack(FHItems.buff_coat.get());
        }

        @Nullable
        @Override
        public InstallInnerRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
            Ingredient input = Ingredient.fromNetwork(buffer);
            int dura = buffer.readVarInt();
            return new InstallInnerRecipe(recipeId, input, dura);
        }

        @Override
        public InstallInnerRecipe readFromJson(ResourceLocation recipeId, JsonObject json,IContext ctx) {
            Ingredient input = Ingredient.fromJson(json.get("input"));
            int dura = JsonHelper.getIntOrDefault(json, "durable", 100);
            return new InstallInnerRecipe(recipeId, input, dura);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, InstallInnerRecipe recipe) {
            recipe.type.toNetwork(buffer);
            buffer.writeVarInt(recipe.durability);
        }
    }

    public static RegistryObject<IERecipeSerializer<InstallInnerRecipe>> SERIALIZER;

    public static Map<ResourceLocation, InstallInnerRecipe> recipeList = Collections.emptyMap();

    Ingredient type;

    int durability;

    protected InstallInnerRecipe(ResourceLocation id, Ingredient t, int d) {
        super(id, CraftingBookCategory.EQUIPMENT);
        type = t;
        durability = d;
    }
    /**
     * Used to determine if this recipe can fit in a grid of the given width/height
     */
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    public ResourceLocation getBuffType() {
        return Optional.fromNullable(type.getItems()[0]).transform(e -> RegistryUtils.getRegistryName(e.getItem()))
                .or(new ResourceLocation("minecraft", "air"));
    }

    /**
     * Returns an Item that is the result of this recipe
     */
    public ItemStack assemble(CraftingContainer inv,RegistryAccess registry) {
        ItemStack buffstack = ItemStack.EMPTY;
        ItemStack armoritem = ItemStack.EMPTY;
        for (int i = 0; i < inv.getContainerSize(); ++i) {
            ItemStack itemstack = inv.getItem(i);
            if (itemstack != null && !itemstack.isEmpty()) {
                if (type.test(itemstack)) {
                    if (!buffstack.isEmpty())
                        return ItemStack.EMPTY;
                    buffstack = itemstack;
                } else {
                    if (!armoritem.isEmpty())
                        return ItemStack.EMPTY;
                    EquipmentSlot type = Mob.getEquipmentSlotForItem(itemstack);
                    if (type != null && type != EquipmentSlot.MAINHAND && type != EquipmentSlot.OFFHAND)
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
            CompoundTag nbt = buffstack.getTag();
            ret.getTag().put("inner_cover_tag", nbt != null ? nbt : new CompoundTag());
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
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer inv) {
        return NonNullList.withSize(inv.getContainerSize(), ItemStack.EMPTY);
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
                EquipmentSlot type = Mob.getEquipmentSlotForItem(itemstack);
                if (type != null && type != EquipmentSlot.MAINHAND && type != EquipmentSlot.OFFHAND)
                    if (itemstack.hasTag()) {
                        if (!itemstack.getTag().getString("inner_cover").isEmpty()) return false;
                    }
                hasArmor = true;
            }
        }
        return hasArmor && hasItem;
    }

    public boolean matches(ItemStack itemstack) {
        EquipmentSlot type = Mob.getEquipmentSlotForItem(itemstack);
        return type != null && type != EquipmentSlot.MAINHAND && type != EquipmentSlot.OFFHAND;
    }

}