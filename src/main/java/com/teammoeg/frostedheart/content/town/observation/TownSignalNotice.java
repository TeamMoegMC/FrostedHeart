/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.observation;

/** Safe, presentation-facing subset of a town event. */
public record TownSignalNotice(
        TownSignalEvent.Type type,
        TownSignalEvent.Severity severity,
        int affectedCount
) {
    public TownSignalNotice {
        affectedCount = Math.max(0, affectedCount);
    }

    public static TownSignalNotice from(TownSignalEvent event) {
        return new TownSignalNotice(event.type(), event.severity(), event.affectedCount());
    }
}
