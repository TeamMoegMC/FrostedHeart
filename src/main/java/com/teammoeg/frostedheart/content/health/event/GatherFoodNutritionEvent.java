package com.teammoeg.frostedheart.content.health.event;

import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Set;

import javax.annotation.Nullable;

import com.teammoeg.frostedheart.content.health.capability.MutableNutrition;
import com.teammoeg.frostedheart.content.health.capability.Nutrition;
import com.teammoeg.frostedheart.content.health.recipe.NutritionRecipe;

import lombok.Getter;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.Event;

public class GatherFoodNutritionEvent extends Event {
	@Getter
	private final Nutrition originalValue;
	@Getter
	private final Level level;
	@Nullable
	@Getter
	private final Player consumer;
	@Getter
	private final ItemStack stack;
	private MutableNutrition modified;
	public GatherFoodNutritionEvent(Nutrition originalValue, Level level,ItemStack stack, Player consumer) {
		super();
		this.originalValue = originalValue;
		this.level = level;
		this.consumer = consumer;
		this.stack=stack;
	}
	public GatherFoodNutritionEvent(Nutrition originalValue, Level level, Player consumer, ItemStack stack,
			Set<ItemStack> recursiveCheck) {
		super();
		this.originalValue = originalValue;
		this.level = level;
		this.consumer = consumer;
		this.stack = stack;
	}
	public MutableNutrition getForModify() {
		if(modified==null)
			modified=new MutableNutrition(0,0,0,0);
		return modified;
	}
	public boolean isModified() {
		return modified==null;
	}
	public Nutrition queryNutrition(ItemStack stack) {
		if(consumer!=null)
			return NutritionRecipe.getRecipeFromItem(consumer, stack);
		return NutritionRecipe.getRecipeFromItem(level, stack);
	}


}
