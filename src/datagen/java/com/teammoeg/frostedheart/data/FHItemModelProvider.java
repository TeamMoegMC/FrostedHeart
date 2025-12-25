/*
 * Copyright (c) 2024 TeamMoeg
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

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.common.FHItems;
import com.tterrag.registrate.util.entry.ItemEntry;
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
//		for(String s:FHItems.colors) {
//			texture(s+"_thermos","flask_i/insulated_flask_i_pouch_"+s);
//			texture(s+"_advanced_thermos","flask_ii/insulated_flask_ii_pouch_"+s);
//		}

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

	public ItemModelBuilder texture(ItemEntry<? extends Item> name) {
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
