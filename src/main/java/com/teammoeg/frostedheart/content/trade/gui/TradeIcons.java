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

package com.teammoeg.frostedheart.content.trade.gui;

import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.chorda.client.icon.CIcons.CTextureIcon;
import com.teammoeg.frostedheart.FHMain;

import net.minecraft.resources.ResourceLocation;

public class TradeIcons {
    public static final CTextureIcon ALL = CIcons.getIcon(new ResourceLocation(FHMain.MODID, "textures/gui/trading.png"));
    public static final CTextureIcon MAIN = ALL.withUV(0, 0, 244, 227, 256, 256);

    public static final CTextureIcon EXP = ALL.withUV(0, 227, 54, 5, 256, 256);
    public static final CTextureIcon REL = ALL.withUV(0, 232, 54, 5, 256, 256);
    public static final CTextureIcon PTR = ALL.withUV(54, 227, 3, 7, 256, 256);

    public static final CTextureIcon DEALN = ALL.withUV(57, 227, 20, 14, 256, 256);
    public static final CTextureIcon DEALO = ALL.withUV(77, 227, 20, 14, 256, 256);
    public static final CTextureIcon DEALD = ALL.withUV(97, 227, 20, 14, 256, 256);
    public static final CTextureIcon DEALYEL = ALL.withUV(117, 227, 20, 14, 256, 256);
    public static final CTextureIcon DEALYELO = ALL.withUV(117, 241, 20, 14, 256, 256);

    public static final CTextureIcon DEALGRN = ALL.withUV(137, 227, 20, 14, 256, 256);
    public static final CTextureIcon DEALGRNO = ALL.withUV(137, 241, 20, 14, 256, 256);

    public static final CTextureIcon BARGAINN = ALL.withUV(57, 241, 20, 14, 256, 256);
    public static final CTextureIcon BARGAINO = ALL.withUV(77, 241, 20, 14, 256, 256);
    public static final CTextureIcon BARGAIND = ALL.withUV(97, 241, 20, 14, 256, 256);

    public static final CTextureIcon SALEABLE = ALL.withUV(0, 237, 7, 6, 256, 256);
    public static final CTextureIcon NORESTOCK = ALL.withUV(7, 237, 7, 6, 256, 256);
    public static final CTextureIcon NOBUY = ALL.withUV(14, 237, 7, 6, 256, 256);
    public static final CTextureIcon RESTOCKS = ALL.withUV(21, 237, 7, 6, 256, 256);
    public static final CTextureIcon FULL = ALL.withUV(28, 237, 7, 6, 256, 256);
    public static final CTextureIcon STOCKOUT = ALL.withUV(35, 237, 7, 6, 256, 256);

    public static final CTextureIcon IMAGES = CIcons.getIcon(new ResourceLocation(FHMain.MODID, "textures/gui/trading_accesories.png"));

    public static final CTextureIcon POFFER_EMP = IMAGES.withUV(153, 90, 76, 60, 256, 256);
    public static final CTextureIcon VOFFER_EMP = IMAGES.withUV(151, 150, 74, 60, 256, 256);
    public static final CTextureIcon POFFER_OVL = IMAGES.withUV(77, 90, 76, 60, 256, 256);
    public static final CTextureIcon VOFFER_OVL = IMAGES.withUV(77, 150, 74, 60, 256, 256);

    public static final CTextureIcon Balance = IMAGES.withUV(0, 0, 56, 45, 256, 256);
    public static final CTextureIcon Balancep1 = IMAGES.withUV(56, 0, 56, 45, 256, 256);
    public static final CTextureIcon Balancep2 = IMAGES.withUV(112, 0, 56, 45, 256, 256);
    public static final CTextureIcon Balancep3 = IMAGES.withUV(168, 0, 56, 45, 256, 256);
    public static final CTextureIcon Balancev1 = IMAGES.withUV(56, 45, 56, 45, 256, 256);
    public static final CTextureIcon Balancev2 = IMAGES.withUV(112, 45, 56, 45, 256, 256);
    public static final CTextureIcon Balancev3 = IMAGES.withUV(168, 45, 56, 45, 256, 256);

    public static final CTextureIcon PTABSELL = IMAGES.withUV(23, 90, 54, 31, 256, 256);
    public static final CTextureIcon PTABBUY = IMAGES.withUV(23, 121, 54, 31, 256, 256);

    public static final CTextureIcon OVLSELL = IMAGES.withUV(15, 152, 48, 48, 256, 256);
    public static final CTextureIcon OVLBUY = IMAGES.withUV(15, 200, 48, 48, 256, 256);

    public static final CTextureIcon SCROLLBTN = IMAGES.withUV(15, 90, 8, 16, 256, 256);
    public static final CTextureIcon SCROLLFRAME = IMAGES.withUV(0, 90, 15, 160, 256, 256);
    public static final CTextureIcon[] icons = new CTextureIcon[]{Balancev3, Balancev2, Balancev1, Balance, Balancep1, Balancep2, Balancep3};

    public TradeIcons() {
    }
}
