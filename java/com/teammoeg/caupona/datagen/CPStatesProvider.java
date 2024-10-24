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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.teammoeg.caupona.CPBlocks;
import com.teammoeg.caupona.CPItems;
import com.teammoeg.caupona.CPMain;
import com.teammoeg.caupona.blocks.decoration.SpokedFenceBlock;
import com.teammoeg.caupona.blocks.decoration.mosaic.MosaicBlock;
import com.teammoeg.caupona.blocks.decoration.mosaic.MosaicMaterial;
import com.teammoeg.caupona.blocks.decoration.mosaic.MosaicPattern;
import com.teammoeg.caupona.blocks.pan.GravyBoatBlock;
import com.teammoeg.caupona.blocks.plants.FruitBlock;
import com.teammoeg.caupona.util.MaterialType;
import com.teammoeg.caupona.util.Utils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.DataGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel.Builder;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder;
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder.PartBuilder;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.ExistingFileHelper.ResourceType;

public class CPStatesProvider extends BlockStateProvider {
	protected static final List<Vec3i> COLUMN_THREE = ImmutableList.of(BlockPos.ZERO, BlockPos.ZERO.above(),
		BlockPos.ZERO.above(2));
	protected static final ResourceType MODEL = new ResourceType(PackType.CLIENT_RESOURCES, ".json", "models");
	protected static final Map<ResourceLocation, String> generatedParticleTextures = new HashMap<>();
	protected final ExistingFileHelper existingFileHelper;
	String modid;

	public CPStatesProvider(DataGenerator gen, String modid, ExistingFileHelper exFileHelper) {
		super(gen.getPackOutput(), modid, exFileHelper);
		this.modid = modid;
		this.existingFileHelper = exFileHelper;
	}

