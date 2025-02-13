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

package com.teammoeg.frostedheart.content.incubator;

import java.util.List;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableList;
import com.teammoeg.frostedheart.util.client.FHClientUtils;

import blusunrize.immersiveengineering.client.gui.IEContainerScreen;
import blusunrize.immersiveengineering.client.gui.info.FluidInfoArea;
import blusunrize.immersiveengineering.client.gui.info.InfoArea;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;

public class IncubatorT2Screen extends IEContainerScreen<IncubatorT2Container> {
    private static final ResourceLocation TEXTURE = FHClientUtils.makeGuiTextureLocation("incubatorii");

    public IncubatorT2Screen(IncubatorT2Container container, Inventory inv, Component title) {
        super(container, inv, title, TEXTURE);
    }

    @Override
	protected List<InfoArea> makeInfoAreas() {
		return ImmutableList.of(new FluidInfoArea(menu.tankin, new Rect2i(leftPos+88,topPos+20,16,46), 177, 177, 20, 51, background),
			new FluidInfoArea(menu.tankout, new Rect2i(leftPos+124,topPos+20,16,46), 177, 177, 20, 51, background));
	}

	@Override
	public void drawContainerBackgroundPre(@Nonnull GuiGraphics transform, float partialTicks, int x, int y) {
		super.drawContainerBackgroundPre(transform, partialTicks, y, y);
		//transform.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        // recipe progress icon
        if (menu.process.getValue() > 0) {
            int w = (int) (14 * (menu.process.getValue()));
            transform.blit(TEXTURE, leftPos + 107, topPos + 28, 176, 0, 14 - w, 29);
        }
        if (menu.heat.getValue() > 0) {
            float v = menu.heat.getValue();
            boolean a = false, b = false;
            if (v > 0.75) {
                a = b = true;
            } else if (v > 0.5) {
                b = true;
            } else if (v > 0.25)
                a = true;
            transform.blit(TEXTURE, leftPos + 10, topPos + 24, 176 + (a ? 38 : 0), 81 + (b ? 38 : 0), 38, 38);
        } else transform.blit(TEXTURE, leftPos + 10, topPos + 24, 176, 81, 38, 38);
        if (menu.efficiency.getValue() > 0) {
            int h = (int) (51 * (menu.efficiency.getValue() / 2f));
            if (menu.isFoodRecipe.getValue())
            	transform.blit(TEXTURE, leftPos + 52, topPos + 16 + (51 - h), 198, 29 + (51 - h), 9, h);
            else
            	transform.blit(TEXTURE, leftPos + 52, topPos + 16 + (51 - h), 207, 29 + (51 - h), 9, h);
        } else
        	transform.blit(TEXTURE, leftPos + 52, topPos + 16, 216, 29, 9, 51);
    }

    @Override
    public boolean isMouseIn(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= leftPos + x && mouseY >= topPos + y
                && mouseX < leftPos + x + w && mouseY < topPos + y + h;
    }
}
