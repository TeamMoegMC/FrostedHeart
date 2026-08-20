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

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

/** Immutable definition snapshot used by layout and projection. */
public record ResearchGraphNode(
        String id,
        String researchTypeId,
        List<String> parentIds,
        List<String> childIds,
        boolean hidden,
        ResearchLayoutHint layoutHint) {

    public ResearchGraphNode {
        id = Objects.requireNonNull(id, "id");
        researchTypeId = ResearchTypeIdNormalizer.normalize(researchTypeId);
        parentIds = sortedCopy(parentIds);
        childIds = sortedCopy(childIds);
        layoutHint = layoutHint == null ? ResearchLayoutHint.AUTO : layoutHint;
    }

    public static ResearchGraphNode automatic(
            String id,
            String researchTypeId,
            Collection<String> parentIds,
            boolean hidden) {
        return new ResearchGraphNode(
                id,
                researchTypeId,
                List.copyOf(parentIds),
                List.of(),
                hidden,
                ResearchLayoutHint.AUTO);
    }

    private static List<String> sortedCopy(Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        TreeSet<String> sorted = new TreeSet<>();
        for (String value : values) {
            if (value != null) {
                sorted.add(value);
            }
        }
        return List.copyOf(sorted);
    }
}
