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

import java.util.Objects;

/**
 * Plain section: a role attached to one slot.
 * <p>
 * The seven roles currently share this implementation because no production rule has been designed yet. A dedicated
 * class in {@link com.teammoeg.frostedheart.content.space.station.sections} can be introduced later without touching
 * the station layout or placement logic.
 */
public class StationSection implements Section {
    private final SectionType type;
    private final StationSlot slot;

    public StationSection(SectionType type, StationSlot slot) {
        this.type = Objects.requireNonNull(type, "type");
        this.slot = Objects.requireNonNull(slot, "slot");
    }

    @Override
    public SectionType getType() {
        return type;
    }

    @Override
    public StationSlot getSlot() {
        return slot;
    }

    @Override
    public void work(SpaceStation station, int time) {
        // Production rules are added together with the section content, not in the geometry model.
    }
}
