package com.teammoeg.frostedheart.mixin.immersiveengineering;

import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import com.teammoeg.frostedheart.research.ResearchListeners;
import com.teammoeg.frostedheart.util.IOwnerTile;

import blusunrize.immersiveengineering.common.blocks.metal.AssemblerTileEntity;
import blusunrize.immersiveengineering.common.blocks.metal.AssemblerTileEntity.CrafterPatternInventory;
import blusunrize.immersiveengineering.common.util.Utils;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.ICraftingRecipe;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.NonNullList;

@Mixin(CrafterPatternInventory.class)
public class CrafterPatternInventoryMixin {
	@Shadow(remap=false)
	NonNullList<ItemStack> inv;
	@Shadow(remap=false)
	IRecipe recipe;
	@Shadow(remap=false)
	AssemblerTileEntity tile;
	public CrafterPatternInventoryMixin() {
	}
	/**
	 * @author khjxiaogu
	 * @reason add our own research pending
	 * */
	@Overwrite(remap=false)
	public void recalculateOutput()
	{
		if(tile.getWorld()!=null)
		{
			CraftingInventory invC = Utils.InventoryCraftingFalse.createFilledCraftingInventory(3, 3, inv);
			this.recipe = Utils.findCraftingRecipe(invC, tile.getWorldNonnull()).orElse(null);
			AssemblerTileEntity nte=tile;
			if(!nte.isDummy()) {
				UUID ow=IOwnerTile.getOwner(nte);
				if(tile.getWorld().isRemote) {
					if(!ResearchListeners.canUseRecipe(recipe))
						this.recipe=null;
				}else if(!ResearchListeners.canUseRecipe(ow, recipe))
					this.recipe=null;
			}
			this.inv.set(9, recipe!=null?recipe.getCraftingResult(invC): ItemStack.EMPTY);
		}
	}

}
