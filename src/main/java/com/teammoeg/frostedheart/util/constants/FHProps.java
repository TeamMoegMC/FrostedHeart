/*
 * Copyright (c) 2021-2024 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart.util.constants;

import com.teammoeg.frostedheart.FHMain;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;
import net.minecraftforge.common.ToolType;

public class FHProps {
    public static final AbstractBlock.Properties stoneDecoProps = AbstractBlock.Properties
            .of(Material.STONE)
            .sound(SoundType.STONE)
            .requiresCorrectToolForDrops()
            .harvestTool(ToolType.PICKAXE)
            .strength(2, 10);

    public static final AbstractBlock.Properties metalDecoProps = AbstractBlock.Properties
            .of(Material.METAL)
            .sound(SoundType.METAL)
            .requiresCorrectToolForDrops()
            .harvestTool(ToolType.PICKAXE)
            .strength(2, 10);
    public static final AbstractBlock.Properties cropProps = AbstractBlock.Properties
            .of(Material.PLANT)
            .noCollission()
            .randomTicks()
            .instabreak()
            .sound(SoundType.CROP);
    public static final AbstractBlock.Properties ore_gravel = AbstractBlock.Properties
            .of(Material.SAND)
            .sound(SoundType.GRAVEL)
            .requiresCorrectToolForDrops()
            .harvestTool(ToolType.SHOVEL)
            .strength(0.6F);
    public static final AbstractBlock.Properties redStoneProps = AbstractBlock.Properties
            .of(Material.BUILDABLE_GLASS)
            .noCollission()
            .instabreak()
            .sound(SoundType.STONE);

    public static final AbstractBlock.Properties stoneProps = AbstractBlock.Properties
            .of(Material.STONE)
            .sound(SoundType.STONE)
            .noCollission()
            .requiresCorrectToolForDrops()
            .harvestTool(ToolType.PICKAXE)
            .strength(2, 10);

    public static final AbstractBlock.Properties woodenProps = AbstractBlock.Properties
            .of(Material.WOOD)
            .noCollission()
            .requiresCorrectToolForDrops()
            .harvestTool(ToolType.AXE)
            .sound(SoundType.WOOD)
            .strength(0.6F);

    public static final AbstractBlock.Properties grassProps = AbstractBlock.Properties
            .of(Material.PLANT)
            .noCollission()
            .requiresCorrectToolForDrops()
            .harvestTool(ToolType.AXE)
            .sound(SoundType.GRASS)
            .strength(0.3F);

    public static final Item.Properties itemProps = new Item.Properties().tab(FHMain.itemGroup);
    public static final AbstractBlock.Properties berryBushBlocks = AbstractBlock.Properties.of(Material.PLANT).randomTicks().noCollission().sound(SoundType.SWEET_BERRY_BUSH);
    public static void init() {
    }
}
