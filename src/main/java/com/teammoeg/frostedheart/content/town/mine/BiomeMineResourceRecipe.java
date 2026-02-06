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

package com.teammoeg.frostedheart.content.town.mine;

import blusunrize.immersiveengineering.api.crafting.IERecipeSerializer;
import blusunrize.immersiveengineering.api.crafting.IERecipeTypes;
import blusunrize.immersiveengineering.api.crafting.IESerializableRecipe;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
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
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 描述城镇矿场在特定生物群系中所产出的资源种类及比例。
 */
public class BiomeMineResourceRecipe extends IESerializableRecipe {
    public static RegistryObject<RecipeType<BiomeMineResourceRecipe>> TYPE;
    public static RegistryObject<IERecipeSerializer<BiomeMineResourceRecipe>> SERIALIZER;
    public static Lazy<IERecipeTypes.TypeWithClass<BiomeMineResourceRecipe>> IEType = Lazy.of(() -> new IERecipeTypes.TypeWithClass<>(TYPE, BiomeMineResourceRecipe.class));

    /**
     * 对应生物群系的ID
     */
    public ResourceLocation biomeID;

    /**
     * 可产出的各个资源的权重
     */
    public Map<Item, Integer> weights;

    public BiomeMineResourceRecipe(ResourceLocation id, ResourceLocation biomeID, Map<Item, Integer> weights) {
        super(Lazy.of(() -> ItemStack.EMPTY), IEType.get(), id);
        this.biomeID = biomeID;
        this.weights = weights;
    }

    @Override
    protected IERecipeSerializer<?> getIESerializer() {
        return SERIALIZER.get();
    }

    @Override
    public @NotNull ItemStack getResultItem(@NotNull RegistryAccess pRegistryAccess) {
        return ItemStack.EMPTY;
    }

    public static class Serializer extends IERecipeSerializer<BiomeMineResourceRecipe> {
        @Override
        public ItemStack getIcon() {
            return new ItemStack(FHBlocks.MINE.get());
        }

        @Override
        public BiomeMineResourceRecipe readFromJson(ResourceLocation recipeId, JsonObject jsonObject, ICondition.IContext iContext) {
            ResourceLocation biomeID = new ResourceLocation(GsonHelper.getAsString(jsonObject, "biome"));

            //"weights"内为多个物品-数字键值对，其中物品为物品ID，数字为权重。将其获取后遍历键值对读取。
            JsonObject weightsJson = GsonHelper.getAsJsonObject(jsonObject, "weights");
            Map<Item, Integer> weights = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : weightsJson.entrySet()) {
                ResourceLocation itemLocation = new ResourceLocation(entry.getKey());
                Item item = ForgeRegistries.ITEMS.getValue(itemLocation);
                if (item != null && item != Items.AIR) {
                    int weight = entry.getValue().getAsInt();
                    weights.put(item, weight);
                }
            }

            return new BiomeMineResourceRecipe(recipeId, biomeID, weights);
        }

        @Override
        public @Nullable BiomeMineResourceRecipe fromNetwork(@NotNull ResourceLocation recipeId, FriendlyByteBuf buffer) {
            ResourceLocation biomeID = buffer.readResourceLocation();

            int size = buffer.readVarInt();
            Map<Item, Integer> weights = new HashMap<>(size);

            for (int i = 0; i < size; i++) {
                Item item = ForgeRegistries.ITEMS.getValue(buffer.readResourceLocation());
                int weight = buffer.readVarInt();
                if (item != null && item != Items.AIR) {
                    weights.put(item, weight);
                }
            }

            return new BiomeMineResourceRecipe(recipeId, biomeID, weights);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, BiomeMineResourceRecipe recipe) {
            buffer.writeResourceLocation(recipe.biomeID);

            buffer.writeVarInt(recipe.weights.size());
            for (Map.Entry<Item, Integer> entry : recipe.weights.entrySet()) {
                buffer.writeResourceLocation(Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(entry.getKey())));
                buffer.writeVarInt(entry.getValue());
            }
        }
    }
}
