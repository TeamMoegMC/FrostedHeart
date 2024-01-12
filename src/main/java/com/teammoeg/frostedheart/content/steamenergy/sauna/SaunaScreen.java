/*
 * Copyright (c) 2022-2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.steamenergy.sauna;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import com.teammoeg.frostedheart.client.util.Point;
import com.teammoeg.frostedheart.client.util.UV;

import blusunrize.immersiveengineering.client.ClientUtils;
import blusunrize.immersiveengineering.client.gui.IEContainerScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

public class SaunaScreen extends IEContainerScreen<SaunaContainer> {
    private static final ResourceLocation TEXTURE = GuiUtils.makeTextureLocation("sauna_vent");

    private static final UV clock1 = new UV(176, 0, 38, 38);
    private static final UV clock2 = new UV(214, 0, 38, 38);
    private static final UV clock3 = new UV(176, 38, 38, 38);
    private static final UV clock4 = new UV(214, 38, 38, 38);

    private static final UV flame1 = new UV(176, 76, 16, 29);
    private static final UV flame2 = new UV(192, 76, 16, 29);
    private static final UV flame3 = new UV(208, 76, 16, 29);
    private static final UV flame4 = new UV(224, 76, 16, 29);
    private static final UV flame5 = new UV(240, 76, 16, 29);

    private static final Point clockPos = new Point(41, 15);
    private static final Point flamePos = new Point(122, 19);

    public SaunaScreen(SaunaContainer inventorySlotsIn, PlayerInventory inv, ITextComponent title) {
        super(inventorySlotsIn, inv, title);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(MatrixStack matrixStack, float partialTicks, int x, int y) {
        Minecraft mc = Minecraft.getInstance();
        ClientUtils.bindTexture(TEXTURE);
        this.blit(matrixStack, guiLeft, guiTop, 0, 0, xSize, ySize);

        SaunaTileEntity tile = this.container.tile;

        // draw the steam clock
        float powerFraction = tile.getPowerFraction();

        if (powerFraction < 0.25) {
            clock1.blit(mc.ingameGUI, matrixStack, guiLeft, guiTop, clockPos);
        } else if (powerFraction < 0.5) {
            clock2.blit(mc.ingameGUI, matrixStack, guiLeft, guiTop, clockPos);
        } else if (powerFraction < 0.75) {
            clock3.blit(mc.ingameGUI, matrixStack, guiLeft, guiTop, clockPos);
        } else {
            clock4.blit(mc.ingameGUI, matrixStack, guiLeft, guiTop, clockPos);
        }

        // draw flame if the sauna is on
        if (tile.isWorking()) {
            // use effect to determine which flame to draw
            EffectInstance effect = tile.getEffectInstance();
            if (effect != null) {
                float effectFrac = tile.getEffectTimeFraction();
                int height = (int) (flame2.getH() * effectFrac);
                int offset = flame2.getH() - height;
                if (effect.getPotion() == Effects.HASTE) {
                    mc.ingameGUI.blit(matrixStack, guiLeft + flamePos.getX(), guiTop + flamePos.getY() + offset,
                            flame2.getX(), flame2.getY() + offset, flame2.getW(), height);
                } else if (effect.getPotion() == Effects.ABSORPTION) {
                    mc.ingameGUI.blit(matrixStack, guiLeft + flamePos.getX(), guiTop + flamePos.getY() + offset,
                            flame3.getX(), flame3.getY() + offset, flame3.getW(), height);
                } else if (effect.getPotion() == Effects.SPEED) {
                    mc.ingameGUI.blit(matrixStack, guiLeft + flamePos.getX(), guiTop + flamePos.getY() + offset,
                            flame4.getX(), flame4.getY() + offset, flame4.getW(), height);
                } else if (effect.getPotion() == Effects.JUMP_BOOST) {
                    mc.ingameGUI.blit(matrixStack, guiLeft + flamePos.getX(), guiTop + flamePos.getY() + offset,
                            flame5.getX(), flame5.getY() + offset, flame5.getW(), height);
                }
            } else {
                flame1.blit(mc.ingameGUI, matrixStack, guiLeft, guiTop, flamePos);
            }
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(MatrixStack matrixStack, int x, int y) {
        super.drawGuiContainerForegroundLayer(matrixStack, x, y);
    }
}
