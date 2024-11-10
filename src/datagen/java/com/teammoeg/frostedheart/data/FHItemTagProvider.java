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
import com.teammoeg.frostedheart.FHBlocks;
import com.teammoeg.frostedheart.FHItems;
import com.teammoeg.frostedheart.FHMain;

import com.teammoeg.frostedheart.FHTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.tags.ItemTags;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.Tags;
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

		/*
		// CONDENSED BALLS
    public static RegistryObject<Item> CONDENSED_BALL_IRON_ORE = register("condensed_ball_iron", n -> new FHSnowballItem(createProps().stacksTo(16)));
    public static RegistryObject<Item> CONDENSED_BALL_COPPER_ORE = register("condensed_ball_copper", n -> new FHSnowballItem(createProps().stacksTo(16)));
    public static RegistryObject<Item> CONDENSED_BALL_GOLD_ORE = register("condensed_ball_gold", n -> new FHSnowballItem(createProps().stacksTo(16)));
    public static RegistryObject<Item> CONDENSED_BALL_ZINC_ORE = register("condensed_ball_zinc", n -> new FHSnowballItem(createProps().stacksTo(16)));
    public static RegistryObject<Item> CONDENSED_BALL_SILVER_ORE = register("condensed_ball_silver", n -> new FHSnowballItem(createProps().stacksTo(16)));
    public static RegistryObject<Item> CONDENSED_BALL_CASSITERITE_ORE = register("condensed_ball_cassiterite", n -> new FHSnowballItem(createProps().stacksTo(16)));
    public static RegistryObject<Item> CONDENSED_BALL_PYRITE_ORE = register("condensed_ball_pyrite", n -> new FHSnowballItem(createProps().stacksTo(16)));
    public static RegistryObject<Item> CONDENSED_BALL_PENTLANDITE_ORE = register("condensed_ball_pentlandite", n -> new FHSnowballItem(createProps().stacksTo(16)));
    public static RegistryObject<Item> CONDENSED_BALL_GALENA_ORE = register("condensed_ball_galena", n -> new FHSnowballItem(createProps().stacksTo(16)));

    // CRUSHED ORES
    public static RegistryObject<Item> CRUSHED_RAW_SILVER = register("crushed_raw_silver", n -> new FHBaseItem(createProps()));
    public static RegistryObject<Item> CRUSHED_RAW_CASSITERITE = register("crushed_raw_cassiterite", n -> new FHBaseItem(createProps()));
    public static RegistryObject<Item> CRUSHED_RAW_PYRITE = register("crushed_raw_pyrite", n -> new FHBaseItem(createProps()));
    public static RegistryObject<Item> CRUSHED_RAW_PENTLANDITE = register("crushed_raw_pentlandite", n -> new FHBaseItem(createProps()));
    public static RegistryObject<Item> CRUSHED_RAW_GALENA = register("crushed_raw_galena", n -> new FHBaseItem(createProps()));
    public static RegistryObject<Item> CRUSHED_RAW_HALITE = register("crushed_raw_halite", n -> new FHBaseItem(createProps()));
    public static RegistryObject<Item> CRUSHED_RAW_POTASH = register("crushed_raw_potash", n -> new FHBaseItem(createProps()));
    public static RegistryObject<Item> CRUSHED_RAW_MAGNESITE = register("crushed_raw_magnesite", n -> new FHBaseItem(createProps()));

    // RAW ORES
    public static RegistryObject<Item> RAW_SILVER = register("raw_silver", n -> new FHBaseItem(createProps()));
    public static RegistryObject<Item> RAW_CASSITERITE = register("raw_cassiterite", n -> new FHBaseItem(createProps()));
    public static RegistryObject<Item> RAW_PYRITE = register("raw_pyrite", n -> new FHBaseItem(createProps()));
    public static RegistryObject<Item> RAW_PENTLANDITE = register("raw_pentlandite", n -> new FHBaseItem(createProps()));
    public static RegistryObject<Item> RAW_GALENA = register("raw_galena", n -> new FHBaseItem(createProps()));
    public static RegistryObject<Item> RAW_HALITE = register("raw_halite", n -> new FHBaseItem(createProps()));
    public static RegistryObject<Item> RAW_POTASH = register("raw_potash", n -> new FHBaseItem(createProps()));
    public static RegistryObject<Item> RAW_MAGNESITE = register("raw_magnesite", n -> new FHBaseItem(createProps()));

		 */
		tag(Tags.Items.RAW_MATERIALS)
				.add(rk(FHItems.RAW_SILVER))
				.add(rk(FHItems.RAW_TIN))
				.add(rk(FHItems.RAW_PYRITE))
				.add(rk(FHItems.RAW_NICKEL))
				.add(rk(FHItems.RAW_LEAD))
				.add(rk(FHItems.RAW_HALITE))
				.add(rk(FHItems.RAW_SYLVITE))
				.add(rk(FHItems.RAW_MAGNESITE));

		tag(frl("raw_materials/copper"))
				.add(rk(FHItems.RUSTED_COPPER_INGOT));
		tag(frl("raw_materials/iron"))
				.add(rk(FHItems.RUSTED_IRON_INGOT))
				.add(rk(FHItems.RAW_PYRITE))
				.add(rk(FHItems.IRON_SLAG));
		tag(frl("raw_materials/tin"))
				.add(rk(FHItems.GRAY_TIN_INGOT))
				.add(rk(FHItems.RAW_TIN));
		tag(frl("raw_materials/silver"))
				.add(rk(FHItems.RAW_SILVER));
		tag(frl("raw_materials/nickel"))
				.add(rk(FHItems.RAW_NICKEL))
				.add(rk(FHItems.NICKEL_MATTE));
		tag(frl("raw_materials/lead"))
				.add(rk(FHItems.RAW_LEAD));
		tag(frl("raw_materials/salt"))
				.add(rk(FHItems.RAW_HALITE));
		tag(frl("raw_materials/potash"))
				.add(rk(FHItems.RAW_SYLVITE));
		tag(frl("raw_materials/magnesite"))
				.add(rk(FHItems.RAW_MAGNESITE));

		tag(frl("crushed_raw_materials/copper"))
				.add(rk(AllItems.CRUSHED_COPPER.asItem()));
		tag(frl("crushed_raw_materials/iron"))
				.add(rk(AllItems.CRUSHED_IRON.asItem()));
		tag(frl("crushed_raw_materials/gold"))
				.add(rk(AllItems.CRUSHED_GOLD.asItem()));
		tag(frl("crushed_raw_materials/zinc"))
				.add(rk(AllItems.CRUSHED_ZINC.asItem()));
		tag(frl("crushed_raw_materials/silver"))
				.add(rk(FHItems.CRUSHED_RAW_SILVER));
		tag(frl("crushed_raw_materials/tin"))
				.add(rk(FHItems.CRUSHED_RAW_TIN));
		tag(frl("crushed_raw_materials/iron"))
				.add(rk(FHItems.CRUSHED_RAW_PYRITE));
		tag(frl("crushed_raw_materials/nickel"))
				.add(rk(FHItems.CRUSHED_RAW_NICKEL));
		tag(frl("crushed_raw_materials/lead"))
				.add(rk(FHItems.CRUSHED_RAW_LEAD));
		tag(frl("crushed_raw_materials/salt"))
				.add(rk(FHItems.CRUSHED_RAW_HALITE));
		tag(frl("crushed_raw_materials/potash"))
				.add(rk(FHItems.CRUSHED_RAW_SYLVITE));
		tag(frl("crushed_raw_materials/magnesite"))
				.add(rk(FHItems.CRUSHED_RAW_MAGNESITE));

		tag(frl("dusts/wood"))
				.add(rk(FHItems.SAWDUST));
		tag(frl("dusts/copper"))
				.add(rk(FHItems.COPPER_DUST));
		tag(frl("dusts/aluminum"))
				.add(rk(FHItems.ALUMINUM_DUST));
