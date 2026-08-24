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
import java.util.Objects;

/**
 * Server-owned reservation state. Its map key owns the consumer block position;
 * distance metrics and reserved capacity are derived by the town authority.
 */
public record TransportReservation(
        TransportEndpointKind endpointKind,
        int rateItemsPerSecond,
        double scaleMetric,
        double reservedTransportCapacity,
        TransportAdmissionStatus admissionStatus
) {
    /** Codec intentionally excludes the derived reserved capacity cache. */
    public static final Codec<TransportReservation> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            TransportEndpointKind.CODEC.fieldOf("endpointKind").forGetter(TransportReservation::endpointKind),
            Codec.INT.fieldOf("rateItemsPerSecond").forGetter(TransportReservation::rateItemsPerSecond),
            Codec.DOUBLE.fieldOf("scaleMetric").forGetter(TransportReservation::scaleMetric),
            TransportAdmissionStatus.CODEC.fieldOf("admissionStatus").forGetter(TransportReservation::admissionStatus)
    ).apply(instance, TransportReservation::fromPersisted));

    /** Network snapshot codec includes the server-derived capacity cache. */
    public static final Codec<TransportReservation> SNAPSHOT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            TransportEndpointKind.CODEC.fieldOf("endpointKind").forGetter(TransportReservation::endpointKind),
            Codec.INT.fieldOf("rateItemsPerSecond").forGetter(TransportReservation::rateItemsPerSecond),
            Codec.DOUBLE.fieldOf("scaleMetric").forGetter(TransportReservation::scaleMetric),
            Codec.DOUBLE.fieldOf("reservedTransportCapacity").forGetter(TransportReservation::reservedTransportCapacity),
            TransportAdmissionStatus.CODEC.fieldOf("admissionStatus").forGetter(TransportReservation::admissionStatus)
    ).apply(instance, TransportReservation::new));

    public TransportReservation {
        Objects.requireNonNull(endpointKind, "endpointKind");
        Objects.requireNonNull(admissionStatus, "admissionStatus");
        if (rateItemsPerSecond < 0) {
            throw new IllegalArgumentException("Transport reservation rate must be non-negative.");
        }
        if (!TransportReservationModel.isFiniteNonNegative(scaleMetric)
                || !TransportReservationModel.isFiniteNonNegative(reservedTransportCapacity)) {
            throw new IllegalArgumentException("Transport reservation metrics must be finite and non-negative.");
        }
        if ((admissionStatus == TransportAdmissionStatus.ACTIVE
                || admissionStatus == TransportAdmissionStatus.REDSTONE_PAUSED)
                && rateItemsPerSecond == 0) {
            throw new IllegalArgumentException("ACTIVE and REDSTONE_PAUSED reservations require a non-zero rate.");
        }
        if (admissionStatus == TransportAdmissionStatus.DISABLED
                && rateItemsPerSecond != 0) {
            throw new IllegalArgumentException("DISABLED reservations must have a zero rate.");
        }
        if (admissionStatus == TransportAdmissionStatus.DISABLED
                && Double.compare(reservedTransportCapacity, 0.0) != 0) {
            throw new IllegalArgumentException("DISABLED reservations cannot reserve capacity.");
        }
        if (admissionStatus == TransportAdmissionStatus.REDSTONE_PAUSED
                && endpointKind != TransportEndpointKind.P2P_DIRECT_LINK) {
            throw new IllegalArgumentException("REDSTONE_PAUSED is only valid for P2P direct links.");
        }
        if (admissionStatus == TransportAdmissionStatus.REDSTONE_PAUSED
                && Double.compare(reservedTransportCapacity, 0.0) != 0) {
            throw new IllegalArgumentException("REDSTONE_PAUSED reservations cannot reserve capacity.");
        }
        if (admissionStatus == TransportAdmissionStatus.UNAVAILABLE
                && (Double.compare(scaleMetric, 0.0) != 0
                || Double.compare(reservedTransportCapacity, 0.0) != 0)) {
            throw new IllegalArgumentException("UNAVAILABLE reservations require zero derived metrics.");
        }
    }

    public static TransportReservation fromPersisted(
            TransportEndpointKind endpointKind,
            int rateItemsPerSecond,
            double scaleMetric,
            TransportAdmissionStatus admissionStatus
    ) {
        return new TransportReservation(endpointKind, rateItemsPerSecond,
                scaleMetric, 0.0, admissionStatus);
    }

    public TransportReservation recalculateReservedCapacity(TransportConsumerParameters parameters) {
        double recalculated = admissionStatus == TransportAdmissionStatus.ACTIVE
                ? TransportReservationModel.capacityForStoredRate(
                endpointKind, rateItemsPerSecond, scaleMetric, parameters)
                : 0.0;
        if (!TransportReservationModel.isFiniteNonNegative(recalculated)) {
            throw new IllegalArgumentException("Reservation cannot be recalculated with the supplied parameters.");
        }
        return new TransportReservation(endpointKind, rateItemsPerSecond,
                scaleMetric, recalculated, admissionStatus);
    }
}
