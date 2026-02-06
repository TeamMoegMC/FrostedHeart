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

package com.teammoeg.frostedheart.data;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.common.FHEntityTypes;
import com.teammoeg.frostedheart.bootstrap.common.FHItems;

import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.data.loot.packs.VanillaBlockLoot;
import net.minecraft.data.loot.packs.VanillaEntityLoot;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.LootingEnchantFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

public class FHLootTableProvider extends LootTableProvider {
    public FHLootTableProvider(PackOutput pOutput) {
        super(pOutput, Set.of(), List.of(
                new SubProviderEntry(FHBlockLoot::new, LootContextParamSets.BLOCK),
                new SubProviderEntry(FHChestLoot::new, LootContextParamSets.CHEST),
                new SubProviderEntry(FHEntityLoot::new, LootContextParamSets.ENTITY)
                ));
    }

    static Block cp(String name) {
        return ForgeRegistries.BLOCKS.getValue(ResourceLocation.tryBuild(FHMain.MODID, name));
    }
    static Item cpi(String name) {
        return ForgeRegistries.ITEMS.getValue(ResourceLocation.tryBuild(FHMain.MODID, name));
    }
//    public static final ResourceKey<LootTable> ASSES= ResourceKey.create(ForgeRegistries.LOOT, FHMain.rl("asses"));


    public static class FHEntityLoot extends VanillaEntityLoot {
        public FHEntityLoot() {
        }

        public void generate() {
            this.add(FHEntityTypes.WANDERING_REFUGEE.get(), LootTable.lootTable()
                    .withPool(LootPool.lootPool()
                                    .setRolls(ConstantValue.exactly(1.0F))
                                    .add(LootItem.lootTableItem(Items.ROTTEN_FLESH)
                                            .apply(SetItemCountFunction.setCount(UniformGenerator.between(0.0F, 2.0F)))
                                            .apply(LootingEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F)))))
                    .withPool(LootPool.lootPool()
                                    .setRolls(ConstantValue.exactly(1.0F))
                                    .add(LootItem.lootTableItem(Items.BONE)
                                            .apply(SetItemCountFunction.setCount(UniformGenerator.between(0.0F, 2.0F)))
                                            .apply(LootingEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F)))))
                    // add FHItems.hide_boots
                    /*.withPool(LootPool.lootPool()
                                    .setRolls(ConstantValue.exactly(1.0F))
                                    .add(LootItem.lootTableItem(FHItems.FH_CLOTHES.get("hide_boots").get())
                                            .apply(SetItemCountFunction.setCount(UniformGenerator.between(0.0F, 1.0F)))
                                            .apply(LootingEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F))))
                    // add FHItems.hide_leggings
                                    .add(LootItem.lootTableItem(FHItems.FH_CLOTHES.get("hide_hat").get())
                                            .apply(SetItemCountFunction.setCount(UniformGenerator.between(0.0F, 1.0F)))
                                            .apply(LootingEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F))))
                    // add FHItems.hide_chestplate
                                    .add(LootItem.lootTableItem(FHItems.FH_CLOTHES.get("hide_jacket").get())
                                            .apply(SetItemCountFunction.setCount(UniformGenerator.between(0.0F, 1.0F)))
                                            .apply(LootingEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F))))
                    // add FHItems.hide_pants
                                    .add(LootItem.lootTableItem(FHItems.FH_CLOTHES.get("hide_pants").get())
                                            .apply(SetItemCountFunction.setCount(UniformGenerator.between(0.0F, 1.0F)))
                                            .apply(LootingEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F)))))*/
                    // add foods
                    .withPool(LootPool.lootPool()
                                    .setRolls(ConstantValue.exactly(1.0F))
                                    .add(LootItem.lootTableItem(FHItems.black_bread.get())
                                            .apply(SetItemCountFunction.setCount(UniformGenerator.between(0.0F, 2.0F)))
                                            .apply(LootingEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F))))
                                    .add(LootItem.lootTableItem(FHItems.rye_bread.get())
                                            .apply(SetItemCountFunction.setCount(UniformGenerator.between(0.0F, 2.0F)))
                                            .apply(LootingEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F))))
                                    .add(LootItem.lootTableItem(FHItems.military_rations.get())
                                            .apply(SetItemCountFunction.setCount(UniformGenerator.between(0.0F, 2.0F)))
                                            .apply(LootingEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F))))
                                    .add(LootItem.lootTableItem(FHItems.compressed_biscuits.get())
                                            .apply(SetItemCountFunction.setCount(UniformGenerator.between(0.0F, 2.0F)))
                                            .apply(LootingEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F)))))
            );

            // curiosity, just add snowballs, todo: add condense ores
            this.add(FHEntityTypes.CURIOSITY.get(), LootTable.lootTable()
                    .withPool(LootPool.lootPool()
                                    .setRolls(ConstantValue.exactly(1.0F))
                                    .add(LootItem.lootTableItem(Items.SNOWBALL)
                                            .apply(SetItemCountFunction.setCount(UniformGenerator.between(0.0F, 8.0F)))
                                            .apply(LootingEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F)))))
            );

        }

        @Override
        protected Stream<EntityType<?>> getKnownEntityTypes() {
            return FHEntityTypes.ENTITY_TYPES.getEntries().stream().map(RegistryObject::get);
        }
    }

    private static class FHChestLoot implements LootTableSubProvider {

        public FHChestLoot() {
        }

        @Override
        public void generate(BiConsumer<ResourceLocation, LootTable.Builder> receiver) {
//            receiver.accept(ASSES, LootTable.lootTable().withPool(LootPool.lootPool().add(LootItem.lootTableItem(cpi("asses"))).when(LootItemRandomChanceCondition.randomChance(0.2f)).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0f, 5.0f)))));
        }
    }

    private static class FHBlockLoot extends VanillaBlockLoot {
        protected FHBlockLoot() {
            super();
        }

        @Override
        protected void generate() {
//            dropSelf(cp("asses"));
        }

        ArrayList<Block> added = new ArrayList<>();

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return added;
        }

        protected void add(Block pBlock, LootTable.Builder pLootTableBuilder) {
            added.add(pBlock);
            super.add(pBlock, pLootTableBuilder);
        }

        @SuppressWarnings("rawtypes")
        protected LootTable.Builder doublePlantDrop(Block pBlock,LootItem.Builder pItemBuilder){
            return LootTable.lootTable();
        }

    }

}
