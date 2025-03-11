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

package com.teammoeg.frostedresearch.research.effects;

import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.chorda.client.icon.CIcons.CIcon;
import com.teammoeg.chorda.dataholders.team.TeamDataHolder;
import com.teammoeg.chorda.io.CodecUtil;
import com.teammoeg.chorda.math.CMath;
import com.teammoeg.chorda.util.CDistHelper;
import com.teammoeg.frostedresearch.Lang;
import com.teammoeg.frostedresearch.FRMain;
import com.teammoeg.frostedresearch.ResearchListeners;
import com.teammoeg.frostedresearch.compat.JEICompat;
import com.teammoeg.frostedresearch.data.ResearchData;
import com.teammoeg.frostedresearch.data.TeamResearchData;

import mezz.jei.library.util.RecipeUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.*;
import java.util.stream.Collectors;

public class EffectCrafting extends Effect {
    public static final MapCodec<EffectCrafting> CODEC = RecordCodecBuilder.mapCodec(t -> t.group(

                    CodecUtil.<EffectCrafting, Ingredient, List<ResourceLocation>>either(
                            CodecUtil.INGREDIENT_CODEC.fieldOf("item"),
                            Codec.list(ResourceLocation.CODEC).optionalFieldOf("recipes", Arrays.asList()),
                            o -> o.ingredient,
                            o -> o.unlocks.stream().map(Recipe::getId).collect(Collectors.toList())
                    ),
                    Effect.BASE_CODEC.forGetter(Effect::getBaseData))
            .apply(t, EffectCrafting::new));
    List<Recipe<?>> unlocks = new ArrayList<>();
    Ingredient ingredient = null;

    EffectCrafting() {
    }

    public EffectCrafting(ItemLike item) {
        super();
        this.ingredient = Ingredient.of(item);
        initItem();
    }

    public EffectCrafting(ItemStack item) {
        super();

        this.ingredient = Ingredient.of(item);
    }

    public EffectCrafting(Either<Ingredient, List<ResourceLocation>> unlocks, BaseData data) {
        super(data);

        unlocks.ifLeft(t -> {
            this.ingredient = t;
        });
        unlocks.ifRight(o -> o.stream().map(CDistHelper.getRecipeManager()::byKey).filter(Optional::isPresent).map(Optional::get).forEach(this.unlocks::add));
    }

    public EffectCrafting(ResourceLocation recipe) {
        super("@gui." + FRMain.MODID + ".effect.crafting", new ArrayList<>());
        Optional<? extends Recipe<?>> r = CDistHelper.getRecipeManager().byKey(recipe);

        r.ifPresent(iRecipe -> unlocks.add(iRecipe));
    }

    @Override
    public String getBrief() {
        if (ingredient != null && !ingredient.isEmpty())
            return "Craft " + ingredient.getItems()[0].getDisplayName().getString() + (ingredient.getItems().length > 1 ? " ..." : "");
        if (!unlocks.isEmpty())
            return "Craft" + unlocks.get(0).getId() + (unlocks.size() > 1 ? " ..." : "");
        return "Craft nothing";
    }

    @Override
    public CIcon getDefaultIcon() {
        if (ingredient != null)
            return CIcons.getIcon(CIcons.getIcon(ingredient), CIcons.getIcon(Items.CRAFTING_TABLE));
        Set<ItemStack> stacks = new HashSet<>();
        for (Recipe<?> r : unlocks) {
            if (!RecipeUtil.getResultItem(r).isEmpty()) {
                stacks.add(RecipeUtil.getResultItem(r));
            }
        }
        if (!stacks.isEmpty())
            return CIcons.getIcon(CIcons.getStackIcons(stacks), CIcons.getIcon(Items.CRAFTING_TABLE));
        return CIcons.getIcon(CIcons.getDelegateIcon("question"), CIcons.getIcon(Items.CRAFTING_TABLE));
    }

    @Override
    public MutableComponent getDefaultName() {
        return Lang.translateGui("effect.crafting");
    }

    @Override
    public List<Component> getDefaultTooltip() {
        List<Component> tooltip = new ArrayList<>();

        if (ingredient != null)
            tooltip.add(CMath.selectElementByTime(ingredient.getItems()).getHoverName());
        else {
            Set<ItemStack> stacks = new HashSet<>();
            for (Recipe<?> r : unlocks) {
                if (!RecipeUtil.getResultItem(r).isEmpty()) {
                    stacks.add(RecipeUtil.getResultItem(r));
                }
            }
            if (stacks.isEmpty())
                tooltip.add(Lang.translateGui("effect.recipe.error"));
            else
                for (ItemStack is : stacks) {
                    tooltip.add(is.getHoverName());
                }
        }

        return tooltip;
    }

    @Override
    public boolean grant(TeamDataHolder team, TeamResearchData trd, Player triggerPlayer, boolean isload) {
        trd.crafting.addAll(unlocks);
        return true;
    }

    @Override
    public void init() {
        if (ingredient != null)
            initItem();
        ResearchListeners.recipe.addAll(unlocks);
    }

    private void initItem() {
        unlocks.clear();
        for (Recipe<?> r : CDistHelper.getRecipeManager().getRecipes()) {
            ItemStack result = r.getResultItem(CDistHelper.getAccess());
            if (result == null) {
                LogUtils.getLogger().debug("Error null recipe " + r);
            }
            if (ingredient.test(result)) {
                unlocks.add(r);
            }
        }
    }


    @OnlyIn(Dist.CLIENT)
    @Override
    public void onClick(ResearchData parent) {
        if (!parent.isEffectGranted(this)) return;
        if (ingredient != null)
            JEICompat.showJEIFor(CMath.selectElementByTime(ingredient.getItems()));
    }

    @Override
    public void reload() {
        if (ingredient != null) {
            initItem();
        } else {
            unlocks.replaceAll(o -> CDistHelper.getRecipeManager().byKey(o.getId()).orElse(null));
            unlocks.removeIf(Objects::isNull);
        }
    }

    @Override
    public void revoke(TeamResearchData team) {
        team.crafting.removeAll(unlocks);
    }

    public void setList(Collection<String> ls) {
        unlocks.clear();
        for (String s : ls) {
            Optional<? extends Recipe<?>> r = CDistHelper.getRecipeManager().byKey(new ResourceLocation(s));

            r.ifPresent(iRecipe -> unlocks.add(iRecipe));
        }
    }

    public List<Recipe<?>> getUnlocks() {
        return unlocks;
    }

    public Ingredient getIngredient() {
        return ingredient;
    }

    @Override
    public String toString() {
        return "EffectCrafting [name=" + name + ", tooltip=" + tooltip + ", icon=" + icon + ", nonce=" + nonce
                + ", hidden=" + hidden + "]";
    }

}
