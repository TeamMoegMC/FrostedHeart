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

package com.teammoeg.frostedheart.infrastructure.data;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.teammoeg.chorda.util.struct.EnumDefaultedMap;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.data.*;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData.BodyPart;
import com.teammoeg.frostedheart.content.climate.recipe.CampfireDefrostRecipe;
import com.teammoeg.frostedheart.content.trade.policy.TradePolicy;
import com.teammoeg.frostedheart.content.wheelmenu.WheelMenuRenderer;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.RegistryObject;

public class FHRecipeCachingReloadListener implements ResourceManagerReloadListener {
    private final ReloadableServerResources dataPackRegistries;
    RecipeManager clientRecipeManager;

    public FHRecipeCachingReloadListener(ReloadableServerResources dataPackRegistries) {
        this.dataPackRegistries = dataPackRegistries;
    }



    public static void buildRecipeLists(RecipeManager recipeManager) {
        FHMain.LOGGER.info("Building recipe lists");
        Collection<Recipe<?>> recipes = recipeManager.getRecipes();
        if (recipes.isEmpty())
            return;
        //filterRecipes(recipes, GeneratorRecipe.class, GeneratorRecipe.TYPE);
        //GeneratorSteamRecipe.recipeList = filterRecipes(recipes, GeneratorSteamRecipe.class, GeneratorSteamRecipe.TYPE);
       /* InstallInnerRecipe.recipeList = recipes.stream()
                .filter(iRecipe -> iRecipe.getClass() == InstallInnerRecipe.class)
                .map(e -> (InstallInnerRecipe) e)
                .collect(Collectors.<InstallInnerRecipe, ResourceLocation, InstallInnerRecipe>toMap(InstallInnerRecipe::getBuffType, recipe -> recipe));
        */CampfireDefrostRecipe.recipeList = recipes.stream()
                .filter(iRecipe -> iRecipe.getClass() == CampfireDefrostRecipe.class)
                .map(e -> (CampfireDefrostRecipe) e)
                .collect(Collectors.toMap(AbstractCookingRecipe::getId, recipe -> recipe));
        /*DietValueRecipe.recipeList = filterRecipes(recipes,DietValueRecipe.class,DietValueRecipe.TYPE).values().stream()
                .filter(iRecipe -> iRecipe.getClass() == DietValueRecipe.class)
                .map(e -> e)
                .collect(Collectors.toMap(recipe -> recipe.item, recipe -> recipe));*/
       // InspireRecipe.recipes = filterRecipes(recipes, InspireRecipe.class, InspireRecipe.TYPE).values().stream().collect(Collectors.toList());
        //ResearchPaperRecipe.recipes = filterRecipes(recipes, ResearchPaperRecipe.class, ResearchPaperRecipe.TYPE).values().stream().collect(Collectors.toList());
       // SaunaRecipe.recipeList = filterRecipes(recipes, SaunaRecipe.class, SaunaRecipe.TYPE);
        //IncubateRecipe.recipeList = filterRecipes(recipes, IncubateRecipe.class, IncubateRecipe.TYPE);
        TradePolicy.policies = filterRecipes(recipes, TradePolicy.class, TradePolicy.TYPE).values().stream().collect(Collectors.toMap(TradePolicy::getName, t -> t));
        System.out.println(TradePolicy.policies);
        //System.out.println(TradePolicy.policies.size());
        TradePolicy.items = TradePolicy.policies.values().stream().map(TradePolicy::asWeight).filter(Objects::nonNull).collect(Collectors.toList());
        //System.out.println(TradePolicy.items.size());
        TradePolicy.totalW = TradePolicy.items.stream().mapToInt(w -> w.getWeight().asInt()).sum();
        
        ArmorTempData.cacheList=new HashMap<>();
        Function<Item,EnumDefaultedMap<BodyPart,ArmorTempData>> armorMapGetter=t->new EnumDefaultedMap<>(BodyPart.class);
        ArmorTempData.TYPE.get().filterRecipes(recipes).forEach(t->{
        	ArmorTempData.cacheList.computeIfAbsent(t.getData().item(), armorMapGetter).put(t.getData().slot().orElse(null), t.getData());
        });;
        BiomeTempData.cacheList=BiomeTempData.TYPE.get().filterRecipes(recipes).collect(Collectors.toMap(t->t.getData().biome(), t->t.getData()));
        BlockTempData.updateCache(recipeManager);
        StateTransitionData.updateCache(recipeManager);
        CupData.cacheList=CupData.TYPE.get().filterRecipes(recipes).collect(Collectors.toMap(t->t.getData().item(), t->t.getData()));
        DrinkTempData.cacheList=DrinkTempData.TYPE.get().filterRecipes(recipes).collect(Collectors.toMap(t->t.getData().fluid(), t->t.getData()));
        FoodTempData.cacheList=FoodTempData.TYPE.get().filterRecipes(recipes).collect(Collectors.toMap(t->t.getData().item(), t->t.getData()));
        PlantTempData.cacheList=PlantTempData.TYPE.get().filterRecipes(recipes).collect(Collectors.toMap(t->t.getData().block(), t->t.getData()));
        WorldTempData.cacheList=WorldTempData.TYPE.get().filterRecipes(recipes).collect(Collectors.toMap(t->t.getData().world(), t->t.getData()));
        //System.out.println(TradePolicy.totalW);

    }
    

    static <R extends Recipe<?>> Map<ResourceLocation, R> filterRecipes(Collection<Recipe<?>> recipes, Class<R> recipeClass) {
        return recipes.stream()
                .filter(iRecipe -> iRecipe.getClass() == recipeClass)
                .map(recipeClass::cast)
                .collect(Collectors.toMap(recipe -> recipe.getId(), recipe -> recipe));
    }

    static <R extends Recipe<?>> Map<ResourceLocation, R> filterRecipes(Collection<Recipe<?>> recipes, Class<R> recipeClass, RegistryObject<RecipeType<R>> recipeType) {
        return recipes.stream()
                .filter(iRecipe -> iRecipe.getType() == recipeType.get())
                .map(recipeClass::cast)
                .collect(Collectors.toMap(recipe -> recipe.getId(), recipe -> recipe));
    }
    @Override
    public void onResourceManagerReload(@Nonnull ResourceManager resourceManager) {
        buildRecipeLists(dataPackRegistries.getRecipeManager());
        
        if(FMLEnvironment.dist==Dist.CLIENT)
        	WheelMenuRenderer.load();
    }
}