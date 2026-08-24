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

/** Frozen source modes; only physical modes participate in the energy ledger. */
public enum ThermalSourceMode {
    POWER_SOURCE,
    BOUNDARY,
    IMPULSE,
    ANALYTIC_CONTROL,
    LEGACY_CONTROL,
    BODY_DEVICE;

    public boolean usesPhysicalEnergyLedger() {
        return this == POWER_SOURCE || this == IMPULSE;
    }
}
