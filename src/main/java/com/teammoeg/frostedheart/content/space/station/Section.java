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

/**
 * A built section occupying one {@link StationSlot} of a station ring.
 * <p>
 * Implementations provide the type-specific work. The station only owns placement and iteration; it never invents a
 * section's behaviour.
 */
public interface Section {
    /**
     * Role of this section.
     */
    SectionType getType();

    /**
     * Slot this section occupies.
     */
    StationSlot getSlot();

    /**
     * Runs one settlement step for this section.
     *
     * @param station owning station
     * @param time    station tick or settlement time, defined by the caller
     */
    void work(SpaceStation station, int time);
}
