/*
 * Copyright (c) 2022-2024 TeamMoeg
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

package com.teammoeg.frostedheart.util.mixin;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.gen.feature.template.Template;
import net.minecraft.world.gen.feature.template.Template.BlockInfo;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StructureUtils {
    static List<Block> baned = new ArrayList<>();
    static Map<Block, Block> remap = new HashMap<>();

    public static void addBanedBlocks() {
        baned.clear();
        remap.clear();
        baned.add(Blocks.CRAFTING_TABLE);
        baned.add(Blocks.FURNACE);
        baned.add(Blocks.SMOKER);
        baned.add(Blocks.BLAST_FURNACE);
        RemapRL(Blocks.CHEST, new ResourceLocation("stone_age", "stone_chest"));
    }

    public static Block getChest() {
        Block b = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("stone_age", "stone_chest"));
        if (b == null || b == Blocks.AIR)
            return Blocks.TRAPPED_CHEST;
        return b;
    }

    public static void handlePalette(List<Template.Palette> p) {

        p.forEach(q -> q.func_237157_a_().replaceAll(r -> {

            if (baned.contains(r.state.getBlock())) {
                return new BlockInfo(r.pos, Blocks.AIR.getDefaultState(), null);
            } else if (remap.containsKey(r.state.getBlock())) {
                return new BlockInfo(r.pos, remap.get(r.state.getBlock()).getDefaultState(), r.nbt);
            }
            return r;
        }));
    }

    public static boolean isBanned(Block b) {
        return baned.contains(b);
    }

    public static void RemapRL(Block org, ResourceLocation dest) {
        remap.put(org, ForgeRegistries.BLOCKS.getValue(dest));
    }
}
