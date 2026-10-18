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

package com.teammoeg.frostedheart.content.town.observation;

/** Client-side latest-value cache with monotonic server-time rejection. */
public final class TownOperationalStatusClientCache {
    private static TownOperationalStatus latest;
    private static long latestServerGameTime = Long.MIN_VALUE;

    private TownOperationalStatusClientCache() {
    }

    public static synchronized boolean accept(TownOperationalStatus status) {
        if (status.serverGameTime() < latestServerGameTime) return false;
        latest = status;
        latestServerGameTime = status.serverGameTime();
        return true;
    }

    public static synchronized TownOperationalStatus get() {
        return latest;
    }

    public static synchronized void reset() {
        latest = null;
        latestServerGameTime = Long.MIN_VALUE;
    }
}
