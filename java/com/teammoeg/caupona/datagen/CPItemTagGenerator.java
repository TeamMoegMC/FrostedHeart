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

import com.google.common.collect.ImmutableList;
import com.teammoeg.caupona.CPBlocks;
import com.teammoeg.caupona.CPItems;
import com.teammoeg.caupona.CPMain;
import com.teammoeg.caupona.util.MaterialType;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredHolder;

@SuppressWarnings("unused")
public class CPItemTagGenerator extends TagsProvider<Item> {

	public CPItemTagGenerator(DataGenerator dataGenerator, String modId, ExistingFileHelper existingFileHelper,CompletableFuture<HolderLookup.Provider> provider) {
		super(dataGenerator.getPackOutput(), Registries.ITEM,provider, modId, existingFileHelper);
	}

	static final String fd = "farmersdelight";
	static final String sf = "simplefarming:";

	@SuppressWarnings("unchecked")
	@Override
	protected void addTags(Provider pProvider) {
		/*
		 * Builder<Item>
		 * i=this.getOrCreateBuilder(ItemTags.createOptional(mrl("cookable"))).add(Items
		 * .EGG);
		 * for(Item it:ForgeRegistries.ITEMS.getValues()) {
		 * if(it.isFood()) {
		 * if(it.getRegistryName().getNamespace().equals("minecraft"))
		 * i.add(it);
		 * else
		 * i.addOptional(it.getRegistryName());
		 * }
		 * }
		 */
		for (String wood : CPBlocks.woods) {

			tag(ItemTags.LEAVES).add(cp(wood + "_leaves"));
			tag(ItemTags.SAPLINGS).add(cp(wood + "_sapling"));

			tag(ItemTags.DOORS).add(cp(wood + "_door"));
			tag(ItemTags.WOODEN_DOORS).add(cp(wood + "_door"));

			tag(ItemTags.FENCES).add(cp(wood + "_fence"));
			tag(ItemTags.WOODEN_FENCES).add(cp(wood + "_fence"));
			tag(ftag("fence_gates")).add(cp(wood + "_fence_gate"));
			tag(ftag("fence_gates/wooden")).add(cp(wood + "_fence_gate"));
			tag(ItemTags.TRAPDOORS).add(cp(wood + "_trapdoor"));
			tag(ItemTags.WOODEN_TRAPDOORS).add(cp(wood + "_trapdoor"));

			tag(ItemTags.WOODEN_PRESSURE_PLATES).add(cp(wood + "_pressure_plate"));
			tag(wood+"_log").add(cp(wood + "_wood")).add(cp(wood + "_log")).add(cp("stripped_" + wood + "_wood")).add(cp("stripped_" + wood + "_log"));
			tag(ItemTags.LOGS).add(cp(wood + "_wood")).add(cp(wood + "_log")).add(cp("stripped_" + wood + "_wood")).add(cp("stripped_" + wood + "_log"));

			tag(ItemTags.SLABS).add(cp(wood + "_slab"));
			tag(ItemTags.WOODEN_SLABS).add(cp(wood + "_slab"));

			tag(ItemTags.PLANKS).add(cp(wood + "_planks"));

			tag(ItemTags.STAIRS).add(cp(wood + "_stairs"));
			tag(ItemTags.WOODEN_STAIRS).add(cp(wood + "_stairs"));

			tag(ItemTags.WOODEN_BUTTONS).add(cp(wood + "_button"));

			tag(ItemTags.SIGNS).add(cp(wood + "_sign"));

		}
		for (String wood : ImmutableList.of("fig", "wolfberry")) {
			tag(ItemTags.LEAVES).add(cp(wood + "_leaves"));
			tag(ItemTags.SAPLINGS).add(cp(wood + "_sapling"));
			tag(ItemTags.LOGS).add(cp(wood + "_log"));
		}
		for (MaterialType typ : CPBlocks.all_materials) {
			if(!typ.isDecorationMaterial())continue;
			String stone=typ.getName();
			tag(ItemTags.SLABS).add(cp(stone + "_slab"));
			tag(ItemTags.STAIRS).add(cp(stone + "_stairs"));
			tag(ItemTags.WALLS).add(cp(stone + "_wall"));
		}
		for (String s : CPItems.aspics) {
			tag("aspics").add(cp(s));
		}
//		tag("fuel/woods").addTags(ItemTags.LOGS, ItemTags.PLANKS, ItemTags.WOODEN_BUTTONS, ItemTags.WOODEN_DOORS,
//				ItemTags.WOODEN_FENCES, ItemTags.WOODEN_PRESSURE_PLATES, ItemTags.WOODEN_SLABS, ItemTags.WOODEN_STAIRS,
//				ItemTags.WOODEN_TRAPDOORS, ItemTags.SAPLINGS);
//		tag("fuel/charcoals").add(rk(Items.CHARCOAL));
//		tag("fuel/fossil").addTags(ItemTags.COALS);
//		tag("fuel/lava").add(rk(Items.LAVA_BUCKET));
		//tag(MEATS).addTag(POULTRY).addTag(MEAT);
		//tag(SEAFOOD).addTag(FISH).addTag(CRUSTACEANS);
		//tag(PUMPKIN).addOptional(rl(fd + ":pumpkin_slice")).add(rk(Items.PUMPKIN), rk(Items.CARVED_PUMPKIN));
		tag(frl("raw_beef")).add(rk(Items.BEEF));
		//tag(WALNUT).add(cp("walnut"));
		//tag(BAKED).add(rk(Items.BREAD)).addTag(ftag("pasta")).addOptional(rl(fd + ":pie_crust"));
		//tag(CEREALS).addTag(RICE).addTag(ftag("grain")).addTag(BAKED).add(rk(Items.WHEAT), rk(Items.WHEAT_SEEDS)).addTag(ftag("bread"));

		//tag(RICE).addTag(ftag("grain/rice"));
//		tag(ROOTS).add(rk(Items.POTATO), rk(Items.BAKED_POTATO)).addTag(ftag("rootvegetables"));
//		tag(VEGETABLES).add(rk(Items.CARROT), rk(Items.BEETROOT), rk(Items.PUMPKIN))
//		.addTag(ftag("vegetables")).addTag(ftag("vegetable")).addTag(GREENS)
//		.addTag(MUSHROOMS).addTag(ROOTS).addTag(ftag("salad_ingredients"))
//		.addTag(PUMPKIN);

		
		

//		tag(GREENS).addTag(ftag("vegetables/asparagus")).add(rk(Items.FERN), rk(Items.LARGE_FERN), rk(Items.ALLIUM)).add(cp("fresh_wolfberry_leaves"));
//		tag(EGGS).add(rk(Items.EGG)).addTag(ftag("cooked_eggs")).add(cp("snail_block"));
//		tag(CRUSTACEANS).add(cp("snail")).add(cp("plump_snail"));
//		tag(FISH).addTag(atag(mcrl("fishes"))).addTag(ftag("raw_fishes"));
//		tag(SEAFOOD).add(rk(Items.KELP), rk(Items.DRIED_KELP));
//		tag(POULTRY).add(rk(Items.CHICKEN), rk(Items.RABBIT)).addTag(ftag("raw_chicken")).addTag(ftag("raw_rabbit"))
//				.addTag(ftag("bread")).addOptional(rl(sf + "raw_chicken_wings")).addOptional(rl(sf + "raw_sausage"))
//				.addOptional(rl(sf + "raw_horse_meat"));

//		tag(MEAT).add(rk(Items.BEEF), rk(Items.MUTTON), rk(Items.PORKCHOP), rk(Items.ROTTEN_FLESH)).addTag(ftag("bacon"))
//
//				.addTag(ftag("raw_pork")).addTag(ftag("raw_beef")).addTag(ftag("raw_mutton"))
//				.addOptional(rl(fd + ":ham")).addTag(ftag("raw_bacon"));

//		tag(SUGAR).add(rk(Items.SUGAR_CANE), rk(Items.HONEYCOMB), rk(Items.HONEY_BOTTLE));
//		tag("bone").add(rk(Items.BONE));
//		tag("ice").add(rk(Items.ICE), rk(Items.BLUE_ICE), rk(Items.PACKED_ICE));
//		tag(MUSHROOMS).add(rk(Items.BROWN_MUSHROOM), rk(Items.RED_MUSHROOM)).addTag(ftag("mushrooms"));
//		tag("fern").add(rk(Items.FERN), rk(Items.LARGE_FERN));

//		tag("wolfberries").add(cp("wolfberries"));
		tag("stews").add(CPItems.stews.stream().map(this::rk).toArray(i->new ResourceKey[i]));
		tag("stoves").add(CPBlocks.stoves.stream().map(e->rk(e.get().asItem())).toArray(ResourceKey[]::new));
//		tag("portable_brazier_fuel").add(rk(Items.MAGMA_CREAM)).add(cp("vivid_charcoal"));
//		tag("garum_fish").add(rk(Items.COD), rk(Items.SALMON));
//		tag("vinegar_fruits").add(rk(Items.APPLE)).add(cp("fig"));
//		tag("vinegar_fruits_small").add(rk(Items.SWEET_BERRIES)).add(cp("wolfberries"));
		tag(ftag("ingots/lead")).add(cp("lead_ingot"));
		tag(ftag("nuggets/lead")).add(cp("lead_nugget"));
		tag(ftag("storage_blocks/lead")).add(cp("lead_block"));
		
//		adds(tag(CPTags.Items.MOSAIC_BASE),Items.WHITE_CONCRETE,Items.LIGHT_GRAY_CONCRETE,Items.GRAY_CONCRETE,Items.BLACK_CONCRETE,Items.BROWN_CONCRETE,
//		Items.RED_CONCRETE,Items.ORANGE_CONCRETE,Items.YELLOW_CONCRETE,Items.LIME_CONCRETE,Items.GREEN_CONCRETE,
//        Items.CYAN_CONCRETE,Items.LIGHT_BLUE_CONCRETE,Items.BLUE_CONCRETE,Items.PURPLE_CONCRETE,Items.MAGENTA_CONCRETE,
//        Items.PINK_CONCRETE);
	}
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@SafeVarargs
	private void adds(TagAppender<Item> ta,Item... keys) {
		
		ResourceKey[] rks=new ResourceKey[keys.length];
		for(int i=0;i<rks.length;i++)
			rks[i]=rk(keys[i]);
		ta.add(rks);
	}
	private TagAppender<Item> tag(String s) {
		return this.tag(ItemTags.create(mrl(s)));
	}

	private TagAppender<Item> tag(ResourceLocation s) {
		return this.tag(ItemTags.create(s));
	}
	private ResourceKey<Item> rk(Item b) {
		
		return BuiltInRegistries.ITEM.getResourceKey(b).orElseThrow();
	}
	private ResourceLocation rl(DeferredHolder<Item,Item> it) {
		return it.getId();
	}

	private ResourceLocation rl(String r) {
		return ResourceLocation.parse(r);
	}

	private TagKey<Item> otag(String s) {
		return ItemTags.create(mrl(s));
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

	private TagKey<Item> ftag(String s) {
		TagKey<Item> tag = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", s));
		this.tag(tag);
		return tag;
	}

	private ResourceLocation mcrl(String s) {
		return ResourceLocation.withDefaultNamespace(s);
	}

	@Override
	public String getName() {
		return CPMain.MODID + " item tags";
	}

	private ResourceKey<Item> cp(String s) {
		return ResourceKey.create(Registries.ITEM,mrl(s));
	}
/*
	@Override
	protected Path getPath(ResourceLocation id) {
		return this.generator.getOutputFolder()
				.resolve("data/" + id.getNamespace() + "/tags/items/" + id.getPath() + ".json");
	}*/

}
