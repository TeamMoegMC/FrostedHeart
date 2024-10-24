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

import java.util.HashMap;
import java.util.Map;

import com.teammoeg.caupona.CPFluids;
import com.teammoeg.caupona.CPMain;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class RegistryJavaGenerator extends FileGenerator {

	public RegistryJavaGenerator(PackOutput output, ExistingFileHelper helper) {
		super(PackType.SERVER_DATA, output, helper,"Caupona Registry Java");
	}
	
	
	@Override
	protected void gather(FileStorage reciver) {
		JavaFileOutput fo=this.createGeneratedJavaOutput("CPStewTexture");
		fo.addImport(HashMap.class);
		fo.addImport(Map.class);
		fo.addImportDelimeter();
		fo.addImport(ResourceLocation.class);
		fo.createMap("public static","texture",HashMap.class,String.class,ResourceLocation.class);
		fo.defineBlock("static");
		for(String sf:CPFluids.getSoupfluids()) {
			ResourceLocation image = ResourceLocation.fromNamespaceAndPath(CPMain.MODID, "textures/block/soups/" + sf + ".png");
			if (helper.exists(image, PackType.CLIENT_RESOURCES)) {
				fo.line().call("texture.put")
					.paramString(sf)
					.paramCall("ResourceLocation.fromNamespaceAndPath")
						.paramString(CPMain.MODID)
						.paramString("block/soups/"+sf)
					.complete()
				.complete().end();
			}
		}
		reciver.accept(fo.complete());
		
	}

}
