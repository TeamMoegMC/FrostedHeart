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
    private static final ResourceLocation TEXTURE = FHClientUtils.makeTextureLocation("incubatorii");
    private HeatIncubatorTileEntity tile;

    public IncubatorT2Screen(IncubatorT2Container container, Inventory inv, Component title) {
        super(container, inv, title, TEXTURE);
        this.tile = container.getBlock();
    }

    @Override
	protected List<InfoArea> makeInfoAreas() {
		return ImmutableList.of(new FluidInfoArea(tile.fluid[0], new Rect2i(88,20,16,46), 177, 177, 20, 51, background),
			new FluidInfoArea(tile.fluid[1], new Rect2i(124,20,16,46), 177, 177, 20, 51, background));
	}

	@Override
	public void drawContainerBackgroundPre(@Nonnull GuiGraphics transform, float partialTicks, int x, int y) {
		super.drawContainerBackgroundPre(transform, partialTicks, y, y);
		//transform.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        // recipe progress icon
        if (tile.processMax > 0 && tile.process > 0) {
            int w = (int) (14 * (tile.process / (float) tile.processMax));
            transform.blit(TEXTURE, leftPos + 107, topPos + 28, 176, 0, 14 - w, 29);
        }
        if (tile.network.getHeat() > 0) {
            float v = tile.network.getHeat() / tile.network.getCapacity();
            boolean a = false, b = false;
            if (v > 0.75) {
                a = b = true;
            } else if (v > 0.5) {
                b = true;
            } else if (v > 0.25)
                a = true;
            transform.blit(TEXTURE, leftPos + 10, topPos + 24, 176 + (a ? 38 : 0), 81 + (b ? 38 : 0), 38, 38);
        } else transform.blit(TEXTURE, leftPos + 10, topPos + 24, 176, 81, 38, 38);
        if (tile.efficiency > 0) {
            int h = (int) (51 * (tile.efficiency / 2f));
            if (tile.isFoodRecipe)
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
