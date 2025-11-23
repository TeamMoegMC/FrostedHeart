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
import com.google.gson.JsonElement;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.common.util.Lazy;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

import java.util.*;
import java.util.function.Function;

import static net.minecraft.world.item.crafting.ShapedRecipe.itemStackFromJson;

/**
 * 表示物品能转换为多少某类型资源。
 * 若物品有某个ItemResourceAttribute的Tag，但是没有表示转换量的recipe，则默认为1.
 * 关于合成json的写法。可参考 src.main.resources.data.frostedneart.recipes.town_resource.test_bedrock.json
 */
public class ItemResourceAmountRecipe extends IESerializableRecipe {
    public static RegistryObject<RecipeType<ItemResourceAmountRecipe>> TYPE;
    public static RegistryObject<IERecipeSerializer<ItemResourceAmountRecipe>> SERIALIZER;
    public static Lazy<IERecipeTypes.TypeWithClass<ItemResourceAmountRecipe>> IEType = Lazy.of(() -> new IERecipeTypes.TypeWithClass<>(TYPE, ItemResourceAmountRecipe.class));
    /**
     * 各个物品转换的资源信息
     */
    public final Map<ItemStack, Map<TagKey<Item>, Float>> data;

    public ItemResourceAmountRecipe(ResourceLocation id, Map<ItemStack, Map<TagKey<Item>, Float>> data) {
        super(Lazy.of(() -> ItemStack.EMPTY), IEType.get(), id);
        this.data = data;
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
            Map<ItemStack, Map<TagKey<Item>, Float>> data = buffer.readMap(FriendlyByteBuf::readItem, Serializer::readSingleData);
            return new ItemResourceAmountRecipe(recipeId, data);
        }
        public static Map<TagKey<Item>, Float> readSingleData(FriendlyByteBuf buffer){
            return buffer.readMap((buf) -> ItemTags.create(buf.readResourceLocation()), FriendlyByteBuf::readFloat);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, ItemResourceAmountRecipe recipe) {
            buffer.writeMap(recipe.data, FriendlyByteBuf::writeItem, Serializer::writeSingleData);
        }
        public static void writeSingleData(FriendlyByteBuf buffer, Map<TagKey<Item>, Float> data){
            buffer.writeMap(data, (buf, tag) -> buf.writeResourceLocation(tag.location()), FriendlyByteBuf::writeFloat);
        }

        @Override
        public ItemResourceAmountRecipe readFromJson(ResourceLocation recipeId, JsonObject json, ICondition.IContext ctx) {
            System.out.println("duck_egg debug: loading ItemResourceAmountRecipe");
            JsonObject dataObject = json.getAsJsonObject("data");
            Map<ItemStack, Map<TagKey<Item>, Float>> data = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : dataObject.entrySet()) {
                JsonObject itemJson = new JsonObject();
                itemJson.addProperty("item", entry.getKey());
                ItemStack itemStack = itemStackFromJson(itemJson);
                JsonObject valueObject = entry.getValue().getAsJsonObject();
                Map<TagKey<Item>, Float> singleData = new HashMap<>();
                for (Map.Entry<String, JsonElement> entry1 : valueObject.entrySet()) {
                    TagKey<Item> tag = ItemTags.create(new ResourceLocation(entry1.getKey()));
                    singleData.put(tag, entry1.getValue().getAsFloat());
                }
                data.put(itemStack, singleData);
            }
            System.out.println("duck_egg debug: loaded ItemResourceAmountRecipe");
            return new ItemResourceAmountRecipe(recipeId, data);
            //下面的已废弃
            //ItemStack item = itemStackFromJson(json);//readOutput(json.get("item")).get();
            //String resourceTagKeyString = GsonHelper.getAsString(json, "resourceTagKey");
            //String[] split = resourceTagKeyString.split(":");
            //TagKey<Item> resourceTagKey = ItemTags.create(Objects.requireNonNull(ResourceLocation.tryBuild(split[0], split[1])));
            //float amount = GsonHelper.getAsFloat(json, "amount");
            //return new ItemResourceAmountRecipe(recipeId, item, resourceTagKey, amount);
        }


    }
}
