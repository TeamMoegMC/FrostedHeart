/*
 * Copyright (c) 2024 TeamMoeg
 *
 * This file is part of Caupona.
 *
 * Caupona is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Caupona is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * Specially, we allow this software to be used alongside with closed source software Minecraft(R) and Forge or other modloader.
 * Any mods or plugins can also use apis provided by forge or com.teammoeg.caupona.api without using GPL or open source.
 *
 * You should have received a copy of the GNU General Public License
 * along with Caupona. If not, see <https://www.gnu.org/licenses/>.
 */

package com.teammoeg.caupona.datagen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

import com.google.common.collect.ImmutableSet;
import com.teammoeg.caupona.CPBlocks;
import com.teammoeg.caupona.CPMain;
import com.teammoeg.caupona.util.MaterialType;

import net.minecraft.advancements.critereon.BlockPredicate;
import net.minecraft.advancements.critereon.LocationPredicate;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.data.loot.packs.VanillaBlockLoot;
import net.minecraft.data.loot.packs.VanillaLootTableProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.predicates.LocationCheck;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;


public class CPLootGenerator extends LootTableProvider {

	public CPLootGenerator(DataGenerator dataGeneratorIn,CompletableFuture<HolderLookup.Provider> provider) {
		super(dataGeneratorIn.getPackOutput(), Set.of(), VanillaLootTableProvider.create(dataGeneratorIn.getPackOutput(), provider).getTables(),provider);
	}

