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

import com.teammoeg.frostedheart.util.client.Lang;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.List;

public class InspireToast implements Toast {
    int level;

    public InspireToast(int level) {
        this.level = level;
    }

    public Toast.Visibility render(GuiGraphics matrixStack, ToastComponent gui, long time) {
        matrixStack.blit(TEXTURE, 0, 0, 0, 0, this.width(), this.height());

        List<FormattedCharSequence> list = gui.getMinecraft().font.split(Lang.translateMessage("inspire.toast.gain_point"), 125);
        int i = 16776960;
        if (list.size() == 1) {
            matrixStack.drawString(gui.getMinecraft().font, Lang.translateMessage("inspire.toast.level", level), 30, 7, i | -16777216);
            matrixStack.drawString(gui.getMinecraft().font, list.get(0), 30, 18, -1);
        } else {
            if (time < 1500L) {
                int k = Mth.floor(Mth.clamp((1500L - time) / 300.0F, 0.0F, 1.0F) * 255.0F) << 24 | 67108864;
                matrixStack.drawString(gui.getMinecraft().font, Lang.translateMessage("inspire.toast.level", level), 30, 11, i | k);
            } else {
                int i1 = Mth.floor(Mth.clamp((time - 1500L) / 300.0F, 0.0F, 1.0F) * 252.0F) << 24 | 67108864;
                int l = this.height() / 2 - list.size() * 9 / 2;

                for (FormattedCharSequence ireorderingprocessor : list) {
                    matrixStack.drawString(gui.getMinecraft().font, ireorderingprocessor, 30, l, 16777215 | i1);
                    l += 9;
                }
            }
        }
        return time >= 5000L ? Toast.Visibility.HIDE : Toast.Visibility.SHOW;
    }
}