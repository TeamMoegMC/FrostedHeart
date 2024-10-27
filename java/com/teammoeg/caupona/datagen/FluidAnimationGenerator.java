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

import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.teammoeg.caupona.CPFluids;
import com.teammoeg.caupona.CPMain;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class FluidAnimationGenerator extends JsonGenerator {

	public FluidAnimationGenerator(PackOutput output, ExistingFileHelper helper) {
		super(PackType.CLIENT_RESOURCES, output, helper,"Caupona Fluid Animation");
	}

	@Override
	protected void gather(JsonStorage reciver) {
		for (String sf : CPFluids.getSoupfluids()) {
			ResourceLocation image = ResourceLocation.fromNamespaceAndPath(CPMain.MODID, "textures/block/soups/" + sf + ".png");
			genImage(image,6,reciver);
		}
		/*for (String sf : new String[]{"soot_smoke","steam"}) {
			ResourceLocation image = ResourceLocation.fromNamespaceAndPath(CPMain.MODID, "textures/particle/" + sf + ".png");
			genImage(image,2,reciver);
		}*/
	}
	protected void genImage(ResourceLocation image,int ticks,JsonStorage reciver) {
		try {
			if (helper.exists(image, PackType.CLIENT_RESOURCES)) {
				Resource rc = helper.getResource(image, PackType.CLIENT_RESOURCES);

				BufferedImage bi = ImageIO.read(rc.open());
				int num = bi.getHeight() / bi.getWidth();
				JsonObject frame = new JsonObject();
				JsonObject anim = new JsonObject();
				frame.add("animation", anim);
				anim.addProperty("frametime", ticks);
				JsonArray ja = new JsonArray();
				anim.add("frames", ja);
				for (int i = 0; i < num; i++)
					ja.add(i);
				// if(rc.)
				reciver.accept(ResourceLocation.fromNamespaceAndPath(image.getNamespace(),image.getPath()+".mcmeta"), frame);
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	} 

}
