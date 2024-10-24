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

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.google.common.collect.ImmutableList;
import com.teammoeg.caupona.CPBlocks;
import com.teammoeg.caupona.CPMain;
import com.teammoeg.caupona.CPWorldGen;
import com.teammoeg.caupona.blocks.plants.BushLogBlock;
import com.teammoeg.caupona.worldgen.BushStraightTrunkPlacer;
import com.teammoeg.caupona.worldgen.LeavingLogReplacer;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.RegistrySetBuilder.RegistryBootstrap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.data.worldgen.placement.VegetationPlacements;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.RandomPatchConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.featuresize.TwoLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.foliageplacers.BlobFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.level.levelgen.feature.trunkplacers.StraightTrunkPlacer;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.RarityFilter;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;


public class CPRegistryGenerator extends DatapackBuiltinEntriesProvider {
	public CPRegistryGenerator(PackOutput output, CompletableFuture<Provider> registries) {
		super(output, registries,new RegistrySetBuilder()
				.add(Registries.CONFIGURED_FEATURE,(RegistryBootstrap<ConfiguredFeature<?, ?>>)CPRegistryGenerator::bootstrapCFeatures)
				.add(Registries.PLACED_FEATURE,CPRegistryGenerator::bootstrapPFeatures),
				Set.of(CPMain.MODID));
	}
	public static void bootstrapPFeatures(BootstrapContext<PlacedFeature> pContext) {
		HolderGetter<ConfiguredFeature<?, ?>> holder=pContext.lookup(Registries.CONFIGURED_FEATURE);
		PlacementUtils.register(pContext, CPWorldGen.TREES_WALNUT,holder.getOrThrow(CPWorldGen.WALNUT),VegetationPlacements
				.treePlacement(PlacementUtils.countExtra(0, 0.125F, 1), sap("walnut")));
		PlacementUtils.register(pContext, CPWorldGen.TREES_FIG,holder.getOrThrow(CPWorldGen.FIG),VegetationPlacements
				.treePlacement(PlacementUtils.countExtra(0, 0.125F, 1), sap("fig")));
		PlacementUtils.register(pContext, CPWorldGen.TREES_WOLFBERRY,holder.getOrThrow(CPWorldGen.WOLFBERRY),VegetationPlacements
				.treePlacement(PlacementUtils.countExtra(0, 0.125F, 1), sap("wolfberry")));
		PlacementUtils.register(pContext, CPWorldGen.PATCH_SILPHIUM, holder.getOrThrow(CPWorldGen.SILPHIUM),
				RarityFilter.onAverageOnceEvery(10), InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP, BiomeFilter.biome());
	}
	public static void bootstrapCFeatures(BootstrapContext<ConfiguredFeature<?,?>> pContext) {
		FeatureUtils.register(pContext,CPWorldGen.WALNUT,Feature.TREE,createStraightBlobTree(log("walnut"),leave("walnut"), 4, 2, 0, 2).ignoreVines().build());
		FeatureUtils.register(pContext,CPWorldGen.FIG,Feature.TREE,createStraightBlobBush(log("fig"), leave("fig"), 4, 2, 0, 2).ignoreVines().build());
		FeatureUtils.register(pContext,CPWorldGen.WOLFBERRY,Feature.TREE,createStraightBlobBush(log("wolfberry"),leave("wolfberry"), 4, 2, 0, 2).ignoreVines().build());
		FeatureUtils.register(pContext,CPWorldGen.SILPHIUM, Feature.RANDOM_PATCH,
				new RandomPatchConfiguration(12,4,3,PlacementUtils.filtered(Feature.SIMPLE_BLOCK,new SimpleBlockConfiguration(BlockStateProvider.simple(CPBlocks.SILPHIUM.get())),BlockPredicate.ONLY_IN_AIR_PREDICATE)));
	}
	private static TreeConfiguration.TreeConfigurationBuilder createStraightBlobTree(Block log, Block leave, int height,
			int randA, int randB, int foliage) {
		return new TreeConfiguration.TreeConfigurationBuilder(BlockStateProvider.simple(log),
				new StraightTrunkPlacer(height, randA, randB), BlockStateProvider.simple(leave),
				new BlobFoliagePlacer(ConstantInt.of(foliage), ConstantInt.of(0), 3),
				new TwoLayersFeatureSize(1, 0, 1));
	}

	private static TreeConfiguration.TreeConfigurationBuilder createStraightBlobBush(Block log, Block leave, int height,
			int randA, int randB, int foliage) {
		return new TreeConfiguration.TreeConfigurationBuilder(BlockStateProvider.simple(log),
				new BushStraightTrunkPlacer(height, randA, randB), BlockStateProvider.simple(leave),
				new BlobFoliagePlacer(ConstantInt.of(foliage), ConstantInt.of(0), 3),
				new TwoLayersFeatureSize(1, 0, 1))
			.decorators(ImmutableList.<TreeDecorator>builder()
				.add(new LeavingLogReplacer(BlockStateProvider.simple(BushLogBlock.setFullShape(log.defaultBlockState())))).build())
			;
	}
	public static Block leave(String type) {
		return block(type+"_leaves");
	}
	public static Block sap(String type) {
		return block(type+"_sapling");
	}
	public static Block log(String type) {
		return block(type+"_log");
	}
	public static Block block(String type) {
		return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(CPMain.MODID,type));
	}
	@Override
	public String getName() {
		return "Caupona Registry Generator";
	}
}
