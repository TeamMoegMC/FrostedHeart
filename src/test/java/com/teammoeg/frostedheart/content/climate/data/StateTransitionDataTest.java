/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.data;

import com.teammoeg.frostedheart.content.climate.PhysicalState;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class StateTransitionDataTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void equalSolidThresholdKeepsLegacyGasPriority() {
        BlockState current = Blocks.BLUE_ICE.defaultBlockState();
        StateTransitionData data = transition(
                current,
                PhysicalState.SOLID,
                Blocks.PACKED_ICE.defaultBlockState(),
                Blocks.ICE.defaultBlockState(),
                9.0F,
                9.0F);

        StateTransitionData.HeatingTransition heating =
                data.heatingTransition(current);

        assertEquals(9.0F, heating.temperatureC());
        assertEquals(PhysicalState.GAS, heating.targetState());
        assertEquals(Blocks.ICE.defaultBlockState(), heating.targetBlock());
    }

    @Test
    void lowerMeltThresholdWinsBeforeLaterSublimation() {
        BlockState current = Blocks.SNOW_BLOCK.defaultBlockState();
        StateTransitionData data = transition(
                current,
                PhysicalState.SOLID,
                Blocks.WATER.defaultBlockState(),
                Blocks.AIR.defaultBlockState(),
                9.0F,
                100.0F);

        StateTransitionData.HeatingTransition heating =
                data.heatingTransition(current);

        assertEquals(9.0F, heating.temperatureC());
        assertEquals(PhysicalState.LIQUID, heating.targetState());
        assertEquals(Blocks.WATER.defaultBlockState(), heating.targetBlock());
    }

    @Test
    void gasAndNoOpTargetsDoNotCreateHotSideProfiles() {
        BlockState current = Blocks.WATER.defaultBlockState();
        StateTransitionData gas = transition(
                current,
                PhysicalState.GAS,
                Blocks.ICE.defaultBlockState(),
                current,
                9.0F,
                100.0F);
        StateTransitionData liquidNoOp = transition(
                current,
                PhysicalState.LIQUID,
                Blocks.ICE.defaultBlockState(),
                current,
                9.0F,
                100.0F);

        assertNull(gas.heatingTransition(current));
        assertNull(liquidNoOp.heatingTransition(current));
    }

    private static StateTransitionData transition(
            BlockState block,
            PhysicalState state,
            BlockState liquid,
            BlockState gas,
            float meltTemperatureC,
            float evaporateTemperatureC
    ) {
        return new StateTransitionData(
                block,
                false,
                state,
                Blocks.ICE.defaultBlockState(),
                liquid,
                gas,
                -10.0F,
                meltTemperatureC,
                -5.0F,
                evaporateTemperatureC,
                1,
                true);
    }
}
