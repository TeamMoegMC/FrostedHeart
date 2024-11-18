/*
 * Copyright (c) 2022 TeamMoeg
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

package com.teammoeg.frostedheart.data;

import com.teammoeg.frostedheart.FHItems;
import com.teammoeg.frostedheart.FHMain;

import net.minecraft.data.DataGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.client.model.generators.ItemModelBuilder;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class FHItemModelProvider extends ItemModelProvider {

	public FHItemModelProvider(DataGenerator generator, String modid, ExistingFileHelper existingFileHelper) {
		super(generator.getPackOutput(), modid, existingFileHelper);
	}
	
	@Override
	protected void registerModels() {
		for(String s:FHItems.colors) {
			texture(s+"_thermos","flask_i/insulated_flask_i_pouch_"+s);
			texture(s+"_advanced_thermos","flask_ii/insulated_flask_ii_pouch_"+s);
		}

		texture(FHItems.PEAT);
		texture(FHItems.KAOLIN);
		texture(FHItems.BAUXITE);
		texture(FHItems.ROTTEN_WOOD);
//		texture(FHItems.CRUSHED_KAOLIN);
		texture(FHItems.CRUSHED_RAW_BAUXITE);
		texture(FHItems.KAOLIN_DUST);
		texture(FHItems.BAUXITE_DUST);

		texture(FHItems.CONDENSED_BALL_IRON_ORE);
		texture(FHItems.CONDENSED_BALL_COPPER_ORE);
		texture(FHItems.CONDENSED_BALL_GOLD_ORE);
		texture(FHItems.CONDENSED_BALL_ZINC_ORE);
		texture(FHItems.CONDENSED_BALL_SILVER_ORE);
		texture(FHItems.CONDENSED_BALL_TIN_ORE);
		texture(FHItems.CONDENSED_BALL_PYRITE_ORE);
		texture(FHItems.CONDENSED_BALL_NICKEL_ORE);
		texture(FHItems.CONDENSED_BALL_LEAD_ORE);

		texture(FHItems.IRON_SLURRY);
		texture(FHItems.COPPER_SLURRY);
		texture(FHItems.GOLD_SLURRY);
		texture(FHItems.ZINC_SLURRY);
		texture(FHItems.SILVER_SLURRY);
		texture(FHItems.TIN_SLURRY);
		texture(FHItems.PYRITE_SLURRY);
		texture(FHItems.NICKEL_SLURRY);
		texture(FHItems.LEAD_SLURRY);

		texture(FHItems.CRUSHED_RAW_SILVER);
		texture(FHItems.CRUSHED_RAW_TIN);
		texture(FHItems.CRUSHED_RAW_PYRITE);
		texture(FHItems.CRUSHED_RAW_NICKEL);
		texture(FHItems.CRUSHED_RAW_LEAD);
		texture(FHItems.CRUSHED_RAW_HALITE);
//		texture(FHItems.CRUSHED_RAW_SYLVITE);
		texture(FHItems.CRUSHED_RAW_MAGNESITE);

		texture(FHItems.RAW_SILVER);
		texture(FHItems.RAW_TIN);
		texture(FHItems.RAW_PYRITE);
		texture(FHItems.RAW_NICKEL);
		texture(FHItems.RAW_LEAD);
		texture(FHItems.RAW_HALITE);
		texture(FHItems.RAW_SYLVITE);
		texture(FHItems.RAW_MAGNESITE);

		texture(FHItems.COPPER_DUST);
		texture(FHItems.ALUMINUM_DUST);
//		texture(FHItems.STEEL_DUST);
//		texture(FHItems.ELECTRUM_DUST);
//		texture(FHItems.CONSTANTAN_DUST);
//		texture(FHItems.IRON_DUST);
//		texture(FHItems.CAST_IRON_DUST);
//		texture(FHItems.BRASS_DUST);
		texture(FHItems.DURALUMIN_DUST);
		texture(FHItems.GOLD_DUST);
		texture(FHItems.SILVER_DUST);
		texture(FHItems.NICKEL_DUST);
		texture(FHItems.LEAD_DUST);
		texture(FHItems.TITANIUM_DUST);
//		texture(FHItems.BRONZE_DUST);
//		texture(FHItems.INVAR_DUST);
//		texture(FHItems.TUNGSTEN_STEEL_DUST);
		texture(FHItems.ZINC_DUST);
		texture(FHItems.TIN_DUST);
		texture(FHItems.MAGNESIUM_DUST);
		texture(FHItems.TUNGSTEN_DUST);

		texture(FHItems.ALUMINUM_INGOT);
		texture(FHItems.STEEL_INGOT);
		texture(FHItems.ELECTRUM_INGOT);
		texture(FHItems.CONSTANTAN_INGOT);
		texture(FHItems.CAST_IRON_INGOT);
		texture(FHItems.DURALUMIN_INGOT);
		texture(FHItems.SILVER_INGOT);
		texture(FHItems.NICKEL_INGOT);
		texture(FHItems.LEAD_INGOT);
		texture(FHItems.TITANIUM_INGOT);
		texture(FHItems.BRONZE_INGOT);
		texture(FHItems.INVAR_INGOT);
		texture(FHItems.TUNGSTEN_STEEL_INGOT);
		texture(FHItems.TIN_INGOT);
		texture(FHItems.MAGNESIUM_INGOT);
		texture(FHItems.TUNGSTEN_INGOT);

		texture(FHItems.ALUMINUM_NUGGET);
		texture(FHItems.STEEL_NUGGET);
		texture(FHItems.ELECTRUM_NUGGET);
		texture(FHItems.CONSTANTAN_NUGGET);
		texture(FHItems.CAST_IRON_NUGGET);
		texture(FHItems.DURALUMIN_NUGGET);
		texture(FHItems.SILVER_NUGGET);
		texture(FHItems.NICKEL_NUGGET);
		texture(FHItems.LEAD_NUGGET);
		texture(FHItems.TITANIUM_NUGGET);
		texture(FHItems.BRONZE_NUGGET);
		texture(FHItems.INVAR_NUGGET);
		texture(FHItems.TUNGSTEN_STEEL_NUGGET);
		texture(FHItems.TIN_NUGGET);
		texture(FHItems.MAGNESIUM_NUGGET);
		texture(FHItems.TUNGSTEN_NUGGET);

		texture(FHItems.ALUMINUM_SHEET);
		texture(FHItems.STEEL_SHEET);
		texture(FHItems.ELECTRUM_SHEET);
		texture(FHItems.CONSTANTAN_SHEET);
		texture(FHItems.CAST_IRON_SHEET);
		texture(FHItems.DURALUMIN_SHEET);
		texture(FHItems.SILVER_SHEET);
		texture(FHItems.NICKEL_SHEET);
		texture(FHItems.LEAD_SHEET);
		texture(FHItems.TITANIUM_SHEET);
		texture(FHItems.BRONZE_SHEET);
		texture(FHItems.INVAR_SHEET);
		texture(FHItems.TUNGSTEN_STEEL_SHEET);

		texture(FHItems.COPPER_ROD);
		texture(FHItems.ALUMINUM_ROD);
		texture(FHItems.STEEL_ROD);
		texture(FHItems.ELECTRUM_ROD);
		texture(FHItems.CONSTANTAN_ROD);
		texture(FHItems.IRON_ROD);
		texture(FHItems.CAST_IRON_ROD);

		texture(FHItems.COPPER_WIRE);
		texture(FHItems.ALUMINUM_WIRE);
		texture(FHItems.STEEL_WIRE);
		texture(FHItems.ELECTRUM_WIRE);
		texture(FHItems.CONSTANTAN_WIRE);

		texture(FHItems.RUSTED_IRON_INGOT);
		texture(FHItems.RUSTED_COPPER_INGOT);
		texture(FHItems.GRAY_TIN_INGOT);

		texture(FHItems.IRON_SLAG);
		texture(FHItems.NICKEL_MATTE);

		texture(FHItems.COPPER_OXIDE_DUST);
		texture(FHItems.ZINC_OXIDE_DUST);
//		texture(FHItems.TIN_OXIDE_DUST);
		texture(FHItems.ALUMINA_DUST);
		texture(FHItems.MAGNESIA_DUST);
		texture(FHItems.LEAD_OXIDE_DUST);
		texture(FHItems.ALUMINIUM_HYDROXIDE_DUST);
		texture(FHItems.SODIUM_HYDROXIDE_DUST);
		texture(FHItems.SODIUM_SULFIDE_DUST);
		texture(FHItems.SODIUM_CHLORIDE_DUST);
//		texture(FHItems.POTASSIUM_CHLORIDE_DUST);
//		texture(FHItems.SULFUR_DUST);
//		texture(FHItems.GRAPHITE_DUST);
		texture(FHItems.CRYOLITE_DUST);

		texture(FHItems.MORTAR);
		texture(FHItems.VULCANIZED_RUBBER);
		texture(FHItems.PULP);
		texture(FHItems.FIRE_CLAY_BALL);
		texture(FHItems.HIGH_REFRACTORY_BRICK);
		texture(FHItems.SAWDUST);
		texture(FHItems.BIOMASS);
		texture(FHItems.SYNTHETIC_LEATHER);
		texture(FHItems.QUICKLIME);
		texture(FHItems.SODIUM_INGOT);
		texture(FHItems.REFRACTORY_BRICK);

		texture(FHItems.MAKESHIFT_KNIFE);
		texture(FHItems.MAKESHIFT_PICKAXE);
		texture(FHItems.MAKESHIFT_AXE);
		texture(FHItems.MAKESHIFT_SHOVEL);
		texture(FHItems.MAKESHIFT_HOE);
		texture(FHItems.MAKESHIFT_SPEAR);

		texture(FHItems.BRONZE_KNIFE);
		texture(FHItems.BRONZE_PICKAXE);
		texture(FHItems.BRONZE_AXE);
		texture(FHItems.BRONZE_SHOVEL);
		texture(FHItems.BRONZE_HOE);
		texture(FHItems.BRONZE_SPEAR);
		texture(FHItems.BRONZE_SWORD);

		texture(FHItems.SNOWSHOES);
		texture(FHItems.ICE_SKATES);

//		texture(FHItems.CURIOSITY_SPAWN_EGG);
//		texture(FHItems.WANDERING_REFUGEE_SPAWN_EGG);

	}



	public ItemModelBuilder itemModel(Item item, String name) {
		return super.withExistingParent(ForgeRegistries.ITEMS.getKey(item).getPath(), new ResourceLocation(FHMain.MODID, "block/" + name));
	}

	public ItemModelBuilder simpleTexture(String name, String par) {
		return super.singleTexture(name, new ResourceLocation("minecraft", "item/generated"), "layer0",
				new ResourceLocation(FHMain.MODID, "item/" + par + name));
	}

	public ItemModelBuilder texture(String name) {
		return texture(name, name);
	}

	public ItemModelBuilder texture(RegistryObject<Item> name) {
		return texture(name.getId().getPath());
	}

	public ItemModelBuilder texture(Item name, String par) {
		return texture(ForgeRegistries.ITEMS.getKey(name).getPath(),par);
	}
	public ItemModelBuilder texture(String name, String par) {
		return super.singleTexture(name, new ResourceLocation("minecraft", "item/generated"), "layer0",
				new ResourceLocation(FHMain.MODID, "item/" + par));
	}
}
