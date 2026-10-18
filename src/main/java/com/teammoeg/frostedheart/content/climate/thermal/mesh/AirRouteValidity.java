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

/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.mesh;

/** Geometry-only witness shared by the routes through one passage component. */
public final class AirRouteValidity {
    private final ThermalPageHandle[] pages;
    private final long[] revisions;
    private volatile boolean retired;
    private volatile boolean committed;

    public AirRouteValidity(ThermalPageHandle[] pages, long[] revisions) {
        this.pages = pages;
        this.revisions = revisions;
    }

    public boolean valid() {
        if (retired || !committed) return false;
        for (int index = 0; index < pages.length; index++) {
            if (pages[index].liveGeometryRevision() != revisions[index]) return false;
        }
        return true;
    }

    public void retire() { retired = true; }

    public void commit() {
        if (retired) return;
        for (int index = 0; index < pages.length; index++) {
            PagePublication publication = pages[index].lastPublication();
            if (publication == null || publication.geometryRevision() != revisions[index]) return;
        }
        committed = true;
    }

    /** Worker cut validity; live world revisions are checked only by queries. */
    public boolean activeForSolve() { return !retired && committed; }
}
