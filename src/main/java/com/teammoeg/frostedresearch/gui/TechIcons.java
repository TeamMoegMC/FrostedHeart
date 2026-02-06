/*
 * Copyright (c) 2026 TeamMoeg
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

package com.teammoeg.frostedresearch.gui;

import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.chorda.client.icon.CIcons.CIcon;
import com.teammoeg.chorda.client.icon.CIcons.CTextureIcon;
import com.teammoeg.chorda.util.IterateUtils;
import com.teammoeg.frostedresearch.FRMain;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class TechIcons {
    public static final CTextureIcon ALL =  CIcons
            .getIcon(new ResourceLocation(FRMain.MODID, "textures/gui/escritoire.png"));
    public static final CTextureIcon Question = ALL.withUV(303, 203, 16, 16, 512, 512);
    public static final CTextureIcon ADD = ALL.withUV(303, 220, 16, 16, 512, 512);
    public static final CTextureIcon DOTS = ALL.withUV(303, 237, 16, 16, 512, 512);
    public static final CTextureIcon SELECTED = ALL.withUV(298, 228, 4, 4, 512, 512);
    public static final CTextureIcon HAND = ALL.withUV(320, 237, 16, 16, 512, 512);
    public static final CTextureIcon INF = ALL.withUV(303, 267, 16, 16, 512, 512);

    public static final CTextureIcon CHECKBOX = ALL.withUV(320, 227, 9, 9, 512, 512);
    public static final CTextureIcon CHECKBOX_CHECKED = ALL.withUV(329, 227, 9, 9, 512, 512);
    public static final CTextureIcon CHECKBOX_CROSS = ALL.withUV(338, 227, 9, 9, 512, 512);

    public static final CTextureIcon SHADOW = ALL.withUV(241, 240, 36, 9, 512, 512);
    public static final CTextureIcon FIN =  ALL.withUV(208, 203, 32, 32, 512, 512);
    public static final CTextureIcon LSLOT = ALL.withUV(241, 203, 36, 36, 512, 512);
    public static final CTextureIcon SLOT = ALL.withUV(278, 203, 24, 24, 512, 512);
    public static final CTextureIcon DIALOG = ALL.withUV(0, 267, 302, 170, 512, 512);
    public static final CTextureIcon BUTTON_FRAME = ALL.withUV(278, 228, 14, 14,512,512).toNineSlice(5);
    public static final CTextureIcon SLIDER_FRAME = ALL.withUV(344, 203, 8, 8,512,512).toNineSlice(3);
    public static final CTextureIcon BUTTON_BG = ALL.withUV(293, 228, 4, 4, 512, 512);
    public static final CTextureIcon BUTTON_BG_ON = ALL.withUV(293, 233, 4, 4, 512, 512);
    public static final LineIcon HLINE_LR = new LineIcon(ALL, 320, 203, 21, 3, 10, 10, 512, 512);
    public static final LineIcon HLINE_L = new LineIcon(ALL, 320, 207, 21, 3, 10, 10, 512, 512);
    public static final LineIcon HLINE = new LineIcon(ALL, 320, 211, 21, 1, 10, 10, 512, 512);
    public static final VLineIcon VLINE = new VLineIcon(ALL, 342, 203, 1, 21, 10, 10, 512, 512);
    public static final CTextureIcon TAB_HL = ALL.withUV(241, 250, 30, 7, 512, 512);
    public static final CTextureIcon Background = ALL.withUV(0, 0, 387, 203, 512, 512);
    public static final int text = 0xFF474139;
    public static final int text_red = 0xFFa92b0d;
    

    static {
        // FIN.color=Color4I.rgba(255, 255, 255, 50);
    	CIcons.internals.put("question", Question);
        CIcons.internals.put("plus", ADD);
        CIcons.internals.put("dots", DOTS);
        CIcons.internals.put("hand", HAND);
        CIcons.internals.put("inf", INF);
    }

    public TechIcons() {
    }
    private static final CIcon[] BTN_VH=new CIcon[32];
    static{
    	for(boolean on:IterateUtils.boolIterable) {
    		for(int i=0;i<4;i++) {
    			for(int j=0;j<4;j++) {
    		        
    		        int uvy = on ? 233 : 228;
    		        BTN_VH[(on?16:0)+i*4+j]=ALL.withUV(293, uvy, i+1, j+1, 512, 512);
        		}
    		}
    	}
    	
    }
    private static CIcon getOf(int vwr,int vhr, boolean hl) {
    	return BTN_VH[(hl?16:0)+(vwr-1)*4+(vhr-1)];
    }
    public static void drawTexturedRect(GuiGraphics matrixStack, int x, int y, int w, int h, boolean hl) {
        int vw = w / 4;
        int vwr = w % 4;
        int vh = h / 4;
        int vhr = h % 4;

        CTextureIcon bg = hl ? BUTTON_BG_ON : BUTTON_BG;
        for (int i = 0; i < vw; i++) {
            for (int j = 0; j < vh; j++) {
                bg.draw(matrixStack, x + i * 4, y + j * 4, 4, 4);
            }
        }

        if (vhr > 0) {
            CIcon bghr = getOf(4,vhr,hl);
            int dy = h - vhr + y;
            for (int i = 0; i < vw; i++) {
                bghr.draw(matrixStack, x + i * 4, dy, 4, vhr);
            }
        }
        if (vwr > 0) {
            CIcon bgwr = getOf(vwr,4,hl);
            int dx = w - vwr + x;
            for (int i = 0; i < vh; i++) {
                bgwr.draw(matrixStack, dx, y + i * 4, vwr, 4);
            }
        }
        if (vwr > 0 && vhr > 0) {
        	getOf(vwr,vhr,hl).draw(matrixStack, x + w - vwr, y + h - vhr, vwr, vhr);
        }
        if(w>1&&h>1)
        	BUTTON_FRAME.draw(matrixStack, x, y, w, h);
        else
        	matrixStack.fill(x, y, w, h, text);
    }
}
