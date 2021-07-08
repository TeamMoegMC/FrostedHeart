package com.teammoeg.frostedheart.common.block.cropblock;

import blusunrize.immersiveengineering.ImmersiveEngineering;
import com.teammoeg.frostedheart.FHContent;
import net.minecraft.block.Block;
import net.minecraft.block.CropsBlock;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;

import java.util.function.BiFunction;

public class FHCropBlock extends CropsBlock {
    public final String name;

    public FHCropBlock(String name, Properties builder, BiFunction<Block, Item.Properties, Item> createItemBlock) {
        super(builder);
        this.name = name;
        FHContent.registeredFHBlocks.add(this);
        ResourceLocation registryName = createRegistryName();
        setRegistryName(registryName);
        Item item = createItemBlock.apply(this, new Item.Properties().group(ImmersiveEngineering.itemGroup));
        if (item != null) {
            item.setRegistryName(registryName);
            FHContent.registeredFHItems.add(item);
        }
    }

    public ResourceLocation createRegistryName() {
        return new ResourceLocation(ImmersiveEngineering.MODID, name);
    }
}
