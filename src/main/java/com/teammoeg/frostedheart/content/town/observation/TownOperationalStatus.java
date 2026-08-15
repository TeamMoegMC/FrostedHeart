/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.observation;

import java.util.List;

/**
 * Lightweight, player-facing town state calculated by the server while the
 * Mayor's Seal is open. Missing metrics are represented explicitly instead of
 * using a fabricated zero.
 */
public record TownOperationalStatus(
        long serverGameTime,
        int population,
        double averageHealth,
        double p10Health,
        double averageMental,
        double p10Mental,
        int unableToWorkCount,
        int exitRiskCount,
        int homelessCount,
        int unemployedCount,
        Metric foodReserveDays,
        Metric fuelReserveDays,
        Metric minimumHouseTemperatureCelsius,
        Metric minimumBuildingTemperatureCelsius,
        int unsafeOccupiedHouseCount,
        int stoppedStaffedHuntingCount,
        TowerStatus tower,
        int climateLevel,
        List<ActiveAlert> activeAlerts
) {
    public TownOperationalStatus {
        population = Math.max(0, population);
        unableToWorkCount = Math.max(0, unableToWorkCount);
        exitRiskCount = Math.max(0, exitRiskCount);
        homelessCount = Math.max(0, homelessCount);
        unemployedCount = Math.max(0, unemployedCount);
        unsafeOccupiedHouseCount = Math.max(0, unsafeOccupiedHouseCount);
        stoppedStaffedHuntingCount = Math.max(0, stoppedStaffedHuntingCount);
        foodReserveDays = foodReserveDays == null ? Metric.unavailable() : foodReserveDays;
        fuelReserveDays = fuelReserveDays == null ? Metric.unavailable() : fuelReserveDays;
        minimumHouseTemperatureCelsius = minimumHouseTemperatureCelsius == null
                ? Metric.unavailable() : minimumHouseTemperatureCelsius;
        minimumBuildingTemperatureCelsius = minimumBuildingTemperatureCelsius == null
                ? Metric.unavailable() : minimumBuildingTemperatureCelsius;
        tower = tower == null ? TowerStatus.absent() : tower;
        activeAlerts = activeAlerts == null ? List.of() : List.copyOf(activeAlerts);
    }

    public static TownOperationalStatus empty(long serverGameTime) {
        return new TownOperationalStatus(serverGameTime, 0, 0, 0, 0, 0,
                0, 0, 0, 0, Metric.unavailable(), Metric.unavailable(),
                Metric.unavailable(), Metric.unavailable(), 0, 0,
                TowerStatus.absent(), 0, List.of());
    }

    public TownOperationalStatus withActiveAlerts(List<ActiveAlert> alerts) {
        return new TownOperationalStatus(serverGameTime, population, averageHealth, p10Health,
                averageMental, p10Mental, unableToWorkCount, exitRiskCount, homelessCount,
                unemployedCount, foodReserveDays, fuelReserveDays,
                minimumHouseTemperatureCelsius, minimumBuildingTemperatureCelsius,
                unsafeOccupiedHouseCount, stoppedStaffedHuntingCount, tower, climateLevel, alerts);
    }

    public record Metric(boolean available, double value) {
        public Metric {
            value = available && Double.isFinite(value) ? value : 0.0;
        }

        public static Metric available(double value) {
            return new Metric(true, value);
        }

        public static Metric unavailable() {
            return new Metric(false, 0.0);
        }
    }

    public record TowerStatus(
            TowerKind kind,
            boolean enabled,
            boolean active,
            boolean broken,
            boolean overdrive,
            double overdriveFraction
    ) {
        public TowerStatus {
            kind = kind == null ? TowerKind.NONE : kind;
            overdriveFraction = Double.isFinite(overdriveFraction)
                    ? Math.max(0.0, Math.min(1.0, overdriveFraction)) : 0.0;
        }

        public static TowerStatus absent() {
            return new TowerStatus(TowerKind.NONE, false, false, false, false, 0.0);
        }

        public boolean present() {
            return kind != TowerKind.NONE;
        }
    }

    public enum TowerKind {
        NONE,
        T1,
        T2,
        UNKNOWN
    }

    public record ActiveAlert(TownSignalEvent.Type type, TownSignalEvent.Severity severity, int affectedCount) {
        public ActiveAlert {
            affectedCount = Math.max(0, affectedCount);
        }
    }
}
