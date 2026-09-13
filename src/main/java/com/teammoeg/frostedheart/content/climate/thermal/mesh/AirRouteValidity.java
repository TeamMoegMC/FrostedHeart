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
