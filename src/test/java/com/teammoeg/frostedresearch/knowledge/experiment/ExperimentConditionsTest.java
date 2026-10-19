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

/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge.experiment;

import com.teammoeg.frostedresearch.knowledge.definition.ProjectRequirement;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.items.ItemStackHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ExperimentConditionsTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    static ProjectRequirement.Material material(String item, int count) {
        return new ProjectRequirement.Material(Optional.of(new ResourceLocation("minecraft", item)), Optional.empty(), count);
    }

    @Test
    void materialsAreAllottedInOrderAndConsumedTogether() {
        var slots = new ItemStackHandler(3);
        slots.setStackInSlot(0, new ItemStack(Items.OAK_PLANKS, 3));
        slots.setStackInSlot(1, new ItemStack(Items.OAK_PLANKS, 2));
        slots.setStackInSlot(2, new ItemStack(Items.STONE, 1));
        var needs = List.of(material("oak_planks", 4), material("stone", 1));
        var lines = ExperimentConditions.materials(slots, needs);
        assertTrue(lines.stream().allMatch(ExperimentReport.Line::ok));
        assertEquals("4", lines.get(0).args().get(1).value());
        assertTrue(ExperimentConditions.consume(slots, needs));
        assertEquals(0, slots.getStackInSlot(0).getCount());
        assertEquals(1, slots.getStackInSlot(1).getCount());
        assertTrue(slots.getStackInSlot(2).isEmpty());

        // One stack serves one material at a time, so two needs of the same item cannot share it.
        var shared = new ItemStackHandler(3);
        shared.setStackInSlot(0, new ItemStack(Items.OAK_PLANKS, 3));
        var twice = List.of(material("oak_planks", 2), material("oak_planks", 2));
        assertFalse(ExperimentConditions.materials(shared, twice).get(1).ok());
        assertFalse(ExperimentConditions.consume(shared, twice));
        assertEquals(3, shared.getStackInSlot(0).getCount(), "nothing is taken when a material falls short");
    }

    @Test
    void reportsRoundTripThroughNbt() {
        var report = new ExperimentReport(List.of(
                new ExperimentReport.Line("space.block", List.of(ExperimentReport.Arg.key("block.minecraft.furnace"), ExperimentReport.Arg.text(0),
                        ExperimentReport.Arg.text(1)), false),
                new ExperimentReport.Line("space.enclosed", List.of(ExperimentReport.Arg.text(4096)), true)));
        CompoundTag tag = report.write();
        assertEquals(report, ExperimentReport.read(tag));
        assertFalse(report.ok());
        assertEquals(ExperimentReport.EMPTY, ExperimentReport.read(new CompoundTag()));
    }
}
