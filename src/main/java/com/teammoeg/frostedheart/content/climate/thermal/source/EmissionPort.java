/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.thermal.source;

import java.util.Objects;

/** Immutable source-port definition compiled into the packed source registry. */
public record EmissionPort(
        int portId,
        double powerShare,
        SourceBinding binding
) {
    public EmissionPort {
        if (portId < 0) {
            throw new IllegalArgumentException("portId must be non-negative");
        }
        if (!Double.isFinite(powerShare) || powerShare < 0.0D || powerShare > 1.0D) {
            throw new IllegalArgumentException("powerShare must be finite and in [0, 1]");
        }
        Objects.requireNonNull(binding, "binding");
    }

    public static EmissionPort of(
            int portId,
            double powerShare,
            SourceBinding binding
    ) {
        return new EmissionPort(portId, powerShare, binding);
    }
}
