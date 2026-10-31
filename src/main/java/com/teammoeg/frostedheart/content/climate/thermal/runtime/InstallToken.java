/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime;

/** One release publication selects every staged view of a local topology exchange. */
public final class InstallToken {
    private enum State { PREPARED, COMMITTED, CANCELLED }
    private volatile State state = State.PREPARED;
    public boolean committed() { return state == State.COMMITTED; }
    public void commit() {
        if (state != State.PREPARED) throw new IllegalStateException("Topology exchange no longer owns preparation");
        state = State.COMMITTED;
    }
    /** A published exchange is retired by cleanup; it is never rolled back. */
    public void cancel() { if (state == State.PREPARED) state = State.CANCELLED; }
}
