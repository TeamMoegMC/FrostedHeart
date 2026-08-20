/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.town.transport;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;

/** Town-owned aggregate state for daily transport service. */
public final class TownTransportState {
    public record DailyReport(
            boolean hasData,
            double totalCapacity,
            double reservedCapacity
    ) {
        public static final DailyReport EMPTY = new DailyReport(false, 0.0, 0.0);
        public static final Codec<DailyReport> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BOOL.optionalFieldOf("hasData", false).forGetter(DailyReport::hasData),
                Codec.DOUBLE.optionalFieldOf("totalCapacity", 0.0).forGetter(DailyReport::totalCapacity),
                Codec.DOUBLE.optionalFieldOf("reservedCapacity", 0.0).forGetter(DailyReport::reservedCapacity)
        ).apply(instance, DailyReport::new));

        public DailyReport {
            totalCapacity = sanitize(totalCapacity);
            reservedCapacity = sanitize(reservedCapacity);
        }
    }

    public static final Codec<TownTransportState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            DailyReport.CODEC.optionalFieldOf("dailyReport", DailyReport.EMPTY)
                    .forGetter(TownTransportState::getDailyReport)
    ).apply(instance, TownTransportState::new));

    @Getter
    private DailyReport dailyReport = DailyReport.EMPTY;

    public TownTransportState() {
    }

    public TownTransportState(DailyReport dailyReport) {
        this.dailyReport = dailyReport == null ? DailyReport.EMPTY : dailyReport;
    }

    /** Replaces the immutable morning snapshot and reports whether it changed. */
    public boolean setDailyReport(DailyReport dailyReport) {
        DailyReport next = dailyReport == null ? DailyReport.EMPTY : dailyReport;
        if (next.equals(this.dailyReport)) return false;
        this.dailyReport = next;
        return true;
    }

    private static double sanitize(double value) {
        return Double.isFinite(value) ? Math.max(0.0, value) : 0.0;
    }
}
