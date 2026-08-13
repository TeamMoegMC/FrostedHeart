/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.observation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Optional operational portion of a daily town snapshot. */
public record TownOperationalHistory(
        Metric foodReserveDays,
        Metric fuelReserveDays,
        Metric minimumHouseTemperatureCelsius,
        Metric minimumBuildingTemperatureCelsius,
        int unsafeOccupiedHouseCount,
        int stoppedStaffedHuntingCount,
        Tower tower
) {
    public static final TownOperationalHistory EMPTY = new TownOperationalHistory(
            Metric.UNAVAILABLE, Metric.UNAVAILABLE, Metric.UNAVAILABLE, Metric.UNAVAILABLE,
            0, 0, Tower.ABSENT);

    public static final Codec<TownOperationalHistory> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Metric.CODEC.optionalFieldOf("foodReserveDays", Metric.UNAVAILABLE)
                    .forGetter(TownOperationalHistory::foodReserveDays),
            Metric.CODEC.optionalFieldOf("fuelReserveDays", Metric.UNAVAILABLE)
                    .forGetter(TownOperationalHistory::fuelReserveDays),
            Metric.CODEC.optionalFieldOf("minimumHouseTemperatureCelsius", Metric.UNAVAILABLE)
                    .forGetter(TownOperationalHistory::minimumHouseTemperatureCelsius),
            Metric.CODEC.optionalFieldOf("minimumBuildingTemperatureCelsius", Metric.UNAVAILABLE)
                    .forGetter(TownOperationalHistory::minimumBuildingTemperatureCelsius),
            Metric.CODEC.optionalFieldOf("minimumHuntingTemperatureCelsius", Metric.UNAVAILABLE)
                    .forGetter(value -> Metric.UNAVAILABLE),
            Codec.INT.optionalFieldOf("unsafeOccupiedHouseCount", 0)
                    .forGetter(TownOperationalHistory::unsafeOccupiedHouseCount),
            Codec.INT.optionalFieldOf("stoppedStaffedHuntingCount", 0)
                    .forGetter(TownOperationalHistory::stoppedStaffedHuntingCount),
            Tower.CODEC.optionalFieldOf("tower", Tower.ABSENT).forGetter(TownOperationalHistory::tower)
    ).apply(instance, (food, fuel, house, building, legacyHunting, unsafeHouses,
                       stoppedHunting, tower) -> new TownOperationalHistory(
            food, fuel, house, building.available() ? building : legacyHunting,
            unsafeHouses, stoppedHunting, tower)));

    public TownOperationalHistory {
        foodReserveDays = foodReserveDays == null ? Metric.UNAVAILABLE : foodReserveDays;
        fuelReserveDays = fuelReserveDays == null ? Metric.UNAVAILABLE : fuelReserveDays;
        minimumHouseTemperatureCelsius = minimumHouseTemperatureCelsius == null
                ? Metric.UNAVAILABLE : minimumHouseTemperatureCelsius;
        minimumBuildingTemperatureCelsius = minimumBuildingTemperatureCelsius == null
                ? Metric.UNAVAILABLE : minimumBuildingTemperatureCelsius;
        unsafeOccupiedHouseCount = Math.max(0, unsafeOccupiedHouseCount);
        stoppedStaffedHuntingCount = Math.max(0, stoppedStaffedHuntingCount);
        tower = tower == null ? Tower.ABSENT : tower;
    }

    public static TownOperationalHistory from(TownOperationalStatus status) {
        return new TownOperationalHistory(
                Metric.from(status.foodReserveDays()), Metric.from(status.fuelReserveDays()),
                Metric.from(status.minimumHouseTemperatureCelsius()),
                Metric.from(status.minimumBuildingTemperatureCelsius()),
                status.unsafeOccupiedHouseCount(), status.stoppedStaffedHuntingCount(),
                Tower.from(status.tower()));
    }

    public record Metric(boolean available, double value) {
        public static final Metric UNAVAILABLE = new Metric(false, 0.0);
        public static final Codec<Metric> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BOOL.optionalFieldOf("available", false).forGetter(Metric::available),
                Codec.DOUBLE.optionalFieldOf("value", 0.0).forGetter(Metric::value)
        ).apply(instance, Metric::new));

        public Metric {
            value = available && Double.isFinite(value) ? value : 0.0;
        }

        public static Metric from(TownOperationalStatus.Metric metric) {
            return new Metric(metric.available(), metric.value());
        }

        public TownOperationalStatus.Metric toLiveMetric() {
            return new TownOperationalStatus.Metric(available, value);
        }
    }

    public record Tower(
            TownOperationalStatus.TowerKind kind,
            boolean enabled,
            boolean active,
            boolean broken,
            boolean overdrive,
            double overdriveFraction
    ) {
        private static final Codec<TownOperationalStatus.TowerKind> KIND_CODEC =
                Codec.STRING.xmap(TownOperationalStatus.TowerKind::valueOf,
                        TownOperationalStatus.TowerKind::name);
        public static final Tower ABSENT = new Tower(TownOperationalStatus.TowerKind.NONE,
                false, false, false, false, 0.0);
        public static final Codec<Tower> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                KIND_CODEC.optionalFieldOf("kind", TownOperationalStatus.TowerKind.NONE).forGetter(Tower::kind),
                Codec.BOOL.optionalFieldOf("enabled", false).forGetter(Tower::enabled),
                Codec.BOOL.optionalFieldOf("active", false).forGetter(Tower::active),
                Codec.BOOL.optionalFieldOf("broken", false).forGetter(Tower::broken),
                Codec.BOOL.optionalFieldOf("overdrive", false).forGetter(Tower::overdrive),
                Codec.DOUBLE.optionalFieldOf("overdriveFraction", 0.0).forGetter(Tower::overdriveFraction)
        ).apply(instance, Tower::new));

        public Tower {
            kind = kind == null ? TownOperationalStatus.TowerKind.NONE : kind;
            overdriveFraction = Double.isFinite(overdriveFraction)
                    ? Math.max(0.0, Math.min(1.0, overdriveFraction)) : 0.0;
        }

        public static Tower from(TownOperationalStatus.TowerStatus tower) {
            return new Tower(tower.kind(), tower.enabled(), tower.active(), tower.broken(),
                    tower.overdrive(), tower.overdriveFraction());
        }
    }
}
