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

import com.teammoeg.caupona.CPBlocks;
import com.teammoeg.caupona.CPItems;
import com.teammoeg.caupona.CPMain;
import com.teammoeg.caupona.util.FoodMaterialInfo;
import com.teammoeg.caupona.util.Utils;

import net.minecraft.data.DataGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class CPItemModelProvider extends ItemModelProvider {

	public CPItemModelProvider(DataGenerator generator, String modid, ExistingFileHelper existingFileHelper) {
		super(generator.getPackOutput(), modid, existingFileHelper);

	}

	@Override
	protected void registerModels() {
		
		for (String s : CPItems.soups) {
			simpleTexture(s, "soups/");
			
		}
		for(String s:CPItems.bread_bowls)
			texture(s+"_loaf", "bread_bowls/"+s);
		for(String s:CPItems.dishes)
			texture(s+"_loaf", "bread_bowls/"+s);
		for (String s : CPItems.base_material)
			texture(s);
		for (FoodMaterialInfo s : CPItems.food_material)
			texture(s.name);
		simpleTexture("water", "soups/");
		simpleTexture("milk", "soups/");
		texture("loaf_bowl", "bread_bowl");
		texture("loaf", "cob_loaf");
		texture("loaf_dough");
		for (String s : CPItems.aspics)
			simpleTexture(s, "aspics/");
		simpleTexture("milk_based", "bases/");
		simpleTexture("stock_based", "bases/");
		simpleTexture("any_based", "bases/");
		simpleTexture("water_or_stock_based", "bases/");
		texture("book", "vade_mecum_for_innkeepers");
		texture(CPItems.clay_pot.get(), "clay_stew_pot");
		texture("culinary_heat_haze");
		texture("soot");
		texture("portable_brazier");
		texture("walnut_boat");
		texture("chronoconis");
		texture("silphium");
		texture("situla");
		texture("snail_block","snail_roe");
		texture("redstone_ladle");
		texture("bamboo_skimmer");
		texture("iron_skimmer");
		texture("scraps");
		texture("walnut_hanging_sign");
		itemModel(CPBlocks.SILPHIUM.get().asItem(),"silphium").transforms().transform(ItemDisplayContext.GUI).scale(0.5f).rotation(0, 45, 0).translation(0, -4, 0).end().end();
		/*System.out.println(new File("").getAbsolutePath());
		try {
			new BufferedReader(new FileReader(new File("../src/datagen/resources/assets/caupona/block/blocks.txt"))).lines().forEach( s -> {
				if(!ForgeRegistries.BLOCKS.containsKey(ResourceLocation.fromNamespaceAndPath(CPMain.MODID,s.substring(0,s.lastIndexOf("."))))) {
					System.out.println(s);
				}
			});
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		super.singleTexture("walnut_sapling", ResourceLocation.fromNamespaceAndPath("minecraft", "item/generated"), "layer0",
				ResourceLocation.fromNamespaceAndPath(CPMain.MODID, "block/walnut_sapling"));
		super.singleTexture("fig_sapling", ResourceLocation.fromNamespaceAndPath("minecraft", "item/generated"), "layer0",
				ResourceLocation.fromNamespaceAndPath(CPMain.MODID, "block/fig_sapling"));
		super.singleTexture("wolfberry_sapling", ResourceLocation.fromNamespaceAndPath("minecraft", "item/generated"), "layer0",
				ResourceLocation.fromNamespaceAndPath(CPMain.MODID, "block/wolfberry_sapling"));
		// super.withExistingParent("clay_cistern",new
		// ResourceLocation(Main.MODID,"block/clay_cistern"));
		for (String s : CPItems.spices)
			simpleTexture(s, "");
		for (String s : CPItems.dishes) {
			simpleTexture(s, "sauteed_dishes/");
			//simpleTexture(s+"_loaf", "bread_bowls/");
		}
		texture("gravy_boat", "walnut_oil_0").override().predicate(ResourceLocation.withDefaultNamespace("damaged"), 1)
				.predicate(ResourceLocation.withDefaultNamespace("damage"), 0.2f).model(texture("walnut_oil_1")).end().override()
				.predicate(ResourceLocation.withDefaultNamespace("damaged"), 1).predicate(ResourceLocation.withDefaultNamespace("damage"), 0.4f)
				.model(texture("walnut_oil_2")).end().override().predicate(ResourceLocation.withDefaultNamespace("damaged"), 1)
				.predicate(ResourceLocation.withDefaultNamespace("damage"), 0.6f).model(texture("walnut_oil_3")).end().override()
				.predicate(ResourceLocation.withDefaultNamespace("damaged"), 1).predicate(ResourceLocation.withDefaultNamespace("damage"), 0.8f)
				.model(texture("walnut_oil_4")).end().override().predicate(ResourceLocation.withDefaultNamespace("damaged"), 1)
				.predicate(ResourceLocation.withDefaultNamespace("damage"), 1f).model(texture("oil_bottle")).end();
	}

	public ItemModelBuilder itemModel(Item item, String name) {
		return super.withExistingParent(Utils.getRegistryName(item).getPath(), ResourceLocation.fromNamespaceAndPath(CPMain.MODID, "block/" + name));
	}

	public ItemModelBuilder simpleTexture(String name, String par) {
		return super.singleTexture(name, ResourceLocation.fromNamespaceAndPath("minecraft", "item/generated"), "layer0",
				ResourceLocation.fromNamespaceAndPath(CPMain.MODID, "item/" + par + name));
	}

	public ItemModelBuilder texture(String name) {
		return texture(name, name);
	}
	public ItemModelBuilder texture(Item name, String par) {
		return texture(Utils.getRegistryName(name).getPath(),par);
	}
	public ItemModelBuilder texture(String name, String par) {
		return super.singleTexture(name, ResourceLocation.fromNamespaceAndPath("minecraft", "item/generated"), "layer0",
				ResourceLocation.fromNamespaceAndPath(CPMain.MODID, "item/" + par));
	}
}
