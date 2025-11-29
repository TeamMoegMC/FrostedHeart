///*
// * Copyright (c) 2024 TeamMoeg
// *
// * This file is part of Frosted Heart.
// *
// * Frosted Heart is free software: you can redistribute it and/or modify
// * it under the terms of the GNU General Public License as published by
// * the Free Software Foundation, version 3.
// *
// * Frosted Heart is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
// *
// */
//
//package com.teammoeg.frostedresearch.data;
//
//import com.teammoeg.frostedheart.FHMain;
//import com.teammoeg.frostedresearch.FRContents;
//
//import net.minecraft.data.PackOutput;
//import net.minecraft.data.loot.LootTableProvider;
//import net.minecraft.data.loot.packs.VanillaBlockLoot;
//import net.minecraft.resources.ResourceLocation;
//import net.minecraft.world.item.Item;
//import net.minecraft.world.level.block.Block;
//import net.minecraft.world.level.storage.loot.LootTable;
//import net.minecraft.world.level.storage.loot.entries.LootItem;
//import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
//import net.minecraftforge.registries.ForgeRegistries;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Set;
//
//public class FRLootTableProvider extends LootTableProvider {
//    public FRLootTableProvider(PackOutput pOutput) {
//        super(pOutput, Set.of(), List.of(
//                new SubProviderEntry(FRBlockLoot::new, LootContextParamSets.BLOCK)
//                ));
//    }
//
//    static Block cp(String name) {
//        return ForgeRegistries.BLOCKS.getValue(ResourceLocation.tryBuild(FHMain.MODID, name));
//    }
//    static Item cpi(String name) {
//        return ForgeRegistries.ITEMS.getValue(ResourceLocation.tryBuild(FHMain.MODID, name));
//    }
////    public static final ResourceKey<LootTable> ASSES= ResourceKey.create(ForgeRegistries.LOOT, FHMain.rl("asses"));
//
//
//
//    private static class FRBlockLoot extends VanillaBlockLoot {
//        protected FRBlockLoot() {
//            super();
//        }
//
//        @Override
//        protected void generate() {
////            dropSelf(cp("asses"));
//        	dropSelf(FRContents.Blocks.MECHANICAL_CALCULATOR.get());
//        }
//
//        ArrayList<Block> added = new ArrayList<>();
//
//        @Override
//        protected Iterable<Block> getKnownBlocks() {
//            return added;
//        }
//
//        protected void add(Block pBlock, LootTable.Builder pLootTableBuilder) {
//            added.add(pBlock);
//            super.add(pBlock, pLootTableBuilder);
//        }
//
//        @SuppressWarnings("rawtypes")
//        protected LootTable.Builder doublePlantDrop(Block pBlock,LootItem.Builder pItemBuilder){
//            return LootTable.lootTable();
//        }
//
//    }
//
//}