	@Override
	protected void registerStatesAndModels() {
		horizontalAxisBlock(CPBlocks.STEW_POT.get(), bmf("stew_pot"));
		horizontalAxisBlock(CPBlocks.STEW_POT_LEAD.get(), bmf("lead_stew_pot"));
		horizontalBlock(CPBlocks.T_BENCH.get(), bmf("tessellation_workbench"));
		blockItemModel("tessellation_workbench");
		CPBlocks.stoves.forEach(e -> stove(e.get()));
		itemModels().basicItem(CPBlocks.STEW_POT.get().asItem());
		itemModels().basicItem(CPBlocks.STEW_POT_LEAD.get().asItem());
		simpleBlock(CPBlocks.BOWL.get(), bmf("bowl_of_liquid"));
		this.horizontalBlock(CPBlocks.KITCHEN_RAIL.get(), bmf("kitchen_rail"));
		blockItemModel("kitchen_rail");
		this.getVariantBuilder(CPBlocks.SNAIL_BAIT.get()).partialState().addModels(ConfiguredModel.allYRotations(bmf("snail_bait"), 0, false));
		this.getVariantBuilder(CPBlocks.SNAIL.get()).partialState().with(FruitBlock.AGE, 0)
			.addModels(ConfiguredModel.allYRotations(bmf("snail_stage_1"), 0, false)).partialState()
			.with(FruitBlock.AGE, 1).addModels(ConfiguredModel.allYRotations(bmf("snail_stage_2"), 0, false))
			.partialState().with(FruitBlock.AGE, 2)
			.addModels(ConfiguredModel.allYRotations(bmf("snail_stage_3"), 0, false)).partialState()
			.with(FruitBlock.AGE, 3).addModels(ConfiguredModel.allYRotations(bmf("snail_stage_4"), 0, false))
			.partialState().with(FruitBlock.AGE, 4)
			.addModels(ConfiguredModel.allYRotations(bmf("snail_stage_5"), 0, false)).partialState()
			.with(FruitBlock.AGE, 5).addModels(ConfiguredModel.allYRotations(bmf("snail_stage_5"), 0, false))
			.partialState().with(FruitBlock.AGE, 6)
			.addModels(ConfiguredModel.allYRotations(bmf("snail_stage_5"), 0, false)).partialState()
			.with(FruitBlock.AGE, 7).addModels(ConfiguredModel.allYRotations(bmf("snail_stage_5"), 0, false));
		this.getVariantBuilder(CPBlocks.LOAF_DOUGH.get())
			.partialState().with(SlabBlock.TYPE, SlabType.TOP).addModels(new ConfiguredModel(bmf("loaf_dough_top")))
			.partialState().with(SlabBlock.TYPE, SlabType.BOTTOM).addModels(new ConfiguredModel(bmf("loaf_dough_bottom")))
			.partialState().with(SlabBlock.TYPE, SlabType.DOUBLE).addModels(new ConfiguredModel(bmf("loaf_dough_top_bottom")));
		blockItemModel(CPBlocks.LOAF_DOUGH, bmf("loaf_dough"));
		this.getVariantBuilder(CPBlocks.LOAF.get())
			.partialState().with(SlabBlock.TYPE, SlabType.TOP).addModels(new ConfiguredModel(bmf("loaf_top")))
			.partialState().with(SlabBlock.TYPE, SlabType.BOTTOM).addModels(new ConfiguredModel(bmf("loaf_bottom")))
			.partialState().with(SlabBlock.TYPE, SlabType.DOUBLE).addModels(new ConfiguredModel(bmf("loaf_top_bottom")));
		blockItemModel(CPBlocks.LOAF, bmf("loaf"));
		for (MosaicMaterial m : MosaicMaterial.values())
			for (MosaicPattern p : MosaicPattern.values())
				for (int i : new int[] { 0, 1 })
					super.models().withExistingParent("block/mosaic/mosaic_" + p + "_" + m.shortName + "_" + i, ResourceLocation.fromNamespaceAndPath(CPMain.MODID, "block/template_mosaic_tile_" + i))
						.texture("" + i, ResourceLocation.fromNamespaceAndPath(CPMain.MODID, "block/mosaic/components/mosaic_" + p + "_" + m.shortName + "_" + i)).texture("particle", "#" + i);
		MultiPartBlockStateBuilder mosaic = this.getMultipartBuilder(CPBlocks.MOSAIC.get());
		for (MosaicMaterial m : MosaicMaterial.values())
			for (MosaicPattern p : MosaicPattern.values())
				for (int i : new int[] { 0, 1 }) {
					this.horizontalMultipart(mosaic, bmf("mosaic/mosaic_" + p + "_" + m.shortName + "_" + i), b -> b.condition(MosaicBlock.MATERIAL[i], m).condition(MosaicBlock.PATTERN, p));

				}
		for(String s:CPItems.dishes) {
			this.getMultipartBuilder(cpblock(s))
			.part().modelFile(bmf("dish")).addModel().end()
			.part().modelFile(bmf("plate_dishes/"+s)).addModel().end();
			this.getMultipartBuilder(cpblock(s+"_loaf"))
			.part().modelFile(bmf("bread_bowl")).addModel().end()
			.part().modelFile(bmf("bread_bowl_dishes/"+s)).addModel().end();
		}
		// itemModels().getBuilder("mosaic").parent(new
		// UncheckedModelFile(ResourceLocation.fromNamespaceAndPath("builtin/entity")));
		/*
		 * this.getVariantBuilder(CPBlocks.MOSAIC.get()).forAllStates(t->{ MosaicPattern
		 * p=t.getValue(MosaicBlock.PATTERN); MosaicMaterial
		 * m1=t.getValue(MosaicBlock.MATERIAL_1); MosaicMaterial
		 * m2=t.getValue(MosaicBlock.MATERIAL_2); return
		 * ConfiguredModel.builder().modelFile(bmf("mosaic/mosaic_"+p+"_"+m1.shortName+
		 * "_"+m2.shortName)).build(); });
		 */
		// super.stairsBlock(null, modid, null, null, null);
		for (MaterialType rtype : CPBlocks.all_materials) {
			String stone = rtype.getName();
			if (rtype.isDecorationMaterial()) {
				for (String type : ImmutableSet.of("", "_slab", "_stairs"))
					blockItemModel(stone + type);
				blockItemModel(stone + "_wall", "_inventory");
			}
			if (rtype.isCounterMaterial()) {
				for (String type : ImmutableSet.of("_chimney_flue", "_chimney_pot", "_counter", "_counter_with_dolium"))
					blockItemModel(stone + type);
			}

			if (rtype.isPillarMaterial()) {
				for (String type : ImmutableSet.of("_column_fluted_plinth", "_column_fluted_shaft", "_column_shaft",
					"_column_plinth", "_ionic_column_capital", "_tuscan_column_capital",
					"_acanthine_column_capital"))
					blockItemModel(stone + type);
				simpleBlockItem(cpblock(stone + "_lacunar_tile"), bmf(stone + "_lacunar_tile"));
				itemModel(cpblock(stone + "_spoked_fence"), bmf(stone + "_spoked_fence_inventory"));
				this.getMultipartBuilder(cpblock(stone + "_spoked_fence"))
					.part().modelFile(bmf(stone + "_spoked_fence_side")).rotationY(270)
					.addModel().condition(SpokedFenceBlock.WEST_WALL, true).end()
					.part().modelFile(bmf(stone + "_spoked_fence_side")).rotationY(0)
					.addModel().condition(SpokedFenceBlock.NORTH_WALL, true).end()
					.part().modelFile(bmf(stone + "_spoked_fence_side")).rotationY(90)
					.addModel().condition(SpokedFenceBlock.EAST_WALL, true).end()
					.part().modelFile(bmf(stone + "_spoked_fence_side")).rotationY(180)
					.addModel().condition(SpokedFenceBlock.SOUTH_WALL, true).end()
					.part().modelFile(bmf(stone + "_spoked_fence_post"))
					.addModel().end();
			}
			if (rtype.isHypocaustMaterial()) {
				blockItemModel(stone + "_hypocaust_firebox");
				blockItemModel(stone + "_caliduct");
			}
			if (rtype.isRoadMaterial()) {
				roadBlock(stone);

			}
		}
		MultiPartBlockStateBuilder boat = horizontalMultipart(this.getMultipartBuilder(CPBlocks.GRAVY_BOAT.get()),
			bmf("gravy_boat"));
		int i = 0;
		for (String s : ImmutableSet.of("_oil_0", "_oil_1", "_oil_2", "_oil_3", "_oil_4")) {
			int j = i++;
			boat = horizontalMultipart(boat, bmf("gravy_boat" + s), c -> c.condition(GravyBoatBlock.LEVEL, j));
		}
		for (String wood : CPBlocks.woods) {
			for (String type : ImmutableSet.of(

				"_fence_gate", "_leaves", "_log", "_planks", "_pressure_plate", "_slab", "_stairs", "_wood"))
				blockItemModel(wood + type);
			blockItemModel(wood + "_fence", "_inventory");
			blockItemModel(wood + "_button", "_inventory");
			blockItemModelBuilder(wood + "_fruits", "_stage_3").transforms().transform(ItemDisplayContext.GUI).scale(1f)
				.rotation(0, 0.1f, 0).translation(0, 0, 0).end().end();

			blockItemModel("stripped_" + wood + "_log");
			blockItemModel("stripped_" + wood + "_wood");
	        super.models().sign(wood+"_hanging_sign",modLoc("block/"+wood+"_planks"));
	        super.models().sign(wood+"_wall_hanging_sign",modLoc("block/"+wood+"_planks"));
			// blockItemModel(wood+"_trapdoor","_top")

		}
		blockItemModel(Utils.getRegistryName(CPBlocks.STONE_PAN).getPath());
		blockItemModel(Utils.getRegistryName(CPBlocks.COPPER_PAN).getPath());
		blockItemModel(Utils.getRegistryName(CPBlocks.IRON_PAN).getPath());
		blockItemModel(Utils.getRegistryName(CPBlocks.LEAD_PAN).getPath());
		blockItemModel("wolf_statue", "_1");
		blockItemModel("fumarole_boulder");
		blockItemModel("fumarole_vent");
		blockItemModel("litharge_fumarole_boulder");
		blockItemModel("litharge_fumarole_vent");
		blockItemModel("litharge_bloom");
		blockItemModel("pumice");
		blockItemModel("pumice_bloom");
		blockItemModel("lead_block");
		blockItemModel("snail_bait");
		blockItemModel("snail_mucus");
		simpleBlock(CPBlocks.SNAIL_MUCUS.get(), bmf("snail_mucus"));
		simpleBlock(CPBlocks.LEAD_BLOCK.get(), bmf("lead_block"));
		simpleBlock(CPBlocks.LOAF_BOWL.get(),bmf("bread_bowl"));
		// itemModels().getBuilder("snail_block").parent(bmf("snail_stage_5")).transforms().transform(ItemDisplayContext.GUI).scale(1.5f).rotation(0,
		// 45, 180).translation(0, 4, 0).end().end();

		for (String bush : ImmutableSet.of("wolfberry", "fig")) {
			blockItemModel(bush + "_log");
			blockItemModelBuilder(bush + "_fruits", "_stage_3").transforms().transform(ItemDisplayContext.GUI).scale(1f)
				.rotation(0, 45, 0).translation(0, 1, 0).end().end();
			blockItemModel(bush + "_leaves");
		}

	}

