/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.network;

import com.teammoeg.frostedheart.content.town.observation.TownOperationalStatus;
import com.teammoeg.frostedheart.content.town.observation.TownSignalEvent;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

final class TownOperationalStatusPacketCodec {
    private TownOperationalStatusPacketCodec() {
    }

    static void write(FriendlyByteBuf buffer, TownOperationalStatus status) {
        buffer.writeLong(status.serverGameTime());
        buffer.writeVarInt(status.population());
        buffer.writeDouble(status.averageHealth());
        buffer.writeDouble(status.p10Health());
        buffer.writeDouble(status.averageMental());
        buffer.writeDouble(status.p10Mental());
        buffer.writeVarInt(status.unableToWorkCount());
        buffer.writeVarInt(status.exitRiskCount());
        buffer.writeVarInt(status.homelessCount());
        buffer.writeVarInt(status.unemployedCount());
        writeMetric(buffer, status.foodReserveDays());
        writeMetric(buffer, status.fuelReserveDays());
        writeMetric(buffer, status.minimumHouseTemperatureCelsius());
        writeMetric(buffer, status.minimumBuildingTemperatureCelsius());
        buffer.writeVarInt(status.unsafeOccupiedHouseCount());
        buffer.writeVarInt(status.stoppedStaffedHuntingCount());
        buffer.writeEnum(status.tower().kind());
        buffer.writeBoolean(status.tower().enabled());
        buffer.writeBoolean(status.tower().active());
        buffer.writeBoolean(status.tower().broken());
        buffer.writeBoolean(status.tower().overdrive());
        buffer.writeDouble(status.tower().overdriveFraction());
        buffer.writeVarInt(status.climateLevel());
        buffer.writeCollection(status.activeAlerts(), (target, alert) -> {
            target.writeEnum(alert.type());
            target.writeEnum(alert.severity());
            target.writeVarInt(alert.affectedCount());
        });
    }

    static TownOperationalStatus read(FriendlyByteBuf buffer) {
        long serverTime = buffer.readLong();
        int population = buffer.readVarInt();
        double averageHealth = buffer.readDouble();
        double p10Health = buffer.readDouble();
        double averageMental = buffer.readDouble();
        double p10Mental = buffer.readDouble();
        int unable = buffer.readVarInt();
        int exitRisk = buffer.readVarInt();
        int homeless = buffer.readVarInt();
        int unemployed = buffer.readVarInt();
        TownOperationalStatus.Metric food = readMetric(buffer);
        TownOperationalStatus.Metric fuel = readMetric(buffer);
        TownOperationalStatus.Metric houseTemperature = readMetric(buffer);
        TownOperationalStatus.Metric huntingTemperature = readMetric(buffer);
        int unsafeHouses = buffer.readVarInt();
        int stoppedHunting = buffer.readVarInt();
        TownOperationalStatus.TowerStatus tower = new TownOperationalStatus.TowerStatus(
                buffer.readEnum(TownOperationalStatus.TowerKind.class), buffer.readBoolean(),
                buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean(), buffer.readDouble());
        int climateLevel = buffer.readVarInt();
        int alertCount = buffer.readVarInt();
        List<TownOperationalStatus.ActiveAlert> alerts = new ArrayList<>(alertCount);
        for (int index = 0; index < alertCount; index++) {
            alerts.add(new TownOperationalStatus.ActiveAlert(
                    buffer.readEnum(TownSignalEvent.Type.class),
                    buffer.readEnum(TownSignalEvent.Severity.class), buffer.readVarInt()));
        }
        return new TownOperationalStatus(serverTime, population, averageHealth, p10Health,
                averageMental, p10Mental, unable, exitRisk, homeless, unemployed, food, fuel,
                houseTemperature, huntingTemperature, unsafeHouses, stoppedHunting, tower,
                climateLevel, alerts);
    }

    private static void writeMetric(FriendlyByteBuf buffer, TownOperationalStatus.Metric metric) {
        buffer.writeBoolean(metric.available());
        if (metric.available()) buffer.writeDouble(metric.value());
    }

    private static TownOperationalStatus.Metric readMetric(FriendlyByteBuf buffer) {
        return buffer.readBoolean() ? TownOperationalStatus.Metric.available(buffer.readDouble())
                : TownOperationalStatus.Metric.unavailable();
    }
}
