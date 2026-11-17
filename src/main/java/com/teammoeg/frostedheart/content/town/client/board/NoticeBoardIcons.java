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

package com.teammoeg.frostedheart.content.town.client.board;

import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.chorda.client.icon.CIcons.CTextureIcon;
import com.teammoeg.frostedheart.FHMain;

import net.minecraft.resources.ResourceLocation;

/**
 * 布告板主题的贴图。尺寸与九宫格边距以 {@code Scripts/gen_town_ui_textures.py} 文件头的表为准，换美术时保持不变。
 * <p>
 * Textures of the notice-board theme. Sizes and nine-slice margins follow the table at the top of
 * {@code Scripts/gen_town_ui_textures.py}; replacement art must keep them.
 */
public final class NoticeBoardIcons {
    public static final CTextureIcon BOARD = nine("board", 0, 0, 64, 64, 64, 64, 8);
    public static final CTextureIcon BOARD_SNOW = part("board_snow", 0, 0, 64, 12, 64, 12);
    public static final CTextureIcon PAPER = nine("paper", 0, 0, 32, 32, 32, 32, 4);
    public static final CTextureIcon NOTE = nine("paper_note", 0, 0, 32, 32, 32, 32, 4);
    public static final CTextureIcon GRID = nine("paper_grid", 0, 0, 32, 32, 32, 32, 4);
    /** Frost overlays by level; index 0 is unused. */
    public static final CTextureIcon[] FROST = {null, nine("frost_1", 0, 0, 32, 32, 32, 32, 8),
            nine("frost_2", 0, 0, 32, 32, 32, 32, 8), nine("frost_3", 0, 0, 32, 32, 32, 32, 8)};
    public static final CTextureIcon ICICLES = part("icicles", 0, 0, 32, 8, 32, 8);
    public static final CTextureIcon PIN_RED = part("pin", 0, 0, 8, 8, 32, 8);
    public static final CTextureIcon PIN_BLUE = part("pin", 8, 0, 8, 8, 32, 8);
    public static final CTextureIcon PIN_GREEN = part("pin", 16, 0, 8, 8, 32, 8);
    public static final CTextureIcon PIN_BRASS = part("pin", 24, 0, 8, 8, 32, 8);
    public static final CTextureIcon TAPE = part("tape", 0, 0, 24, 8, 24, 8);
    public static final CTextureIcon STAMP_ROUND = part("stamp_round", 0, 0, 32, 32, 32, 32);
    public static final CTextureIcon STAMP_SQUARE = part("stamp_square", 0, 0, 24, 24, 24, 24);
    public static final CTextureIcon PLAQUE = nine("plaque", 0, 0, 28, 24, 84, 24, 4);
    public static final CTextureIcon PLAQUE_SELECTED = nine("plaque", 28, 0, 28, 24, 84, 24, 4);
    public static final CTextureIcon PLAQUE_DEBUG = nine("plaque", 56, 0, 28, 24, 84, 24, 4);
    public static final CTextureIcon BAR_TRACK = nine("bar", 0, 0, 16, 6, 16, 12, 2);
    public static final CTextureIcon BAR_FILL = nine("bar", 0, 6, 16, 6, 16, 12, 2);
    public static final CTextureIcon STAR_FULL = part("stars", 0, 0, 8, 8, 16, 8);
    public static final CTextureIcon STAR_EMPTY = part("stars", 8, 0, 8, 8, 16, 8);
    public static final CTextureIcon TAG = nine("tag_button", 0, 0, 24, 16, 72, 16, 4);
    public static final CTextureIcon TAG_HOVER = nine("tag_button", 24, 0, 24, 16, 72, 16, 4);
    public static final CTextureIcon TAG_PRESSED = nine("tag_button", 48, 0, 24, 16, 72, 16, 4);
    public static final CTextureIcon CORNER_NOTE = part("note_corner", 0, 0, 12, 12, 24, 12);
    public static final CTextureIcon CORNER_PAPER = part("note_corner", 12, 0, 12, 12, 24, 12);

    private NoticeBoardIcons() {
    }

    private static ResourceLocation location(String name) {
        return new ResourceLocation(FHMain.MODID, "textures/gui/town/" + name + ".png");
    }

    private static CTextureIcon part(String name, int x, int y, int w, int h, int tw, int th) {
        return CIcons.getIcon(location(name)).withUV(x, y, w, h, tw, th);
    }

    private static CTextureIcon nine(String name, int x, int y, int w, int h, int tw, int th, int corner) {
        return part(name, x, y, w, h, tw, th).toNineSlice(corner);
    }
}
