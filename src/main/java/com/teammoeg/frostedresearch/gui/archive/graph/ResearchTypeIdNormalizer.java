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

import com.teammoeg.frostedresearch.FRMain;
import com.teammoeg.frostedresearch.research.ResearchCategory;

import javax.annotation.Nullable;
import java.util.Locale;

/** Normalizes legacy Frosted Heart category aliases for presentation filtering only. */
public final class ResearchTypeIdNormalizer {
    public static final String ALL_TYPES = "*";

    private ResearchTypeIdNormalizer() {
    }

    public static String normalize(ResearchCategory category) {
        return category.getId().toString();
    }

    public static String normalize(@Nullable String rawId) {
        if (rawId == null || rawId.isBlank() || ALL_TYPES.equals(rawId)) {
            return ALL_TYPES;
        }
        String id = rawId.trim().toLowerCase(Locale.ROOT);
        int separator = id.indexOf(':');
        if (separator < 0) {
            return FRMain.MODID + ":" + id;
        }
        String namespace = id.substring(0, separator);
        String path = id.substring(separator + 1);
        if ("frostedheart".equals(namespace) || FRMain.MODID.equals(namespace)) {
            return FRMain.MODID + ":" + path;
        }
        return id;
    }
}
