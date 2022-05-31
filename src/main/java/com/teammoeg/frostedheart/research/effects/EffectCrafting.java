package com.teammoeg.frostedheart.research.effects;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import com.teammoeg.frostedheart.research.ResearchDataManager;
import com.teammoeg.frostedheart.research.ResearchGlobals;
import com.teammoeg.frostedheart.research.TeamResearchData;
import com.teammoeg.frostedheart.research.gui.FHIcons;
import com.teammoeg.frostedheart.research.gui.FHIcons.FHIcon;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.RecipeManager;
import net.minecraft.util.IItemProvider;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;

public class EffectCrafting extends Effect{
	List<String> unlocks=new ArrayList<>();
    public EffectCrafting(ItemStack item) {
    	super(GuiUtils.translateGui("effect.crafting"),new ArrayList<>(),FHIcons.getIcon(FHIcons.getIcon(item),FHIcons.getIcon(Items.CRAFTING_TABLE)));
    	for(IRecipe<?> r:ResearchDataManager.server.getRecipeManager().getRecipes()) {
    		if(r.getRecipeOutput().equals(item)) {
    			unlocks.add(r.getId().toString());
    		}
    	}
    	tooltip.add(new TranslationTextComponent(item.getTranslationKey()));
    }
    public EffectCrafting(IItemProvider item) {
    	super(GuiUtils.translateGui("effect.crafting"),new ArrayList<>(),FHIcons.getIcon(FHIcons.getIcon(item),FHIcons.getIcon(Items.CRAFTING_TABLE)));
    	for(IRecipe<?> r:ResearchDataManager.server.getRecipeManager().getRecipes()) {
    		if(r.getRecipeOutput().getItem().equals(item.asItem())) {
    			unlocks.add(r.getId().toString());
    		}
    	}
    	tooltip.add(new TranslationTextComponent(item.asItem().getTranslationKey()));
    }
    public EffectCrafting(ResourceLocation recipe) {
    	super(GuiUtils.translateGui("effect.crafting"),new ArrayList<>());
    	Optional<? extends IRecipe<?>> r=ResearchDataManager.server.getRecipeManager().getRecipe(recipe);
    	unlocks.add(recipe.toString());
    	if(r.isPresent()) {
    		ItemStack output=r.get().getRecipeOutput();
    		
    		tooltip.add(new TranslationTextComponent(output.getTranslationKey()));
    	}else
    		tooltip.add(GuiUtils.translateGui("effect.recipe.error"));
    }
    public EffectCrafting(JsonObject jo) {
    	super(jo);
    }

	@Override
    public void init() {
		for(String s:unlocks)
			ResearchGlobals.recipe.unlock(s);//This list treat as blacklist, so unlock is lock,
											 //THIS IS NOT AN ERROR
    }

    @Override
    public void grant(TeamResearchData team, PlayerEntity triggerPlayer) {
    	for(String s:unlocks)
    		team.crafting.unlock(s);
    }

    @Override
    public void revoke(TeamResearchData team) {
    	for(String s:unlocks)
    		team.crafting.lock(s);
    }

	@Override
	public String getId() {
		return "recipe";
	}
}
