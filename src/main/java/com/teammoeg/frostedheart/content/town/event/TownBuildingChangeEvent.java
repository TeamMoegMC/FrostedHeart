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

package com.teammoeg.frostedheart.content.town.event;

import java.util.EventObject;
import java.util.Set;
import net.minecraft.core.BlockPos;

/** Describes what changed; the town decides which derived data needs rebuilding. */
public final class TownBuildingChangeEvent extends EventObject {
    public enum ChangeKind {
        /** Business values or environment inputs; save and synchronize the building. */
        DATA,
        /** Occupied space changed; also recompute room overlap. */
        STRUCTURE,
        /** Working status or capacity changed; refresh affected services. */
        AVAILABILITY,
        /** A building was added, removed or replaced. */
        REGISTRATION
    }

    public final BlockPos changedBuildingPos;
    private final Set<ChangeKind> changes;

    public TownBuildingChangeEvent(Object source, BlockPos position) {
        this(source, position, ChangeKind.DATA);
    }

    public TownBuildingChangeEvent(Object source, BlockPos position, ChangeKind change) {
        this(source, position, Set.of(change));
    }

    public TownBuildingChangeEvent(Object source, BlockPos position, Set<ChangeKind> changes) {
        super(source);
        this.changedBuildingPos = position;
        this.changes = Set.copyOf(changes);
    }

    public boolean includes(ChangeKind change) {
        return changes.contains(change);
    }

    public boolean requiresSpaceRecheck() {
        return includes(ChangeKind.STRUCTURE) || includes(ChangeKind.REGISTRATION);
    }

    public boolean affectsAvailability() {
        return requiresSpaceRecheck() || includes(ChangeKind.AVAILABILITY);
    }
}
