/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.frostedheart.bootstrap.reference;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public class FHProps {
    public static final BlockBehaviour.Properties stoneDecoProps = BlockBehaviour.Properties
            .of()
            .sound(SoundType.STONE)
            .mapColor(MapColor.STONE)
            .requiresCorrectToolForDrops()
            .strength(2, 10);

    public static final BlockBehaviour.Properties metalDecoProps = BlockBehaviour.Properties
            .of()
            .sound(SoundType.METAL)
            .mapColor(MapColor.METAL)
            .requiresCorrectToolForDrops()
            .strength(2, 10);
    public static final BlockBehaviour.Properties cropProps = BlockBehaviour.Properties
            .of()
            .mapColor(MapColor.PLANT)
            .noCollission()
            .randomTicks()
            .instabreak()
            .sound(SoundType.CROP);
    public static final BlockBehaviour.Properties ore_gravel = BlockBehaviour.Properties
            .of()
            .sound(SoundType.GRAVEL)
            .mapColor(MapColor.SAND)
            .requiresCorrectToolForDrops()
            .strength(0.6F);
    public static final BlockBehaviour.Properties redStoneProps = BlockBehaviour.Properties
            .of()
            .mapColor(MapColor.NONE)
            .noCollission()
            .instabreak()
            .sound(SoundType.STONE);

    public static final BlockBehaviour.Properties stoneProps = BlockBehaviour.Properties
            .of()
            .sound(SoundType.STONE)
            .mapColor(MapColor.STONE)
            .noCollission()
            .requiresCorrectToolForDrops()
            .strength(2, 10);

    public static final BlockBehaviour.Properties woodenProps = BlockBehaviour.Properties
    		.of()
            .noCollission()
            .mapColor(MapColor.WOOD)
            .requiresCorrectToolForDrops()
            .sound(SoundType.WOOD)
            .strength(0.6F);

    public static final BlockBehaviour.Properties grassProps = BlockBehaviour.Properties
            .of()
            .mapColor(MapColor.PLANT)
            .noCollission()
            .requiresCorrectToolForDrops()
            .sound(SoundType.GRASS)
            .strength(0.3F);

    public static final Item.Properties itemProps = new Item.Properties();
    public static final BlockBehaviour.Properties berryBushBlocks = BlockBehaviour.Properties.of()
    	.mapColor(MapColor.PLANT)
    	.randomTicks().noCollission().sound(SoundType.SWEET_BERRY_BUSH);
    public static void init() {
    }
}
