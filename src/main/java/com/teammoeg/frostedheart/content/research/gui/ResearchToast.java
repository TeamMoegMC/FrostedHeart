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

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.teammoeg.frostedheart.content.research.research.Research;
import com.teammoeg.frostedheart.util.TranslateUtils;

import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

public class ResearchToast implements Toast {
    private final Research r;
    private boolean hasPlayedSound;

    public ResearchToast(Research r) {
        this.r = r;
    }

    public Toast.Visibility render(PoseStack matrixStack, ToastComponent gui, long time) {
        gui.getMinecraft().getTextureManager().bind(TEXTURE);
        RenderSystem.color3f(1.0F, 1.0F, 1.0F);
        gui.blit(matrixStack, 0, 0, 0, 0, this.width(), this.height());

        if (r != null) {
            List<FormattedCharSequence> list = gui.getMinecraft().font.split(r.getName(), 125);
            int i = 16776960;
            if (list.size() == 1) {
                gui.getMinecraft().font.draw(matrixStack, TranslateUtils.translateMessage("toast.research_complete"), 30.0F, 7.0F, i | -16777216);
                gui.getMinecraft().font.draw(matrixStack, list.get(0), 30.0F, 18.0F, -1);
            } else {
                int j = 1500;
                float f = 300.0F;
                if (time < 1500L) {
                    int k = Mth.floor(Mth.clamp((1500L - time) / 300.0F, 0.0F, 1.0F) * 255.0F) << 24 | 67108864;
                    gui.getMinecraft().font.draw(matrixStack, TranslateUtils.translateMessage("toast.research_complete"), 30.0F, 11.0F, i | k);
                } else {
                    int i1 = Mth.floor(Mth.clamp((time - 1500L) / 300.0F, 0.0F, 1.0F) * 252.0F) << 24 | 67108864;
                    int l = this.height() / 2 - list.size() * 9 / 2;

                    for (FormattedCharSequence ireorderingprocessor : list) {
                        gui.getMinecraft().font.draw(matrixStack, ireorderingprocessor, 30.0F, l, 16777215 | i1);
                        l += 9;
                    }
                }
            }

         /*if (!this.hasPlayedSound && time > 0L) {
            this.hasPlayedSound = true;
               gui.getMinecraft().getSoundHandler().play(SimpleSound.master(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.0F));
         }*/
            r.getIcon().draw(matrixStack, 8, 8, 16, 16);
            return time >= 5000L ? Toast.Visibility.HIDE : Toast.Visibility.SHOW;
        }
        return Toast.Visibility.HIDE;
    }
}