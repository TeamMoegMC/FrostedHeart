/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
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
