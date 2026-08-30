/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart.content.climate.player;

import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData.BodyPart;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.ThermalEnvironmentSample;

import lombok.Getter;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.type.ISlotType;

public class HeatingDeviceContext {
    @Getter
    private ServerPlayer player;
    private double physiologicalSeconds;
    private final ThermalEnvironmentSample environmentSample =
            new ThermalEnvironmentSample();
    private final PartClothData clothing = new PartClothData();
    private final HeatingDeviceSlot curiosSlot = new HeatingDeviceSlot();
    private final double[] partTemperatureC = new double[BodyPart.VALUES.length];
    private final double[] powerW = new double[BodyPart.VALUES.length];
    private final double[] dryConductanceWPerK = new double[BodyPart.VALUES.length];
    private final double[] weightedBoundaryWPerK = new double[BodyPart.VALUES.length];
    private final double[] wetConductanceWPerK = new double[BodyPart.VALUES.length];
    private final double[] operativeTemperatureC = new double[BodyPart.VALUES.length];
    private final double[] radiantHeatProof = new double[BodyPart.VALUES.length];
    private final double[] airFraction = new double[BodyPart.VALUES.length];

    HeatingDeviceContext() {
    }

    void reset(
            ServerPlayer player,
            double physiologicalSeconds,
            PlayerTemperatureData data
    ) {
        this.player = player;
        this.physiologicalSeconds = physiologicalSeconds;
        for (BodyPart part : BodyPart.VALUES) {
            int index = part.ordinal();
            partTemperatureC[index] = data.getAbsoluteBodyTempByPart(part);
            powerW[index] = 0.0D;
        }
    }

    public double getBodyTemperatureC(BodyPart part) {
        return partTemperatureC[part.ordinal()];
    }

    public void addPower(BodyPart part, double addedPowerW) {
        if (Double.isFinite(addedPowerW)) {
            powerW[part.ordinal()] += addedPowerW;
        }
    }

    double getPowerW(BodyPart part) {
        return powerW[part.ordinal()];
    }

    ThermalEnvironmentSample environmentSample() {
        return environmentSample;
    }

    PartClothData clothing() {
        return clothing;
    }

    HeatingDeviceSlot curiosSlot(ISlotType slotType) {
        curiosSlot.setCurios(slotType);
        return curiosSlot;
    }

    void setPartEnvironment(
            BodyPart part,
            double dryConductanceWPerK,
            double weightedBoundaryWPerK,
            double wetConductanceWPerK,
            double operativeTemperatureC,
            double radiantHeatProof,
            double airFraction
    ) {
        int index = part.ordinal();
        this.dryConductanceWPerK[index] = dryConductanceWPerK;
        this.weightedBoundaryWPerK[index] = weightedBoundaryWPerK;
        this.wetConductanceWPerK[index] = wetConductanceWPerK;
        this.operativeTemperatureC[index] = operativeTemperatureC;
        this.radiantHeatProof[index] = radiantHeatProof;
        this.airFraction[index] = airFraction;
    }

    double getDryConductanceWPerK(BodyPart part) {
        return dryConductanceWPerK[part.ordinal()];
    }

    double getWeightedBoundaryWPerK(BodyPart part) {
        return weightedBoundaryWPerK[part.ordinal()];
    }

    double getWetConductanceWPerK(BodyPart part) {
        return wetConductanceWPerK[part.ordinal()];
    }

    double getOperativeTemperatureC(BodyPart part) {
        return operativeTemperatureC[part.ordinal()];
    }

    double getRadiantHeatProof(BodyPart part) {
        return radiantHeatProof[part.ordinal()];
    }

    double getAirFraction(BodyPart part) {
        return airFraction[part.ordinal()];
    }

    public double getPassivePowerW(BodyPart part) {
        int index = part.ordinal();
        return weightedBoundaryWPerK[index]
                - dryConductanceWPerK[index] * partTemperatureC[index];
    }

    public double getPhysiologicalSeconds() {
        return physiologicalSeconds;
    }

    public Level getLevel() {
        return player.level();
    }
}
