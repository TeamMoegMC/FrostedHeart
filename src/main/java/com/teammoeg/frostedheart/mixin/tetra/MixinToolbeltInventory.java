package com.teammoeg.frostedheart.mixin.tetra;

import java.util.function.Predicate;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import blusunrize.immersiveengineering.api.IEApi;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import se.mickelus.tetra.items.modular.impl.toolbelt.ModularToolbeltItem;
import se.mickelus.tetra.items.modular.impl.toolbelt.inventory.ToolbeltInventory;

@Mixin(ToolbeltInventory.class)
public abstract class MixinToolbeltInventory  implements Container{

	public MixinToolbeltInventory() {
	}
	@Shadow(remap=false)
	protected Predicate<ItemStack> predicate;
	@Overwrite
	@Override
	public boolean canPlaceItem(int pIndex, ItemStack pStack) {
		return (!pStack.is(ModularToolbeltItem.instance.get()) && this.predicate.test(pStack)&&IEApi.isAllowedInCrate(pStack));
	}

	@Overwrite(remap=false)
	public boolean isItemValid(ItemStack itemStack) {
		return (!itemStack.is(ModularToolbeltItem.instance.get())&&(!itemStack.hasTag()||itemStack.getItem().getMaxStackSize()==1) && this.predicate.test(itemStack)&&IEApi.isAllowedInCrate(itemStack));
	}
}
