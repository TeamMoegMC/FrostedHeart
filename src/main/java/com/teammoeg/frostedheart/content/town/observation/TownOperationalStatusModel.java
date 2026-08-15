/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.observation;

import java.util.ArrayList;
import java.util.List;

/** Forge-independent equations used by both gameplay adapters and tests. */
public final class TownOperationalStatusModel {
    private TownOperationalStatusModel() {
    }

    public static TownOperationalStatus.Metric foodReserveDays(
            double storedFoodUnits,
            int population,
            double foodUnitsPerResidentDay
    ) {
        if (population <= 0 || foodUnitsPerResidentDay <= 0.0) {
            return TownOperationalStatus.Metric.unavailable();
        }
        double dailyDemand = population * foodUnitsPerResidentDay;
        return TownOperationalStatus.Metric.available(Math.max(0.0, finite(storedFoodUnits)) / dailyDemand);
    }

    /** Number of whole recipe applications that can be made from a stored item amount. */
    public static long wholeRecipeApplications(double storedItemCount, int recipeInputCount) {
        if (recipeInputCount <= 0) return 0L;
        return Math.max(0L, (long) Math.floor(Math.max(0.0, finite(storedItemCount)) / recipeInputCount));
    }

    /**
     * Adds already-loaded process ticks and every whole fuel recipe application.
     * Fractional warehouse item amounts and incomplete multi-item recipes are not
     * silently rounded up.
     */
    public static long totalProcessTicks(long loadedProcessTicks, List<FuelStock> stocks) {
        long total = Math.max(0L, loadedProcessTicks);
        for (FuelStock stock : stocks) {
            long applications = wholeRecipeApplications(stock.itemCount(), stock.recipeInputCount());
            long contribution;
            try {
                contribution = Math.multiplyExact(applications, Math.max(0, stock.processTicksPerRecipe()));
                total = Math.addExact(total, contribution);
            } catch (ArithmeticException overflow) {
                return Long.MAX_VALUE;
            }
        }
        return total;
    }

    public static TownOperationalStatus.Metric t1FuelReserveDays(
            long totalProcessTicks,
            int baseProcessTicksPerGameTick,
            int overdriveExtraProcessTicksPerGameTick,
            boolean overdrive,
            int gameTicksPerDay
    ) {
        long perGameTick = Math.max(0L, baseProcessTicksPerGameTick)
                + (overdrive ? Math.max(0L, overdriveExtraProcessTicksPerGameTick) : 0L);
        long dailyDemand;
        try {
            dailyDemand = Math.multiplyExact(perGameTick, Math.max(0L, gameTicksPerDay));
        } catch (ArithmeticException overflow) {
            dailyDemand = Long.MAX_VALUE;
        }
        if (dailyDemand <= 0L) return TownOperationalStatus.Metric.unavailable();
        return TownOperationalStatus.Metric.available(Math.max(0L, totalProcessTicks) / (double) dailyDemand);
    }

    public static ReserveBand reserveBand(
            TownOperationalStatus.Metric metric,
            double warningDays,
            double criticalDays
    ) {
        if (!metric.available()) return ReserveBand.UNAVAILABLE;
        if (metric.value() < criticalDays) return ReserveBand.CRITICAL;
        if (metric.value() < warningDays) return ReserveBand.WARNING;
        return ReserveBand.SAFE;
    }

    /** One daily reserve crossing; crossing both lines emits only CRITICAL. */
    public static ReserveTransition reserveTransition(
            TownOperationalStatus.Metric previous,
            TownOperationalStatus.Metric current,
            double warningDays,
            double criticalDays
    ) {
        ReserveBand before = reserveBand(previous, warningDays, criticalDays);
        ReserveBand after = reserveBand(current, warningDays, criticalDays);
        if (before == ReserveBand.UNAVAILABLE || after == ReserveBand.UNAVAILABLE || before == after) {
            return ReserveTransition.NONE;
        }
        if (after == ReserveBand.CRITICAL && before != ReserveBand.CRITICAL) return ReserveTransition.CRITICAL;
        if (after == ReserveBand.WARNING && before == ReserveBand.SAFE) return ReserveTransition.WARNING;
        if (after == ReserveBand.SAFE && (before == ReserveBand.WARNING || before == ReserveBand.CRITICAL)) {
            return ReserveTransition.RECOVERED;
        }
        return ReserveTransition.NONE;
    }

