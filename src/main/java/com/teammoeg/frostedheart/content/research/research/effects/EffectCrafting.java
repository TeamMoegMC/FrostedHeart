/*
 * Copyright (c) 2022-2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.research.research.effects;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.FHTeamDataManager;
import com.teammoeg.frostedheart.compat.jei.JEICompat;
import com.teammoeg.frostedheart.content.research.ResearchListeners;
import com.teammoeg.frostedheart.content.research.data.TeamResearchData;
import com.teammoeg.frostedheart.content.research.gui.FHIcons;
import com.teammoeg.frostedheart.content.research.gui.FHIcons.FHIcon;
import com.teammoeg.frostedheart.util.TranslateUtils;
import com.teammoeg.frostedheart.util.io.CodecUtil;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.ItemLike;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.text.TranslationTextComponent;
import com.teammoeg.frostedheart.util.TranslateUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class EffectCrafting extends Effect {
	public static final Codec<EffectCrafting> CODEC=RecordCodecBuilder.create(t->t.group(
		Effect.BASE_CODEC.forGetter(Effect::getBaseData),
		CodecUtil.<EffectCrafting,Item,ItemStack,List<ResourceLocation>>either(
			CodecUtil.registryCodec(()->Registry.ITEM).fieldOf("item"),
			CodecUtil.ITEMSTACK_CODEC.fieldOf("item"),
			Codec.list(ResourceLocation.CODEC).fieldOf("recipes"),
			o->o.item,
			o->o.itemStack,
			o->o.unlocks.stream().map(Recipe::getId).collect(Collectors.toList()))
		)
	.apply(t,EffectCrafting::new));
    List<Recipe<?>> unlocks = new ArrayList<>();
    ItemStack itemStack = null;
    Item item = null;
    EffectCrafting() {
    }

    public EffectCrafting(ItemLike item) {
        super();
        this.item = item.asItem();
        initItem();
    }

    public EffectCrafting(ItemStack item) {
        super();

        this.itemStack = item;
    }

    public EffectCrafting(BaseData data,Either<Item,Either<ItemStack,List<ResourceLocation>>> unlocks) {
		super(data);
		unlocks.ifLeft(t->{this.item=t;initItem();});
		unlocks.ifRight(t->{
			t.ifLeft(o->{this.itemStack=o;initStack();});
			t.ifRight(o->o.stream().map(FHTeamDataManager.getRecipeManager()::byKey).filter(Optional::isPresent).map(Optional::get).forEach(this.unlocks::add));
		});

	}

    public EffectCrafting(ResourceLocation recipe) {
        super("@gui." + FHMain.MODID + ".effect.crafting", new ArrayList<>());
        Optional<? extends Recipe<?>> r = FHTeamDataManager.getRecipeManager().byKey(recipe);

        r.ifPresent(iRecipe -> unlocks.add(iRecipe));
    }

    @Override
    public String getBrief() {
        if (item != null)
            return "Craft " + TranslateUtils.translate(item.getDescriptionId()).getString();
        if (itemStack != null)
            return "Craft " + itemStack.getHoverName().getString();
        if (!unlocks.isEmpty())
            return "Craft" + unlocks.get(0).getId() + (unlocks.size() > 1 ? " ..." : "");
        return "Craft nothing";
    }

    @Override
    public FHIcon getDefaultIcon() {
        if (item != null)
            return FHIcons.getIcon(FHIcons.getIcon(item), FHIcons.getIcon(Items.CRAFTING_TABLE));
        else if (itemStack != null)
            return FHIcons.getIcon(FHIcons.getIcon(itemStack), FHIcons.getIcon(Items.CRAFTING_TABLE));
        else {
            Set<ItemStack> stacks = new HashSet<>();
            for (Recipe<?> r : unlocks) {
                if (!r.getResultItem().isEmpty()) {
                    stacks.add(r.getResultItem());
                }
            }
            if (!stacks.isEmpty())
                return FHIcons.getIcon(FHIcons.getStackIcons(stacks), FHIcons.getIcon(Items.CRAFTING_TABLE));
        }
        return FHIcons.getIcon(FHIcons.getDelegateIcon("question"), FHIcons.getIcon(Items.CRAFTING_TABLE));
    }

    @Override
    public MutableComponent getDefaultName() {
        return TranslateUtils.translateGui("effect.crafting");
    }

    @Override
    public List<Component> getDefaultTooltip() {
        List<Component> tooltip = new ArrayList<>();

        if (item != null)
            tooltip.add(TranslateUtils.translate(item.getDescriptionId()));
        else if (itemStack != null)
            tooltip.add(itemStack.getHoverName());
        else {
            Set<ItemStack> stacks = new HashSet<>();
            for (Recipe<?> r : unlocks) {
                if (!r.getResultItem().isEmpty()) {
                    stacks.add(r.getResultItem());
                }
            }
            if (stacks.isEmpty())
                tooltip.add(TranslateUtils.translateGui("effect.recipe.error"));
            else
                for (ItemStack is : stacks) {
                    tooltip.add(is.getHoverName());
                }
        }

        return tooltip;
    }

    @Override
    public boolean grant(TeamResearchData team, Player triggerPlayer, boolean isload) {
        team.crafting.addAll(unlocks);
        return true;
    }

    @Override
    public void init() {
        ResearchListeners.recipe.addAll(unlocks);
    }

    private void initItem() {
        unlocks.clear();
        for (Recipe<?> r : FHTeamDataManager.getRecipeManager().getRecipes()) {
            if (r.getResultItem().getItem().equals(this.item)) {
                unlocks.add(r);
            }
        }
    }


    private void initStack() {
        unlocks.clear();
        for (Recipe<?> r : FHTeamDataManager.getRecipeManager().getRecipes()) {
            if (r.getResultItem().equals(item)) {
                unlocks.add(r);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void onClick() {
        if (!this.isGranted()) return;
        if (item != null)
            JEICompat.showJEIFor(new ItemStack(item));
        else if (itemStack != null)
            JEICompat.showJEIFor(itemStack);
    }

    @Override
    public void reload() {
        if (item != null) {
            initItem();
        } else if (itemStack != null) {
            initStack();
        } else {
            unlocks.replaceAll(o -> FHTeamDataManager.getRecipeManager().byKey(o.getId()).orElse(null));
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
            Optional<? extends Recipe<?>> r = FHTeamDataManager.getRecipeManager().byKey(new ResourceLocation(s));

            r.ifPresent(iRecipe -> unlocks.add(iRecipe));
        }
    }

	public List<Recipe<?>> getUnlocks() {
		return unlocks;
	}

	public ItemStack getItemStack() {
		return itemStack;
	}

	public Item getItem() {
		return item;
	}

}
