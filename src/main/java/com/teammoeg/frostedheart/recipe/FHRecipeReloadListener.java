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

package com.teammoeg.frostedheart.recipe;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.teammoeg.frostedheart.compat.jei.JEICompat;
import com.teammoeg.frostedheart.content.generator.GeneratorRecipe;
import com.teammoeg.frostedheart.content.generator.GeneratorSteamRecipe;
import com.teammoeg.frostedheart.content.incubator.IncubateRecipe;
import com.teammoeg.frostedheart.content.recipes.CampfireDefrostRecipe;
import com.teammoeg.frostedheart.content.recipes.DefrostRecipe;
import com.teammoeg.frostedheart.content.recipes.DietValueRecipe;
import com.teammoeg.frostedheart.content.recipes.InspireRecipe;
import com.teammoeg.frostedheart.content.recipes.ResearchPaperRecipe;
import com.teammoeg.frostedheart.content.recipes.InstallInnerRecipe;
import com.teammoeg.frostedheart.content.recipes.SmokingDefrostRecipe;
import com.teammoeg.frostedheart.content.steamenergy.charger.ChargerRecipe;
import com.teammoeg.frostedheart.content.steamenergy.sauna.SaunaRecipe;
import com.teammoeg.frostedheart.trade.TradePolicy;

import blusunrize.immersiveengineering.api.ApiUtils;
import blusunrize.immersiveengineering.api.utils.TagUtils;
import blusunrize.immersiveengineering.common.blocks.multiblocks.StaticTemplateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.item.crafting.RecipeManager;
import net.minecraft.resources.DataPackRegistries;
import net.minecraft.resources.IResourceManager;
import net.minecraft.resources.IResourceManagerReloadListener;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RecipesUpdatedEvent;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

public class FHRecipeReloadListener implements IResourceManagerReloadListener {
    private final DataPackRegistries dataPackRegistries;

    public FHRecipeReloadListener(DataPackRegistries dataPackRegistries) {
        this.dataPackRegistries = dataPackRegistries;
    }

    @Override
    public void onResourceManagerReload(@Nonnull IResourceManager resourceManager) {
        if (dataPackRegistries != null) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                Iterator<ServerWorld> it = server.getWorlds().iterator();
                // Should only be false when no players are loaded, so the data will be synced on login
                if (it.hasNext())
                    ApiUtils.addFutureServerTask(it.next(),
                            () -> StaticTemplateManager.syncMultiblockTemplates(PacketDistributor.ALL.noArg(), true)
                    );
            }
        }
    }

    RecipeManager clientRecipeManager;

    @SubscribeEvent
    public void onTagsUpdated(TagsUpdatedEvent event) {
        if (clientRecipeManager != null)
            TagUtils.setTagCollectionGetters(ItemTags::getCollection, BlockTags::getCollection);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onRecipesUpdated(RecipesUpdatedEvent event) {
        clientRecipeManager = event.getRecipeManager();
        if (!Minecraft.getInstance().isSingleplayer())
            buildRecipeLists(clientRecipeManager);
        
    }

    public static void buildRecipeLists(RecipeManager recipeManager) {
        Collection<IRecipe<?>> recipes = recipeManager.getRecipes();
        if (recipes.size() == 0)
            return;
        GeneratorRecipe.recipeList = filterRecipes(recipes, GeneratorRecipe.class, GeneratorRecipe.TYPE);
        ChargerRecipe.recipeList = filterRecipes(recipes, ChargerRecipe.class, ChargerRecipe.TYPE);
        GeneratorSteamRecipe.recipeList = filterRecipes(recipes, GeneratorSteamRecipe.class, GeneratorSteamRecipe.TYPE);
        InstallInnerRecipe.recipeList = recipes.stream()
                .filter(iRecipe -> iRecipe.getClass() == InstallInnerRecipe.class)
                .map(e -> (InstallInnerRecipe) e)
                .collect(Collectors.<InstallInnerRecipe, ResourceLocation, InstallInnerRecipe>toMap(recipe -> recipe.getBuffType(), recipe -> recipe));
        CampfireDefrostRecipe.recipeList = recipes.stream()
                .filter(iRecipe -> iRecipe.getClass() == CampfireDefrostRecipe.class)
                .map(e -> (CampfireDefrostRecipe) e)
                .collect(Collectors.toMap(recipe -> recipe.getId(), recipe -> recipe));
        SmokingDefrostRecipe.recipeList = recipes.stream()
                .filter(iRecipe -> iRecipe.getClass() == SmokingDefrostRecipe.class)
                .map(e -> (DefrostRecipe) e)
                .collect(Collectors.toMap(recipe -> recipe.getId(), recipe -> recipe));
        DietValueRecipe.recipeList = recipes.stream()
                .filter(iRecipe -> iRecipe.getClass() == DietValueRecipe.class)
                .map(e -> (DietValueRecipe) e)
                .collect(Collectors.toMap(recipe -> recipe.item, recipe -> recipe));
        InspireRecipe.recipes=filterRecipes(recipes, InspireRecipe.class, InspireRecipe.TYPE).values().stream().collect(Collectors.toList());
        ResearchPaperRecipe.recipes = filterRecipes(recipes, ResearchPaperRecipe.class, ResearchPaperRecipe.TYPE).values().stream().collect(Collectors.toList());
        SaunaRecipe.recipeList = filterRecipes(recipes, SaunaRecipe.class, SaunaRecipe.TYPE);
        IncubateRecipe.recipeList=filterRecipes(recipes, IncubateRecipe.class, IncubateRecipe.TYPE);
        TradePolicy.policies=filterRecipes(recipes,TradePolicy.class,TradePolicy.TYPE).values().stream().collect(Collectors.toMap(t->t.getName(),t->t));
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> JEICompat::scheduleSyncJEI);
        
    }

    static <R extends IRecipe<?>> Map<ResourceLocation, R> filterRecipes(Collection<IRecipe<?>> recipes, Class<R> recipeClass, IRecipeType<R> recipeType) {
        return recipes.stream()
                .filter(iRecipe -> iRecipe.getType() == recipeType)
                .map(recipeClass::cast)
                .collect(Collectors.toMap(recipe -> recipe.getId(), recipe -> recipe));
    }

    static <R extends IRecipe<?>> Map<ResourceLocation, R> filterRecipes(Collection<IRecipe<?>> recipes, Class<R> recipeClass) {
        return recipes.stream()
                .filter(iRecipe -> iRecipe.getClass() == recipeClass)
                .map(recipeClass::cast)
                .collect(Collectors.toMap(recipe -> recipe.getId(), recipe -> recipe));
    }
}
