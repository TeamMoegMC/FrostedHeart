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
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.generators.ItemModelBuilder;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

public class FHItemModelProvider extends ItemModelProvider {

	public FHItemModelProvider(DataGenerator generator,  ExistingFileHelper existingFileHelper) {
		super(generator,FHMain.MODID, existingFileHelper);
	}
	
	@Override
	protected void registerModels() {
		for(String s:FHItems.colors) {
			texture(s+"_thermos","flask_i/insulated_flask_i_pouch_"+s);
			texture(s+"_advanced_thermos","flask_ii/insulated_flask_ii_pouch_"+s);
		}
	}
	public ItemModelBuilder texture(String name, String par) {
		return super.singleTexture(name, new ResourceLocation("minecraft", "item/generated"), "layer0",
				new ResourceLocation(FHMain.MODID, "item/" + par));
	}
}
