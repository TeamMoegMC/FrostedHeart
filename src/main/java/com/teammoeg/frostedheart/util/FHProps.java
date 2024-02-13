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

package com.teammoeg.frostedheart.util;

import com.teammoeg.frostedheart.FHMain;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;
import net.minecraftforge.common.ToolType;

public class FHProps {
    public static final AbstractBlock.Properties stoneDecoProps = AbstractBlock.Properties
            .create(Material.ROCK)
            .sound(SoundType.STONE)
            .setRequiresTool()
            .harvestTool(ToolType.PICKAXE)
            .hardnessAndResistance(2, 10);

    public static final AbstractBlock.Properties metalDecoProps = AbstractBlock.Properties
            .create(Material.IRON)
            .sound(SoundType.METAL)
            .setRequiresTool()
            .harvestTool(ToolType.PICKAXE)
            .hardnessAndResistance(2, 10);
    public static final AbstractBlock.Properties cropProps = AbstractBlock.Properties
            .create(Material.PLANTS)
            .doesNotBlockMovement()
            .tickRandomly()
            .zeroHardnessAndResistance()
            .sound(SoundType.CROP);
    public static final AbstractBlock.Properties ore_gravel = AbstractBlock.Properties
            .create(Material.SAND)
            .sound(SoundType.GROUND)
            .setRequiresTool()
            .harvestTool(ToolType.SHOVEL)
            .hardnessAndResistance(0.6F);
    public static final AbstractBlock.Properties redStoneProps = AbstractBlock.Properties
            .create(Material.REDSTONE_LIGHT)
            .doesNotBlockMovement()
            .zeroHardnessAndResistance()
            .sound(SoundType.STONE);

    public static final AbstractBlock.Properties stoneProps = AbstractBlock.Properties
            .create(Material.ROCK)
            .sound(SoundType.STONE)
            .doesNotBlockMovement()
            .setRequiresTool()
            .harvestTool(ToolType.PICKAXE)
            .hardnessAndResistance(2, 10);

    public static final AbstractBlock.Properties woodenProps = AbstractBlock.Properties
            .create(Material.WOOD)
            .doesNotBlockMovement()
            .setRequiresTool()
            .harvestTool(ToolType.AXE)
            .sound(SoundType.WOOD)
            .hardnessAndResistance(0.6F);

    public static final AbstractBlock.Properties grassProps = AbstractBlock.Properties
            .create(Material.PLANTS)
            .doesNotBlockMovement()
            .setRequiresTool()
            .harvestTool(ToolType.AXE)
            .sound(SoundType.PLANT)
            .hardnessAndResistance(0.3F);

    public static final Item.Properties itemProps = new Item.Properties().group(FHMain.itemGroup);
    public static final AbstractBlock.Properties berryBushBlocks = AbstractBlock.Properties.create(Material.PLANTS).tickRandomly().doesNotBlockMovement().sound(SoundType.SWEET_BERRY_BUSH);
    public static void init() {
    }
}
