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

package com.teammoeg.frostedheart.content.research.gui;

import java.util.List;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.teammoeg.frostedheart.content.research.research.Research;
import com.teammoeg.frostedheart.util.TranslateUtils;

import net.minecraft.client.gui.toasts.IToast;
import net.minecraft.client.gui.toasts.ToastGui;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.math.MathHelper;

public class InspireToast implements IToast {
	int level;
	
    public InspireToast(int level) {
        this.level = level;
    }

    public IToast.Visibility func_230444_a_(MatrixStack matrixStack, ToastGui gui, long time) {
        gui.getMinecraft().getTextureManager().bindTexture(TEXTURE_TOASTS);
        RenderSystem.color3f(1.0F, 1.0F, 1.0F);
        gui.blit(matrixStack, 0, 0, 0, 0, this.func_230445_a_(), this.func_238540_d_());

        List<IReorderingProcessor> list = gui.getMinecraft().fontRenderer.trimStringToWidth(TranslateUtils.translateMessage("inspire.toast.gain_point"), 125);
        int i = 16776960;
        if (list.size() == 1) {
            gui.getMinecraft().fontRenderer.drawText(matrixStack, TranslateUtils.translateMessage("inspire.toast.level",level), 30.0F, 7.0F, i | -16777216);
            gui.getMinecraft().fontRenderer.func_238422_b_(matrixStack, list.get(0), 30.0F, 18.0F, -1);
        } else {
            if (time < 1500L) {
                int k = MathHelper.floor(MathHelper.clamp((1500L - time) / 300.0F, 0.0F, 1.0F) * 255.0F) << 24 | 67108864;
                gui.getMinecraft().fontRenderer.drawText(matrixStack, TranslateUtils.translateMessage("inspire.toast.level",level), 30.0F, 11.0F, i | k);
            } else {
                int i1 = MathHelper.floor(MathHelper.clamp((time - 1500L) / 300.0F, 0.0F, 1.0F) * 252.0F) << 24 | 67108864;
                int l = this.func_238540_d_() / 2 - list.size() * 9 / 2;

                for (IReorderingProcessor ireorderingprocessor : list) {
                    gui.getMinecraft().fontRenderer.func_238422_b_(matrixStack, ireorderingprocessor, 30.0F, l, 16777215 | i1);
                    l += 9;
                }
            }
        }
        return time >= 5000L ? IToast.Visibility.HIDE : IToast.Visibility.SHOW;
    }
}