	public void roadBlock(String name) {

		itemModels().getBuilder(name + "_road_side").parent(bmf("roads/" + name + "_road_side"));

		itemModels().getBuilder(name + "_road").parent(bmf("roads/" + name + "_road"));
		getVariantBuilder(cpblock(name + "_road_side")).forAllStates(state -> {
			Direction facing = state.getValue(StairBlock.FACING);
			StairsShape shape = state.getValue(StairBlock.SHAPE);
			int yRot = (int) facing.getClockWise().toYRot(); // Stairs model is rotated 90 degrees
																// clockwise for some reason
			if (shape == StairsShape.INNER_LEFT || shape == StairsShape.OUTER_LEFT) {
				yRot += 270; // Left facing stairs are rotated 90 degrees clockwise
			}
			yRot %= 360;
			Builder<?> builder = null;
			String ext = shape == StairsShape.STRAIGHT ? "_side"
				: shape == StairsShape.INNER_LEFT || shape == StairsShape.INNER_RIGHT ? "_outer_corner"
					: "_inner_corner";
			int i = 0;
			while (true) {
				ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(this.modid, "block/roads/" + name + "_road" + ext + "_" + i);
				if (!existingFileHelper.exists(rl, MODEL))
					break;
				if (builder == null)
					builder = ConfiguredModel.builder();
				else
					builder = builder.nextModel();
				builder = builder.modelFile(new ModelFile.ExistingModelFile(rl, existingFileHelper)).rotationY(yRot);
				i++;

			}
			return builder.build();

		});
		Builder<?> builder = null;
		int i = 0;
		while (true) {
			ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(this.modid, "block/roads/" + name + "_road_" + i);
			if (!existingFileHelper.exists(rl, MODEL))
				break;
			i++;
			if (builder == null)
				builder = ConfiguredModel.builder();
			else
				builder = builder.nextModel();
			builder = builder.modelFile(new ModelFile.ExistingModelFile(rl, existingFileHelper));
		}
		this.getVariantBuilder(cpblock(name + "_road")).partialState().addModels(builder.build());
	}

