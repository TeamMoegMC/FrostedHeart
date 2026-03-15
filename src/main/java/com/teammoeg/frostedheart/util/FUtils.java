/*
 * Copyright (c) 2026 TeamMoeg
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

import com.teammoeg.caupona.blocks.plants.BushLogBlock;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.infrastructure.gen.FHBlockStateGen;
import com.teammoeg.frostedheart.infrastructure.gen.FHLootGen;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.grower.AbstractTreeGrower;
import net.minecraft.world.level.material.MapColor;

import java.util.function.Supplier;

import static com.teammoeg.frostedheart.FHMain.REGISTRATE;

public class FUtils {
    /*public static ItemStack ArmorLiningNBT(ItemStack stack) {
        stack.getOrCreateTag().putString("inner_cover", FHMain.MODID + ":straw_lining");
        stack.getTag().putBoolean("inner_bounded", true);//bound lining to arm or
        return CUtils.ArmorNBT(stack, 107, 6);
    }*/

    public static boolean isBeSnowed(Block block) {
        return block == FHBlocks.BESNOWED_DEBRIS.get() || block == FHBlocks.BESNOWED_TWIGS.get();
    }

    public record BushWoodSet(
            BlockEntry<BushLogBlock> log,
            BlockEntry<LeavesBlock> leaves,
            BlockEntry<SaplingBlock> sapling
    ) {

    }
    public static BushWoodSet registerBushSet(String woodName, Supplier<AbstractTreeGrower> growth) {
        BlockEntry<BushLogBlock> log = REGISTRATE.block(woodName + "_log", BushLogBlock::new)
                .properties(p -> p.mapColor(MapColor.WOOD).strength(2.0F).noOcclusion().sound(SoundType.WOOD))
                .blockstate(FHBlockStateGen.existed())
                .tag(BlockTags.LOGS)
                .tag(BlockTags.MINEABLE_WITH_AXE)
                .item()
                .build()
                .register();

        BlockEntry<LeavesBlock> leaves = REGISTRATE.block(woodName + "_leaves", LeavesBlock::new)
                .initialProperties(() -> Blocks.SPRUCE_LEAVES)
                .blockstate(FHBlockStateGen.simpleCubeAll("tree/jack_pine_leaves"))
                .tag(BlockTags.LEAVES)
                .loot(FHLootGen.existed())
                .item()
                .build()
                .register();

        BlockEntry<SaplingBlock> sapling = REGISTRATE.block(woodName + "_sapling", p -> new SaplingBlock(growth.get(), p))
                .initialProperties(() -> Blocks.SPRUCE_SAPLING)
                .blockstate(FHBlockStateGen.existed())
                .tag(BlockTags.SAPLINGS)
                .item()
                .build()
                .register();

        return new BushWoodSet(log, leaves, sapling);
    }

}
