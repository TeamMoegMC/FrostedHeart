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
import com.teammoeg.frostedheart.FHItems;
import com.teammoeg.frostedheart.FHMain;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.concurrent.CompletableFuture;

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
