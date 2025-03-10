package com.teammoeg.frostedresearch.item;

import com.teammoeg.chorda.creativeTab.CreativeTabItemHelper;
import com.teammoeg.chorda.creativeTab.ICreativeModeTabItem;
import com.teammoeg.frostedresearch.FRContents;

import net.minecraft.world.item.Item;

public class FRBaseItem extends Item implements ICreativeModeTabItem {

	public FRBaseItem(Properties pProperties) {
		super(pProperties);
	}

	@Override
	public void fillItemCategory(CreativeTabItemHelper helper) {
		if(helper.isType(FRContents.Tabs.BLOCK_TAB_TYPE))
			helper.accept(this);
		
	}

}
