/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.network;

/** One outstanding display transaction; elapsed time never makes its reply obsolete. */
public final class InfraredRequestState {
    public static final int NO_REQUEST = -1;
    private int requestId;
    private boolean pending;

    public int tryBegin() {
        if (pending) return NO_REQUEST;
        requestId = requestId == Integer.MAX_VALUE ? 0 : requestId + 1;
        pending = true;
        return requestId;
    }

    public boolean isPending() {
        return pending;
    }

    public boolean accepts(int responseId) {
        return pending && responseId == requestId;
    }

    public boolean complete(int responseId) {
        if (!accepts(responseId)) return false;
        pending = false;
        return true;
    }

    public void invalidate() {
        pending = false;
    }
}
