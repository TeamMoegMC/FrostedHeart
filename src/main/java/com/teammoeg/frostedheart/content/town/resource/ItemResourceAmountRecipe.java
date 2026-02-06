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

package com.teammoeg.frostedheart.content.town.resource;

import blusunrize.immersiveengineering.api.crafting.IERecipeSerializer;
import blusunrize.immersiveengineering.api.crafting.IERecipeTypes;
import blusunrize.immersiveengineering.api.crafting.IESerializableRecipe;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.FHMain;
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
 * 关于合成json的写法。可参考 src.main.resources.data.frostedneart.recipes.town_resource.example.json
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
            Map<ItemStack, Map<TagKey<Item>, Float>> data = new HashMap<>();
            //某个物品的转换到各个ItemResourceAttribute的值
            JsonArray dataArray = json.getAsJsonArray("byItem");
            if(dataArray != null){
                for (JsonElement element : dataArray) {
                    JsonObject elementObject = element.getAsJsonObject();
                    //读取物品
                    JsonObject itemJson;
                    if (elementObject.has("itemStack")) {
                        itemJson = elementObject.getAsJsonObject("itemStack");
                    } else {
                        // 当不存在"itemStack"key，但存在"item"key时，直接把entry本身输入进itemStackFromJson里
                        itemJson = elementObject;
                    }
                    ItemStack itemStack = itemStackFromJson(itemJson);

                    //读取物品转换的资源
                    JsonArray valueArray = elementObject.getAsJsonArray("values");
                    for (JsonElement element1 : valueArray) {
                        JsonObject element1Object = element1.getAsJsonObject();
                        //读取资源类型
                        TagKey<Item> tag = ItemTags.create(new ResourceLocation(element1Object.get("tag").getAsString()));
                        //读取转换数值
                        float value = element1Object.get("amount").getAsFloat();
                        //获取已读取的单个物品的数据并融合
                        Map<TagKey<Item>, Float> dataOfItem = data.computeIfAbsent(itemStack, k -> new HashMap<>());
                        if(dataOfItem.containsKey(tag)){
                            FHMain.LOGGER.warn("ItemResourceAmountRecipe: Trying to add ItemResourceAmount data, but there is already another data existed!\n{\nItem: {}\nTag: {}\nOld Amount: {}\nNew Amount: {}\n}1", itemStack.getDisplayName().getString(), tag.location(), dataOfItem.get(tag), value);
                        }
                        dataOfItem.put(tag, value);

                    }
                }
            }
            //某个ItemResourceAttribute被各个物品转换的值。只是另一种写法，没有本质区别。
            dataArray = json.getAsJsonArray("byTag");
            if(dataArray != null){
                for (JsonElement element : dataArray) {
                    JsonObject elementObject = element.getAsJsonObject();
                    //读取tag数据
                    TagKey<Item> tag = ItemTags.create(new ResourceLocation(elementObject.get("tag").getAsString()));
                    JsonArray valueArray = elementObject.getAsJsonArray("values");
                    for (JsonElement element1: valueArray) {
                        //读取物品
                        JsonObject element1Object = element1.getAsJsonObject();
                        JsonObject itemJson;
                        if (element1Object.has("itemStack")) {
                            itemJson = element1Object.getAsJsonObject("itemStack");
                        } else {
                            // 当不存在"itemStack"key，但存在"item"key时，直接把entry本身输入进itemStackFromJson里
                            itemJson = element1Object;
                        }
                        ItemStack itemStack = itemStackFromJson(itemJson);
                        
                        //读取转换数值
                        Float value = element1Object.get("amount").getAsFloat();
                        //获取已读取的单个物品的数据并融合
                        data.computeIfAbsent(itemStack, k -> new HashMap<>()).put(tag, value);
                        if(data.get(itemStack).containsKey(tag)){
                            FHMain.LOGGER.warn("ItemResourceAmountRecipe: Trying to add ItemResourceAmount data, but there is already another data existed!\n{\nItem: {}\nTag: {}\nOld Amount: {}\nNew Amount: {}\n}2", itemStack.getDisplayName().getString(), tag.location(), data.get(itemStack).get(tag), value);
                        }
                        data.get(itemStack).put(tag, value);

                    }
                }
            }
            JsonArray objectList = json.getAsJsonArray("commonList");
            if(objectList != null){
                for (JsonElement element : objectList){
                    JsonObject object = element.getAsJsonObject();
                    JsonObject itemJson;
                    if (object.has("itemStack")) {
                        itemJson = object.getAsJsonObject("itemStack");
                    } else {
                        // 当不存在"itemStack"key，但存在"item"key时，直接把object本身输入进itemStackFromJson里
                        itemJson = object;
                    }
                    ItemStack itemStack = itemStackFromJson(itemJson);
                    TagKey<Item> tag = ItemTags.create(new ResourceLocation(object.get("tag").getAsString()));
                    float value = object.get("amount").getAsFloat();
                    data.computeIfAbsent(itemStack, k -> new HashMap<>()).put(tag, value);
                }
            }
            return new ItemResourceAmountRecipe(recipeId, data);
        }
    }
}