//		tag(frl("dusts/steel"))
//				.add(rk(FHItems.STEEL_DUST));
//		tag(frl("dusts/electrum"))
//				.add(rk(FHItems.ELECTRUM_DUST));
//		tag(frl("dusts/constantan"))
//				.add(rk(FHItems.CONSTANTAN_DUST));
//		tag(frl("dusts/iron"))
//				.add(rk(FHItems.IRON_DUST));
//		tag(frl("dusts/cast_iron"))
//				.add(rk(FHItems.CAST_IRON_DUST));
//		tag(frl("dusts/brass"))
//				.add(rk(FHItems.BRASS_DUST));
		tag(frl("dusts/duralumin"))
				.add(rk(FHItems.DURALUMIN_DUST));
		tag(frl("dusts/gold"))
				.add(rk(FHItems.GOLD_DUST));
		tag(frl("dusts/silver"))
				.add(rk(FHItems.SILVER_DUST));
		tag(frl("dusts/nickel"))
				.add(rk(FHItems.NICKEL_DUST));
		tag(frl("dusts/lead"))
				.add(rk(FHItems.LEAD_DUST));
		tag(frl("dusts/titanium"))
				.add(rk(FHItems.TITANIUM_DUST));
//		tag(frl("dusts/bronze"))
//				.add(rk(FHItems.BRONZE_DUST));
//		tag(frl("dusts/invar"))
//				.add(rk(FHItems.INVAR_DUST));
//		tag(frl("dusts/tungsten_steel"))
//				.add(rk(FHItems.TUNGSTEN_STEEL_DUST));
		tag(frl("dusts/zinc"))
				.add(rk(FHItems.ZINC_DUST));
		tag(frl("dusts/tin"))
				.add(rk(FHItems.TIN_DUST));
		tag(frl("dusts/magnesium"))
				.add(rk(FHItems.MAGNESIUM_DUST));
		tag(frl("dusts/tungsten"))
				.add(rk(FHItems.TUNGSTEN_DUST));

		// ingots
		tag(frl("ingots/aluminum"))
				.add(rk(FHItems.ALUMINUM_INGOT));
		tag(frl("ingots/steel"))
				.add(rk(FHItems.STEEL_INGOT));
		tag(frl("ingots/electrum"))
				.add(rk(FHItems.ELECTRUM_INGOT));
		tag(frl("ingots/constantan"))
				.add(rk(FHItems.CONSTANTAN_INGOT));
		tag(frl("ingots/cast_iron"))
				.add(rk(FHItems.CAST_IRON_INGOT));
		tag(frl("ingots/duralumin"))
				.add(rk(FHItems.DURALUMIN_INGOT));
		tag(frl("ingots/silver"))
				.add(rk(FHItems.SILVER_INGOT));
		tag(frl("ingots/nickel"))
				.add(rk(FHItems.NICKEL_INGOT));
		tag(frl("ingots/lead"))
				.add(rk(FHItems.LEAD_INGOT));
		tag(frl("ingots/titanium"))
				.add(rk(FHItems.TITANIUM_INGOT));
		tag(frl("ingots/bronze"))
				.add(rk(FHItems.BRONZE_INGOT));
		tag(frl("ingots/invar"))
				.add(rk(FHItems.INVAR_INGOT));
		tag(frl("ingots/tungsten_steel"))
				.add(rk(FHItems.TUNGSTEN_STEEL_INGOT));
		tag(frl("ingots/tin"))
				.add(rk(FHItems.TIN_INGOT));
		tag(frl("ingots/magnesium"))
				.add(rk(FHItems.MAGNESIUM_INGOT));
		tag(frl("ingots/tungsten"))
				.add(rk(FHItems.TUNGSTEN_INGOT));
		tag(frl("ingots/sodium"))
				.add(rk(FHItems.SODIUM_INGOT));

		// nuggets
		tag(frl("nuggets/aluminum"))
				.add(rk(FHItems.ALUMINUM_NUGGET));
		tag(frl("nuggets/steel"))
				.add(rk(FHItems.STEEL_NUGGET));
		tag(frl("nuggets/electrum"))
				.add(rk(FHItems.ELECTRUM_NUGGET));
		tag(frl("nuggets/constantan"))
				.add(rk(FHItems.CONSTANTAN_NUGGET));
		tag(frl("nuggets/cast_iron"))
				.add(rk(FHItems.CAST_IRON_NUGGET));
		tag(frl("nuggets/duralumin"))
				.add(rk(FHItems.DURALUMIN_NUGGET));
		tag(frl("nuggets/silver"))
				.add(rk(FHItems.SILVER_NUGGET));
		tag(frl("nuggets/nickel"))
				.add(rk(FHItems.NICKEL_NUGGET));
		tag(frl("nuggets/lead"))
				.add(rk(FHItems.LEAD_NUGGET));
		tag(frl("nuggets/titanium"))
				.add(rk(FHItems.TITANIUM_NUGGET));
		tag(frl("nuggets/bronze"))
				.add(rk(FHItems.BRONZE_NUGGET));
		tag(frl("nuggets/invar"))
				.add(rk(FHItems.INVAR_NUGGET));
		tag(frl("nuggets/tungsten_steel"))
				.add(rk(FHItems.TUNGSTEN_STEEL_NUGGET));
		tag(frl("nuggets/tin"))
				.add(rk(FHItems.TIN_NUGGET));
		tag(frl("nuggets/magnesium"))
				.add(rk(FHItems.MAGNESIUM_NUGGET));
		tag(frl("nuggets/tungsten"))
				.add(rk(FHItems.TUNGSTEN_NUGGET));

		// sheets
		tag(frl("sheets/aluminum"))
				.add(rk(FHItems.ALUMINUM_SHEET));
		tag(frl("sheets/steel"))
				.add(rk(FHItems.STEEL_SHEET));
		tag(frl("sheets/electrum"))
				.add(rk(FHItems.ELECTRUM_SHEET));
		tag(frl("sheets/constantan"))
				.add(rk(FHItems.CONSTANTAN_SHEET));
		tag(frl("sheets/cast_iron"))
				.add(rk(FHItems.CAST_IRON_SHEET));
		tag(frl("sheets/duralumin"))
				.add(rk(FHItems.DURALUMIN_SHEET));
		tag(frl("sheets/silver"))
				.add(rk(FHItems.SILVER_SHEET));
		tag(frl("sheets/nickel"))
				.add(rk(FHItems.NICKEL_SHEET));
		tag(frl("sheets/lead"))
				.add(rk(FHItems.LEAD_SHEET));
		tag(frl("sheets/titanium"))
				.add(rk(FHItems.TITANIUM_SHEET));
		tag(frl("sheets/bronze"))
				.add(rk(FHItems.BRONZE_SHEET));
		tag(frl("sheets/invar"))
				.add(rk(FHItems.INVAR_SHEET));
		tag(frl("sheets/tungsten_steel"))
				.add(rk(FHItems.TUNGSTEN_STEEL_SHEET));

		// repeat plates from sheets
		tag(frl("plates/aluminum"))
				.add(rk(FHItems.ALUMINUM_SHEET));
		tag(frl("plates/steel"))
				.add(rk(FHItems.STEEL_SHEET));
		tag(frl("plates/electrum"))
				.add(rk(FHItems.ELECTRUM_SHEET));
		tag(frl("plates/constantan"))
				.add(rk(FHItems.CONSTANTAN_SHEET));
		tag(frl("plates/cast_iron"))
				.add(rk(FHItems.CAST_IRON_SHEET));
		tag(frl("plates/duralumin"))
				.add(rk(FHItems.DURALUMIN_SHEET));
		tag(frl("plates/silver"))
				.add(rk(FHItems.SILVER_SHEET));
		tag(frl("plates/nickel"))
				.add(rk(FHItems.NICKEL_SHEET));
		tag(frl("plates/lead"))
				.add(rk(FHItems.LEAD_SHEET));
		tag(frl("plates/titanium"))
				.add(rk(FHItems.TITANIUM_SHEET));
		tag(frl("plates/bronze"))
				.add(rk(FHItems.BRONZE_SHEET));
		tag(frl("plates/invar"))
				.add(rk(FHItems.INVAR_SHEET));
		tag(frl("plates/tungsten_steel"))
				.add(rk(FHItems.TUNGSTEN_STEEL_SHEET));

		// rods
		tag(frl("rods/copper"))
				.add(rk(FHItems.COPPER_ROD));
		tag(frl("rods/aluminum"))
				.add(rk(FHItems.ALUMINUM_ROD));
		tag(frl("rods/steel"))
				.add(rk(FHItems.STEEL_ROD));
		tag(frl("rods/electrum"))
				.add(rk(FHItems.ELECTRUM_ROD));
		tag(frl("rods/constantan"))
				.add(rk(FHItems.CONSTANTAN_ROD));
		tag(frl("rods/iron"))
				.add(rk(FHItems.IRON_ROD));
		tag(frl("rods/cast_iron"))
				.add(rk(FHItems.CAST_IRON_ROD));

		// wires
		tag(frl("wires/copper"))
				.add(rk(FHItems.COPPER_WIRE));
		tag(frl("wires/aluminum"))
				.add(rk(FHItems.ALUMINUM_WIRE));
		tag(frl("wires/steel"))
				.add(rk(FHItems.STEEL_WIRE));
		tag(frl("wires/electrum"))
				.add(rk(FHItems.ELECTRUM_WIRE));
		tag(frl("wires/constantan"))
				.add(rk(FHItems.CONSTANTAN_WIRE));

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

		// condense balls
		tag(FHTags.Items.CONDENSED_BALLS)
				.add(rk(FHItems.CONDENSED_BALL_IRON_ORE))
				.add(rk(FHItems.CONDENSED_BALL_COPPER_ORE))
				.add(rk(FHItems.CONDENSED_BALL_GOLD_ORE))
				.add(rk(FHItems.CONDENSED_BALL_ZINC_ORE))
				.add(rk(FHItems.CONDENSED_BALL_SILVER_ORE))
				.add(rk(FHItems.CONDENSED_BALL_TIN_ORE))
				.add(rk(FHItems.CONDENSED_BALL_PYRITE_ORE))
				.add(rk(FHItems.CONDENSED_BALL_NICKEL_ORE))
				.add(rk(FHItems.CONDENSED_BALL_LEAD_ORE));

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
