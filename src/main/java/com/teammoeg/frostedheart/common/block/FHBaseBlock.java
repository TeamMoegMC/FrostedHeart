package com.teammoeg.frostedheart.common.block;

import blusunrize.immersiveengineering.ImmersiveEngineering;
import blusunrize.immersiveengineering.common.blocks.IEBaseBlock;
import com.teammoeg.frostedheart.FHMain;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;

import java.util.function.BiFunction;

public class FHBaseBlock extends IEBaseBlock {
    public FHBaseBlock(String name, Properties blockProps, BiFunction<Block, Item.Properties, Item> createItemBlock) {
        super(name, blockProps, createItemBlock);
    }

    @Override
    public ResourceLocation createRegistryName()
    {
        return new ResourceLocation(FHMain.MODID, name);
    }
}
