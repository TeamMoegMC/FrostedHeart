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

package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.content.climate.thermal.profile.minecraft.MinecraftThermalProfiles;
import com.teammoeg.frostedheart.content.climate.thermal.query.QueryPublication;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget;
import com.teammoeg.frostedheart.content.climate.thermal.solver.TiledThermalField;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * A runtime that has published nothing yet, as in a new world before anything is resident, still gives infrared its
 * window and natural climate.
 */
@GameTestHolder("frostedheart_climate_cover")
@PrefixGameTestTemplate(false)
public final class InfraredUnpublishedFieldGameTests {
    private InfraredUnpublishedFieldGameTests() {
    }

    @GameTest(template = "phase0a_empty", batch = "infrared_unpublished_field", timeoutTicks = 100)
    public static void anUnpublishedFieldStillSendsTheWindowAndNaturalClimate(GameTestHelper helper) {
        var level = helper.getLevel();
        var profiles = MinecraftThermalProfiles.prepare();
        var budget = new ThermalMemoryBudget(8L << 20);
        ServerPlayer player = FakePlayerFactory.getMinecraft(level);
        var previous = player.position();
        BlockPos origin = helper.absolutePos(new BlockPos(2, 2, 2));
        try (var field = new TiledThermalField(budget, profiles.signatures(), profiles.materials(),
                profiles.tuning().airHeatCapacityJPerBlockK(), profiles.tuning().airMixingWPerBlockK(), 0, 4, 0);
                var infrared = new InfraredCapture()) {
            var publication = QueryPublication.createFixed(budget, 16, field);
            try {
                player.setPos(origin.getX() + .5, origin.getY(), origin.getZ() + .5);
                helper.assertTrue(!publication.beginRead(new QueryPublication.ReadCursor()),
                        "a field that has published nothing cannot be read");
                var full = infrared.capture(null, publication, 1, player, true, 0, 0, 0, new long[12], false,
                        new long[0], 0, 0, -1, -1, 0);
                helper.assertTrue(full != null && full.full() && !full.readable() && full.natural() != null,
                        "a client without a readable baseline gets the window and the natural climate");
                long center = SectionPos.asLong(full.centerChunkX(), full.centerSectionY(), full.centerChunkZ());
                var kept = infrared.capture(null, publication, 1, player, false, 1, center, 5, new long[12], true,
                        new long[0], full.storedEpoch(), full.storedSampleTick(), -1, -1, full.natural().revision());
                helper.assertTrue(kept == null, "a readable baseline is kept rather than mistaken for deleted");
            } finally {
                publication.close();
            }
        } finally {
            player.setPos(previous.x, previous.y, previous.z);
        }
        helper.succeed();
    }
}
