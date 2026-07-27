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
import java.util.function.Function;
import java.util.stream.Stream;
@JeiPlugin
public class JEICompat implements IModPlugin {

    public static IRecipeManager man;

    public static IJeiRuntime jei;

    static Map<Object,Set<RecipeType<?>>> types = new IdentityHashMap<>(3000);


    private static boolean cachedInfoAdd = false;

    public static Map<ResourceLocation, Recipe<?>> overrides = new HashMap<>();

    public static Map<ItemStack, List<IJeiIngredientInfoRecipe>> infos = new HashMap<>();
    public static Map<ItemStack, Map<String, Component>> research = new HashMap<>();

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
        for (Recipe<?> i : ResearchHooks.getLockList(ResearchHooks.RECIPE_UNLOCK_LIST)) {
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
    }

    public static void scheduleSyncJEI() {
        //cachedInfoAdd=true;
        Minecraft.getInstance().executeBlocking(JEICompat::syncJEI);
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

        Map<ItemStack, Boolean> stackLockedStatus = new HashMap<>(); // true=锁定, false=解锁
        UnlockList<Recipe> unlockList = ClientResearchDataAPI.getData().get().getUnlockList(ResearchHooks.RECIPE_UNLOCK_LIST);

        for (Recipe<?> i : ResearchHooks.getLockList(ResearchHooks.RECIPE_UNLOCK_LIST)) {
            ItemStack out = RecipeUtil.getResultItem(i);
            if (out == null || out.isEmpty()) continue;

            Set<RecipeType<?>> type = types.get(i);
            if (type != null) {
                for (RecipeType<?> rl : type) {
                    try {
                        if (!unlockList.has(i)) {
                            man.hideRecipes((RecipeType) rl, Collections.singletonList(i));
                        } else {
                            man.unhideRecipes((RecipeType) rl, Collections.singletonList(i));
                        }
                    } catch (Exception ex) {
                        FRMain.LOGGER.error("Error hiding recipe", ex);
                    }
                }
            }

            // 记录对应 ItemStack 的锁定状态（若有解锁的配方，则整体视为解锁）
            boolean locked = !unlockList.has(i);
            stackLockedStatus.merge(out.copy(), locked, (oldVal, newVal) -> oldVal && newVal);
        }

        // 根据 ItemStack 控制提示的显隐
        for (Entry<ItemStack, List<IJeiIngredientInfoRecipe>> entry : infos.entrySet()) {
            Boolean locked = stackLockedStatus.get(entry.getKey());
            // 如果该 ItemStack 对应的所有配方都被锁定，或没有找到状态（不在表中），则显示提示
            if (locked == null || locked) {
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

    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(FRMain.MODID, "jei_plugin");
    }
    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        man = jeiRuntime.getRecipeManager();
        jei = jeiRuntime;
        generateRecipeType();
        syncJEI();
        // man.hideRecipeCategory(RecipeTypes.BLASTING);
        // man.hideRecipeCategory(RecipeTypes.SMOKING);
        // man.hideRecipeCategory(RecipeTypes.SMELTING);


    }
    public void generateRecipeType() {
        Function<? super Object, ? extends Set<RecipeType<?>>> creator=o->new HashSet<>();
        Function<? super Object,Set<RecipeType<?>>> getter=o->types.computeIfAbsent((Object)o, creator);
        man.createRecipeCategoryLookup().includeHidden().get().forEach(t->{
        	man.createRecipeLookup(t.getRecipeType()).includeHidden().get().map(getter).forEach(o->o.add(t.getRecipeType()));
        	
        });;
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
