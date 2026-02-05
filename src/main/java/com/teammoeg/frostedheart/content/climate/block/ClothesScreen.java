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

package com.teammoeg.frostedheart.content.climate.block;

import java.util.function.Consumer;

import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.ui.TexturedUV;
import com.teammoeg.chorda.text.Components;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData.BodyPart;
import com.teammoeg.frostedheart.util.client.FHClientUtils;

import blusunrize.immersiveengineering.client.gui.IEContainerScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class ClothesScreen extends IEContainerScreen<ClothesInventoryMenu> {
	private static final ResourceLocation TEXTURE = FHClientUtils.makeGuiTextureLocation("changing");
	public static final TexturedUV SLOT_DISABLED=new TexturedUV(FHClientUtils.makeGuiTextureLocation("empty_lining_slot_disabled"),0,0,16,16,16,16);
	public ClothesScreen(ClothesInventoryMenu inventorySlotsIn, Inventory inv, Component title) {
		super(inventorySlotsIn, inv, title, TEXTURE);
		super.imageHeight = 202;
	}

	@Override
	protected void drawBackgroundTexture(GuiGraphics graphics) {
		super.drawBackgroundTexture(graphics);
		
	}

	protected void drawContainerBackgroundPre(GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
	      int i = this.leftPos;
	      int j = this.topPos;
	      InventoryScreen.renderEntityInInventoryFollowsMouse(pGuiGraphics, i + 70, j + 97, 45, (float) (i + 40) - pMouseX,
	                (float) (j + 75 - 50) - pMouseY, ClientUtils.getPlayer());
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY)
	{
	}

	@Override
	protected void gatherAdditionalTooltips(int mouseX, int mouseY, Consumer<Component> addLine,
			Consumer<Component> addGray) {
		/*for (int j = 0; j < 5; j++) {
			BodyPart bp=BodyPart.values()[j];
			if(super.isMouseIn(mouseX, mouseY, 118, 6+18*j, 54, 18)) {
				addLine.accept(Components.str(String.valueOf(menu.partInsulation.get(bp).getFirst().getValue())));
				addLine.accept(Components.str(String.valueOf(menu.partInsulation.get(bp).getSecond().getValue())));
			}
			

		}*/
		super.gatherAdditionalTooltips(mouseX, mouseY, addLine, addGray);
	}
	
}
