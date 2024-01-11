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

package com.teammoeg.frostedheart.content.generator.t1;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.FHPacketHandler;
import com.teammoeg.frostedheart.client.util.GuiUtils;

import blusunrize.immersiveengineering.client.ClientUtils;
import blusunrize.immersiveengineering.client.gui.IEContainerScreen;
import blusunrize.immersiveengineering.client.gui.elements.GuiButtonBoolean;
import blusunrize.immersiveengineering.common.network.MessageTileSync;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

public class T1GeneratorScreen extends IEContainerScreen<T1GeneratorContainer> {
    private static final ResourceLocation TEXTURE = GuiUtils.makeTextureLocation("generator_t1");
    private T1GeneratorTileEntity tile;

    public T1GeneratorScreen(T1GeneratorContainer container, PlayerInventory inv, ITextComponent title) {
        super(container, inv, title);
        this.tile = container.tile;
        this.clearIntArray(tile.guiData);
        
    }

    @Override
    public void init() {
        super.init();
        this.buttons.clear();
        this.addButton(new GuiButtonBoolean(guiLeft + 56, guiTop + 35, 19, 10, "", tile.isWorking(), TEXTURE, 0, 245, 0,
                btn -> {
                    CompoundNBT tag = new CompoundNBT();
                    tile.setWorking(!btn.getState());
                    tag.putBoolean("isWorking", tile.isWorking());
                    FHPacketHandler.sendToServer(new MessageTileSync(tile.master(), tag));
                    fullInit();
                }));
        this.addButton(new GuiButtonBoolean(guiLeft + 101, guiTop + 35, 19, 10, "", tile.isOverdrive(), TEXTURE, 0, 245, 0,
                btn -> {
                    CompoundNBT tag = new CompoundNBT();
                    tile.setOverdrive(!btn.getState());
                    tag.putBoolean("isOverdrive", tile.isOverdrive());
                    FHPacketHandler.sendToServer(new MessageTileSync(tile.master(), tag));
                    fullInit();
                }));
    }

    @Override
    public void render(MatrixStack transform, int mouseX, int mouseY, float partial) {
        super.render(transform, mouseX, mouseY, partial);
        List<ITextComponent> tooltip = new ArrayList<>();

        if (isMouseIn(mouseX, mouseY, 57, 36, 19, 10)) {
            if (tile.isWorking()) {
                tooltip.add(GuiUtils.translateGui("generator.mode.off"));
            } else {
                tooltip.add(GuiUtils.translateGui("generator.mode.on"));
            }
        }

        if (isMouseIn(mouseX, mouseY, 102, 36, 19, 10)) {
            if (tile.isOverdrive()) {
                tooltip.add(GuiUtils.translateGui("generator.overdrive.off"));
            } else {
                tooltip.add(GuiUtils.translateGui("generator.overdrive.on"));
            }
        }

        if (isMouseIn(mouseX, mouseY, 12, 13, 2, 54)) {
            //tooltip.add(GuiUtils.translateGui("generator.temperature.level").appendString(GuiUtils.toTemperatureDeltaIntString(tile.getActualTemp())));
            tooltip.add(GuiUtils.translateGui("generator.temperature.level").appendString(GuiUtils.toTemperatureDeltaIntString(tile.getActualTemp())));
        }

        if (isMouseIn(mouseX, mouseY, 161, 13, 2, 54)) {
            tooltip.add(GuiUtils.translateGui("generator.range.level").appendString(Integer.toString(tile.getActualRange())));
        }

        if (!tooltip.isEmpty()) {
            net.minecraftforge.fml.client.gui.GuiUtils.drawHoveringText(transform, tooltip, mouseX, mouseY, width, height, -1, font);
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(MatrixStack transform, float partial, int x, int y) {
        ClientUtils.bindTexture(TEXTURE);
        this.blit(transform, guiLeft, guiTop, 0, 0, xSize, ySize);

        // recipe progress icon
        if (tile.processMax > 0 && tile.process > 0) {
            int h = (int) (12 * (tile.process / (float) tile.processMax));
            this.blit(transform, guiLeft + 84, guiTop + 47 - h, 179, 1 + 12 - h, 9, h);
        }

        // work button
        if (tile.isWorking()) {
            this.blit(transform, guiLeft + 56, guiTop + 35, 232, 1, 19, 10);
        }

        // overdrive button
        if (tile.isOverdrive()) {
            this.blit(transform, guiLeft + 101, guiTop + 35, 232, 12, 19, 10);
        }

        float tempLevel = tile.getActualTemp() / 10F;
        float rangeLevel = tile.getRangeLevel();
        int offset, bar;

        // temperature bar (182, 30)
        offset = (int) ((4 - tempLevel) * 14);
        //int bar = (int) ((tempLevel - 1) * 14);
        bar = 56 - offset;
        this.blit(transform, guiLeft + 12, guiTop + 13 + offset, 181, 30, 2, bar);

        // range bar
        offset = (int) ((4 - rangeLevel) * 14);
        bar = (int) ((rangeLevel - 1) * 14);
        this.blit(transform, guiLeft + 161, guiTop + 13 + offset, 181, 30, 2, 12 + bar);
    }

    @Override
    public boolean isMouseIn(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= guiLeft + x && mouseY >= guiTop + y
                && mouseX < guiLeft + x + w && mouseY < guiTop + y + h;
    }
}
