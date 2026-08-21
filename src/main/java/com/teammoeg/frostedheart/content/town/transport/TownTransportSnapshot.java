/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.transport;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Complete small transport snapshot used by incremental client synchronization. */
public record TownTransportSnapshot(
        TownTransportState.DailyReport dailyReport,
        double totalCapacity,
        List<TownTransportState.ReservationEntry> reservations
) {
    public static final int MAX_RESERVATIONS = 4096;
    public static final TownTransportSnapshot EMPTY = new TownTransportSnapshot(
            TownTransportState.DailyReport.EMPTY, 0.0, List.of());

    private static final Codec<TownTransportSnapshot> RAW_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            TownTransportState.DailyReport.CODEC.fieldOf("dailyReport")
                    .forGetter(TownTransportSnapshot::dailyReport),
            Codec.DOUBLE.fieldOf("totalCapacity").forGetter(TownTransportSnapshot::totalCapacity),
            TownTransportState.ReservationEntry.SNAPSHOT_CODEC.listOf().fieldOf("reservations")
                    .forGetter(TownTransportSnapshot::reservations)
    ).apply(instance, TownTransportSnapshot::new));

    public static final Codec<TownTransportSnapshot> CODEC = RAW_CODEC.flatXmap(
            TownTransportSnapshot::validate,
            TownTransportSnapshot::validate);

    public TownTransportSnapshot {
        dailyReport = dailyReport == null ? TownTransportState.DailyReport.EMPTY : dailyReport;
        List<TownTransportState.ReservationEntry> sorted = new ArrayList<>(
                reservations == null ? List.of() : reservations);
        sorted.sort(java.util.Comparator.comparing(
                TownTransportState.ReservationEntry::endpointId,
                TransportEndpointId.STABLE_COMPARATOR));
        reservations = List.copyOf(sorted);
    }

    public static TownTransportSnapshot from(double totalCapacity, TownTransportState state) {
        if (state == null) {
            return EMPTY;
        }
        return new TownTransportSnapshot(state.getDailyReport(), totalCapacity, state.getSnapshotEntries());
    }

    public TownTransportSummary summary() {
        return TownTransportSummary.from(totalCapacity, TownTransportState.fromSnapshot(this));
    }

    private static DataResult<TownTransportSnapshot> validate(TownTransportSnapshot snapshot) {
        if (!TransportReservationModel.isFiniteNonNegative(snapshot.totalCapacity)) {
            return DataResult.error(() -> "Transport snapshot total capacity must be finite and non-negative.");
        }
        if (snapshot.reservations.size() > MAX_RESERVATIONS) {
            return DataResult.error(() -> "Transport snapshot exceeds " + MAX_RESERVATIONS + " reservations.");
        }
        Set<TransportEndpointId> endpointIds = new HashSet<>();
        for (TownTransportState.ReservationEntry entry : snapshot.reservations) {
            if (entry == null || !endpointIds.add(entry.endpointId())) {
                return DataResult.error(() -> "Transport snapshot contains a null or duplicate endpoint.");
            }
        }
        return DataResult.success(snapshot);
    }
}
