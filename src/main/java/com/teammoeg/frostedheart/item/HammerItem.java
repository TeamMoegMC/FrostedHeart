package com.teammoeg.frostedheart.item;

import com.teammoeg.frostedheart.bootstrap.common.ToolCompat;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraftforge.common.ToolAction;

public class HammerItem extends DiggerItem {

	public HammerItem(Tier pTier, int pAttackDamageModifier, float pAttackSpeedModifier, Properties pProperties) {
		super(pAttackDamageModifier, pAttackSpeedModifier, pTier, BlockTags.MINEABLE_WITH_PICKAXE, pProperties);
	}

	@Override
	public boolean canPerformAction(ItemStack stack, ToolAction toolAction) {
		if(toolAction==ToolCompat.hammer)
			return true;
		return super.canPerformAction(stack, toolAction);
	}

}
