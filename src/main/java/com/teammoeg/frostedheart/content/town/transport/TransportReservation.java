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
import net.minecraft.core.GlobalPos;

import java.util.Objects;

/**
 * Server-owned reservation state. Its map key owns the consumer block position;
 * {@code boundWarehouseCorePos} identifies the warehouse that supplies its scale metric.
 */
public record TransportReservation(
        TransportEndpointKind endpointKind,
        GlobalPos boundWarehouseCorePos,
        int rateItemsPerSecond,
        double scaleMetric,
        double reservedTransportCapacity,
        TransportAdmissionStatus admissionStatus
) {
    /** Codec intentionally excludes the derived reserved capacity cache. */
    public static final Codec<TransportReservation> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            TransportEndpointKind.CODEC.fieldOf("endpointKind").forGetter(TransportReservation::endpointKind),
            GlobalPos.CODEC.fieldOf("boundWarehouseCorePos").forGetter(TransportReservation::boundWarehouseCorePos),
            Codec.INT.fieldOf("rateItemsPerSecond").forGetter(TransportReservation::rateItemsPerSecond),
            Codec.DOUBLE.fieldOf("scaleMetric").forGetter(TransportReservation::scaleMetric),
            TransportAdmissionStatus.CODEC.fieldOf("admissionStatus").forGetter(TransportReservation::admissionStatus)
    ).apply(instance, TransportReservation::fromPersisted));

    /** Network snapshot codec includes the server-derived capacity cache. */
    public static final Codec<TransportReservation> SNAPSHOT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            TransportEndpointKind.CODEC.fieldOf("endpointKind").forGetter(TransportReservation::endpointKind),
            GlobalPos.CODEC.fieldOf("boundWarehouseCorePos").forGetter(TransportReservation::boundWarehouseCorePos),
            Codec.INT.fieldOf("rateItemsPerSecond").forGetter(TransportReservation::rateItemsPerSecond),
            Codec.DOUBLE.fieldOf("scaleMetric").forGetter(TransportReservation::scaleMetric),
            Codec.DOUBLE.fieldOf("reservedTransportCapacity").forGetter(TransportReservation::reservedTransportCapacity),
            TransportAdmissionStatus.CODEC.fieldOf("admissionStatus").forGetter(TransportReservation::admissionStatus)
    ).apply(instance, TransportReservation::new));

    public TransportReservation {
        Objects.requireNonNull(endpointKind, "endpointKind");
        Objects.requireNonNull(boundWarehouseCorePos, "boundWarehouseCorePos");
        Objects.requireNonNull(admissionStatus, "admissionStatus");
        if (rateItemsPerSecond < 0) {
            throw new IllegalArgumentException("Transport reservation rate must be non-negative.");
        }
        if (!TransportReservationModel.isFiniteNonNegative(scaleMetric)
                || !TransportReservationModel.isFiniteNonNegative(reservedTransportCapacity)) {
            throw new IllegalArgumentException("Transport reservation metrics must be finite and non-negative.");
        }
        if (admissionStatus == TransportAdmissionStatus.ACTIVE && rateItemsPerSecond == 0) {
            throw new IllegalArgumentException("ACTIVE reservations require a non-zero rate.");
        }
        if (admissionStatus == TransportAdmissionStatus.DISABLED
                && rateItemsPerSecond != 0) {
            throw new IllegalArgumentException("DISABLED reservations must have a zero rate.");
        }
    }

    public static TransportReservation fromPersisted(
            TransportEndpointKind endpointKind,
            GlobalPos boundWarehouseCorePos,
            int rateItemsPerSecond,
            double scaleMetric,
            TransportAdmissionStatus admissionStatus
    ) {
        return new TransportReservation(endpointKind, boundWarehouseCorePos,
                rateItemsPerSecond, scaleMetric, 0.0, admissionStatus);
    }

    public boolean hasValidRateInputs(TransportConsumerParameters parameters) {
        return parameters != null
                && parameters.isRateValid(rateItemsPerSecond);
    }

    public TransportReservation recalculateReservedCapacity(TransportConsumerParameters parameters) {
        double recalculated = TransportReservationModel.capacityForStoredRate(
                endpointKind, rateItemsPerSecond, scaleMetric, parameters);
        if (!TransportReservationModel.isFiniteNonNegative(recalculated)) {
            throw new IllegalArgumentException("Reservation cannot be recalculated with the supplied parameters.");
        }
        return new TransportReservation(endpointKind, boundWarehouseCorePos,
                rateItemsPerSecond, scaleMetric, recalculated, admissionStatus);
    }
}
