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

package com.teammoeg.frostedresearch.gui.drawdesk;

import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.chorda.client.icon.CIcons.CTextureIcon;
import com.teammoeg.frostedresearch.FRMain;
import com.teammoeg.frostedresearch.gui.drawdesk.game.CardType;
import net.minecraft.resources.ResourceLocation;

public class DrawDeskIcons {
	public static final ResourceLocation LOCATION = new ResourceLocation(FRMain.MODID, "textures/gui/draw_desk.png");
	public static final CTextureIcon ALL = CIcons.getIcon(LOCATION);
	public static final CTextureIcon Background = ALL.withUV(0, 0, 387, 203, 512, 512);
	public static final CTextureIcon HELP = ALL.withUV(0, 267, 243, 170, 512, 512);
	public static final CTextureIcon[] icons = new CTextureIcon[16];
	public static final CTextureIcon[] icons_grey = new CTextureIcon[16];
	public static final CTextureIcon SELECTED = ALL.withUV(16 * 16, 203, 16, 16, 512, 512);
	public static final CTextureIcon TECH = ALL.withUV(17 * 16, 203, 16, 16, 512, 512);
	public static final CTextureIcon RESET = ALL.withUV(16 * 16, 219, 16, 16, 512, 512);
	public static final CTextureIcon STOP = ALL.withUV(17 * 16, 219, 16, 16, 512, 512);

	// public static final CTextureIcon STAT_FRAME=ALL.withUV(0,235,15,28,512,512);
	public static final CTextureIcon DIALOG_FRAME = ALL.withUV(243, 267, 137, 52, 512, 512);
	public static final CTextureIcon ORDER_FRAME = ALL.withUV(16, 235, 16, 16, 512, 512);
	public static final CTextureIcon ORDER_ARROW = ALL.withUV(16, 235 + 16, 16, 12, 512, 512);
	public static final CTextureIcon EXAMINE = ALL.withUV(32, 235, 18, 18, 512, 512);

	static {
		for (int i = 0; i < 16; i++) {
			icons[i] = ALL.withUV(i * 16, 203, 16, 16, 512, 512);
			icons_grey[i] = ALL.withUV(i * 16, 219, 16, 16, 512, 512);
		}
	}

	private DrawDeskIcons() {
	}

	public static CTextureIcon getIcon(CardType ct, int card, boolean active) {
		if (active)
			return icons[getIconIndex(ct, card)];
		return icons_grey[getIconIndex(ct, card)];
	}

	public static int getIconIndex(CardType ct, int card) {
		switch (ct) {
		case SIMPLE:
			if (card == 0) return 1;
			return card + 3;
		case ADDING:
			if (card == 0) return 0;
			return card + 7;
		case PAIR:
			return card + 2;
		}
		return -1;
	}

}
