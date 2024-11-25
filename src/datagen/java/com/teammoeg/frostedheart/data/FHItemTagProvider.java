/*
 * Copyright (c) 2022-2024 TeamMoeg
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

import com.simibubi.create.AllItems;
import com.teammoeg.caupona.CPItems;
import com.teammoeg.frostedheart.FHBlocks;
import com.teammoeg.frostedheart.FHItems;
import com.teammoeg.frostedheart.FHMain;

import com.teammoeg.frostedheart.FHTags;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.tags.ItemTags;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class FHItemTagProvider extends TagsProvider<Item> {

	public FHItemTagProvider(DataGenerator dataGenerator, String modId, ExistingFileHelper existingFileHelper, CompletableFuture<HolderLookup.Provider> provider) {
		super(dataGenerator.getPackOutput(), Registries.ITEM, provider, modId, existingFileHelper);
	}


	@Override
	protected void addTags(HolderLookup.Provider pProvider) {
		tag("colored_thermos")
				.add(FHItems.allthermos.stream().map(t->rk(t.get())).toArray(ResourceKey[]::new));
		tag("colored_advanced_thermos")
				.add(FHItems.alladvthermos.stream().map(t->rk(t.get())).toArray(ResourceKey[]::new));
		tag("thermos")
				.addTag(ItemTags.create(mrl("colored_thermos")))
				.add(rk(FHItems.thermos.get()))
				.addTag(ItemTags.create(mrl("colored_advanced_thermos")))
				.add(rk(FHItems.advanced_thermos.get()));
		tag("chicken_feed").addTags(ftag("seeds"), ftag("breedables/chicken"));

		tag(frl("crushed_raw_materials/copper"))
				.add(rk(AllItems.CRUSHED_COPPER.asItem()));
		tag(frl("crushed_raw_materials/iron"))
				.add(rk(AllItems.CRUSHED_IRON.asItem()));
		tag(frl("crushed_raw_materials/gold"))
				.add(rk(AllItems.CRUSHED_GOLD.asItem()));
		tag(frl("crushed_raw_materials/zinc"))
				.add(rk(AllItems.CRUSHED_ZINC.asItem()));

		// storage_blocks
		tag(frl("storage_blocks/aluminum"))
				.add(rk(FHBlocks.ALUMINUM_BLOCK.get().asItem()));
		tag(frl("storage_blocks/steel"))
				.add(rk(FHBlocks.STEEL_BLOCK.get().asItem()));
		tag(frl("storage_blocks/electrum"))
				.add(rk(FHBlocks.ELECTRUM_BLOCK.get().asItem()));
		tag(frl("storage_blocks/constantan"))
				.add(rk(FHBlocks.CONSTANTAN_BLOCK.get().asItem()));
		tag(frl("storage_blocks/cast_iron"))
				.add(rk(FHBlocks.CAST_IRON_BLOCK.get().asItem()));
		tag(frl("storage_blocks/duralumin"))
				.add(rk(FHBlocks.DURALUMIN_BLOCK.get().asItem()));
		tag(frl("storage_blocks/silver"))
				.add(rk(FHBlocks.SILVER_BLOCK.get().asItem()));
		tag(frl("storage_blocks/nickel"))
				.add(rk(FHBlocks.NICKEL_BLOCK.get().asItem()));
		tag(frl("storage_blocks/lead"))
				.add(rk(FHBlocks.LEAD_BLOCK.get().asItem()));
		tag(frl("storage_blocks/titanium"))
				.add(rk(FHBlocks.TITANIUM_BLOCK.get().asItem()));
		tag(frl("storage_blocks/bronze"))
				.add(rk(FHBlocks.BRONZE_BLOCK.get().asItem()));
		tag(frl("storage_blocks/invar"))
				.add(rk(FHBlocks.INVAR_BLOCK.get().asItem()));
		tag(frl("storage_blocks/tungsten_steel"))
				.add(rk(FHBlocks.TUNGSTEN_STEEL_BLOCK.get().asItem()));
		tag(frl("storage_blocks/tin"))
				.add(rk(FHBlocks.TIN_BLOCK.get().asItem()));
		tag(frl("storage_blocks/magnesium"))
				.add(rk(FHBlocks.MAGNESIUM_BLOCK.get().asItem()));
		tag(frl("storage_blocks/tungsten"))
				.add(rk(FHBlocks.TUNGSTEN_BLOCK.get().asItem()));

		// permafrost
		tag(FHTags.Items.PERMAFROST)
				.add(rk(FHBlocks.DIRT_PERMAFROST.get().asItem()))
				.add(rk(FHBlocks.MYCELIUM_PERMAFROST.get().asItem()))
				.add(rk(FHBlocks.PODZOL_PERMAFROST.get().asItem()))
				.add(rk(FHBlocks.ROOTED_DIRT_PERMAFROST.get().asItem()))
				.add(rk(FHBlocks.COARSE_DIRT_PERMAFROST.get().asItem()))
				.add(rk(FHBlocks.MUD_PERMAFROST.get().asItem()))
				.add(rk(FHBlocks.GRAVEL_PERMAFROST.get().asItem()))
				.add(rk(FHBlocks.SAND_PERMAFROST.get().asItem()))
				.add(rk(FHBlocks.RED_SAND_PERMAFROST.get().asItem()))
				.add(rk(FHBlocks.CLAY_PERMAFROST.get().asItem()))
				.add(rk(FHBlocks.PEAT_PERMAFROST.get().asItem()))
				.add(rk(FHBlocks.ROTTEN_WOOD_PERMAFROST.get().asItem()))
				.add(rk(FHBlocks.BAUXITE_PERMAFROST.get().asItem()))
				.add(rk(FHBlocks.KAOLIN_PERMAFROST.get().asItem()));

		tag(FHTags.Items.IGNITION_MATERIAL)
				.add(rk(Items.FLINT));

		tag(FHTags.Items.IGNITION_METAL)
				.add(rk(Items.IRON_INGOT))
				.add(rk(Items.IRON_NUGGET));

		tag(FHTags.Items.REFUGEE_NEEDS)
				.add(rk(FHItems.military_rations))
				.add(rk(FHItems.compressed_biscuits_pack))
				.add(rk(FHItems.compressed_biscuits))
				.add(rk(FHItems.packed_nuts))
				.add(rk(FHItems.dried_vegetables))
				.add(rk(FHItems.chocolate))
				.add(rk(FHItems.black_bread))
				.add(rk(FHItems.rye_bread))
				.add(rk(FHItems.rye_porridge))
				.add(rk(FHItems.rye_sawdust_porridge))
				.add(rk(FHItems.vegetable_sawdust_soup))
				.add(rk(FHItems.vegetable_soup))
				.add(rk(Items.COOKED_PORKCHOP))
				.add(rk(Items.COOKED_BEEF))
				.add(rk(Items.COOKED_CHICKEN))
				.add(rk(Items.COOKED_COD))
				.add(rk(Items.COOKED_SALMON))
				.add(rk(Items.COOKED_RABBIT))
				.add(rk(Items.COOKED_MUTTON))
				.add(rk(Items.BAKED_POTATO))
				.add(rk(Items.BREAD))
				.add(rk(Items.APPLE))
				.add(rk(Items.BEETROOT_SOUP))
				.add(rk(Items.MUSHROOM_STEW))
				.add(rk(Items.RABBIT_STEW))
				.add(rk(Items.PUMPKIN_PIE));

		tag(FHTags.Items.DRY_FOOD)
				.add(rk(FHItems.compressed_biscuits_pack))
				.add(rk(FHItems.compressed_biscuits))
				.add(rk(FHItems.packed_nuts))
				.add(rk(FHItems.dried_vegetables))
				.add(rk(FHItems.chocolate));

		tag(FHTags.Items.INSULATED_FOOD)
				.add(rk(FHItems.military_rations))
				.add(rk(FHItems.thermos))
				.add(rk(FHItems.advanced_thermos));

		FHItems.alladvthermos.stream().forEach(t->tag(FHTags.Items.INSULATED_FOOD).add(rk(t.get())));
		FHItems.allthermos.stream().forEach(t->tag(FHTags.Items.INSULATED_FOOD).add(rk(t.get())));

	}


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

		return ForgeRegistries.ITEMS.getResourceKey(b).orElseGet(()->b.builtInRegistryHolder().key());
	}
	private ResourceKey<Item> rk(RegistryObject<Item> b) {
		return rk(b.get());
	}

	private ResourceKey<? extends Item> rk(ItemEntry<? extends Item> b) {
		return rk(b.get());
	}

	private ResourceLocation rl(RegistryObject<Item> it) {
		return it.getId();
	}

	private ResourceLocation rl(String r) {
		return new ResourceLocation(r);
	}

	private TagKey<Item> otag(String s) {
		return ItemTags.create(mrl(s));
	}

	private TagKey<Item> atag(ResourceLocation s) {
		return ItemTags.create(s);
	}

	private ResourceLocation mrl(String s) {
		return new ResourceLocation(FHMain.MODID, s);
	}

	private ResourceLocation frl(String s) {
		return new ResourceLocation("forge", s);
	}

	private TagKey<Item> ftag(String s) {
		TagKey<Item> tag = ItemTags.create(new ResourceLocation("forge", s));
		this.tag(tag);
		return tag;
	}

	private ResourceLocation mcrl(String s) {
		return new ResourceLocation(s);
	}

	@Override
	public String getName() {
		return FHMain.MODID + " item tags";
	}

	private ResourceKey<Item> cp(String s) {
		return ResourceKey.create(Registries.ITEM,mrl(s));
	}
}
