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

package com.teammoeg.frostedresearch.knowledge.client.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.FormattedText;

/**
 * 札记里放不下的标题：截断时末尾加省略号，不悄悄剪掉；窄到连一个字都放不下时只留省略号，不留空白。
 * <p>
 * Titles in the journal that do not fit: cut with an ellipsis rather than clipped silently, and when not even one
 * character fits, the ellipsis alone, never a blank.
 */
public final class Clip {
    private static final String ELLIPSIS = "…";

    private Clip() {
    }

    public static FormattedText fit(Font font, FormattedText text, int width) {
        if (font.width(text) <= width) return text;
        int room = Math.max(0, width - font.width(ELLIPSIS));
        FormattedText head = font.substrByWidth(text, room);
        return FormattedText.composite(head, FormattedText.of(ELLIPSIS));
    }
}
