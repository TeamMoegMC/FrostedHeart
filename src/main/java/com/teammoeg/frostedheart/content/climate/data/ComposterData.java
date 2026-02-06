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

package com.teammoeg.frostedheart.content.climate.data;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.chorda.recipe.CodecRecipeSerializer;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

public record ComposterData(Item item, float value) {
    public static final Codec<ComposterData> CODEC= RecordCodecBuilder.create(t->t.group(
            ForgeRegistries.ITEMS.getCodec().fieldOf("item").forGetter(o->o.item),
            Codec.FLOAT.optionalFieldOf("value",0f).forGetter(o->o.value)).apply(t, ComposterData::new));
    public static RegistryObject<CodecRecipeSerializer<ComposterData>> TYPE;
    private static Map<Item, ComposterData> CACHE = ImmutableMap.of();

    @Nullable
    public static ComposterData getData(Block block) {
        return CACHE.get(block.asItem());
    }

    public static void updateCache(RecipeManager manager) {
        Collection<Recipe<?>> recipes = manager.getRecipes();
        ComposterData.CACHE = ComposterData.TYPE.get().filterRecipes(recipes).collect(Collectors.toMap(t->t.getData().item(), t->t.getData()));
    }

    public FinishedRecipe toFinished(ResourceLocation name) {
        return TYPE.get().toFinished(name, this);
    }
}
