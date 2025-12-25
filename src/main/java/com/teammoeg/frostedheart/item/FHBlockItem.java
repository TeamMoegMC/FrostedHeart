package com.teammoeg.frostedheart.item;

import com.teammoeg.chorda.creativeTab.TabType;
import com.teammoeg.chorda.item.CBlockItem;
import com.teammoeg.frostedheart.bootstrap.client.FHTabs;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class FHBlockItem extends CBlockItem {

    public FHBlockItem(Block arg0, Properties arg1, TabType arg2) {
		super(arg0, arg1, arg2);
	}
	public FHBlockItem(Block block) {
        this(block, new Item.Properties(), FHTabs.itemGroup);
        
    }
    public FHBlockItem(Block block, Item.Properties props) {
    	this(block, props, FHTabs.itemGroup);
    }

}
