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
//package com.teammoeg.frostedheart.data;
//
//import com.teammoeg.frostedheart.FHMain;
//import net.minecraft.core.HolderLookup;
//import net.minecraft.core.registries.BuiltInRegistries;
//import net.minecraft.core.registries.Registries;
//import net.minecraft.data.PackOutput;
//import net.minecraft.data.loot.LootTableProvider;
//import net.minecraft.data.loot.LootTableSubProvider;
//import net.minecraft.data.loot.packs.VanillaBlockLoot;
//import net.minecraft.resources.ResourceKey;
//import net.minecraft.resources.ResourceLocation;
//import net.minecraft.world.item.Item;
//import net.minecraft.world.level.block.Block;
//import net.minecraft.world.level.storage.loot.LootPool;
//import net.minecraft.world.level.storage.loot.LootTable;
//import net.minecraft.world.level.storage.loot.entries.LootItem;
//import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
//import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
//import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
//import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
//import net.minecraftforge.registries.ForgeRegistries;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//import java.util.Set;
//import java.util.function.BiConsumer;
//
//public class FHLootTableProvider extends LootTableProvider {
//    public FHLootTableProvider(PackOutput pOutput, Set<ResourceLocation> pRequiredTables, List<SubProviderEntry> pSubProviders) {
//        super(pOutput, pRequiredTables, pSubProviders);
//    }
//
//    @Override
//    public List<SubProviderEntry> getTables() {
//        return Arrays.asList(new SubProviderEntry(LTBuilder::new, LootContextParamSets.BLOCK),new SubProviderEntry(OTHBuilder::new, LootContextParamSets.CHEST));
//    }
//
//    static Block cp(String name) {
//        return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(FHMain.MODID, name));
//    }
//    static Item cpi(String name) {
//        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(FHMain.MODID, name));
//    }
//    public static final ResourceKey<LootTable> ASSES= ResourceKey.create(ForgeRegistries.LOOT, FHMain.rl("asses"));
//
//    private static class OTHBuilder implements LootTableSubProvider {
//
//        public OTHBuilder(HolderLookup.Provider registry) {
//        }
//
//        @Override
//        public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> receiver) {
//            receiver.accept(ASSES, LootTable.lootTable().withPool(LootPool.lootPool().add(LootItem.lootTableItem(cpi("asses"))).when(LootItemRandomChanceCondition.randomChance(0.2f)).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0f, 5.0f)))));
//        }
//
//    }
//
//    private static class LTBuilder extends VanillaBlockLoot {
//        protected LTBuilder(HolderLookup.Provider registry) {
//            super(registry);
//        }
//
//        @Override
//        protected void generate() {
//            dropSelf(cp("asses"));
//        }
//
//        ArrayList<Block> added = new ArrayList<>();
//
//        @Override
//        protected Iterable<Block> getKnownBlocks() {
//            return added;
//        }
//
//
//        protected void add(Block pBlock, LootTable.Builder pLootTableBuilder) {
//            added.add(pBlock);
//            super.add(pBlock, pLootTableBuilder);
//        }
//        @SuppressWarnings("rawtypes")
//        protected LootTable.Builder doublePlantDrop(Block pBlock,LootItem.Builder pItemBuilder){
//            return LootTable.lootTable();
//        }
//
//    }
//
//}
