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

package com.teammoeg.frostedheart.events;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.teammoeg.frostedheart.compat.jei.JEICompat;
import com.teammoeg.frostedheart.content.climate.heatdevice.generator.GeneratorSteamRecipe;
import com.teammoeg.frostedheart.recipes.CampfireDefrostRecipe;
import com.teammoeg.frostedheart.recipes.DietValueRecipe;
import com.teammoeg.frostedheart.recipes.InstallInnerRecipe;
import com.teammoeg.frostedheart.content.trade.policy.TradePolicy;

import blusunrize.immersiveengineering.api.ApiUtils;
import blusunrize.immersiveengineering.api.utils.TagUtils;
import blusunrize.immersiveengineering.common.blocks.multiblocks.StaticTemplateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.item.crafting.AbstractCookingRecipe;
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

    RecipeManager clientRecipeManager;

    public static void buildRecipeLists(RecipeManager recipeManager) {
        Collection<IRecipe<?>> recipes = recipeManager.getRecipes();
        if (recipes.isEmpty())
            return;
        //filterRecipes(recipes, GeneratorRecipe.class, GeneratorRecipe.TYPE);
        GeneratorSteamRecipe.recipeList = filterRecipes(recipes, GeneratorSteamRecipe.class, GeneratorSteamRecipe.TYPE);
        InstallInnerRecipe.recipeList = recipes.stream()
                .filter(iRecipe -> iRecipe.getClass() == InstallInnerRecipe.class)
                .map(e -> (InstallInnerRecipe) e)
                .collect(Collectors.<InstallInnerRecipe, ResourceLocation, InstallInnerRecipe>toMap(InstallInnerRecipe::getBuffType, recipe -> recipe));
        CampfireDefrostRecipe.recipeList = recipes.stream()
                .filter(iRecipe -> iRecipe.getClass() == CampfireDefrostRecipe.class)
                .map(e -> (CampfireDefrostRecipe) e)
                .collect(Collectors.toMap(AbstractCookingRecipe::getId, recipe -> recipe));
        DietValueRecipe.recipeList = filterRecipes(recipes,DietValueRecipe.class,DietValueRecipe.TYPE).values().stream()
                .filter(iRecipe -> iRecipe.getClass() == DietValueRecipe.class)
                .map(e -> e)
                .collect(Collectors.toMap(recipe -> recipe.item, recipe -> recipe));
       // InspireRecipe.recipes = filterRecipes(recipes, InspireRecipe.class, InspireRecipe.TYPE).values().stream().collect(Collectors.toList());
        //ResearchPaperRecipe.recipes = filterRecipes(recipes, ResearchPaperRecipe.class, ResearchPaperRecipe.TYPE).values().stream().collect(Collectors.toList());
       // SaunaRecipe.recipeList = filterRecipes(recipes, SaunaRecipe.class, SaunaRecipe.TYPE);
        //IncubateRecipe.recipeList = filterRecipes(recipes, IncubateRecipe.class, IncubateRecipe.TYPE);
        TradePolicy.policies = filterRecipes(recipes, TradePolicy.class, TradePolicy.TYPE).values().stream().collect(Collectors.toMap(TradePolicy::getName, t -> t));
        //System.out.println(TradePolicy.policies.size());
        TradePolicy.items = TradePolicy.policies.values().stream().map(TradePolicy::asWeight).filter(Objects::nonNull).collect(Collectors.toList());
        //System.out.println(TradePolicy.items.size());
        TradePolicy.totalW = TradePolicy.items.stream().mapToInt(w -> w.itemWeight).sum();
        //System.out.println(TradePolicy.totalW);
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> JEICompat::scheduleSyncJEI);
    }
    

    static <R extends IRecipe<?>> Map<ResourceLocation, R> filterRecipes(Collection<IRecipe<?>> recipes, Class<R> recipeClass) {
        return recipes.stream()
                .filter(iRecipe -> iRecipe.getClass() == recipeClass)
                .map(recipeClass::cast)
                .collect(Collectors.toMap(recipe -> recipe.getId(), recipe -> recipe));
    }

    static <R extends IRecipe<?>> Map<ResourceLocation, R> filterRecipes(Collection<IRecipe<?>> recipes, Class<R> recipeClass, IRecipeType<R> recipeType) {
        return recipes.stream()
                .filter(iRecipe -> iRecipe.getType() == recipeType)
                .map(recipeClass::cast)
                .collect(Collectors.toMap(recipe -> recipe.getId(), recipe -> recipe));
    }

    public FHRecipeReloadListener(DataPackRegistries dataPackRegistries) {
        this.dataPackRegistries = dataPackRegistries;
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onRecipesUpdated(RecipesUpdatedEvent event) {
        clientRecipeManager = event.getRecipeManager();
        if (!Minecraft.getInstance().isSingleplayer())
            buildRecipeLists(clientRecipeManager);

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

    @SubscribeEvent
    public void onTagsUpdated(TagsUpdatedEvent event) {
        if (clientRecipeManager != null)
            TagUtils.setTagCollectionGetters(ItemTags::getCollection, BlockTags::getCollection);
    }
}
