package com.teammoeg.frostedheart.content.decoration;

import com.teammoeg.frostedheart.base.block.FHBaseBlock;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;

import java.util.function.BiFunction;

public class AccessControlBlock extends FHBaseBlock {
    public AccessControlBlock(String name, BiFunction<Block, Item.Properties, Item> createItemBlock) {
        super(name, Block.Properties.create(Material.IRON).sound(SoundType.STONE).setRequiresTool()
                .hardnessAndResistance(0, 2000).notSolid(), createItemBlock);

    }
}
