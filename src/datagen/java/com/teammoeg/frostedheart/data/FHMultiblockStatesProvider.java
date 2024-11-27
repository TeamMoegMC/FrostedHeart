/*
 * Copyright (c) 2021 TeamMoeg
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
 */

package com.teammoeg.frostedheart.data;

import blusunrize.immersiveengineering.ImmersiveEngineering;
import blusunrize.immersiveengineering.api.IEProperties;
import blusunrize.immersiveengineering.api.multiblocks.TemplateMultiblock;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.teammoeg.frostedheart.FHBlocks;
import com.teammoeg.frostedheart.FHMain;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.data.DataGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.client.model.generators.VariantBlockStateBuilder;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.data.ExistingFileHelper.ResourceType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FHMultiblockStatesProvider extends FHExtendedStatesProvider {
	protected static final ResourceType MODEL = new ResourceType(PackType.CLIENT_RESOURCES, ".json", "models");
	private static final List<Vec3i> CUBE_THREE = BlockPos.betweenClosedStream(-1, -1, -1, 1, 1, 1)
			.map(BlockPos::immutable).collect(Collectors.toList());
	private static final List<Vec3i> CUBE_TWO = BlockPos.betweenClosedStream(0, 0, -1, 1, 1, 0).map(BlockPos::immutable)
			.collect(Collectors.toList());

	public FHMultiblockStatesProvider(DataGenerator gen, String modid, ExistingFileHelper exFileHelper) {
		super(gen, modid, exFileHelper);
	}

	protected void registerStatesAndModels() {
		// createMultiblock(FHContent.FHMultiblocks.generator,
		// split(obj("block/multiblocks/generator.obj"),
		// FHContent.FHMultiblocks.GENERATOR));
//        createMultiblock(FHBlocks.Multi.crucible, split(obj("block/multiblocks/crucible.obj"), FHMultiblocks.CRUCIBLE));
		// createMultiblock(FHContent.FHMultiblocks.generator_t2,split(obj("block/multiblocks/generator_t2.obj"),
		// FHContent.FHMultiblocks.GENERATOR_T2));
		super.horizontalBlock(FHBlocks.MECHANICAL_CALCULATOR.get(), obj("block/mechanical_calculator_base.obj"));
		obj("block/mechanical_calculator.obj");

		super.horizontalBlock(FHBlocks.INCUBATOR.get(), bmf("incubator"));
		super.itemModel(FHBlocks.INCUBATOR.get(), bmf("incubator"));
		super.itemModel(FHBlocks.HEAT_INCUBATOR.get(), bmf("heat_incubator"));
		super.horizontalBlock(FHBlocks.HEAT_INCUBATOR.get(),
				s -> s.getValue(BlockStateProperties.LIT) ? bmf("heat_incubator_active") : bmf("heat_incubator"));
		// super.itemModel(FHBlocks.mech_calc,models().withExistingParent("block/mechanical_calculator",
		// modLoc("block/mechanical_calculator")));

//		super.cubeAll(FHBlocks.IRON_SLUDGE.get(), modLoc("block/ore/iron_sludge"));

		super.layered((SnowLayerBlock) FHBlocks.CONDENSED_COPPER_ORE.get(), FHBlocks.CONDENSED_COPPER_ORE_BLOCK.get(), modLoc("block/ore/condensed_copper_ore"));
//		super.layered((SnowLayerBlock) FHBlocks.CONDENSED_IRON_ORE.get(), FHBlocks.CONDENSED_IRON_ORE_BLOCK.get(), modLoc("block/ore/condensed_iron_ore"));
		super.layered((SnowLayerBlock) FHBlocks.CONDENSED_GOLD_ORE.get(), FHBlocks.CONDENSED_GOLD_ORE_BLOCK.get(), modLoc("block/ore/condensed_gold_ore"));
		super.layered((SnowLayerBlock) FHBlocks.CONDENSED_ZINC_ORE.get(), FHBlocks.CONDENSED_ZINC_ORE_BLOCK.get(), modLoc("block/ore/condensed_zinc_ore"));
		super.layered((SnowLayerBlock) FHBlocks.CONDENSED_SILVER_ORE.get(), FHBlocks.CONDENSED_SILVER_ORE_BLOCK.get(), modLoc("block/ore/condensed_silver_ore"));
		super.layered((SnowLayerBlock) FHBlocks.CONDENSED_TIN_ORE.get(), FHBlocks.CONDENSED_TIN_ORE_BLOCK.get(), modLoc("block/ore/condensed_tin_ore"));
		super.layered((SnowLayerBlock) FHBlocks.CONDENSED_PYRITE_ORE.get(), FHBlocks.CONDENSED_PYRITE_ORE_BLOCK.get(), modLoc("block/ore/condensed_pyrite_ore"));
		super.layered((SnowLayerBlock) FHBlocks.CONDENSED_NICKEL_ORE.get(), FHBlocks.CONDENSED_NICKEL_ORE_BLOCK.get(), modLoc("block/ore/condensed_nickel_ore"));
		super.layered((SnowLayerBlock) FHBlocks.CONDENSED_LEAD_ORE.get(), FHBlocks.CONDENSED_LEAD_ORE_BLOCK.get(), modLoc("block/ore/condensed_lead_ore"));

		super.layered((SnowLayerBlock) FHBlocks.COPPER_SLUDGE.get(), FHBlocks.COPPER_SLUDGE_BLOCK.get(), modLoc("block/ore/copper_sludge"));
		super.layered((SnowLayerBlock) FHBlocks.IRON_SLUDGE.get(), FHBlocks.IRON_SLUDGE_BLOCK.get(), modLoc("block/ore/iron_sludge"));
		super.layered((SnowLayerBlock) FHBlocks.GOLD_SLUDGE.get(), FHBlocks.GOLD_SLUDGE_BLOCK.get(), modLoc("block/ore/gold_sludge"));
		super.layered((SnowLayerBlock) FHBlocks.ZINC_SLUDGE.get(), FHBlocks.ZINC_SLUDGE_BLOCK.get(), modLoc("block/ore/zinc_sludge"));
		super.layered((SnowLayerBlock) FHBlocks.SILVER_SLUDGE.get(), FHBlocks.SILVER_SLUDGE_BLOCK.get(), modLoc("block/ore/silver_sludge"));
		super.layered((SnowLayerBlock) FHBlocks.TIN_SLUDGE.get(), FHBlocks.TIN_SLUDGE_BLOCK.get(), modLoc("block/ore/tin_sludge"));
		super.layered((SnowLayerBlock) FHBlocks.PYRITE_SLUDGE.get(), FHBlocks.PYRITE_SLUDGE_BLOCK.get(), modLoc("block/ore/pyrite_sludge"));
		super.layered((SnowLayerBlock) FHBlocks.NICKEL_SLUDGE.get(), FHBlocks.NICKEL_SLUDGE_BLOCK.get(), modLoc("block/ore/nickel_sludge"));
		super.layered((SnowLayerBlock) FHBlocks.LEAD_SLUDGE.get(), FHBlocks.LEAD_SLUDGE_BLOCK.get(), modLoc("block/ore/lead_sludge"));

		super.cubeAll(FHBlocks.SILVER_ORE.get(), modLoc("block/ore/silver_ore"));
		super.cubeAll(FHBlocks.TIN_ORE.get(), modLoc("block/ore/tin_ore"));
		super.cubeAll(FHBlocks.PYRITE_ORE.get(), modLoc("block/ore/pyrite_ore"));
		super.cubeAll(FHBlocks.NICKEL_ORE.get(), modLoc("block/ore/nickel_ore"));
		super.cubeAll(FHBlocks.LEAD_ORE.get(), modLoc("block/ore/lead_ore"));
		super.cubeAll(FHBlocks.HALITE_ORE.get(), modLoc("block/ore/halite_ore"));
		super.cubeAll(FHBlocks.SYLVITE_ORE.get(), modLoc("block/ore/sylvite_ore"));
		super.cubeAll(FHBlocks.MAGNESITE_ORE.get(), modLoc("block/ore/magnesite_ore"));

		super.cubeAll(FHBlocks.DEEPSLATE_SILVER_ORE.get(), modLoc("block/ore/deepslate_silver_ore"));
		super.cubeAll(FHBlocks.DEEPSLATE_TIN_ORE.get(), modLoc("block/ore/deepslate_tin_ore"));
		super.cubeAll(FHBlocks.DEEPSLATE_PYRITE_ORE.get(), modLoc("block/ore/deepslate_pyrite_ore"));
		super.cubeAll(FHBlocks.DEEPSLATE_NICKEL_ORE.get(), modLoc("block/ore/deepslate_nickel_ore"));
		super.cubeAll(FHBlocks.DEEPSLATE_LEAD_ORE.get(), modLoc("block/ore/deepslate_lead_ore"));
		super.cubeAll(FHBlocks.DEEPSLATE_HALITE_ORE.get(), modLoc("block/ore/deepslate_halite_ore"));
		super.cubeAll(FHBlocks.DEEPSLATE_SYLVITE_ORE.get(), modLoc("block/ore/deepslate_sylvite_ore"));
		super.cubeAll(FHBlocks.DEEPSLATE_MAGNESITE_ORE.get(), modLoc("block/ore/deepslate_magnesite_ore"));

		super.cubeAll(FHBlocks.MAGNESITE_BLOCK.get(), modLoc("block/magnesite_block"));
		super.cubeAll(FHBlocks.MAGNESIA_BLOCK.get(), modLoc("block/magnesia_block"));
		super.cubeAll(FHBlocks.QUICKLIME_BLOCK.get(), modLoc("block/quicklime_block"));
		super.cubeAll(FHBlocks.DURALUMIN_SHEETMETAL.get(), modLoc("block/duralumin_sheetmetal"));
		super.cubeAll(FHBlocks.REFRACTORY_BRICKS.get(), modLoc("block/refractory_bricks"));
		super.cubeAll(FHBlocks.HIGH_REFRACTORY_BRICKS.get(), modLoc("block/high_refractory_bricks"));
		super.cubeAll(FHBlocks.PACKED_SNOW.get(), modLoc("block/packed_snow"));
		super.slab((SlabBlock) FHBlocks.PACKED_SNOW_SLAB.get(), modLoc("block/packed_snow"));

		super.cubeAll(FHBlocks.PEAT.get(), modLoc("block/sediment/peat_block"));
//		super.cubeAll(FHBlocks.ROTTEN_WOOD.get(), modLoc("block/sediment/rotten_wood_block"));
		super.cubeAll(FHBlocks.BAUXITE.get(), modLoc("block/sediment/bauxite_block"));
		super.cubeAll(FHBlocks.KAOLIN.get(), modLoc("block/sediment/kaolin_block"));
//		super.cubeAll(FHBlocks.BURIED_MYCELIUM.get(), modLoc("block/sediment/buried_mycelium"));
//		super.cubeAll(FHBlocks.BURIED_PODZOL.get(), modLoc("block/sediment/buried_podzol"));


	}

	@Nonnull
	@Override
	public String getName() {
		return "FH Multiblock models/block states";
	}

	public ModelFile bmf(String name) {
		ResourceLocation rl = new ResourceLocation(FHMain.MODID, "block/" + name);
		if (!existingFileHelper.exists(rl, MODEL)) {// not exists, let's guess
			List<String> rn = Arrays.asList(name.split("_"));
			for (int i = rn.size(); i >= 0; i--) {
				List<String> rrn = new ArrayList<>(rn);
				rrn.add(i, "0");
				rl = new ResourceLocation(FHMain.MODID, "block/" + String.join("_", rrn));
				if (existingFileHelper.exists(rl, MODEL))
					break;
			}

		}
		return new ModelFile.ExistingModelFile(rl, existingFileHelper);
	}

	private ModelFile cubeTwo(String name, ResourceLocation top, ResourceLocation bottom, ResourceLocation side,
			ResourceLocation front) {
		ModelFile baseModel = obj(name, ImmersiveEngineering.rl("block/stone_multiblocks/cube_two.obj"),
				ImmutableMap.<String, ResourceLocation>builder().put("side", side).put("top", top).put("bottom", bottom)
						.put("front", front).build());
		return splitModel(name + "_split", baseModel, CUBE_TWO, false);
	}

	private ModelFile cubeThree(String name, ResourceLocation def, ResourceLocation front) {
		ModelFile baseModel = obj(name, ImmersiveEngineering.rl("block/stone_multiblocks/cube_three.obj"),
				ImmutableMap.of("side", def, "front", front));
		return splitModel(name + "_split", baseModel, CUBE_THREE, false);
	}

	private void createMultiblock(Block b, ModelFile masterModel, ModelFile mirroredModel) {
		createMultiblock(b, masterModel, mirroredModel, IEProperties.FACING_HORIZONTAL, IEProperties.MIRRORED);
	}

	private void createMultiblock(Block b, ModelFile masterModel) {
		createMultiblock(b, masterModel, null, IEProperties.FACING_HORIZONTAL, null);
	}

	private void createMultiblock(Block b, ModelFile masterModel, @Nullable ModelFile mirroredModel,
			@Nullable Property<Boolean> mirroredState) {
		createMultiblock(b, masterModel, mirroredModel, IEProperties.FACING_HORIZONTAL, mirroredState);
	}

	private void createMultiblock(Block b, ModelFile masterModel, @Nullable ModelFile mirroredModel,
			EnumProperty<Direction> facing, @Nullable Property<Boolean> mirroredState) {
		Preconditions.checkArgument((mirroredModel == null) == (mirroredState == null));
		VariantBlockStateBuilder builder = getVariantBuilder(b);
		boolean[] possibleMirrorStates;
		if (mirroredState != null)
			possibleMirrorStates = new boolean[] { false, true };
		else
			possibleMirrorStates = new boolean[1];
		for (boolean mirrored : possibleMirrorStates)
			for (Direction dir : facing.getPossibleValues()) {
				final int angleY;
				final int angleX;
				if (facing.getPossibleValues().contains(Direction.UP)) {
					angleX = -90 * dir.getStepY();
					if (dir.getAxis() != Direction.Axis.Y)
						angleY = getAngle(dir, 180);
					else
						angleY = 0;
				} else {
					angleY = getAngle(dir, 180);
					angleX = 0;
				}
				ModelFile model = mirrored ? mirroredModel : masterModel;
				VariantBlockStateBuilder.PartialBlockstate partialState = builder.partialState().with(facing, dir);
				if (mirroredState != null)
					partialState = partialState.with(mirroredState, mirrored);
				partialState.setModels(new ConfiguredModel(model, angleX, angleY, true));
			}
	}

	private ModelFile split(ModelFile loc, TemplateMultiblock mb) {
		return split(loc, mb, false);
	}

	private ModelFile split(ModelFile loc, TemplateMultiblock mb, boolean mirror) {
		return split(loc, mb, mirror, false);
	}

	private ModelFile splitDynamic(ModelFile loc, TemplateMultiblock mb, boolean mirror) {
		return split(loc, mb, mirror, true);
	}

	private ModelFile split(ModelFile loc, TemplateMultiblock mb, boolean mirror, boolean dynamic) {
		UnaryOperator<BlockPos> transform = UnaryOperator.identity();
		if (mirror) {
			Vec3i size = mb.getSize(null);
			transform = p -> new BlockPos(size.getX() - p.getX() - 1, p.getY(), p.getZ());
		}
		return split(loc, mb, transform, dynamic);
	}

	private ModelFile split(ModelFile name, TemplateMultiblock multiblock, UnaryOperator<BlockPos> transform,
			boolean dynamic) {
		final Vec3i offset = multiblock.getMasterFromOriginOffset();
		Stream<Vec3i> partsStream = multiblock.getStructure(null).stream().filter(info -> !info.state().isAir())
				.map(info -> info.pos()).map(transform).map(p -> p.subtract(offset));
		return split(name, partsStream.collect(Collectors.toList()), dynamic);
	}

}
