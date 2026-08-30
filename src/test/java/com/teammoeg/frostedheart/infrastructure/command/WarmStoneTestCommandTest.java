/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.infrastructure.command;

import com.mojang.brigadier.tree.CommandNode;
import com.teammoeg.frostedheart.content.climate.player.thermalitem.WearableThermalState;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WarmStoneTestCommandTest {
    private static final double EPSILON = 1.0e-6D;

    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void presetsFreezeTheFourDocumentedTemperatureStates() {
        assertTemperatures(WarmStoneTestCommand.TestPreset.COLD.temperaturesAt(17.5D),
                -20.0D, -20.0D);
        assertTemperatures(WarmStoneTestCommand.TestPreset.ENVIRONMENT.temperaturesAt(17.5D),
                17.5D, 17.5D);
        assertTemperatures(WarmStoneTestCommand.TestPreset.HOT.temperaturesAt(17.5D),
                60.0D, 60.0D);
        assertTemperatures(
                WarmStoneTestCommand.TestPreset.CORE_HOT_SURFACE_COLD.temperaturesAt(17.5D),
                60.0D, 0.0D);
    }

    @Test
    void generatedTestStackUsesOnlyTheVersionOneThermalState() {
        ItemStack stack = WarmStoneTestCommand.createTestStack(
                Items.STONE,
                WarmStoneTestCommand.TestPreset.CORE_HOT_SURFACE_COLD,
                12.0D);

        WearableThermalState state = WearableThermalState.read(stack).orElseThrow();
        assertEquals(60.0D, state.coreTemperatureC(), EPSILON);
        assertEquals(0.0D, state.surfaceTemperatureC(), EPSILON);
        assertTrue(stack.getTag().contains(WearableThermalState.ROOT_KEY));
    }

    @Test
    void commandExposesBothReservoirsAllPresetsAndObservationControls() {
        CommandNode<CommandSourceStack> root = WarmStoneTestCommand
                .warmStoneTestCommand().build();

        CommandNode<CommandSourceStack> give = requiredChild(root, "give");
        assertPresets(requiredChild(give, "warm_stone"));
        assertPresets(requiredChild(give, "hot_water_bag"));

        CommandNode<CommandSourceStack> observe = requiredChild(root, "observe");
        CommandNode<CommandSourceStack> start = requiredChild(observe, "start");
        assertNotNull(start.getCommand());
        assertNotNull(requiredChild(start, "interval_ticks").getCommand());
        assertNotNull(requiredChild(observe, "status").getCommand());
        assertNotNull(requiredChild(observe, "stop").getCommand());
    }

    @Test
    void observationLineIncludesPlayerAndIndependentReservoirNodeTemperatures() {
        ItemStack stack = WarmStoneTestCommand.createTestStack(
                Items.STONE,
                WarmStoneTestCommand.TestPreset.CORE_HOT_SURFACE_COLD,
                12.0D);

        String line = WarmStoneTestCommand.observationLine("tester", 240L, 37.25D, stack);

        assertTrue(line.startsWith("FH_WARM_STONE_OBSERVE"));
        assertTrue(line.contains("player=tester"));
        assertTrue(line.contains("game_tick=240"));
        assertTrue(line.contains("player_core_c=37.250"));
        assertTrue(line.contains("reservoir_core_c=60.000"));
        assertTrue(line.contains("reservoir_surface_c=0.000"));
    }

    private static void assertPresets(CommandNode<CommandSourceStack> item) {
        assertNotNull(requiredChild(item, "cold").getCommand());
        assertNotNull(requiredChild(item, "environment").getCommand());
        assertNotNull(requiredChild(item, "hot").getCommand());
        assertNotNull(requiredChild(item, "core_hot_surface_cold").getCommand());
    }

    private static void assertTemperatures(
            WarmStoneTestCommand.TestTemperatures actual,
            double expectedCore,
            double expectedSurface
    ) {
        assertEquals(expectedCore, actual.coreTemperatureC(), EPSILON);
        assertEquals(expectedSurface, actual.surfaceTemperatureC(), EPSILON);
    }

    private static CommandNode<CommandSourceStack> requiredChild(
            CommandNode<CommandSourceStack> parent,
            String name
    ) {
        CommandNode<CommandSourceStack> child = parent.getChild(name);
        assertNotNull(child);
        return child;
    }
}
