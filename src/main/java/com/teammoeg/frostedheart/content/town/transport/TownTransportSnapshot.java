/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.transport;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.content.town.transport.device.P2PFilterSummaryState;
import com.teammoeg.frostedheart.content.town.model.TownModelParameters;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Complete small transport snapshot used by incremental client synchronization. */
public record TownTransportSnapshot(
        TownTransportState.DailyReport dailyReport,
        double totalCapacity,
        int effectiveWarehouseCount,
        double warehouseDistanceCostPerBlock,
        double p2pDistanceCostPerBlock,
        List<TownTransportState.ReservationEntry> reservations,
        P2PBindingState p2pBindingState,
        P2PFilterSummaryState p2pFilterSummaryState
) {
    public static final int MAX_RESERVATIONS = 4096;
    public static final TownTransportSnapshot EMPTY = new TownTransportSnapshot(
            TownTransportState.DailyReport.EMPTY, 0.0, 0, 0.0,
            TownModelParameters.Defaults.TRANSPORT_CONSUMER_P2P_DISTANCE_COST_PER_BLOCK,
            List.of(),
            P2PBindingState.EMPTY, P2PFilterSummaryState.EMPTY);

    private static final Codec<TownTransportSnapshot> RAW_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            TownTransportState.DailyReport.CODEC.fieldOf("dailyReport")
                    .forGetter(TownTransportSnapshot::dailyReport),
            Codec.DOUBLE.fieldOf("totalCapacity").forGetter(TownTransportSnapshot::totalCapacity),
            Codec.INT.fieldOf("effectiveWarehouseCount")
                    .forGetter(TownTransportSnapshot::effectiveWarehouseCount),
            Codec.DOUBLE.fieldOf("warehouseDistanceCostPerBlock")
                    .forGetter(TownTransportSnapshot::warehouseDistanceCostPerBlock),
            Codec.DOUBLE.optionalFieldOf("p2pDistanceCostPerBlock",
                            TownModelParameters.Defaults.TRANSPORT_CONSUMER_P2P_DISTANCE_COST_PER_BLOCK)
                    .forGetter(TownTransportSnapshot::p2pDistanceCostPerBlock),
            TownTransportState.ReservationEntry.SNAPSHOT_CODEC.listOf().fieldOf("reservations")
                    .forGetter(TownTransportSnapshot::reservations),
            P2PBindingState.CODEC.optionalFieldOf("p2pBindingState", P2PBindingState.EMPTY)
                    .forGetter(TownTransportSnapshot::p2pBindingState),
            P2PFilterSummaryState.CODEC.optionalFieldOf(
                            "p2pFilterSummaryState", P2PFilterSummaryState.EMPTY)
                    .forGetter(TownTransportSnapshot::p2pFilterSummaryState)
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
        p2pBindingState = p2pBindingState == null ? P2PBindingState.EMPTY : p2pBindingState;
        p2pFilterSummaryState = p2pFilterSummaryState == null
                ? P2PFilterSummaryState.EMPTY : p2pFilterSummaryState;
    }

    public TownTransportSnapshot(
            TownTransportState.DailyReport dailyReport,
            double totalCapacity,
            int effectiveWarehouseCount,
            double warehouseDistanceCostPerBlock,
            List<TownTransportState.ReservationEntry> reservations
    ) {
        this(dailyReport, totalCapacity, effectiveWarehouseCount,
                warehouseDistanceCostPerBlock,
                TownModelParameters.Defaults.TRANSPORT_CONSUMER_P2P_DISTANCE_COST_PER_BLOCK,
                reservations,
                P2PBindingState.EMPTY, P2PFilterSummaryState.EMPTY);
    }

    public TownTransportSnapshot(
            TownTransportState.DailyReport dailyReport,
            double totalCapacity,
            List<TownTransportState.ReservationEntry> reservations
    ) {
        this(dailyReport, totalCapacity, 0, 0.0, reservations);
    }

    public static TownTransportSnapshot from(double totalCapacity, TownTransportState state) {
        if (state == null) {
            return EMPTY;
        }
        return new TownTransportSnapshot(
                state.getDailyReport(), totalCapacity,
                state.getEffectiveWarehouseCount(), state.getWarehouseDistanceCostPerBlock(),
                TownModelParameters.Defaults.TRANSPORT_CONSUMER_P2P_DISTANCE_COST_PER_BLOCK,
                state.getSnapshotEntries(), P2PBindingState.EMPTY,
                P2PFilterSummaryState.EMPTY);
    }

    public static TownTransportSnapshot from(
            double totalCapacity,
            TownTransportState state,
            int effectiveWarehouseCount,
            double warehouseDistanceCostPerBlock
    ) {
        if (state == null) {
            return EMPTY;
        }
        return new TownTransportSnapshot(
                state.getDailyReport(), totalCapacity,
                effectiveWarehouseCount, warehouseDistanceCostPerBlock,
                TownModelParameters.Defaults.TRANSPORT_CONSUMER_P2P_DISTANCE_COST_PER_BLOCK,
                state.getSnapshotEntries(), P2PBindingState.EMPTY,
                P2PFilterSummaryState.EMPTY);
    }

    public static TownTransportSnapshot from(
            double totalCapacity,
            TownTransportState state,
            int effectiveWarehouseCount,
            double warehouseDistanceCostPerBlock,
            double p2pDistanceCostPerBlock,
            P2PBindingState p2pBindingState,
            P2PFilterSummaryState p2pFilterSummaryState
    ) {
        if (state == null) {
            return EMPTY;
        }
        return new TownTransportSnapshot(
                state.getDailyReport(), totalCapacity, effectiveWarehouseCount,
                warehouseDistanceCostPerBlock, p2pDistanceCostPerBlock,
                state.getSnapshotEntries(),
                p2pBindingState, p2pFilterSummaryState);
    }

    public TownTransportSummary summary() {
        return TownTransportSummary.from(totalCapacity, TownTransportState.fromSnapshot(this));
    }

    private static DataResult<TownTransportSnapshot> validate(TownTransportSnapshot snapshot) {
        if (!TransportReservationModel.isFiniteNonNegative(snapshot.totalCapacity)) {
            return DataResult.error(() -> "Transport snapshot total capacity must be finite and non-negative.");
        }
        if (snapshot.effectiveWarehouseCount < 0) {
            return DataResult.error(() -> "Transport snapshot warehouse count must be non-negative.");
        }
        if (!TransportReservationModel.isFiniteNonNegative(
                snapshot.warehouseDistanceCostPerBlock)) {
            return DataResult.error(() ->
                    "Transport snapshot warehouse distance cost must be finite and non-negative.");
        }
        if (!TransportReservationModel.isFiniteNonNegative(
                snapshot.p2pDistanceCostPerBlock)) {
            return DataResult.error(() ->
                    "Transport snapshot P2P distance cost must be finite and non-negative.");
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
