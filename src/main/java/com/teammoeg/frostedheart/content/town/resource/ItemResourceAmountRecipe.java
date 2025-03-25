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

package com.teammoeg.frostedheart.content.town.resource;

import blusunrize.immersiveengineering.api.crafting.IERecipeSerializer;
import blusunrize.immersiveengineering.api.crafting.IERecipeTypes;
import blusunrize.immersiveengineering.api.crafting.IESerializableRecipe;
import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.common.util.Lazy;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

import static net.minecraft.world.item.crafting.ShapedRecipe.itemStackFromJson;

/**
 * 表示物品能转换为多少某类型资源。
 * 若物品有某个ItemResourceKey的Tag，但是没有表示转换量的recipe，则默认为1.
 * 关于合成json的写法。可参考 src.main.resources.data.frostedneart.recipes.town_resource.test_bedrock.json
 */
public class ItemResourceAmountRecipe extends IESerializableRecipe {
    public static RegistryObject<RecipeType<ItemResourceAmountRecipe>> TYPE;
    public static RegistryObject<IERecipeSerializer<ItemResourceAmountRecipe>> SERIALIZER;
    public static Lazy<IERecipeTypes.TypeWithClass<ItemResourceAmountRecipe>> IEType = Lazy.of(() -> new IERecipeTypes.TypeWithClass<>(TYPE, ItemResourceAmountRecipe.class));
    /**
     * 物品
     */
    public final ItemStack item;
    /**
     * 要转化为的ItemResourceKey对应的Tag
     */
    public final TagKey<Item> resourceTagKey;
    /**
     * 转化数量
     * 可为小数，但不应为负数
     */
    public final float amount;

    public ItemResourceAmountRecipe(ResourceLocation id, ItemStack item, TagKey<Item> resourceTagKey, float amount) {
        super(Lazy.of(() -> ItemStack.EMPTY), IEType.get(), id);
        this.item = item;
        this.resourceTagKey = resourceTagKey;
        this.amount = amount;
    }

    @Override
    protected IERecipeSerializer<?> getIESerializer() {
        return SERIALIZER.get();
    }

    @Override
    public @NotNull ItemStack getResultItem(@NotNull RegistryAccess registryAccess) {
        return ItemStack.EMPTY;
    }

    public static class Serializer extends IERecipeSerializer<ItemResourceAmountRecipe> {
        @Override
        public ItemStack getIcon() {
            return new ItemStack(FHBlocks.WAREHOUSE.get());
        }

        @Nullable
        @Override
        public ItemResourceAmountRecipe fromNetwork(@NotNull ResourceLocation recipeId, FriendlyByteBuf buffer) {
            ItemStack item = buffer.readItem();
            TagKey<Item> itemOfResourceKey = ItemTags.create(buffer.readResourceLocation());
            float amount = buffer.readFloat();
            return new ItemResourceAmountRecipe(recipeId, item, itemOfResourceKey, amount);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, ItemResourceAmountRecipe recipe) {
            buffer.writeItem(recipe.item);
            buffer.writeResourceLocation(recipe.resourceTagKey.location());
            buffer.writeFloat(recipe.amount);
        }

        @Override
        public ItemResourceAmountRecipe readFromJson(ResourceLocation recipeId, JsonObject json, ICondition.IContext ctx) {
            ItemStack item = itemStackFromJson(json);//readOutput(json.get("item")).get();
            //itemStackFromJson(outputObject.getAsJsonObject())
            TagKey<Item> resourceTagKey = ItemTags.create(new ResourceLocation(GsonHelper.getAsString(json, "resourceTagKey")));
            float amount = GsonHelper.getAsFloat(json, "amount");
            return new ItemResourceAmountRecipe(recipeId, item, resourceTagKey, amount);
        }


    }
}
