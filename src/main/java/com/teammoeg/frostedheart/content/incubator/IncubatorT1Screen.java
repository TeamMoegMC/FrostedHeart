/*
 * Copyright (c) 2021 TeamMoeg
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
 */

package com.teammoeg.frostedheart.content.incubator;

import blusunrize.immersiveengineering.client.ClientUtils;
import blusunrize.immersiveengineering.client.gui.IEContainerScreen;
import blusunrize.immersiveengineering.client.utils.GuiHelper;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

import java.util.ArrayList;
import java.util.List;

public class IncubatorT1Screen extends IEContainerScreen<IncubatorT1Container> {
    private static final ResourceLocation TEXTURE = GuiUtils.makeTextureLocation("incubator");
    private IncubatorTileEntity tile;

    public IncubatorT1Screen(IncubatorT1Container container, PlayerInventory inv, ITextComponent title) {
        super(container, inv, title);
        this.tile = container.tile;
    }


    @Override
    public void render(MatrixStack transform, int mouseX, int mouseY, float partial) {
        super.render(transform, mouseX, mouseY, partial);
        List<ITextComponent> tooltip = new ArrayList<>();
        GuiHelper.handleGuiTank(transform, tile.fluid[0], guiLeft + 61, guiTop + 20, 16, 46, 177, 177, 20, 51, mouseX, mouseY, TEXTURE, tooltip);
        GuiHelper.handleGuiTank(transform, tile.fluid[1], guiLeft + 117, guiTop + 20, 16, 46, 177, 177, 20, 51, mouseX, mouseY, TEXTURE, tooltip);

        if (!tooltip.isEmpty()) {
            net.minecraftforge.fml.client.gui.GuiUtils.drawHoveringText(transform, tooltip, mouseX, mouseY, width, height, -1, font);
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(MatrixStack transform, float partial, int x, int y) {
        ClientUtils.bindTexture(TEXTURE);
        this.blit(transform, guiLeft, guiTop, 0, 0, xSize, ySize);
        GuiHelper.handleGuiTank(transform, tile.fluid[0], guiLeft + 61, guiTop + 20, 16, 46, 177, 177, 20, 51, x, y, TEXTURE, null);
        GuiHelper.handleGuiTank(transform, tile.fluid[1], guiLeft + 117, guiTop + 20, 16, 46, 177, 177, 20, 51, x,y, TEXTURE,null);
        // recipe progress icon
        if (tile.processMax > 0 && tile.process > 0) {
            int w = (int) (32 * (tile.process / (float) tile.processMax));
            this.blit(transform, guiLeft + 80, guiTop + 28, 176,0,32-w,29);
        }
        if(tile.fuel>0&&tile.fuelMax>0) {
        	int h = (int) (14 * (tile.fuel / (float) tile.fuelMax));
        	this.blit(transform, guiLeft + 35, guiTop + 35+(14-h), 198, 64+(14-h), 14,h);
        }
    }

    @Override
    public boolean isMouseIn(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= guiLeft + x && mouseY >= guiTop + y
                && mouseX < guiLeft + x + w && mouseY < guiTop + y + h;
    }
}
