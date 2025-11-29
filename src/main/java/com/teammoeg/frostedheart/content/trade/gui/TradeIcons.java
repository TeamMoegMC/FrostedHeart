/*
 * Copyright (c) 2024 TeamMoeg
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

import com.teammoeg.frostedheart.FHMain;

import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.ImageIcon;
import net.minecraft.resources.ResourceLocation;

public class TradeIcons {
    public static final ImageIcon ALL = (ImageIcon) Icon
            .getIcon(new ResourceLocation(FHMain.MODID, "textures/gui/trading.png"));
    public static final Icon MAIN = ALL.withUV(0, 0, 244, 227, 256, 256);

    public static final Icon EXP = ALL.withUV(0, 227, 54, 5, 256, 256);
    public static final Icon REL = ALL.withUV(0, 232, 54, 5, 256, 256);
    public static final Icon PTR = ALL.withUV(54, 227, 3, 7, 256, 256);

    public static final Icon DEALN = ALL.withUV(57, 227, 20, 14, 256, 256);
    public static final Icon DEALO = ALL.withUV(77, 227, 20, 14, 256, 256);
    public static final Icon DEALD = ALL.withUV(97, 227, 20, 14, 256, 256);
    public static final Icon DEALYEL = ALL.withUV(117, 227, 20, 14, 256, 256);
    public static final Icon DEALYELO = ALL.withUV(117, 241, 20, 14, 256, 256);

    public static final Icon DEALGRN = ALL.withUV(137, 227, 20, 14, 256, 256);
    public static final Icon DEALGRNO = ALL.withUV(137, 241, 20, 14, 256, 256);

    public static final Icon BARGAINN = ALL.withUV(57, 241, 20, 14, 256, 256);
    public static final Icon BARGAINO = ALL.withUV(77, 241, 20, 14, 256, 256);
    public static final Icon BARGAIND = ALL.withUV(97, 241, 20, 14, 256, 256);

    public static final Icon SALEABLE = ALL.withUV(0, 237, 7, 6, 256, 256);
    public static final Icon NORESTOCK = ALL.withUV(7, 237, 7, 6, 256, 256);
    public static final Icon NOBUY = ALL.withUV(14, 237, 7, 6, 256, 256);
    public static final Icon RESTOCKS = ALL.withUV(21, 237, 7, 6, 256, 256);
    public static final Icon FULL = ALL.withUV(28, 237, 7, 6, 256, 256);
    public static final Icon STOCKOUT = ALL.withUV(35, 237, 7, 6, 256, 256);

    public static final ImageIcon IMAGES = (ImageIcon) Icon
            .getIcon(new ResourceLocation(FHMain.MODID, "textures/gui/trading_accesories.png"));

    public static final Icon POFFER_EMP = IMAGES.withUV(153, 90, 76, 60, 256, 256);
    public static final Icon VOFFER_EMP = IMAGES.withUV(151, 150, 74, 60, 256, 256);
    public static final Icon POFFER_OVL = IMAGES.withUV(77, 90, 76, 60, 256, 256);
    public static final Icon VOFFER_OVL = IMAGES.withUV(77, 150, 74, 60, 256, 256);

    public static final Icon Balance = IMAGES.withUV(0, 0, 56, 45, 256, 256);
    public static final Icon Balancep1 = IMAGES.withUV(56, 0, 56, 45, 256, 256);
    public static final Icon Balancep2 = IMAGES.withUV(112, 0, 56, 45, 256, 256);
    public static final Icon Balancep3 = IMAGES.withUV(168, 0, 56, 45, 256, 256);
    public static final Icon Balancev1 = IMAGES.withUV(56, 45, 56, 45, 256, 256);
    public static final Icon Balancev2 = IMAGES.withUV(112, 45, 56, 45, 256, 256);
    public static final Icon Balancev3 = IMAGES.withUV(168, 45, 56, 45, 256, 256);

    public static final Icon PTABSELL = IMAGES.withUV(23, 90, 54, 31, 256, 256);
    public static final Icon PTABBUY = IMAGES.withUV(23, 121, 54, 31, 256, 256);

    public static final Icon OVLSELL = IMAGES.withUV(15, 152, 48, 48, 256, 256);
    public static final Icon OVLBUY = IMAGES.withUV(15, 200, 48, 48, 256, 256);

    public static final Icon SCROLLBTN = IMAGES.withUV(15, 90, 8, 16, 256, 256);
    public static final Icon SCROLLFRAME = IMAGES.withUV(0, 90, 15, 160, 256, 256);
    public static final Icon[] icons = new Icon[]{Balancev3, Balancev2, Balancev1, Balance, Balancep1, Balancep2, Balancep3};

    public TradeIcons() {
    }
}
