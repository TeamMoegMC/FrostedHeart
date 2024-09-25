/*
 * Copyright (c) 2021-2024 TeamMoeg
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

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.teammoeg.frostedheart.util.TranslateUtils;
import com.teammoeg.frostedheart.util.client.FHGuiHelper;

import blusunrize.immersiveengineering.client.gui.IEContainerScreen;
import blusunrize.immersiveengineering.client.utils.GuiHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;

public class IncubatorT2Screen extends IEContainerScreen<IncubatorT2Container> {
    private static final ResourceLocation TEXTURE = TranslateUtils.makeTextureLocation("incubatorii");
    private HeatIncubatorTileEntity tile;

    public IncubatorT2Screen(IncubatorT2Container container, Inventory inv, Component title) {
        super(container, inv, title);
        this.tile = container.tile;
    }


    @Override
    protected void renderBg(PoseStack transform, float partial, int x, int y) {
        FHGuiHelper.bindTexture(TEXTURE);
        this.blit(transform, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        GuiHelper.handleGuiTank(transform, tile.fluid[0], leftPos + 88, topPos + 20, 16, 46, 177, 177, 20, 51, x, y, TEXTURE, null);
        GuiHelper.handleGuiTank(transform, tile.fluid[1], leftPos + 124, topPos + 20, 16, 46, 177, 177, 20, 51, x, y, TEXTURE, null);
        // recipe progress icon
        if (tile.processMax > 0 && tile.process > 0) {
            int w = (int) (14 * (tile.process / (float) tile.processMax));
            this.blit(transform, leftPos + 107, topPos + 28, 176, 0, 14 - w, 29);
        }
        if (tile.network.getPower() > 0) {
            float v = tile.network.getPower() / tile.network.getMaxPower();
            boolean a = false, b = false;
            if (v > 0.75) {
                a = b = true;
            } else if (v > 0.5) {
                b = true;
            } else if (v > 0.25)
                a = true;
            this.blit(transform, leftPos + 10, topPos + 24, 176 + (a ? 38 : 0), 81 + (b ? 38 : 0), 38, 38);
        } else this.blit(transform, leftPos + 10, topPos + 24, 176, 81, 38, 38);
        if (tile.efficiency > 0) {
            int h = (int) (51 * (tile.efficiency / 2f));
            if (tile.isFoodRecipe)
                this.blit(transform, leftPos + 52, topPos + 16 + (51 - h), 198, 29 + (51 - h), 9, h);
            else
                this.blit(transform, leftPos + 52, topPos + 16 + (51 - h), 207, 29 + (51 - h), 9, h);
        } else
            this.blit(transform, leftPos + 52, topPos + 16, 216, 29, 9, 51);
    }

    @Override
    public boolean isMouseIn(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= leftPos + x && mouseY >= topPos + y
                && mouseX < leftPos + x + w && mouseY < topPos + y + h;
    }

    @Override
    public void render(PoseStack transform, int mouseX, int mouseY, float partial) {
        super.render(transform, mouseX, mouseY, partial);
        List<Component> tooltip = new ArrayList<>();
        GuiHelper.handleGuiTank(transform, tile.fluid[0], leftPos + 88, topPos + 20, 16, 46, 177, 177, 20, 51, mouseX, mouseY, TEXTURE, tooltip);
        GuiHelper.handleGuiTank(transform, tile.fluid[1], leftPos + 124, topPos + 20, 16, 46, 177, 177, 20, 51, mouseX, mouseY, TEXTURE, tooltip);

        if (!tooltip.isEmpty()) {
            net.minecraftforge.fml.client.gui.GuiUtils.drawHoveringText(transform, tooltip, mouseX, mouseY, width, height, -1, font);
        }
    }
}
