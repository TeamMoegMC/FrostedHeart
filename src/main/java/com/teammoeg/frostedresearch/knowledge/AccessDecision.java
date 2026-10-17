/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge;

import java.util.List;

/** Default-open access answer with all currently valid provenance. */
public record AccessDecision(boolean managed, boolean allowed, List<AccessSource> sources) {
    public AccessDecision {
        sources = List.copyOf(sources);
    }

    public static AccessDecision unmanaged() {
        return new AccessDecision(false, true, List.of());
    }
}
