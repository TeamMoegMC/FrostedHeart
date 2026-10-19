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
package com.teammoeg.frostedresearch.knowledge;

import com.teammoeg.frostedresearch.FRContents;
import com.teammoeg.frostedresearch.FRMain;
import com.teammoeg.frostedresearch.knowledge.definition.ProjectRequirement;
import com.teammoeg.frostedresearch.knowledge.experiment.ExperimentConditions;
import com.teammoeg.frostedresearch.knowledge.experiment.ExperimentReport;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Optional;

/**
 * The experiment table's room scan in a real level: a one-block room counts its air and walls, and an opening fails it.
 */
@GameTestHolder(FRMain.MODID)
@PrefixGameTestTemplate(false)
public final class ExperimentSpaceGameTests {
    private ExperimentSpaceGameTests() {
    }

    @GameTest(template = "empty")
    public static void enclosedRoomCountsAirAndWallsAndAnOpeningFailsIt(GameTestHelper helper) {
        BlockPos table = new BlockPos(1, 0, 1);
        helper.setBlock(table, FRContents.Blocks.EXPERIMENT_TABLE.get());
        for (BlockPos wall : List.of(new BlockPos(0, 1, 1), new BlockPos(2, 1, 1), new BlockPos(1, 1, 0), new BlockPos(1, 1, 2), new BlockPos(1, 2, 1)))
            helper.setBlock(wall, Blocks.OAK_PLANKS);
        var planks = new ProjectRequirement.BlockCount(Optional.of(new ResourceLocation("minecraft", "oak_planks")), Optional.empty(), 5);
        var space = new ProjectRequirement.Space(true, 1, 8, Optional.empty(), Optional.empty(), List.of(planks));
        var lines = ExperimentConditions.space(helper.getLevel(), helper.absolutePos(table), space);
        helper.assertTrue(lines.stream().allMatch(ExperimentReport.Line::ok), "the closed room satisfies every line: " + lines);
        helper.assertTrue(lines.get(1).args().get(0).value().equals("1"), "the room holds one air block: " + lines);

        helper.setBlock(new BlockPos(1, 2, 1), Blocks.AIR);
        var open = ExperimentConditions.space(helper.getLevel(), helper.absolutePos(table), space);
        helper.assertTrue(open.get(0).key().equals("space.open") && !open.get(0).ok(), "an opening to the sky is not enclosed: " + open);
        helper.succeed();
    }
}
