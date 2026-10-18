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

/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge.client;

/**
 * One responsive layout calculation, shared by rendering and hit targets.
 */
public record KnowledgeLayout(int width, int height, int header, int left, int rightX, int rightWidth, int listTop,
                              int bottom) {
    public static KnowledgeLayout of(int width, int height, boolean tray) {
        int header = width < 420 ? 53 : 34;
        int left = width >= 520 ? 162 : width >= 380 ? 136 : 108;
        int rightX = left + 29;
        return new KnowledgeLayout(width, height, header, left, rightX, width - rightX - 18, header + 47, height - 22 - (tray ? 36 : 0));
    }
}