	private Block cpblock(String name) {
		return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(this.modid, name));
	}

	protected void blockItemModel(String n) {
		blockItemModel(n, "");
	}

	protected void blockItemModel(String n, String p) {
		if (this.existingFileHelper.exists(ResourceLocation.fromNamespaceAndPath(CPMain.MODID, "textures/item/" + n + p + ".png"),
			PackType.CLIENT_RESOURCES)) {
			itemModels().basicItem(ResourceLocation.fromNamespaceAndPath(CPMain.MODID, n));
		} else {
			itemModels().getBuilder(n).parent(bmf(n + p));
		}
	}

	protected void blockItemModel(Holder<Block> n, ModelFile p) {

		itemModels().getBuilder(n.getRegisteredName()).parent(p);
	}

	protected ItemModelBuilder blockItemModelBuilder(String n, String p) {
		return itemModels().getBuilder(n).parent(bmf(n + p));
	}

	public void stove(Block block) {
		horizontalMultipart(
			horizontalMultipart(this.getMultipartBuilder(block),
				bmf(Utils.getRegistryName(block).getPath())),
			bmf("kitchen_stove_fuel"), i -> i);
		itemModel(block, bmf(Utils.getRegistryName(block).getPath()));

	}

	public ModelFile bmf(String name) {
		ResourceLocation orl = ResourceLocation.fromNamespaceAndPath(this.modid, "block/" + name);
		ResourceLocation rl = orl;
		if (!existingFileHelper.exists(rl, MODEL)) {// not exists, let's guess
			List<String> rn = Arrays.asList(name.split("_"));
			for (int i = rn.size(); i >= 0; i--) {
				List<String> rrn = new ArrayList<>(rn);
				rrn.add(i, "0");
				rl = ResourceLocation.fromNamespaceAndPath(this.modid, "block/" + String.join("_", rrn));
				if (existingFileHelper.exists(rl, MODEL))
					return new ModelFile.ExistingModelFile(rl, existingFileHelper);
			}

		}
		CPMain.logger.warn("Model file "+orl+" not exists, using unchecked");
		return new ModelFile.UncheckedModelFile(orl);
	}
	public void simpleBlockItem(String name) {
		simpleBlockItem(cpblock(name), bmf(name));
	}
	public void simpleBlockItem(Block b, ModelFile model) {
		simpleBlockItem(b, new ConfiguredModel(model));
	}

	protected void simpleBlockItem(Block b, ConfiguredModel model) {
		simpleBlock(b, model);
		itemModel(b, model.model);
	}

	public void horizontalAxisBlock(Block block, ModelFile mf) {
		getVariantBuilder(block).partialState().with(BlockStateProperties.HORIZONTAL_AXIS, Axis.Z).modelForState()
			.modelFile(mf).addModel().partialState().with(BlockStateProperties.HORIZONTAL_AXIS, Axis.X)
			.modelForState().modelFile(mf).rotationY(90).addModel();
	}

	public MultiPartBlockStateBuilder horizontalMultipart(MultiPartBlockStateBuilder block, ModelFile mf) {
		return horizontalMultipart(block, mf, UnaryOperator.identity());
	}

	public MultiPartBlockStateBuilder horizontalMultipart(MultiPartBlockStateBuilder block, ModelFile mf,
		UnaryOperator<PartBuilder> act) {
		for (Direction d : BlockStateProperties.HORIZONTAL_FACING.getPossibleValues())
			block = act.apply(block.part().modelFile(mf).rotationY(((int) d.toYRot()) % 360).addModel()
				.condition(BlockStateProperties.HORIZONTAL_FACING, d)).end();
		return block;
	}

	protected void itemModel(Block block, ModelFile model) {
		itemModels().getBuilder(Utils.getRegistryName(block).getPath()).parent(model);
	}
}
