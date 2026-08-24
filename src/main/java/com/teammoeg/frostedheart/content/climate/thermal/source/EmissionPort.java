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

/** Immutable source-port definition used at registry and resync boundaries. */
public record EmissionPort(
        int portId,
        int portRevision,
        SourceChannel channel,
        double powerShare,
        SourceBinding binding
) {
    public EmissionPort {
        if (portId < 0) {
            throw new IllegalArgumentException("portId must be non-negative");
        }
        if (portRevision < 0) {
            throw new IllegalArgumentException("portRevision must be non-negative");
        }
        Objects.requireNonNull(channel, "channel");
        if (!Double.isFinite(powerShare) || powerShare < 0.0D || powerShare > 1.0D) {
            throw new IllegalArgumentException("powerShare must be finite and in [0, 1]");
        }
        Objects.requireNonNull(binding, "binding");
    }

    public static EmissionPort of(
            int portId,
            SourceChannel channel,
            double powerShare,
            SourceBinding binding
    ) {
        return new EmissionPort(portId, 1, channel, powerShare, binding);
    }

    public EmissionPort rebound(SourceBinding newBinding) {
        return new EmissionPort(
                portId,
                Math.incrementExact(portRevision),
                channel,
                powerShare,
                newBinding
        );
    }
}
