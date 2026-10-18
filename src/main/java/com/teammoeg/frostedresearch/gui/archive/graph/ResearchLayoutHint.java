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

package com.teammoeg.frostedresearch.gui.archive.graph;

/** Optional display-only node placement. Research definitions currently default to {@link Mode#AUTO}. */
public record ResearchLayoutHint(Mode mode, double x, double y) {
    public static final ResearchLayoutHint AUTO = new ResearchLayoutHint(Mode.AUTO, 0.0D, 0.0D);

    public ResearchLayoutHint {
        if (mode == null) {
            mode = Mode.AUTO;
        }
    }

    public static ResearchLayoutHint manual(double x, double y) {
        return new ResearchLayoutHint(Mode.MANUAL, x, y);
    }

    public boolean isManual() {
        return mode == Mode.MANUAL;
    }

    public enum Mode {
        AUTO,
        MANUAL
    }
}
