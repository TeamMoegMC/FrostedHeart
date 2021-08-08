package com.teammoeg.frostedheart.block;

import net.minecraft.block.Block;
import net.minecraft.item.Item;

import java.util.function.BiFunction;

public class GeneratorCoreBlock extends FHBaseBlock {

    public GeneratorCoreBlock(String name, Properties blockProps, BiFunction<Block, Item.Properties, Item> createItemBlock) {
        super(name, blockProps, createItemBlock);
    }
}
