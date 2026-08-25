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

package com.teammoeg.frostedresearch.compat;

import com.google.common.collect.ImmutableList;
import com.teammoeg.frostedresearch.FHResearch;
import com.teammoeg.frostedresearch.FRMain;
import com.teammoeg.frostedresearch.Lang;
import com.teammoeg.frostedresearch.ResearchHooks;
import com.teammoeg.frostedresearch.UnlockList;
import com.teammoeg.frostedresearch.api.ClientKnowledgeDataAPI;
import com.teammoeg.frostedresearch.api.ClientResearchDataAPI;
import com.teammoeg.frostedresearch.research.Research;
import com.teammoeg.frostedresearch.research.effects.Effect;
import com.teammoeg.frostedresearch.research.effects.EffectCrafting;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.vanilla.IJeiIngredientInfoRecipe;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IVanillaCategoryExtensionRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.library.plugins.jei.info.IngredientInfoRecipe;
import mezz.jei.library.util.RecipeUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;

import javax.annotation.Nullable;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Stream;
@JeiPlugin
public class JEICompat implements IModPlugin {

    public static IRecipeManager man;

    public static IJeiRuntime jei;

    /**
     * JEI may wrap or replace the recipe objects supplied by Minecraft. Keep the
     * actual JEI registrations indexed by the stable recipe id instead of relying
     * on object identity when access changes at runtime.
     */
    private static final Map<ResourceLocation, List<RegisteredRecipe>> registeredRecipes = new LinkedHashMap<>();


    private static boolean cachedInfoAdd = false;

    public static Map<ResourceLocation, Recipe<?>> overrides = new HashMap<>();

    public static Map<ItemStack, List<IJeiIngredientInfoRecipe>> infos = new HashMap<>();
    public static Map<ItemStack, Map<String, Component>> research = new HashMap<>();
    private static Map<ResourceLocation, Recipe<?>> lastManagedRecipes = new LinkedHashMap<>();

    public static void addInfo() {
        if (man == null) {
            cachedInfoAdd = true;
            return;
        }
        FRMain.LOGGER.info("added research jei info");
        cachedInfoAdd = false;
        infos.clear();
        Component it = Lang.translateKey("gui.jei.info.require_research");

        // 使用 Map 去重，对每个不同的输出 ItemStack 只创建一个信息配方
        Map<ItemStack, List<IJeiIngredientInfoRecipe>> newInfos = new HashMap<>();
        for (Recipe<?> i : managedRecipes()) {
            ItemStack out = RecipeUtil.getResultItem(i);
            if (out != null && !out.isEmpty()) {
                newInfos.computeIfAbsent(out.copy(), stack -> {
                    List<IJeiIngredientInfoRecipe> il = Collections.singletonList(IngredientInfoRecipe.create(
                            jei.getIngredientManager(),
                            ImmutableList.of(stack),
                            VanillaTypes.ITEM_STACK, it
                    ));
                    man.addRecipes(RecipeTypes.INFORMATION, il);
                    return il;
                });
            }
        }
        infos = newInfos;
    }

    public static <T> void checkNotNull(@Nullable T object, String name) {
        if (object == null) {
            throw new NullPointerException(name + " must not be null.");
        }
    }
    public static void resetRuntime() {
        man = null;
        jei = null;
        registeredRecipes.clear();
        lastManagedRecipes.clear();
    }

    public static void scheduleSyncJEI() {
        Minecraft.getInstance().execute(JEICompat::syncJEI);
    }

    public static void showJEICategory(ResourceLocation rl) {
    	man.getRecipeType(rl).ifPresent(o->jei.getRecipesGui().showTypes(Arrays.asList(o)));
    }

    public static void showJEIFor(ItemStack stack) {
        jei.getRecipesGui().show(jei.getJeiHelpers().getFocusFactory().createFocus(RecipeIngredientRole.OUTPUT,VanillaTypes.ITEM_STACK,stack));
    }

