/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
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