    public static List<TownOperationalStatus.ActiveAlert> activeAlerts(
            TownOperationalStatus status,
            double warningDays,
            double criticalDays
    ) {
        List<TownOperationalStatus.ActiveAlert> result = new ArrayList<>();
        addReserveAlert(result, status.foodReserveDays(), warningDays, criticalDays,
                TownSignalEvent.Type.FOOD_RESERVE_WARNING, TownSignalEvent.Type.FOOD_SHORTAGE,
                Math.max(1, status.population()));
        addReserveAlert(result, status.fuelReserveDays(), warningDays, criticalDays,
                TownSignalEvent.Type.FUEL_RESERVE_WARNING, TownSignalEvent.Type.FUEL_SHORTAGE, 1);
        if (status.population() > 0
                && (!status.tower().present() || !status.tower().active() || status.tower().broken())) {
            result.add(new TownOperationalStatus.ActiveAlert(TownSignalEvent.Type.TOWER_SERVICE_LOST,
                    TownSignalEvent.Severity.CRITICAL, 1));
        }
        if (status.unsafeOccupiedHouseCount() > 0) {
            result.add(new TownOperationalStatus.ActiveAlert(TownSignalEvent.Type.HOUSE_TEMPERATURE_UNSAFE,
                    TownSignalEvent.Severity.CRITICAL, status.unsafeOccupiedHouseCount()));
        }
        if (status.stoppedStaffedHuntingCount() > 0) {
            result.add(new TownOperationalStatus.ActiveAlert(TownSignalEvent.Type.HUNTING_TEMPERATURE_STOP,
                    TownSignalEvent.Severity.WARNING, status.stoppedStaffedHuntingCount()));
        }
        if (status.unableToWorkCount() > 0) {
            result.add(new TownOperationalStatus.ActiveAlert(TownSignalEvent.Type.WORK_CAPACITY_LOST,
                    TownSignalEvent.Severity.WARNING, status.unableToWorkCount()));
        }
        if (status.exitRiskCount() > 0) {
            result.add(new TownOperationalStatus.ActiveAlert(TownSignalEvent.Type.EXIT_RISK_ENTERED,
                    TownSignalEvent.Severity.CRITICAL, status.exitRiskCount()));
        }
        if (status.climateLevel() < 0) {
            result.add(new TownOperationalStatus.ActiveAlert(TownSignalEvent.Type.CLIMATE_COLD_WARNING,
                    TownSignalEvent.Severity.WARNING, 1));
        }
        return List.copyOf(result);
    }

    private static void addReserveAlert(
            List<TownOperationalStatus.ActiveAlert> result,
            TownOperationalStatus.Metric metric,
            double warningDays,
            double criticalDays,
            TownSignalEvent.Type warningType,
            TownSignalEvent.Type criticalType,
            int affectedCount
    ) {
        ReserveBand band = reserveBand(metric, warningDays, criticalDays);
        if (band == ReserveBand.WARNING) {
            result.add(new TownOperationalStatus.ActiveAlert(warningType,
                    TownSignalEvent.Severity.WARNING, affectedCount));
        } else if (band == ReserveBand.CRITICAL) {
            result.add(new TownOperationalStatus.ActiveAlert(criticalType,
                    TownSignalEvent.Severity.CRITICAL, affectedCount));
        }
    }

    private static double finite(double value) {
        return Double.isFinite(value) ? value : 0.0;
    }

    public record FuelStock(double itemCount, int recipeInputCount, int processTicksPerRecipe) {
    }

    public enum ReserveBand {
        UNAVAILABLE,
        SAFE,
        WARNING,
        CRITICAL
    }

    public enum ReserveTransition {
        NONE,
        WARNING,
        CRITICAL,
        RECOVERED
    }
}
