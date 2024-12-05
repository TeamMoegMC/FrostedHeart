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

import com.teammoeg.frostedheart.util.Lang;
import com.teammoeg.frostedheart.util.client.Point;
import com.teammoeg.frostedheart.util.client.TexturedUV;
import com.teammoeg.frostedheart.util.client.UV.Transition;

import blusunrize.immersiveengineering.client.gui.IEContainerScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;

public class SaunaScreen extends IEContainerScreen<SaunaContainer> {
    private static final ResourceLocation TEXTURE = Lang.makeTextureLocation("sauna_vent");

    private static final TexturedUV clock1 = new TexturedUV(TEXTURE,176, 0, 38, 38);
    private static final TexturedUV clock2 = new TexturedUV(TEXTURE,214, 0, 38, 38);
    private static final TexturedUV clock3 = new TexturedUV(TEXTURE,176, 38, 38, 38);
    private static final TexturedUV clock4 = new TexturedUV(TEXTURE,214, 38, 38, 38);

    private static final TexturedUV flame1 = new TexturedUV(TEXTURE,176, 76, 16, 29);
    private static final TexturedUV flame2 = new TexturedUV(TEXTURE,192, 76, 16, 29);
    private static final TexturedUV flame3 = new TexturedUV(TEXTURE,208, 76, 16, 29);
    private static final TexturedUV flame4 = new TexturedUV(TEXTURE,224, 76, 16, 29);
    private static final TexturedUV flame5 = new TexturedUV(TEXTURE,240, 76, 16, 29);

    private static final Point clockPos = new Point(41, 15);
    private static final Point flamePos = new Point(122, 19);

    public SaunaScreen(SaunaContainer inventorySlotsIn, Inventory inv, Component title) {
        super(inventorySlotsIn, inv, title, TEXTURE);
    }

    @Override
	public void render(GuiGraphics matrixStack, int x, int y, float partialTicks) {
        Minecraft mc = Minecraft.getInstance();

        SaunaTileEntity tile = this.menu.getBlock();

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
                if (effect.getEffect() == MobEffects.DIG_SPEED) {
                	flame2.blit(matrixStack, leftPos, topPos, flamePos, Transition.UP,effectFrac);
                } else if (effect.getEffect() == MobEffects.ABSORPTION) {
                	flame3.blit(matrixStack, leftPos, topPos, flamePos, Transition.UP,effectFrac);
                } else if (effect.getEffect() == MobEffects.MOVEMENT_SPEED) {
                	flame4.blit(matrixStack, leftPos, topPos, flamePos, Transition.UP,effectFrac);
                } else if (effect.getEffect() == MobEffects.JUMP) {
                	flame5.blit(matrixStack, leftPos, topPos, flamePos, Transition.UP,effectFrac);
                }
            } else {
                flame1.blitAt(matrixStack, leftPos, topPos, flamePos);
            }
        }
    }

}
