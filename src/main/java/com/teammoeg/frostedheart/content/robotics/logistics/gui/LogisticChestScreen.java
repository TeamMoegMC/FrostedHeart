/*
 * Copyright (c) 2026 TeamMoeg
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

package com.teammoeg.frostedheart.content.robotics.logistics.gui;

import java.util.function.Consumer;

import com.teammoeg.frostedheart.util.client.FHClientUtils;

import blusunrize.immersiveengineering.client.gui.IEContainerScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class LogisticChestScreen extends IEContainerScreen<LogisticChestMenu> {
    private static final ResourceLocation TEXTURE = FHClientUtils.makeGuiTextureLocation("bot_dock");

    public LogisticChestScreen(LogisticChestMenu inventorySlotsIn, Inventory inv, Component title) {
        super(inventorySlotsIn, inv, title,TEXTURE);
        this.imageHeight = 200;
        this.inventoryLabelY = this.imageHeight - 94;
    }
    @Override
	protected void drawBackgroundTexture(GuiGraphics graphics) {
		graphics.blit(background, leftPos, topPos, 0, 0, imageWidth, imageHeight);
	}

	@Override
	protected void drawContainerBackgroundPre(GuiGraphics matrixStack, float partialTicks, int x, int y) {

	}

	protected void renderLabels(GuiGraphics matrixStack, int x, int y) {
		// titles
		matrixStack.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xff404040);
		// this.font.drawText(matrixStack, this.playerInventory.getDisplayName(),
		// this.playerInventoryTitleX, this.playerInventoryTitleY+5, 0xff404040); 
	}

	@Override
	public void init() {
		super.init();


	}

	@Override
	protected void gatherAdditionalTooltips(int mouseX, int mouseY, Consumer<Component> addLine, Consumer<Component> addGray) {
		super.gatherAdditionalTooltips(mouseX, mouseY, addLine, addGray);
	
	}
}
