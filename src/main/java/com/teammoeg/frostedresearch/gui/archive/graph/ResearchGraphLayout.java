/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedresearch.gui.archive.graph;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Result of one definition-revision layout pass. */
public record ResearchGraphLayout(
        Map<String, NodePosition> positions,
        Bounds worldBounds,
        List<ResearchGraphDiagnostic> diagnostics) {

    public ResearchGraphLayout {
        positions = Collections.unmodifiableMap(new LinkedHashMap<>(positions));
        diagnostics = List.copyOf(diagnostics);
    }

    public record NodePosition(double x, double y, int rank, boolean manual) {
    }

    public record Bounds(double minX, double minY, double maxX, double maxY) {
        public static final Bounds EMPTY = new Bounds(0.0D, 0.0D, 0.0D, 0.0D);

        public double width() {
            return maxX - minX;
        }

        public double height() {
            return maxY - minY;
        }
    }
}
