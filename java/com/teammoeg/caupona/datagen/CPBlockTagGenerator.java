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


import java.util.concurrent.CompletableFuture;

import com.google.common.collect.ImmutableSet;
import com.teammoeg.caupona.CPBlocks;
import com.teammoeg.caupona.CPMain;
import com.teammoeg.caupona.CPTags;
import com.teammoeg.caupona.util.MaterialType;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredHolder;

public class CPBlockTagGenerator extends TagsProvider<Block> {

	public CPBlockTagGenerator(DataGenerator dataGenerator, String modId, ExistingFileHelper existingFileHelper,CompletableFuture<HolderLookup.Provider> provider) {
		super(dataGenerator.getPackOutput(), Registries.BLOCK,provider,modId, existingFileHelper);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void addTags(Provider pProvider) {
		TagAppender<Block> pickaxe = tag(BlockTags.MINEABLE_WITH_PICKAXE);
		adds(tag(CPTags.Blocks.STOVES),CPBlocks.stoves.stream().map(e->e.getKey()).toArray(ResourceKey[]::new));
		adds(pickaxe,CPBlocks.stoves.stream().map(e->e.getKey()).toArray(ResourceKey[]::new));
		adds(pickaxe,CPBlocks.STEW_POT.getKey(),CPBlocks.STEW_POT_LEAD.getKey(),CPBlocks.T_BENCH.getKey(),CPBlocks.MOSAIC.getKey());
		pickaxe.add(CPBlocks.LEAD_BLOCK.getKey());
		for (String wood : CPBlocks.woods) {
			for (String type : ImmutableSet.of("_button", "_door", "_fence", "_fence_gate", "_log", "_planks",
					"_pressure_plate", "_sapling", "_sign", "_wall_sign", "_slab", "_stairs", "_trapdoor", "_wood")) {
				tag(BlockTags.MINEABLE_WITH_AXE).add(cp(wood + type));
				
			}
			tag(BlockTags.MINEABLE_WITH_AXE).add(cp("stripped_" + wood + "_log"), cp("stripped_" + wood + "_wood"));
			tag(BlockTags.MINEABLE_WITH_HOE).add(cp(wood + "_leaves")).add(cp(wood + "_fruits"));
			tag(BlockTags.LEAVES).add(cp(wood + "_leaves"));
			tag(BlockTags.SAPLINGS).add(cp(wood + "_sapling"));
			tag(BlockTags.DOORS).add(cp(wood + "_door"));
			tag(BlockTags.WOODEN_DOORS).add(cp(wood + "_door"));
			tag(BlockTags.FENCES).add(cp(wood + "_fence"));
			tag(BlockTags.WOODEN_FENCES).add(cp(wood + "_fence"));
			tag(BlockTags.TRAPDOORS).add(cp(wood + "_trapdoor"));
			tag(BlockTags.PRESSURE_PLATES).add(cp(wood + "_pressure_plate"));
			tag(BlockTags.WOODEN_PRESSURE_PLATES).add(cp(wood + "_pressure_plate"));
			tag(BlockTags.WALL_POST_OVERRIDE).add(cp(wood + "_pressure_plate"));
			tag(BlockTags.LOGS_THAT_BURN).add(cp(wood + "_wood"),cp(wood + "_log"),cp("stripped_" + wood + "_log"), cp("stripped_" + wood + "_wood"));
			tag(BlockTags.LOGS).add(cp(wood + "_wood")).add(cp(wood + "_log"), cp("stripped_" + wood + "_log"),
					cp("stripped_" + wood + "_wood"));
			tag(BlockTags.SLABS).add(cp(wood + "_slab"));
			tag(BlockTags.WOODEN_SLABS).add(cp(wood + "_slab"));
			tag(BlockTags.PLANKS).add(cp(wood + "_planks"));
			tag(BlockTags.STAIRS).add(cp(wood + "_stairs"));
			tag(BlockTags.WOODEN_STAIRS).add(cp(wood + "_stairs"));
			tag(BlockTags.SIGNS).add(cp(wood + "_sign")).add(cp(wood + "_wall_sign"));
			tag(BlockTags.STANDING_SIGNS).add(cp(wood + "_sign"));
			tag(BlockTags.WALL_SIGNS).add(cp(wood + "_wall_sign"));
			tag(BlockTags.FENCE_GATES).add(cp(wood + "_fence_gate"));
			tag(BlockTags.UNSTABLE_BOTTOM_CENTER).add(cp(wood + "_fence_gate"));
			tag(BlockTags.WALL_HANGING_SIGNS).add(cp(wood+"_hanging_sign"));
			tag(CPTags.Blocks.FRUITS_GROWABLE_ON).add(cp(wood + "_leaves"));
			tag(frl("fence_gates")).add(cp(wood + "_fence_gate"));
			tag(frl("fence_gates/wooden")).add(cp(wood + "_fence_gate"));
			
		}
		tag(CPTags.Blocks.FUMAROLE_BLOOM).add(CPBlocks.PUMICE_BLOOM.getKey()).add(CPBlocks.LITHARGE_BLOOM.getKey());
		tag(CPTags.Blocks.LOAF_HEATING_BLOCKS).add(rk(Blocks.FIRE)).add(rk(Blocks.CAMPFIRE)).add(rk(Blocks.LAVA)).add(rk(Blocks.LAVA_CAULDRON)).add(CPBlocks.FUMAROLE_VENT.getKey());
		tag(CPTags.Blocks.LOAF_HEATING_IGNORE).addTags(BlockTags.ALL_SIGNS,BlockTags.ALL_HANGING_SIGNS,BlockTags.BANNERS,BlockTags.BUTTONS,BlockTags.FENCES,BlockTags.CROPS,CPTags.Blocks.PANS).add(CPBlocks.WOLF.getKey(),CPBlocks.LOAF.getKey(),CPBlocks.LOAF_DOUGH.getKey(),CPBlocks.PUMICE_BLOOM.getKey());
		tag(BlockTags.CLIMBABLE).add(CPBlocks.LOAF_DOUGH.getKey()).add(CPBlocks.LOAF.getKey());
		for (MaterialType tye : CPBlocks.all_materials) {
			String str=tye.getName();
			if(tye.isPillarMaterial())
				for (String type : ImmutableSet.of("_column_fluted_plinth", "_column_fluted_shaft", "_column_shaft",
						"_column_plinth", "_ionic_column_capital", "_tuscan_column_capital", "_acanthine_column_capital","_lacunar_tile","_spoked_fence"))
					pickaxe.add(cp(str + type));
			if(tye.isDecorationMaterial()) {
				pickaxe.add(cp(str), cp(str + "_slab"), cp(str + "_stairs"), cp(str + "_wall"));
				tag(BlockTags.SLABS).add(cp(str + "_slab"));
				tag(BlockTags.STAIRS).add(cp(str + "_stairs"));
				tag(BlockTags.WALLS).add(cp(str + "_wall"));
			}
			if(tye.isCounterMaterial()) {
				pickaxe.add(cp(str + "_chimney_flue"), cp(str + "_chimney_pot"), cp(str + "_counter"),
						cp(str + "_counter_with_dolium"));
				tag(CPTags.Blocks.COUNTERS).add(cp(str + "_chimney_flue"), cp(str + "_chimney_pot"), cp(str + "_counter"),
						cp(str + "_counter_with_dolium"));
				tag(CPTags.Blocks.CHINMEY_BLOCK).add(cp(str + "_chimney_flue"));
				tag(CPTags.Blocks.CHIMNEY_POT).add(cp(str + "_chimney_pot"));
			}
			if(tye.isHypocaustMaterial()) {
				tag(CPTags.Blocks.CALIDUCTS).add(cp(str + "_caliduct"));
				tag(CPTags.Blocks.HYPOCAUST_HEAT_CONDUCTOR).add(cp(str + "_hypocaust_firebox"));
				tag(CPTags.Blocks.CHIMNEY_IGNORES).add(cp(str + "_hypocaust_firebox"));
				pickaxe.add(cp(str + "_caliduct")).add(cp(str + "_hypocaust_firebox"));
			}
			if(tye.isRoadMaterial()) {
				pickaxe.add(cp(str+"_road")).add(cp(str+"_road_side"));
			}
		}

		adds(tag(CPTags.Blocks.PANS),CPBlocks.STONE_PAN.getKey(), CPBlocks.COPPER_PAN.getKey(), CPBlocks.IRON_PAN.getKey());
		adds(tag(CPTags.Blocks.CHIMNEY_IGNORES)
				.addTags(otag("pans"), BlockTags.SIGNS, BlockTags.BUTTONS, BlockTags.LEAVES, BlockTags.BANNERS,
						BlockTags.CANDLES, BlockTags.WALL_SIGNS, BlockTags.STANDING_SIGNS, BlockTags.CANDLES,
						BlockTags.CORAL_PLANTS, BlockTags.FENCES, BlockTags.WALLS, BlockTags.TRAPDOORS, BlockTags.DOORS,
						BlockTags.FLOWER_POTS, BlockTags.WALL_POST_OVERRIDE, BlockTags.FLOWERS)
				,rk(Blocks.AIR), rk(Blocks.VINE), rk(Blocks.CAVE_VINES), CPBlocks.STEW_POT.getKey(), CPBlocks.WOLF.getKey());
		tag(CPTags.Blocks.FUMAROLE_HOT_BLOCK).add(rk(Blocks.MAGMA_BLOCK));
		tag(CPTags.Blocks.FUMAROLE_VERY_HOT_BLOCK).add(rk(Blocks.LAVA));
		for (String bush : ImmutableSet.of("wolfberry", "fig")) {
			tag(BlockTags.LOGS).add(cp(bush + "_log"));
			tag(BlockTags.LOGS_THAT_BURN).add(cp(bush + "_log"));
			tag(BlockTags.LEAVES).add(cp(bush + "_leaves"));
			tag(CPTags.Blocks.FRUITS_GROWABLE_ON).add(cp(bush + "_leaves"));
			tag(BlockTags.SAPLINGS).add(cp(bush + "_sapling"));
			tag(BlockTags.MINEABLE_WITH_AXE).add(cp(bush + "_log"));
			tag(BlockTags.MINEABLE_WITH_HOE).add(cp(bush + "_leaves")).add(cp(bush + "_fruits"));
			tag(CPTags.Blocks.SNAIL_PLUMP_FOOD).add(cp(bush + "_fruits"));
		}
		adds(tag(BlockTags.MINEABLE_WITH_HOE),CPBlocks.SNAIL.getKey(),CPBlocks.SNAIL_BAIT.getKey(),CPBlocks.SNAIL_MUCUS.getKey());
		adds(pickaxe,CPBlocks.PUMICE_BLOOM.getKey(), CPBlocks.FUMAROLE_BOULDER.getKey(), CPBlocks.FUMAROLE_VENT.getKey(), CPBlocks.PUMICE.getKey());
		adds(pickaxe,CPBlocks.WOLF.getKey(), CPBlocks.STONE_PAN.getKey(), CPBlocks.COPPER_PAN.getKey(), CPBlocks.IRON_PAN.getKey(),CPBlocks.LEAD_PAN.getKey());
		adds(tag(BlockTags.NEEDS_STONE_TOOL),CPBlocks.WOLF.getKey(), CPBlocks.COPPER_PAN.getKey(), CPBlocks.IRON_PAN.getKey());
		tag(CPTags.Blocks.HYPOCAUST_HEAT_CONDUCTOR).addTag(otag("caliducts"));
		tag(CPTags.Blocks.SNAIL_GROWABLE_ON).addTag(CPTags.Blocks.FRUITS_GROWABLE_ON).addTag(BlockTags.LEAVES).add(CPBlocks.SNAIL_MUCUS.getKey());
		tag(CPTags.Blocks.SNAIL_PLUMP_FOOD).add(CPBlocks.WALNUT_FRUIT.getKey());
		adds(tag(CPTags.Blocks.SNAIL_FOOD).addTag(CPTags.Blocks.FRUITS_GROWABLE_ON).addTag(BlockTags.LEAVES).addTag(CPTags.Blocks.SNAIL_PLUMP_FOOD),CPBlocks.SNAIL_BAIT.getKey());

	}
	@SuppressWarnings("unchecked")
	@SafeVarargs
	private void adds(TagAppender<Block> ta,ResourceKey<? extends Block>... keys) {
		for(ResourceKey<? extends Block> blk:keys)
		ta.add((ResourceKey<Block>) blk);
	}
	private TagAppender<Block> tag(String s) {
		return this.tag(BlockTags.create(mrl(s)));
	}

	private ResourceKey<Block> cp(String s) {
		return ResourceKey.create(Registries.BLOCK,mrl(s));
	}
	private ResourceKey<Block> rk(Block  b) {
		return BuiltInRegistries.BLOCK.getResourceKey(b).get();
	}
	private TagAppender<Block> tag(ResourceLocation s) {
		return this.tag(BlockTags.create(s));
	}
	private ResourceLocation rl(DeferredHolder<Item,Item> it) {
		return it.getId();
	}

	private ResourceLocation rl(String r) {
		return ResourceLocation.parse(r);
	}

	private TagKey<Block> otag(String s) {
		return BlockTags.create(mrl(s));
	}

	private TagKey<Item> atag(ResourceLocation s) {
		return ItemTags.create(s);
	}

	private ResourceLocation mrl(String s) {
		return ResourceLocation.fromNamespaceAndPath(CPMain.MODID, s);
	}

	private ResourceLocation frl(String s) {
		return ResourceLocation.fromNamespaceAndPath("c", s);
	}

	private ResourceLocation mcrl(String s) {
		return ResourceLocation.withDefaultNamespace(s);
	}

	@Override
	public String getName() {
		return CPMain.MODID + " block tags";
	}


	/*@Override
	protected Path getPath(ResourceLocation id) {
		return super.pathProvider.json("data/" + id.getNamespace() + "/tags/blocks/" + id.getPath() + ".json");
	}*/
}
