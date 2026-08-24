/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.thermal.profile;

import java.util.Objects;

/** Main-thread resolution outcome ready to publish as a primitive signature ID. */
public record ThermalSignatureResolution(
        ThermalResolution.Status status,
        ThermalResolution.Reason reason,
        int signatureId
) {
    public static final int NO_SIGNATURE_ID = -1;

    public ThermalSignatureResolution {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(reason, "reason");
        if (reason.expectedStatus() != status) {
            throw new IllegalArgumentException("resolution reason does not match status");
        }
        if (status == ThermalResolution.Status.RESOLVED && signatureId < 0) {
            throw new IllegalArgumentException("resolved signature ID must be non-negative");
        }
        if (status != ThermalResolution.Status.RESOLVED && signatureId != NO_SIGNATURE_ID) {
            throw new IllegalArgumentException("non-resolved result must not publish a signature ID");
        }
    }

    public static ThermalSignatureResolution resolved(int signatureId) {
        return new ThermalSignatureResolution(
                ThermalResolution.Status.RESOLVED,
                ThermalResolution.Reason.NONE,
                signatureId
        );
    }

    public static ThermalSignatureResolution failure(ThermalResolution<?> resolution) {
        Objects.requireNonNull(resolution, "resolution");
        if (resolution.isResolved()) {
            throw new IllegalArgumentException("resolved value is not a signature failure");
        }
        return new ThermalSignatureResolution(
                resolution.status(),
                resolution.reason(),
                NO_SIGNATURE_ID
        );
    }

    public boolean isResolved() {
        return status == ThermalResolution.Status.RESOLVED;
    }
}
