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

package com.teammoeg.frostedheart.content.climate.block.wardrobe;

import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.widget.HoverableImageButton;
import com.teammoeg.chorda.client.widget.TabImageButton;
import com.teammoeg.frostedheart.util.client.FHClientUtils;

import blusunrize.immersiveengineering.client.gui.IEContainerScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class WardrobeScreen extends IEContainerScreen<WardrobeMenu> {
	private static final ResourceLocation TEXTURE = FHClientUtils.makeGuiTextureLocation("closet");
	public WardrobeScreen(WardrobeMenu inventorySlotsIn, Inventory inv, Component title) {
		super(inventorySlotsIn, inv, title, TEXTURE);
		super.imageHeight = 202;
		super.imageWidth = 261;
	}
	
	@Override
	protected void init() {
		super.init();
		this.addRenderableWidget(new HoverableImageButton(TEXTURE,leftPos + 88, topPos + 40, 21, 21, 197, 63,btn -> { menu.sendMessage(1, 0);}));
		for(int i=0;i<WardrobeBlockEntity.NUM_INVENTORY;i++) {
			final int ctab=i;
			this.addRenderableWidget(new TabImageButton(TEXTURE,leftPos+193,topPos+i*21,27,21,197,ctab==0?0:21,ctab,btn->{ menu.sendMessage(2, ctab);}).bind(menu.page.asSupplier()));
		}
		
	}

	protected void drawBackgroundTexture(GuiGraphics graphics)
	{
		graphics.blit(background, leftPos, topPos, 0, 0, 197, 202);
		
	}

	protected void drawContainerBackgroundPre(GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
	      int i = this.leftPos;
	      int j = this.topPos;
	      InventoryScreen.renderEntityInInventoryFollowsMouse(pGuiGraphics, i - 60, j + 99, 45, (float) (i -80) - pMouseX,
	                (float) (j + 77 - 50) - pMouseY, ClientUtils.getPlayer());
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY)
	{
	}
}
