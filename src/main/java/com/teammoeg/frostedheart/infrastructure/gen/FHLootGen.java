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

package com.teammoeg.frostedheart.infrastructure.gen;

import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;

import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.providers.loot.RegistrateBlockLootTables;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import net.minecraft.advancements.critereon.EnchantmentPredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.server.packs.PackType;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.AlternativesEntry;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemEntityPropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

public class FHLootGen {
    public static final LootItemCondition.Builder HAS_SILK_TOUCH = MatchTool.toolMatches(ItemPredicate.Builder.item().hasEnchantment(new EnchantmentPredicate(Enchantments.SILK_TOUCH, MinMaxBounds.Ints.atLeast(1))));
    public static final LootItemCondition.Builder HAS_NO_SILK_TOUCH = HAS_SILK_TOUCH.invert();
    public static final Set<Block> existedBlocks=new HashSet<>();
    public static <T extends Block, P> NonNullFunction<BlockBuilder<T, P>, BlockBuilder<T, P>> pickaxeOnly() {
        return b -> b.tag(BlockTags.MINEABLE_WITH_PICKAXE);
    }
    
    
    /**
     * Nop
     * */
    public static <T extends Block> NonNullBiConsumer <RegistrateBlockLootTables, T> existed() {
        return (c, p) -> {existedBlocks.add(p);};
    }

//    public static <T extends Block, P> NonNullFunction<BlockBuilder<T, P>, BlockBuilder<T, P>> snow() {
//        return b -> LootTable.lootTable().withPool(LootPool.lootPool().when(LootItemEntityPropertyCondition.entityPresent(LootContext.EntityTarget.THIS)).add(AlternativesEntry.alternatives(AlternativesEntry.alternatives(SnowLayerBlock.LAYERS.getPossibleValues(), (p_252097_) -> {
//            return LootItem.lootTableItem(Items.SNOWBALL).when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(b.get().get()).setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(SnowLayerBlock.LAYERS, p_252097_))).apply(SetItemCountFunction.setCount(ConstantValue.exactly((float)p_252097_.intValue())));
//        }).when(HAS_NO_SILK_TOUCH), AlternativesEntry.alternatives(SnowLayerBlock.LAYERS.getPossibleValues(), (p_251216_) -> {
//            return (LootPoolEntryContainer.Builder<?>)(p_251216_ == 8 ? LootItem.lootTableItem(Blocks.SNOW_BLOCK) : LootItem.lootTableItem(Blocks.SNOW).apply(SetItemCountFunction.setCount(ConstantValue.exactly((float)p_251216_.intValue()))).when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(b.getEntry()).setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(SnowLayerBlock.LAYERS, p_251216_))));
//        }))));
//    }

    /**
     * Build a loot table for snow like layers.
     * @param self The snow layer block
     * @param block The snow block
     * @param item The item to drop when not silk touch
     * @return The loot table builder
     */
    public static LootTable.Builder buildSnowLootTable(Block self, Block block, Item item) {
        return LootTable.lootTable().withPool(LootPool.lootPool()
                .when(LootItemEntityPropertyCondition.entityPresent(LootContext.EntityTarget.THIS))
                .add(AlternativesEntry.alternatives(
                                // Snowball drops when not silk touch
                                AlternativesEntry.alternatives(SnowLayerBlock.LAYERS.getPossibleValues(),
                                        (layers) -> LootItem.lootTableItem(item)
                                                .when(LootItemBlockStatePropertyCondition
                                                        .hasBlockStateProperties(self)
                                                        .setProperties(StatePropertiesPredicate.Builder.properties()
                                                                .hasProperty(SnowLayerBlock.LAYERS, layers)))
                                                .apply(SetItemCountFunction.setCount(ConstantValue.exactly((float) layers)))
                                ).when(FHLootGen.HAS_NO_SILK_TOUCH),
                                // Snow block drops when silk touches
                                AlternativesEntry.alternatives(SnowLayerBlock.LAYERS.getPossibleValues(),
                                        (layers) -> layers == 8 ? LootItem.lootTableItem(block) :
                                                LootItem.lootTableItem(self)
                                                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly((float) layers)))
                                                        .when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(self).setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(SnowLayerBlock.LAYERS, layers)))
                                )
                        )
                )
        );
    }
}
