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
import com.mojang.serialization.DataResult;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.FHMain;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Objects;

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

        private static double sanitize(double value) {
            return Double.isFinite(value) ? Math.max(0.0, value) : 0.0;
        }
    }

    /** A stable, explicit entry format avoids relying on a GlobalPos string-map representation. */
    public record ReservationEntry(TransportEndpointId endpointId, TransportReservation reservation) {
        public static final Codec<ReservationEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                TransportEndpointId.CODEC.fieldOf("endpointId").forGetter(ReservationEntry::endpointId),
                TransportReservation.CODEC.fieldOf("reservation").forGetter(ReservationEntry::reservation)
        ).apply(instance, ReservationEntry::new));
        public static final Codec<ReservationEntry> SNAPSHOT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
                TransportEndpointId.CODEC.fieldOf("endpointId").forGetter(ReservationEntry::endpointId),
                TransportReservation.SNAPSHOT_CODEC.fieldOf("reservation").forGetter(ReservationEntry::reservation)
        ).apply(instance, ReservationEntry::new));

        public ReservationEntry {
            Objects.requireNonNull(endpointId, "endpointId");
            Objects.requireNonNull(reservation, "reservation");
        }
    }

    /**
     * A malformed reservation must not discard the whole town. List codecs retain valid partial entries;
     * convert that partial result into a successful state field after logging the rejected entries.
     */
    private static final Codec<List<ReservationEntry>> TOLERANT_RESERVATION_LIST_CODEC = new Codec<>() {
        private final Codec<List<ReservationEntry>> delegate = ReservationEntry.CODEC.listOf();

        @Override
        public <T> DataResult<T> encode(List<ReservationEntry> input, com.mojang.serialization.DynamicOps<T> ops, T prefix) {
            return delegate.encode(input, ops, prefix);
        }

        @Override
        public <T> DataResult<Pair<List<ReservationEntry>, T>> decode(
                com.mojang.serialization.DynamicOps<T> ops, T input
        ) {
            DataResult<Pair<List<ReservationEntry>, T>> decoded = delegate.decode(ops, input);
            Pair<List<ReservationEntry>, T> recovered = decoded.resultOrPartial(message ->
                    FHMain.LOGGER.warn("Discarding invalid transport reservation entries: {}", message))
                    .orElseGet(() -> Pair.of(List.of(), input));
            return DataResult.success(recovered);
        }
    };

    public static final Codec<TownTransportState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            DailyReport.CODEC.optionalFieldOf("dailyReport", DailyReport.EMPTY)
                    .forGetter(TownTransportState::getDailyReport),
            TOLERANT_RESERVATION_LIST_CODEC.optionalFieldOf("reservations", List.of())
                    .forGetter(TownTransportState::getReservationEntries)
    ).apply(instance, TownTransportState::new));

    @Getter
    private DailyReport dailyReport = DailyReport.EMPTY;
    private final Map<TransportEndpointId, TransportReservation> reservations =
            new TreeMap<>(TransportEndpointId.STABLE_COMPARATOR);
    private double reservedTransportCapacity;
    private transient TransportConsumerParameters appliedParameters;

    public TownTransportState() {
    }

    public TownTransportState(DailyReport dailyReport) {
        this(dailyReport, List.of());
    }

    public TownTransportState(
            DailyReport dailyReport,
            Map<TransportEndpointId, TransportReservation> reservations
    ) {
        this(dailyReport, reservations == null ? List.of() : reservations.entrySet().stream()
                .map(entry -> new ReservationEntry(entry.getKey(), entry.getValue()))
                .toList());
    }

    private TownTransportState(DailyReport dailyReport, List<ReservationEntry> entries) {
        this.dailyReport = dailyReport == null ? DailyReport.EMPTY : dailyReport;
        this.reservations.putAll(normalizeEntries(entries));
        this.reservedTransportCapacity = sumReservedCapacity(this.reservations.values());
    }

    /** Replaces the immutable morning snapshot and reports whether it changed. */
    public boolean setDailyReport(DailyReport dailyReport) {
        DailyReport next = dailyReport == null ? DailyReport.EMPTY : dailyReport;
        if (next.equals(this.dailyReport)) return false;
        this.dailyReport = next;
        return true;
    }

    /** Stable, read-only view keyed by the physical endpoint position. */
    public Map<TransportEndpointId, TransportReservation> getReservations() {
        return Collections.unmodifiableMap(reservations);
    }

    public TransportReservation getReservation(TransportEndpointId endpointId) {
        return reservations.get(endpointId);
    }

    /** Internal authority hook used by TeamTown; callers cannot mutate the returned map. */
    public boolean replaceReservation(TransportEndpointId endpointId, TransportReservation reservation) {
        Objects.requireNonNull(endpointId, "endpointId");
        Objects.requireNonNull(reservation, "reservation");
        TransportReservation previous = reservations.put(endpointId, reservation);
        reservedTransportCapacity = adjustReservedCapacity(
                reservedTransportCapacity,
                previous == null ? 0.0 : previous.reservedTransportCapacity(),
                reservation.reservedTransportCapacity());
        return !reservation.equals(previous);
    }

    /** Internal authority hook used by TeamTown. */
    public boolean removeReservation(TransportEndpointId endpointId) {
        if (endpointId == null) {
            return false;
        }
        TransportReservation removed = reservations.remove(endpointId);
        if (removed == null) {
            return false;
        }
        reservedTransportCapacity = adjustReservedCapacity(
                reservedTransportCapacity, removed.reservedTransportCapacity(), 0.0);
        return true;
    }

    public double getReservedTransportCapacity() {
        return reservedTransportCapacity;
    }

    public double getRemainingRegistrableCapacity(double totalCapacity) {
        return TransportReservationModel.isFiniteNonNegative(totalCapacity)
                ? Math.max(0.0, totalCapacity - getReservedTransportCapacity())
                : Double.NaN;
    }

    public double getTransportCapacityShortfall(double totalCapacity) {
        return TransportReservationModel.isFiniteNonNegative(totalCapacity)
                ? Math.max(0.0, getReservedTransportCapacity() - totalCapacity)
                : Double.NaN;
    }

    public double getEffectiveRateScale(double totalCapacity) {
        return TransportReservationModel.effectiveRateScale(totalCapacity, getReservedTransportCapacity());
    }

    /** Rebuilds the non-persisted capacity cache from the current parameter snapshot. */
    public boolean recalculateReservedCapacities(TransportConsumerParameters parameters) {
        Objects.requireNonNull(parameters, "parameters");
        if (parameters.equals(appliedParameters)) {
            return false;
        }
        boolean changed = false;
        double recalculatedTotal = 0.0;
        var iterator = reservations.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<TransportEndpointId, TransportReservation> entry = iterator.next();
            try {
                TransportReservation recalculated = entry.getValue().recalculateReservedCapacity(parameters);
                recalculatedTotal += recalculated.reservedTransportCapacity();
                if (!recalculated.equals(entry.getValue())) {
                    entry.setValue(recalculated);
                    changed = true;
                }
            } catch (IllegalArgumentException exception) {
                FHMain.LOGGER.warn("Discarding invalid transport reservation for {}: {}",
                        entry.getKey(), exception.getMessage());
                iterator.remove();
                changed = true;
            }
        }
        reservedTransportCapacity = recalculatedTotal;
        appliedParameters = parameters;
        return changed;
    }

    /** Replaces client-side transport data from one validated authoritative snapshot. */
    public boolean applySnapshot(TownTransportSnapshot snapshot) {
        TownTransportSnapshot next = snapshot == null ? TownTransportSnapshot.EMPTY : snapshot;
        Map<TransportEndpointId, TransportReservation> replacement = new TreeMap<>(
                TransportEndpointId.STABLE_COMPARATOR);
        for (ReservationEntry entry : next.reservations()) {
            replacement.put(entry.endpointId(), entry.reservation());
        }
        boolean changed = !dailyReport.equals(next.dailyReport()) || !reservations.equals(replacement);
        dailyReport = next.dailyReport();
        reservations.clear();
        reservations.putAll(replacement);
        reservedTransportCapacity = sumReservedCapacity(reservations.values());
        appliedParameters = null;
        return changed;
    }

    public static TownTransportState fromSnapshot(TownTransportSnapshot snapshot) {
        TownTransportState state = new TownTransportState();
        state.applySnapshot(snapshot);
        return state;
    }

    public List<ReservationEntry> getSnapshotEntries() {
        return getReservationEntries();
    }

    private List<ReservationEntry> getReservationEntries() {
        List<ReservationEntry> entries = new ArrayList<>(reservations.size());
        reservations.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(TransportEndpointId.STABLE_COMPARATOR))
                .forEach(entry -> entries.add(new ReservationEntry(entry.getKey(), entry.getValue())));
        return entries;
    }

    private static Map<TransportEndpointId, TransportReservation> normalizeEntries(
            Collection<ReservationEntry> entries
    ) {
        Map<TransportEndpointId, Integer> counts = new HashMap<>();
        for (ReservationEntry entry : entries == null ? List.<ReservationEntry>of() : entries) {
            if (entry != null) {
                counts.merge(entry.endpointId(), 1, Integer::sum);
            }
        }
        List<ReservationEntry> uniqueEntries = new ArrayList<>();
        for (ReservationEntry entry : entries == null ? List.<ReservationEntry>of() : entries) {
            if (entry == null) {
                continue;
            }
            if (counts.getOrDefault(entry.endpointId(), 0) != 1) {
                continue;
            }
            uniqueEntries.add(entry);
        }
        uniqueEntries.sort(java.util.Comparator.comparing(
                ReservationEntry::endpointId, TransportEndpointId.STABLE_COMPARATOR));
        Map<TransportEndpointId, TransportReservation> normalized = new LinkedHashMap<>();
        for (ReservationEntry entry : uniqueEntries) {
            normalized.put(entry.endpointId(), entry.reservation());
        }
        counts.entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .sorted(Map.Entry.comparingByKey(TransportEndpointId.STABLE_COMPARATOR))
                .forEach(entry -> FHMain.LOGGER.warn(
                        "Discarding {} conflicting transport reservations for endpoint {}.",
                        entry.getValue(), entry.getKey()));
        return normalized;
    }

    private static double sumReservedCapacity(Collection<TransportReservation> reservations) {
        double total = 0.0;
        for (TransportReservation reservation : reservations) {
            total += reservation.reservedTransportCapacity();
        }
        return total;
    }

    private static double adjustReservedCapacity(double total, double previous, double replacement) {
        double adjusted = total - previous + replacement;
        return adjusted <= TransportReservationModel.comparisonTolerance(total, previous)
                ? 0.0 : adjusted;
    }
}
