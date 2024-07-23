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

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ToolType;

public class FHProps {
    public static final BlockBehaviour.Properties stoneDecoProps = BlockBehaviour.Properties
            .of(Material.STONE)
            .sound(SoundType.STONE)
            .requiresCorrectToolForDrops()
            .harvestTool(ToolType.PICKAXE)
            .strength(2, 10);

    public static final BlockBehaviour.Properties metalDecoProps = BlockBehaviour.Properties
            .of(Material.METAL)
            .sound(SoundType.METAL)
            .requiresCorrectToolForDrops()
            .harvestTool(ToolType.PICKAXE)
            .strength(2, 10);
    public static final BlockBehaviour.Properties cropProps = BlockBehaviour.Properties
            .of(Material.PLANT)
            .noCollission()
            .randomTicks()
            .instabreak()
            .sound(SoundType.CROP);
    public static final BlockBehaviour.Properties ore_gravel = BlockBehaviour.Properties
            .of(Material.SAND)
            .sound(SoundType.GRAVEL)
            .requiresCorrectToolForDrops()
            .harvestTool(ToolType.SHOVEL)
            .strength(0.6F);
    public static final BlockBehaviour.Properties redStoneProps = BlockBehaviour.Properties
            .of(Material.BUILDABLE_GLASS)
            .noCollission()
            .instabreak()
            .sound(SoundType.STONE);

    public static final BlockBehaviour.Properties stoneProps = BlockBehaviour.Properties
            .of(Material.STONE)
            .sound(SoundType.STONE)
            .noCollission()
            .requiresCorrectToolForDrops()
            .harvestTool(ToolType.PICKAXE)
            .strength(2, 10);

    public static final BlockBehaviour.Properties woodenProps = BlockBehaviour.Properties
            .of(Material.WOOD)
            .noCollission()
            .requiresCorrectToolForDrops()
            .harvestTool(ToolType.AXE)
            .sound(SoundType.WOOD)
            .strength(0.6F);

    public static final BlockBehaviour.Properties grassProps = BlockBehaviour.Properties
            .of(Material.PLANT)
            .noCollission()
            .requiresCorrectToolForDrops()
            .harvestTool(ToolType.AXE)
            .sound(SoundType.GRASS)
            .strength(0.3F);

    public static final Item.Properties itemProps = new Item.Properties().tab(FHMain.itemGroup);
    public static final BlockBehaviour.Properties berryBushBlocks = BlockBehaviour.Properties.of(Material.PLANT).randomTicks().noCollission().sound(SoundType.SWEET_BERRY_BUSH);
    public static void init() {
    }
}
