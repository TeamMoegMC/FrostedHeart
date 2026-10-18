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

package com.teammoeg.frostedheart.content.town;

import java.util.Locale;

/** Mutually-exclusive laws in the first town policy domain: residential care. */
public enum TownCareLaw {
    CLINICAL_TRIAGE("clinical_triage"),
    DEPENDENT_FIRST("dependent_first"),
    WORKFORCE_FIRST("workforce_first");

    private final String id;

    TownCareLaw(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static TownCareLaw fromId(String id) {
        if (id != null) {
            for (TownCareLaw law : values()) {
                if (law.id.equals(id.toLowerCase(Locale.ROOT))) return law;
            }
        }
        return CLINICAL_TRIAGE;
    }
}
