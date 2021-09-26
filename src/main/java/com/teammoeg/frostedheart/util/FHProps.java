package com.teammoeg.frostedheart.util;

import com.teammoeg.frostedheart.FHMain;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;
import net.minecraftforge.common.ToolType;

public class FHProps {
    public static void init() {
    }

    public static final AbstractBlock.Properties stoneDecoProps = AbstractBlock.Properties
            .create(Material.ROCK)
            .sound(SoundType.STONE)
            .setRequiresTool()
            .harvestTool(ToolType.PICKAXE)
            .hardnessAndResistance(2, 10);
    public static final AbstractBlock.Properties cropProps = AbstractBlock.Properties
            .create(Material.PLANTS)
            .doesNotBlockMovement()
            .tickRandomly()
            .zeroHardnessAndResistance()
            .sound(SoundType.CROP);
    public static final Item.Properties itemProps = new Item.Properties().group(FHMain.itemGroup);
}