    public static void syncJEI() {
        if (Minecraft.getInstance().level == null)
            return;
        if (man == null)
            return;
        if (cachedInfoAdd)
            addInfo();

        Map<ResourceLocation, Recipe<?>> currentManagedRecipes = managedRecipesById();
        Map<ResourceLocation, Recipe<?>> recipesToRefresh = new LinkedHashMap<>(lastManagedRecipes);
        recipesToRefresh.putAll(currentManagedRecipes);
        Map<ItemStack, Boolean> stackLockedStatus = new HashMap<>(); // true=锁定, false=解锁
        for (Recipe<?> i : recipesToRefresh.values()) {
            boolean currentlyManaged = currentManagedRecipes.containsKey(i.getId());
            boolean locked = currentlyManaged
                    && !ClientKnowledgeDataAPI.technologyProjection().recipe(i.getId()).allowed();
            updateRecipeVisibility(i.getId(), locked);

            ItemStack out = RecipeUtil.getResultItem(i);
            if (out == null || out.isEmpty()) continue;

            // 记录对应 ItemStack 的锁定状态（若有解锁的配方，则整体视为解锁）
            if (currentlyManaged)
                stackLockedStatus.merge(out.copy(), locked, (oldVal, newVal) -> oldVal && newVal);
        }
        lastManagedRecipes = currentManagedRecipes;

        // 根据 ItemStack 控制提示的显隐
        for (Entry<ItemStack, List<IJeiIngredientInfoRecipe>> entry : infos.entrySet()) {
            Boolean locked = stackLockedStatus.get(entry.getKey());
            // 仅在该输出仍受管理且所有已管理配方均锁定时显示提示。
            if (Boolean.TRUE.equals(locked)) {
                man.unhideRecipes(RecipeTypes.INFORMATION, entry.getValue());
            } else {
                man.hideRecipes(RecipeTypes.INFORMATION, entry.getValue());
            }
        }

        UnlockList<ResourceLocation> categoryUnlockList=ClientResearchDataAPI.getData().get().getUnlockList(ResearchHooks.CATEGORY_UNLOCK_LIST);
        for (ResourceLocation rl : ResearchHooks.getLockList(ResearchHooks.CATEGORY_UNLOCK_LIST)) {
        	RecipeType<?> type=man.getRecipeType(rl).orElse(null);
        	if(type!=null) {
	            if (!categoryUnlockList.has(rl)) {
	                man.hideRecipeCategory(type);
	            } else
	                man.unhideRecipeCategory(type);
        	}
        }

        research.clear();
        for (Research research : FHResearch.getAllResearch()) {
            for (Effect effect : research.getEffects()) {
                if (!ClientResearchDataAPI.getData().get().isEffectGranted(research, effect) && effect instanceof EffectCrafting) {
                    Set<ItemStack> items = new HashSet<>();
                    EffectCrafting crafting = (EffectCrafting) effect;
                    if (crafting.getIngredient() != null)
                        Stream.of(crafting.getIngredient().getItems()).forEach(items::add);
                    else if (crafting.getUnlocks() != null)
                        crafting.getUnlocks().stream()
                                .map(RecipeUtil::getResultItem)
                                .filter(t -> t != null && !t.isEmpty())
                                .forEach(items::add);
                    for (ItemStack stack : items) {
                        JEICompat.research.computeIfAbsent(stack.copy(), i -> new LinkedHashMap<>())
                                .put(research.getId(), Lang.translateTooltip("research_unlockable", research.getName()));
                    }
                }
            }
        }
    }

    private static List<Recipe<?>> managedRecipes() {
        return new ArrayList<>(managedRecipesById().values());
    }

    private static Map<ResourceLocation, Recipe<?>> managedRecipesById() {
        Map<ResourceLocation, Recipe<?>> managed = new LinkedHashMap<>();
        for (Recipe<?> recipe : ResearchHooks.getLockList(ResearchHooks.RECIPE_UNLOCK_LIST)) {
            managed.put(recipe.getId(), recipe);
        }
        RecipeManager recipes = Minecraft.getInstance().level == null
                ? null : Minecraft.getInstance().level.getRecipeManager();
        if (recipes != null) {
            for (ResourceLocation id : ClientKnowledgeDataAPI.technologyProjection().managedRecipes()) {
                recipes.byKey(id).ifPresent(recipe -> managed.put(id, recipe));
            }
        }
        return managed;
    }

    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(FRMain.MODID, "jei_plugin");
    }
    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        man = jeiRuntime.getRecipeManager();
        jei = jeiRuntime;
        indexRegisteredRecipes();
        syncJEI();
        // man.hideRecipeCategory(RecipeTypes.BLASTING);
        // man.hideRecipeCategory(RecipeTypes.SMOKING);
        // man.hideRecipeCategory(RecipeTypes.SMELTING);


    }
    private static void indexRegisteredRecipes() {
        registeredRecipes.clear();
        man.createRecipeCategoryLookup().includeHidden().get().forEach(category -> {
            RecipeType<?> type = category.getRecipeType();
            man.createRecipeLookup(type).includeHidden().get().forEach(recipe -> {
                if (recipe instanceof Recipe<?> minecraftRecipe) {
                    registeredRecipes.computeIfAbsent(minecraftRecipe.getId(), ignored -> new ArrayList<>())
                            .add(new RegisteredRecipe(type, recipe));
                }
            });
        });
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void updateRecipeVisibility(ResourceLocation recipeId, boolean hidden) {
        for (RegisteredRecipe registered : registeredRecipes.getOrDefault(recipeId, List.of())) {
            try {
                if (hidden) {
                    man.hideRecipes((RecipeType) registered.type(), Collections.singletonList(registered.recipe()));
                } else {
                    man.unhideRecipes((RecipeType) registered.type(), Collections.singletonList(registered.recipe()));
                }
            } catch (RuntimeException exception) {
                FRMain.LOGGER.error("Error updating JEI visibility for recipe {}", recipeId, exception);
            }
        }
    }

    private record RegisteredRecipe(RecipeType<?> type, Object recipe) {
    }
    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registry) {
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {

    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        ClientLevel world = Minecraft.getInstance().level;
        checkNotNull(world, "minecraft world");
        RecipeManager recipeManager = world.getRecipeManager();
        JEICompat.scheduleSyncJEI();
    }


    @Override
    public void registerVanillaCategoryExtensions(IVanillaCategoryExtensionRegistration registration) {
    }
}
