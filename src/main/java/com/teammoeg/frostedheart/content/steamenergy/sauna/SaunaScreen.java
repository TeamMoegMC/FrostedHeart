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

import com.mojang.blaze3d.vertex.PoseStack;
import com.teammoeg.frostedheart.util.TranslateUtils;
import com.teammoeg.frostedheart.util.client.FHGuiHelper;
import com.teammoeg.frostedheart.util.client.Point;
import com.teammoeg.frostedheart.util.client.UV;

import blusunrize.immersiveengineering.client.gui.IEContainerScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;

public class SaunaScreen extends IEContainerScreen<SaunaContainer> {
    private static final ResourceLocation TEXTURE = TranslateUtils.makeTextureLocation("sauna_vent");

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

    public SaunaScreen(SaunaContainer inventorySlotsIn, Inventory inv, Component title) {
        super(inventorySlotsIn, inv, title);
    }

    @Override
    protected void renderBg(PoseStack matrixStack, float partialTicks, int x, int y) {
        Minecraft mc = Minecraft.getInstance();
        FHGuiHelper.bindTexture(TEXTURE);
        this.blit(matrixStack, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        SaunaTileEntity tile = this.menu.tile;

        // draw the steam clock
        float powerFraction = tile.getPowerFraction();

        if (powerFraction < 0.25) {
            clock1.blitAt(matrixStack, leftPos, topPos, clockPos);
        } else if (powerFraction < 0.5) {
            clock2.blitAt(matrixStack, leftPos, topPos, clockPos);
        } else if (powerFraction < 0.75) {
            clock3.blitAt(matrixStack, leftPos, topPos, clockPos);
        } else {
            clock4.blitAt(matrixStack, leftPos, topPos, clockPos);
        }

        // draw flame if the sauna is on
        if (tile.isWorking()) {
            // use effect to determine which flame to draw
            MobEffectInstance effect = tile.getEffectInstance();
            if (effect != null) {
                float effectFrac = tile.getEffectTimeFraction();
                int height = (int) (flame2.getH() * effectFrac);
                int offset = flame2.getH() - height;
                if (effect.getEffect() == MobEffects.DIG_SPEED) {
                    mc.gui.blit(matrixStack, leftPos + flamePos.getX(), topPos + flamePos.getY() + offset,
                            flame2.getX(), flame2.getY() + offset, flame2.getW(), height);
                } else if (effect.getEffect() == MobEffects.ABSORPTION) {
                    mc.gui.blit(matrixStack, leftPos + flamePos.getX(), topPos + flamePos.getY() + offset,
                            flame3.getX(), flame3.getY() + offset, flame3.getW(), height);
                } else if (effect.getEffect() == MobEffects.MOVEMENT_SPEED) {
                    mc.gui.blit(matrixStack, leftPos + flamePos.getX(), topPos + flamePos.getY() + offset,
                            flame4.getX(), flame4.getY() + offset, flame4.getW(), height);
                } else if (effect.getEffect() == MobEffects.JUMP) {
                    mc.gui.blit(matrixStack, leftPos + flamePos.getX(), topPos + flamePos.getY() + offset,
                            flame5.getX(), flame5.getY() + offset, flame5.getW(), height);
                }
            } else {
                flame1.blitAt(matrixStack, leftPos, topPos, flamePos);
            }
        }
    }

    @Override
    protected void renderLabels(PoseStack matrixStack, int x, int y) {
        super.renderLabels(matrixStack, x, y);
    }
}
