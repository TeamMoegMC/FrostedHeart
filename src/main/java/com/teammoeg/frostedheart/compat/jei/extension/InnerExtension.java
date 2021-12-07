package com.teammoeg.frostedheart.compat.jei.extension;

import blusunrize.immersiveengineering.common.util.ItemNBTHelper;
import com.teammoeg.frostedheart.content.recipes.RecipeInner;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class InnerExtension implements ICraftingCategoryExtension {
	RecipeInner inner;
	public InnerExtension(RecipeInner ri) {
		inner=ri;
	}
	@Override
	public ResourceLocation getRegistryName() {
		return inner.getId();
	}
	@Override
	public void setIngredients(IIngredients ingredients) {
		List<ItemStack> armors=new ArrayList<>();
		ArrayList<ItemStack> armorsout=new ArrayList<>();
		ForgeRegistries.ITEMS.getValues().stream().map(ItemStack::new).filter(i->inner.matches(i)).forEach(e->armors.add(e));
		armorsout.ensureCapacity(armors.size());
		armors.forEach(e->{
			ItemStack n=e.copy();
			ItemNBTHelper.putString(n, "inner_cover",inner.getBuffType().toString());
			armorsout.add(n);
		});
		ingredients.setInputLists(VanillaTypes.ITEM,Arrays.asList(armors,Arrays.asList(inner.getIngredient().getMatchingStacks())));
		
		ingredients.setOutputLists(VanillaTypes.ITEM,Arrays.asList(armorsout));
	}

}
