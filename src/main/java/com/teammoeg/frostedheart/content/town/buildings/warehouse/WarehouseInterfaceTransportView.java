/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.buildings.warehouse;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.content.town.transport.TownTransportSummary;
import com.teammoeg.frostedheart.content.town.transport.TransportAdmissionStatus;
import com.teammoeg.frostedheart.content.town.transport.TransportReservation;
import com.teammoeg.frostedheart.content.town.transport.TransportReservationDecision;
import com.teammoeg.frostedheart.content.town.transport.TransportReservationModel;
import com.teammoeg.frostedheart.content.town.model.TownModelParameters;

import java.util.Optional;

/** Immutable server snapshot for one warehouse interface menu. */
public record WarehouseInterfaceTransportView(
        WarehouseInterfaceTransportStatus status,
        TransportReservationDecision decision,
        int rateItemsPerSecond,
        int maximumRateItemsPerSecond,
        double effectiveRateItemsPerSecond,
        double reservedCapacity,
        double distanceFactor,
        double townTotalCapacity,
        double townRemainingCapacity
) {
    public static final WarehouseInterfaceTransportView EMPTY = empty(
            TownModelParameters.Defaults.TRANSPORT_CONSUMER_MAXIMUM_RATE_ITEMS_PER_SECOND);

    private static final Codec<WarehouseInterfaceTransportView> RAW_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            WarehouseInterfaceTransportStatus.CODEC.fieldOf("status").forGetter(WarehouseInterfaceTransportView::status),
            TransportReservationDecision.CODEC.fieldOf("decision").forGetter(WarehouseInterfaceTransportView::decision),
            Codec.INT.fieldOf("rateItemsPerSecond").forGetter(WarehouseInterfaceTransportView::rateItemsPerSecond),
            Codec.INT.fieldOf("maximumRateItemsPerSecond")
                    .forGetter(WarehouseInterfaceTransportView::maximumRateItemsPerSecond),
            Codec.DOUBLE.fieldOf("effectiveRateItemsPerSecond").forGetter(WarehouseInterfaceTransportView::effectiveRateItemsPerSecond),
            Codec.DOUBLE.fieldOf("reservedCapacity").forGetter(WarehouseInterfaceTransportView::reservedCapacity),
            Codec.DOUBLE.fieldOf("distanceFactor").forGetter(WarehouseInterfaceTransportView::distanceFactor),
            Codec.DOUBLE.fieldOf("townTotalCapacity").forGetter(WarehouseInterfaceTransportView::townTotalCapacity),
            Codec.DOUBLE.fieldOf("townRemainingCapacity").forGetter(WarehouseInterfaceTransportView::townRemainingCapacity)
    ).apply(instance, WarehouseInterfaceTransportView::new));

    public static final Codec<WarehouseInterfaceTransportView> CODEC = RAW_CODEC.flatXmap(
            view -> view.isValid()
                    ? DataResult.success(view)
                    : DataResult.error(() -> "Invalid warehouse interface transport view."),
            view -> view.isValid()
                    ? DataResult.success(view)
                    : DataResult.error(() -> "Invalid warehouse interface transport view."));

    public WarehouseInterfaceTransportView {
        if (status == null || decision == null) {
            throw new IllegalArgumentException("Warehouse interface transport status and decision are required.");
        }
    }

    public static WarehouseInterfaceTransportView from(
            int connectionStatus,
            Optional<TransportReservation> reservation,
            TownTransportSummary summary,
            TransportReservationDecision decision
    ) {
        return from(connectionStatus, reservation, summary, decision,
                TownModelParameters.Defaults.TRANSPORT_CONSUMER_MAXIMUM_RATE_ITEMS_PER_SECOND,
                TownModelParameters.Defaults.TRANSPORT_CONSUMER_WAREHOUSE_DISTANCE_COST_PER_BLOCK);
    }

    public static WarehouseInterfaceTransportView from(
            int connectionStatus,
            Optional<TransportReservation> reservation,
            TownTransportSummary summary,
            TransportReservationDecision decision,
            int maximumRateItemsPerSecond
    ) {
        return from(connectionStatus, reservation, summary, decision,
                maximumRateItemsPerSecond,
                TownModelParameters.Defaults.TRANSPORT_CONSUMER_WAREHOUSE_DISTANCE_COST_PER_BLOCK);
    }

    public static WarehouseInterfaceTransportView from(
            int connectionStatus,
            Optional<TransportReservation> reservation,
            TownTransportSummary summary,
            TransportReservationDecision decision,
            int maximumRateItemsPerSecond,
            double warehouseDistanceCostPerBlock
    ) {
        TownTransportSummary safeSummary = summary == null
                ? new TownTransportSummary(0.0, 0.0, 0.0, 0.0, 1.0)
                : summary;
        if (connectionStatus == WarehouseInterfaceBlockEntity.STATUS_UNBOUND) {
            return empty(maximumRateItemsPerSecond);
        }
        TransportReservation value = reservation == null ? null : reservation.orElse(null);
        WarehouseInterfaceTransportStatus status;
        if (connectionStatus != WarehouseInterfaceBlockEntity.STATUS_WORKING) {
            status = WarehouseInterfaceTransportStatus.WAREHOUSE_UNAVAILABLE;
        } else if (value == null) {
            status = WarehouseInterfaceTransportStatus.DISABLED;
        } else if (value.admissionStatus() == TransportAdmissionStatus.UNAVAILABLE) {
            status = WarehouseInterfaceTransportStatus.WAREHOUSE_UNAVAILABLE;
        } else if (value.admissionStatus() == TransportAdmissionStatus.DISABLED) {
            status = WarehouseInterfaceTransportStatus.DISABLED;
        } else if (TransportReservationModel.meaningfullyGreater(
                safeSummary.reservedCapacity(), safeSummary.totalCapacity())) {
            status = WarehouseInterfaceTransportStatus.THROTTLED;
        } else {
            status = WarehouseInterfaceTransportStatus.ACTIVE;
        }
        int rate = value == null ? 0 : value.rateItemsPerSecond();
        double distanceFactor = value == null
                || value.admissionStatus() == TransportAdmissionStatus.UNAVAILABLE
                ? 0.0
                : 1.0 + warehouseDistanceCostPerBlock * value.scaleMetric();
        return new WarehouseInterfaceTransportView(
                status,
                decision == null ? TransportReservationDecision.INVALID_BINDING : decision,
                rate,
                maximumRateItemsPerSecond,
                status == WarehouseInterfaceTransportStatus.ACTIVE
                        || status == WarehouseInterfaceTransportStatus.THROTTLED
                        ? rate * safeSummary.effectiveRateScale()
                        : 0.0,
                value == null ? 0.0 : value.reservedTransportCapacity(),
                distanceFactor,
                safeSummary.totalCapacity(),
                safeSummary.remainingRegistrableCapacity());
    }

    public boolean isRateLimited() {
        return TransportReservationModel.meaningfullyGreater(
                rateItemsPerSecond, effectiveRateItemsPerSecond);
    }

    public static WarehouseInterfaceTransportView empty(int maximumRateItemsPerSecond) {
        return new WarehouseInterfaceTransportView(
                WarehouseInterfaceTransportStatus.UNBOUND,
                TransportReservationDecision.INVALID_BINDING,
                0, maximumRateItemsPerSecond, 0.0, 0.0, 0.0, 0.0, 0.0);
    }

    private boolean isValid() {
        return maximumRateItemsPerSecond > 0
                && rateItemsPerSecond >= 0
                && TransportReservationModel.isFiniteNonNegative(effectiveRateItemsPerSecond)
                && TransportReservationModel.isFiniteNonNegative(reservedCapacity)
                && TransportReservationModel.isFiniteNonNegative(distanceFactor)
                && TransportReservationModel.isFiniteNonNegative(townTotalCapacity)
                && TransportReservationModel.isFiniteNonNegative(townRemainingCapacity)
                && !TransportReservationModel.meaningfullyGreater(
                effectiveRateItemsPerSecond, rateItemsPerSecond);
    }
}
