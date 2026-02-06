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

package com.teammoeg.frostedheart.content.incubator;

import java.util.List;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableList;
import com.teammoeg.frostedheart.util.client.FHClientUtils;

import blusunrize.immersiveengineering.client.gui.IEContainerScreen;
import blusunrize.immersiveengineering.client.gui.info.FluidInfoArea;
import blusunrize.immersiveengineering.client.gui.info.InfoArea;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class IncubatorT1Screen extends IEContainerScreen<IncubatorT1Container> {
    private static final ResourceLocation TEXTURE = FHClientUtils.makeGuiTextureLocation("incubator");

    public IncubatorT1Screen(IncubatorT1Container container, Inventory inv, Component title) {
        super(container, inv, title, TEXTURE);
    }


    @Override
	protected List<InfoArea> makeInfoAreas() {
		return ImmutableList.of(new FluidInfoArea(menu.tankin, new Rect2i(leftPos+61,topPos+20,16,46), 177, 177, 20, 51, background),
			new FluidInfoArea(menu.tankout, new Rect2i(leftPos+117,topPos+20,16,46), 177, 177, 20, 51, background));
	}


	@Override
	public void drawContainerBackgroundPre(@Nonnull GuiGraphics transform, float partialTicks, int x, int y) {
		super.drawContainerBackgroundPre(transform, partialTicks, y, y);
		
       // transform.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        // recipe progress icon
        if (menu.process.getValue()>0) {
            int w = (int) (32 * menu.process.getValue());
            transform.blit(TEXTURE, leftPos + 80, topPos + 28, 176, 0, 32 - w, 29);
        }
        if (menu.fuel.getValue() > 0) {
            int h = (int) (14 * menu.fuel.getValue());
            transform.blit(TEXTURE, leftPos + 35, topPos + 35 + (14 - h), 198, 64 + (14 - h), 14, h);
        }
        if (menu.efficiency.getValue() > 0) {
            int h = (int) (35 * menu.efficiency.getValue());
            if (menu.isFoodRecipe.getValue())
            	transform.blit(TEXTURE, leftPos + 19, topPos + 35 + (35 - h), 198, 29 + (35 - h), 9, h);
            else
            	transform.blit(TEXTURE, leftPos + 19, topPos + 35 + (35 - h), 207, 29 + (35 - h), 9, h);
        } else
        	transform.blit(TEXTURE, leftPos + 19, topPos + 35, 216, 29, 9, 35);
    }

    @Override
    public boolean isMouseIn(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= leftPos + x && mouseY >= topPos + y
                && mouseX < leftPos + x + w && mouseY < topPos + y + h;
    }

}
