/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.town.observation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * A discrete, player-observable threshold crossing in the town system.
 * Continuous state remains in {@link TownObservationModel.ResidentSnapshot};
 * this record is only for changes that can form a crisis episode.
 */
public record TownSignalEvent(
        long day,
        int hour,
        Type type,
        Severity severity,
        int affectedCount,
        long episodeId,
        String detail
) {
    private static final Codec<Type> TYPE_CODEC = Codec.STRING.xmap(Type::valueOf, Type::name);
    private static final Codec<Severity> SEVERITY_CODEC =
            Codec.STRING.xmap(Severity::valueOf, Severity::name);

    public static final Codec<TownSignalEvent> CODEC = RecordCodecBuilder.create(t -> t.group(
            Codec.LONG.fieldOf("day").forGetter(TownSignalEvent::day),
            Codec.INT.optionalFieldOf("hour", 0).forGetter(TownSignalEvent::hour),
            TYPE_CODEC.fieldOf("type").forGetter(TownSignalEvent::type),
            SEVERITY_CODEC.fieldOf("severity").forGetter(TownSignalEvent::severity),
            Codec.INT.optionalFieldOf("affectedCount", 1).forGetter(TownSignalEvent::affectedCount),
            Codec.LONG.optionalFieldOf("episodeId", 0L).forGetter(TownSignalEvent::episodeId),
            Codec.STRING.optionalFieldOf("detail", "").forGetter(TownSignalEvent::detail)
    ).apply(t, TownSignalEvent::new));

    public TownSignalEvent(
            long day,
            int hour,
            Type type,
            Severity severity,
            int affectedCount,
            String detail
    ) {
        this(day, hour, type, severity, affectedCount, 0L, detail);
    }

    public TownSignalEvent {
        hour = Math.max(0, Math.min(23, hour));
        affectedCount = Math.max(0, affectedCount);
        detail = detail == null ? "" : detail;
    }

    public enum Type {
        CLIMATE_COLD_WARNING,
        CLIMATE_COLD_ENDED,
        TOWER_SERVICE_LOST,
        TOWER_SERVICE_RESTORED,
        HOUSE_TEMPERATURE_UNSAFE,
        HOUSE_TEMPERATURE_RECOVERED,
        HUNTING_TEMPERATURE_STOP,
        HUNTING_TEMPERATURE_RECOVERED,
        FOOD_RESERVE_WARNING,
        FUEL_RESERVE_WARNING,
        FOOD_SHORTAGE,
        FUEL_SHORTAGE,
        FOOD_RESERVE_RECOVERED,
        FUEL_RESERVE_RECOVERED,
        WORK_CAPACITY_LOST,
        WORK_CAPACITY_RECOVERED,
        EXIT_RISK_ENTERED,
        EXIT_RISK_RECOVERED,
        RESIDENT_EXIT_HEALTH,
        RESIDENT_EXIT_MENTAL,
        RESIDENT_EXIT_BOTH,
        CRISIS_RECOVERED
    }

    public enum Severity {
        INFORMATION,
        WARNING,
        CRITICAL,
        IRREVERSIBLE
    }
}
