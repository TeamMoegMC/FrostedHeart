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

package com.teammoeg.frostedheart.content.space.station;

import java.util.Locale;

/**
 * The seven kinds of section a station ring can hold.
 * <p>
 * A type is only a role label here. Production, consumption and power behaviour belong to the future section
 * implementations in {@link com.teammoeg.frostedheart.content.space.station.sections}; this enum deliberately keeps
 * balance data out of the geometry model.
 */
public enum SectionType {
    /** Housing and life of the crew. */
    RESIDENTIAL,
    /** Food production. */
    AGRICULTURE,
    /** General manufacturing. */
    MANUFACTURING,
    /** Ore and material refining. */
    REFINING,
    /** Power generation. */
    POWER,
    /** Docking and cargo handling. */
    PORT,
    /** Oxygen, water and temperature control. */
    LIFE_SUPPORT;

    private final String key = name().toLowerCase(Locale.ROOT);

    /**
     * Stable lowercase identifier used by data files and serialization.
     */
    public String getKey() {
        return key;
    }

    /**
     * Translation key for the display name.
     */
    public String getTranslationKey() {
        return "space_station.section." + key;
    }
}
