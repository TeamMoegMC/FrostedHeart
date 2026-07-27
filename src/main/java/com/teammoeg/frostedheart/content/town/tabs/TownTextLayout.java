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
 */

package com.teammoeg.frostedheart.content.town.tabs;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * Shared text layout rules for compact town GUIs.
 */
public final class TownTextLayout {
    private static final String ELLIPSIS = "…";

    private TownTextLayout() {}

    /**
     * Wraps a styled component without converting it to a plain string.
     * An explicitly empty component still occupies one visual line.
     */
    public static List<FormattedCharSequence> wrap(
            Font font, Component text, int maximumWidth
    ) {
        if (text == null) return List.of();
        if (text.getString().isEmpty()) {
            return List.of(FormattedCharSequence.EMPTY);
        }
        List<FormattedCharSequence> wrapped =
                font.split(text, Math.max(1, maximumWidth));
        return wrapped.isEmpty()
                ? List.of(FormattedCharSequence.EMPTY)
                : wrapped;
    }

    /**
     * Keeps selector labels on one line and makes truncation visible.
     */
    public static String ellipsize(Font font, String value, int maximumWidth) {
        if (value == null || maximumWidth <= 0) return "";
        if (font.width(value) <= maximumWidth) return value;
        int textWidth = maximumWidth - font.width(ELLIPSIS);
        if (textWidth <= 0) {
            return font.plainSubstrByWidth(ELLIPSIS, maximumWidth);
        }
        return font.plainSubstrByWidth(value, textWidth) + ELLIPSIS;
    }
}
