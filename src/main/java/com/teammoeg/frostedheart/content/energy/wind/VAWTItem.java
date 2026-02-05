package com.teammoeg.frostedheart.content.energy.wind;

import com.teammoeg.frostedheart.content.energy.wind.VAWTBlock.VAWTType;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public class VAWTItem extends BlockItem {
	@Override
	public boolean isDamageable(ItemStack stack) {
		return true;
	}

	VAWTType type;
	public VAWTItem(Block block, Properties props,VAWTType type) {
		super(block, props);
		this.type=type;
		// TODO Auto-generated constructor stub
	}

	@Override
	public int getMaxDamage(ItemStack stack) {
		return type.getDurability();
	}

}
