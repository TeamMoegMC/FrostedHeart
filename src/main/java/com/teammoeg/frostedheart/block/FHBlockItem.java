package com.teammoeg.frostedheart.block;

import com.teammoeg.frostedheart.FHMain;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;

public class FHBlockItem extends BlockItem {
    public FHBlockItem(Block block, Item.Properties props) {
        super(block, props);
    }

    public FHBlockItem(Block block) {
        this(block, new Item.Properties().group(FHMain.itemGroup));
        setRegistryName(block.getRegistryName());
    }
}