	@Override
	public List<SubProviderEntry> getTables() {
		return Arrays.asList(new SubProviderEntry(LTBuilder::new, LootContextParamSets.BLOCK),new SubProviderEntry(OTHBuilder::new, LootContextParamSets.CHEST));
	}
	static Block cp(String name) {
		return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(CPMain.MODID, name));
	}
	static Item cpi(String name) {
		return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(CPMain.MODID, name));
	}
	/*@Override
	protected void validate(Map<ResourceLocation, LootTable> map, ValidationContext validationcontext) {
		map.forEach((p_278897_, p_278898_) -> {
			p_278898_.validate(validationcontext.setParams(p_278898_.getParamSet())
					.enterElement("{" + p_278897_ + "}", new LootDataId<>(LootDataType.TABLE, p_278897_)));
		});
		//map.forEach((name, table) -> LootTables.validate(validationtracker, name, table));
	}*/
	public static final ResourceKey<LootTable> ASSES=ResourceKey.create(Registries.LOOT_TABLE, CPMain.rl("asses"));
	private static class OTHBuilder implements LootTableSubProvider {

		public OTHBuilder(HolderLookup.Provider registry) {
		}

		@Override
		public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> receiver) {
			receiver.accept(ASSES, LootTable.lootTable().withPool(LootPool.lootPool().add(LootItem.lootTableItem(cpi("asses"))).when(LootItemRandomChanceCondition.randomChance(0.2f)).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0f, 5.0f)))));
		}
		
	}
	private static class LTBuilder extends VanillaBlockLoot {
		protected LTBuilder(HolderLookup.Provider registry) {
			super(registry);
		}

		@Override
		protected void generate() {
			dropSelf(CPBlocks.STEW_POT.get());
			dropSelf(CPBlocks.STONE_PAN.get());
			dropSelf(CPBlocks.COPPER_PAN.get());
			dropSelf(CPBlocks.IRON_PAN.get());
			dropSelf(CPBlocks.LEAD_PAN.get());
			dropSelf(CPBlocks.STEW_POT_LEAD.get());
			dropSelf(CPBlocks.LEAD_BLOCK.get());
			dropSelf(CPBlocks.SNAIL_BAIT.get());
			add(CPBlocks.FUMAROLE_VENT.get(), createSilkTouchDispatchTable(CPBlocks.FUMAROLE_VENT.get(),
					LootItem.lootTableItem(Blocks.BASALT)));
			add(CPBlocks.PUMICE_BLOOM.get(), createSilkTouchDispatchTable(CPBlocks.PUMICE_BLOOM.get(),
					LootItem.lootTableItem(CPBlocks.PUMICE.get())));
			
			add(CPBlocks.SILPHIUM.get(),doublePlantDrop(CPBlocks.SILPHIUM.get(),LootItem.lootTableItem(cpi("silphium")).apply(SetItemCountFunction.setCount(UniformGenerator.between(3, 5), false))));
			dropSelf(CPBlocks.PUMICE.get());
			dropSelf(CPBlocks.T_BENCH.get());
			dropSelf(CPBlocks.WOLF.get());
			/*
			 * dropSelf(CPBlocks.stove1);
			 * dropSelf(CPBlocks.stove2);
			 * dropSelf(CPBlocks.stove3);
			 * dropSelf(CPBlocks.stove4);
			 * dropSelf(CPBlocks.stove5);
			 */
			for (String wood : CPBlocks.woods) {
				for (String type : ImmutableSet.of("_button",

						"_fence", "_fence_gate", "_log", "_planks", "_pressure_plate", "_sapling", "_sign",
						"_stairs", "_trapdoor", "_wood"))
					dropSelf(cp(wood + type));
				add(cp(wood+"_slab"),super.createSlabItemTable(cp(wood+"_slab")));
				add(cp(wood + "_door"), createDoorTable(cp(wood + "_door")));
				add(cp(wood + "_leaves"), createLeavesDrops(cp(wood + "_leaves"), cp(wood + "_sapling"), 0.05F, 0.0625F,
						0.083333336F, 0.1F));
				dropSelf(cp("stripped_"+wood+"_log"));
				dropSelf(cp("stripped_"+wood+"_wood"));
				dropOther(cp(wood + "_wall_sign"), cp(wood + "_sign"));
			}
			for(MaterialType rtype:CPBlocks.all_materials) {
				String stone=rtype.getName();
				if(rtype.isDecorationMaterial()) {
					for (String type : ImmutableSet.of("", "_stairs", "_wall"))
						dropSelf(cp(stone + type));
					add(cp(stone+"_slab"),super.createSlabItemTable(cp(stone+"_slab")));
				}
				if(rtype.isCounterMaterial()) {
					for (String type : ImmutableSet.of("_chimney_flue", "_chimney_pot", "_counter", "_counter_with_dolium",
							"_kitchen_stove"))
						dropSelf(cp(stone + type));
				}
				if(rtype.isPillarMaterial()) {
					for (String type : ImmutableSet.of("_column_fluted_plinth", "_column_fluted_shaft", "_column_shaft",
							"_column_plinth", "_ionic_column_capital", "_tuscan_column_capital",
							"_acanthine_column_capital","_lacunar_tile","_spoked_fence"))
						dropSelf(cp(stone + type));
				}
				if(rtype.isHypocaustMaterial()) {
					dropSelf(cp(stone + "_caliduct"));
					dropSelf(cp(stone + "_hypocaust_firebox"));
				}
				if(rtype.isRoadMaterial()) {
					dropSelf(cp(stone + "_road"));
					dropSelf(cp(stone + "_road_side"));
				}
			}
			dropSelf(CPBlocks.GRAVY_BOAT.get());
			for (String wood : ImmutableSet.of("fig", "wolfberry")) {
				dropSelf(cp(wood + "_sapling"));
		
			}		add(cp("fig_leaves"), createLeavesDrops(cp("fig_leaves"), cp("fig_sapling"), 0.05F, 0.0625F,
				0.083333336F, 0.1F));
			add(CPBlocks.LOAF.get(),super.createSlabItemTable(CPBlocks.LOAF.get()));
			add(CPBlocks.LOAF_DOUGH.get(),super.createSlabItemTable(CPBlocks.LOAF_DOUGH.get()));
			/*add(CPBlocks.SNAIL_MUCUS.get(),createSilkTouchOrShearsDispatchTable(CPBlocks.SNAIL_MUCUS.get(),
					this.applyExplosionDecay(CPBlocks.SNAIL_MUCUS.get(), LootItem.lootTableItem(Items.STICK)
							.apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))
									.when(BonusLevelTableCondition.bonusLevelFlatChance(Enchantments.BLOCK_FORTUNE, new float[]{0.02F, 0.022222223F, 0.025F, 0.033333335F, 0.1F}))
									.when(HAS_SHEARS.or(HAS_SILK_TOUCH).invert())
							
							))));*/
			
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
			return LootTable.lootTable().withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(pItemBuilder)
			.when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(pBlock)
					.setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER)))
			.when(LocationCheck.checkLocation(LocationPredicate.Builder.location().setBlock(BlockPredicate.Builder.block()
					.of(pBlock)
					.setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER)))
					, new BlockPos(0, 1, 0))))
			.withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(pItemBuilder)
				.when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(pBlock)
							.setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER)))
				.when(LocationCheck.checkLocation(LocationPredicate.Builder.location().setBlock(BlockPredicate.Builder.block()
					.of(pBlock)
					.setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER)))
					, new BlockPos(0, -1, 0))));
		}

	}
}
