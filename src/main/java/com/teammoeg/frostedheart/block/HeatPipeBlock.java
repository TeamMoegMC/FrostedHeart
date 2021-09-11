package com.teammoeg.frostedheart.block;

import java.util.function.BiFunction;

import net.minecraft.block.Block;
import net.minecraft.item.Item;

public class HeatPipeBlock extends FHBaseBlock {

	public HeatPipeBlock(String name, Properties blockProps,
			BiFunction<Block, net.minecraft.item.Item.Properties, Item> createItemBlock) {
		super(name, blockProps, createItemBlock);
	}

}
