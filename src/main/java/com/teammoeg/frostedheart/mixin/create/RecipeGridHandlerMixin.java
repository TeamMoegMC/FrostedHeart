package com.teammoeg.frostedheart.mixin.create;

import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.simibubi.create.content.contraptions.components.crafter.MechanicalCrafterTileEntity;
import com.simibubi.create.content.contraptions.components.crafter.RecipeGridHandler;
import com.teammoeg.frostedheart.research.ResearchListeners;
import com.teammoeg.frostedheart.util.IOwnerTile;

import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.crafting.ICraftingRecipe;

@Mixin(RecipeGridHandler.class)
public class RecipeGridHandlerMixin {
	private static UUID te;
	public RecipeGridHandlerMixin() {
	}
	@Inject(at=@At("HEAD"),method="getTargetingCrafter",remap=false)
	private static void fh$getTargetingCrafter(MechanicalCrafterTileEntity crafter,CallbackInfoReturnable<MechanicalCrafterTileEntity> cbi) {
		te=IOwnerTile.getOwner(crafter);
	}
	@Inject(at=@At("HEAD"),method="isRecipeAllowed",cancellable=true,remap=false)
	private static void fh$isRecipeAllowed(ICraftingRecipe recipe, CraftingInventory inventory,CallbackInfoReturnable<Boolean> cbi) {
		if(!ResearchListeners.canUseRecipe(te, recipe))
			cbi.setReturnValue(false);
		
	}
}
