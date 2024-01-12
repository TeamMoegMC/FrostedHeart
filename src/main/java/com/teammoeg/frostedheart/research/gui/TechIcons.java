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

package com.teammoeg.frostedheart.research.gui;

import java.util.HashMap;
import java.util.Map;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.FHMain;

import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.ImageIcon;
import dev.ftb.mods.ftblibrary.icon.PartIcon;
import net.minecraft.util.ResourceLocation;

public class TechIcons {
    public static final ImageIcon ALL = (ImageIcon) Icon
            .getIcon(new ResourceLocation(FHMain.MODID, "textures/gui/escritoire.png"));
    public static final Icon Question = ALL.withUV(303, 203, 16, 16, 512, 512);
    public static final Icon ADD = ALL.withUV(303, 220, 16, 16, 512, 512);
    public static final Icon DOTS = ALL.withUV(303, 237, 16, 16, 512, 512);
    public static final Icon SELECTED = ALL.withUV(298, 228, 4, 4, 512, 512);
    public static final Icon HAND = ALL.withUV(320, 237, 16, 16, 512, 512);
    public static final Icon INF = ALL.withUV(303, 267, 16, 16, 512, 512);

    public static final Icon CHECKBOX = ALL.withUV(320, 227, 9, 9, 512, 512);
    public static final Icon CHECKBOX_CHECKED = ALL.withUV(329, 227, 9, 9, 512, 512);
    public static final Icon CHECKBOX_CROSS = ALL.withUV(338, 227, 9, 9, 512, 512);

    public static final Icon SHADOW = ALL.withUV(241, 240, 36, 9, 512, 512);
    public static final ImageIcon FIN = (ImageIcon) ALL.withUV(208, 203, 32, 32, 512, 512);
    public static final Icon LSLOT = ALL.withUV(241, 203, 36, 36, 512, 512);
    public static final Icon SLOT = ALL.withUV(278, 203, 24, 24, 512, 512);
    public static final Icon DIALOG = ALL.withUV(0, 267, 302, 170, 512, 512);
    public static final PartIcon BUTTON_FRAME = new PartIcon(ALL, 278, 228, 14, 14, 5).setTextureSize(512, 512);
    public static final PartIcon SLIDER_FRAME = new PartIcon(ALL, 344, 203, 8, 8, 3).setTextureSize(512, 512);
    public static final Icon BUTTON_BG = ALL.withUV(293, 228, 4, 4, 512, 512);
    public static final Icon BUTTON_BG_ON = ALL.withUV(293, 233, 4, 4, 512, 512);
    public static final LineIcon HLINE_LR = new LineIcon(ALL, 320, 203, 21, 3, 10, 10, 512, 512);
    public static final LineIcon HLINE_L = new LineIcon(ALL, 320, 207, 21, 3, 10, 10, 512, 512);
    public static final LineIcon HLINE = new LineIcon(ALL, 320, 211, 21, 1, 10, 10, 512, 512);
    public static final VLineIcon VLINE = new VLineIcon(ALL, 342, 203, 1, 21, 10, 10, 512, 512);
    public static final Icon TAB_HL = ALL.withUV(241, 250, 30, 7, 512, 512);
    public static final Icon Background = ALL.withUV(0, 0, 387, 203, 512, 512);
    public static final Color4I text = Color4I.rgb(0x474139);
    public static final Color4I text_red = Color4I.rgb(0xa92b0d);
    public static final Map<String, Icon> internals = new HashMap<>();

    static {
        BUTTON_FRAME.updateParts();
        SLIDER_FRAME.updateParts();
        // FIN.color=Color4I.rgba(255, 255, 255, 50);
        internals.put("question", Question);
        internals.put("plus", ADD);
        internals.put("dots", DOTS);
        internals.put("hand", HAND);
        internals.put("inf", INF);
    }

    public static void drawTexturedRect(MatrixStack matrixStack, int x, int y, int w, int h, boolean hl) {
        int vw = w / 4;
        int vwr = w % 4;
        int vh = h / 4;
        int vhr = h % 4;

        Icon bg = hl ? BUTTON_BG_ON : BUTTON_BG;
        int uvy = hl ? 233 : 228;
        for (int i = 0; i < vw; i++) {
            for (int j = 0; j < vh; j++) {
                bg.draw(matrixStack, x + i * 4, y + j * 4, 4, 4);
            }
        }

        if (vhr > 0) {
            Icon bghr = ALL.withUV(293, uvy, 4, vhr, 512, 512);
            int dy = h - vhr + y;
            for (int i = 0; i < vw; i++) {
                bghr.draw(matrixStack, x + i * 4, dy, 4, vhr);
            }
        }
        if (vwr > 0) {
            Icon bgwr = bg.withUV(293, uvy, vwr, 4, 512, 512);
            int dx = w - vwr + x;
            for (int i = 0; i < vh; i++) {
                bgwr.draw(matrixStack, dx, y + i * 4, vwr, 4);
            }
        }
        if (vwr > 0 && vhr > 0) {
            bg.withUV(293, uvy, vwr, vhr, 512, 512).draw(matrixStack, x + w - vwr, y + h - vhr, vwr, vhr);
        }

        BUTTON_FRAME.draw(matrixStack, x, y, w, h);

    }

    public TechIcons() {
    }
}
