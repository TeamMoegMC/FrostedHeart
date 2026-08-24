/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.thermal.phase0.reference;

/** Test-only routing assertion used while no V1 production runtime exists. */
public final class PhaseZeroThermalRouting {
    public static final RequestedMode DEFAULT_MODE = RequestedMode.LEGACY;

    private PhaseZeroThermalRouting() {
    }

    public enum RequestedMode {
        LEGACY,
        SHADOW,
        V1_PRODUCTION
    }

    public enum GameplayAuthority {
        LEGACY
    }

    public enum Reason {
        DEFAULT_LEGACY,
        NON_AUTHORITATIVE_SHADOW,
        V1_PRODUCTION_NOT_APPROVED
    }

    public record Decision(
            RequestedMode requestedMode,
            GameplayAuthority gameplayAuthority,
            boolean v1ShadowEnabled,
            boolean v1ProductionEnabled,
            Reason reason
    ) {
    }

    public static Decision defaults() {
        return select(DEFAULT_MODE);
    }

    public static Decision select(RequestedMode requestedMode) {
        if (requestedMode == null) {
            return defaults();
        }
        return switch (requestedMode) {
            case LEGACY -> new Decision(
                    requestedMode,
                    GameplayAuthority.LEGACY,
                    false,
                    false,
                    Reason.DEFAULT_LEGACY
            );
            case SHADOW -> new Decision(
                    requestedMode,
                    GameplayAuthority.LEGACY,
                    true,
                    false,
                    Reason.NON_AUTHORITATIVE_SHADOW
            );
            case V1_PRODUCTION -> new Decision(
                    requestedMode,
                    GameplayAuthority.LEGACY,
                    true,
                    false,
                    Reason.V1_PRODUCTION_NOT_APPROVED
            );
        };
    }
}
