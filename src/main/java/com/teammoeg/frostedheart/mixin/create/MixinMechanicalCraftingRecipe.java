package com.teammoeg.frostedheart.mixin.create;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.simibubi.create.content.contraptions.components.crafter.MechanicalCraftingRecipe;
import com.teammoeg.frostedheart.research.ResearchListeners;

import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapedRecipe;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
@Mixin(MechanicalCraftingRecipe.class)
public class MixinMechanicalCraftingRecipe extends ShapedRecipe{


	public MixinMechanicalCraftingRecipe(ResourceLocation idIn, String groupIn, int recipeWidthIn, int recipeHeightIn,
			NonNullList<Ingredient> recipeItemsIn, ItemStack recipeOutputIn) {
		super(idIn, groupIn, recipeWidthIn, recipeHeightIn, recipeItemsIn, recipeOutputIn);
	}

	@Inject(at=@At("HEAD"),method="matches",remap=false,cancellable=true)
	public void fh$matches(CraftingInventory inv, World worldIn,CallbackInfoReturnable<Boolean> cbi) {
		if (!ResearchListeners.canUseRecipe(ResearchListeners.te, this))cbi.setReturnValue(false);
	}
}
