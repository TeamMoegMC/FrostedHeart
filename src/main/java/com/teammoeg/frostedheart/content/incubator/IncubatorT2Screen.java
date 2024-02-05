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

import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.client.util.ClientUtils;
import com.teammoeg.frostedheart.client.util.GuiUtils;

import blusunrize.immersiveengineering.client.gui.IEContainerScreen;
import blusunrize.immersiveengineering.client.utils.GuiHelper;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

public class IncubatorT2Screen extends IEContainerScreen<IncubatorT2Container> {
    private static final ResourceLocation TEXTURE = GuiUtils.makeTextureLocation("incubatorii");
    private HeatIncubatorTileEntity tile;

    public IncubatorT2Screen(IncubatorT2Container container, PlayerInventory inv, ITextComponent title) {
        super(container, inv, title);
        this.tile = container.tile;
    }


    @Override
    protected void drawGuiContainerBackgroundLayer(MatrixStack transform, float partial, int x, int y) {
        ClientUtils.bindTexture(TEXTURE);
        this.blit(transform, guiLeft, guiTop, 0, 0, xSize, ySize);
        GuiHelper.handleGuiTank(transform, tile.fluid[0], guiLeft + 88, guiTop + 20, 16, 46, 177, 177, 20, 51, x, y, TEXTURE, null);
        GuiHelper.handleGuiTank(transform, tile.fluid[1], guiLeft + 124, guiTop + 20, 16, 46, 177, 177, 20, 51, x, y, TEXTURE, null);
        // recipe progress icon
        if (tile.processMax > 0 && tile.process > 0) {
            int w = (int) (14 * (tile.process / (float) tile.processMax));
            this.blit(transform, guiLeft + 107, guiTop + 28, 176, 0, 14 - w, 29);
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
            this.blit(transform, guiLeft + 10, guiTop + 24, 176 + (a ? 38 : 0), 81 + (b ? 38 : 0), 38, 38);
        } else this.blit(transform, guiLeft + 10, guiTop + 24, 176, 81, 38, 38);
        if (tile.efficiency > 0) {
            int h = (int) (51 * (tile.efficiency / 2f));
            if (tile.isFoodRecipe)
                this.blit(transform, guiLeft + 52, guiTop + 16 + (51 - h), 198, 29 + (51 - h), 9, h);
            else
                this.blit(transform, guiLeft + 52, guiTop + 16 + (51 - h), 207, 29 + (51 - h), 9, h);
        } else
            this.blit(transform, guiLeft + 52, guiTop + 16, 216, 29, 9, 51);
    }

    @Override
    public boolean isMouseIn(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= guiLeft + x && mouseY >= guiTop + y
                && mouseX < guiLeft + x + w && mouseY < guiTop + y + h;
    }

    @Override
    public void render(MatrixStack transform, int mouseX, int mouseY, float partial) {
        super.render(transform, mouseX, mouseY, partial);
        List<ITextComponent> tooltip = new ArrayList<>();
        GuiHelper.handleGuiTank(transform, tile.fluid[0], guiLeft + 88, guiTop + 20, 16, 46, 177, 177, 20, 51, mouseX, mouseY, TEXTURE, tooltip);
        GuiHelper.handleGuiTank(transform, tile.fluid[1], guiLeft + 124, guiTop + 20, 16, 46, 177, 177, 20, 51, mouseX, mouseY, TEXTURE, tooltip);

        if (!tooltip.isEmpty()) {
            net.minecraftforge.fml.client.gui.GuiUtils.drawHoveringText(transform, tooltip, mouseX, mouseY, width, height, -1, font);
        }
    }
}
