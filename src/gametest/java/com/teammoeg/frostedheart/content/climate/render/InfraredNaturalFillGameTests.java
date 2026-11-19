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

package com.teammoeg.frostedheart.content.climate.render;

import com.teammoeg.frostedheart.content.climate.ClimateCover;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.gamedata.climate.WorldClimate;
import com.teammoeg.frostedheart.content.climate.network.FHRequestInfraredViewDataSyncPacket;
import com.teammoeg.frostedheart.content.climate.network.FHResponseInfraredViewDataSyncPacket;
import com.teammoeg.frostedheart.content.climate.network.InfraredBrickCodec;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftThermalInput;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftThermalInput.InfraredSnapshot;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Infrared shows the natural temperature wherever the server sends no value, computed from the client's own world. */
@GameTestHolder("frostedheart_climate_cover")
@PrefixGameTestTemplate(false)
public final class InfraredNaturalFillGameTests {
    private InfraredNaturalFillGameTests() {
    }

    @GameTest(template = "phase0a_empty", batch = "infrared_natural_climate", timeoutTicks = 100)
    public static void repliesCarryTheNaturalClimateUntilTheClientHoldsIt(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        // Without a live publication a repeated request has nothing new but the natural climate.
        MinecraftThermalInput.closeActiveLevel(level);
        BlockPos origin = helper.absolutePos(new BlockPos(2, 2, 2));
        ServerPlayer player = FakePlayerFactory.getMinecraft(level);
        var previous = player.position();
        try {
            player.setPos(origin.getX() + .5, origin.getY(), origin.getZ() + .5);
            InfraredSnapshot full = MinecraftThermalInput.captureInfraredSnapshot(player, request(null, 0));
            helper.assertTrue(full != null && full.natural() != null, "a reply to a client that holds nothing carries it");
            var natural = full.natural();
            int limit = ClimateCover.limit();
            helper.assertTrue(natural.coverLevels() == limit + 1, "one value per cover up to the limit");
            int windowChunk = (origin.getX() >> 4) - full.centerChunkX() + 4 + 9 * ((origin.getZ() >> 4) - full.centerChunkZ() + 4);
            for (int cover = 0; cover <= limit; cover++)
                helper.assertTrue(natural.climate(windowChunk, cover) == WorldClimate.getCoveredTemp(level, origin, cover),
                        "the window's climate matches the server's at cover " + cover);
            helper.assertTrue(natural.noUnderground() == WorldTemperature.hasNoUnderground(level)
                            && natural.absoluteZeroC() == FHConfig.SERVER.CLIMATE.absoluteZeroCelsius.get().floatValue(),
                    "the superflat flag and the lower clamp are the server's");
            // Analytic fields refresh on every reply, so only a window without them can be quiet.
            if (full.fieldPages().length == 0)
                helper.assertTrue(MinecraftThermalInput.captureInfraredSnapshot(player, request(full, natural.revision())) == null,
                        "a client that holds this revision and nothing else changed gets no reply");
            InfraredSnapshot stale = MinecraftThermalInput.captureInfraredSnapshot(player, request(full, natural.revision() + 1));
            helper.assertTrue(stale != null && natural.equals(stale.natural()), "a client with another revision gets it again");
            var wire = new FriendlyByteBuf(Unpooled.buffer());
            try {
                new FHResponseInfraredViewDataSyncPacket(9, full, 0).encode(wire);
                var decoded = new FHResponseInfraredViewDataSyncPacket(wire);
                helper.assertTrue(natural.equals(decoded.snapshot().natural()), "the first part carries it over the wire");
            } finally {
                wire.release();
            }
        } finally {
            player.setPos(previous.x, previous.y, previous.z);
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "infrared_natural_fill", timeoutTicks = 100)
    public static void filledBlocksReadTheirNaturalAirAndFollowCoverChanges(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
        BlockPos floor = origin, roofed = origin.east(2);
        level.setBlockAndUpdate(floor, Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(roofed, Blocks.DIRT.defaultBlockState());
        level.setBlockAndUpdate(roofed.above(2), Blocks.GLASS.defaultBlockState());
        ServerPlayer player = FakePlayerFactory.getMinecraft(level);
        var previous = player.position();
        try {
            player.setPos(origin.getX() + .5, origin.getY(), origin.getZ() + .5);
            InfraredSnapshot full = MinecraftThermalInput.captureInfraredSnapshot(player, request(null, 0));
            helper.assertTrue(full != null && full.natural() != null, "the reply carries the natural climate");
            long center = SectionPos.asLong(full.centerChunkX(), full.centerSectionY(), full.centerChunkZ());
            var fill = new InfraredNaturalFill();
            fill.recenter(center);
            fill.accept(full.natural());
            for (int round = 0; round < 32 && fill.advance(level); round++) fill.takeComputed();
            compareSection(helper, level, fill, center, origin, "before the roof");
            // An opaque block above the glass adds cover to the dirt below, which reaches it only after a recompute.
            BlockPos roof = roofed.above(3);
            level.setBlockAndUpdate(roof, Blocks.STONE.defaultBlockState());
            fill.blockChanged(roof.getX(), roof.getY(), roof.getZ(), true);
            helper.assertTrue(fill.advance(level), "the changed column is computed again");
            compareSection(helper, level, fill, center, origin, "after the roof");
        } finally {
            player.setPos(previous.x, previous.y, previous.z);
        }
        helper.succeed();
    }

    /** Every block of the Section holding {@code origin} reads its natural air; air has no value. */
    private static void compareSection(GameTestHelper helper, ServerLevel level, InfraredNaturalFill fill, long center,
            BlockPos origin, String when) {
        int windowX = (SectionPos.x(center) - 4) << 4, windowY = (SectionPos.y(center) - 4) << 4;
        int windowZ = (SectionPos.z(center) - 4) << 4;
        int minX = origin.getX() & ~15, minY = origin.getY() & ~15, minZ = origin.getZ() & ~15;
        var position = new BlockPos.MutableBlockPos();
        var brickCentre = new BlockPos.MutableBlockPos();
        int compared = 0;
        for (int y = 0; y < 16; y++)
            for (int z = 0; z < 16; z++)
                for (int x = 0; x < 16; x++) {
                    position.set(minX + x, minY + y, minZ + z);
                    brickCentre.set(minX + (x & ~3) + 2, minY + (y & ~3) + 2, minZ + (z & ~3) + 2);
                    // The fill samples the biome at Brick centres, as the field's background does.
                    if (level.getBiome(position).value() != level.getBiome(brickCentre).value()) continue;
                    short actual = fill.at(InfraredNaturalFill.texel(position.getX() - windowX,
                            position.getY() - windowY, position.getZ() - windowZ));
                    if (level.getBlockState(position).isAir()) {
                        helper.assertTrue(actual == InfraredBrickCodec.INVALID_TEMPERATURE, "air has no value at " + position);
                        continue;
                    }
                    short expected = InfraredBrickCodec.quantize(WorldTemperature.naturalAir(level, position));
                    helper.assertTrue(Math.abs(expected - actual) <= 1, when + ": " + position + " reads its natural air "
                            + expected * .25 + ", got " + actual * .25);
                    compared++;
                }
        helper.assertTrue(compared > 0, when + ": the Section has blocks to compare");
    }

    /** A request with no display cache; {@code known} null asks for a full reply. */
    private static FHRequestInfraredViewDataSyncPacket request(InfraredSnapshot known, long naturalRevision) {
        if (known == null)
            return new FHRequestInfraredViewDataSyncPacket(-1, true, 0, 0, 0, new long[12], false, new long[0], 0, 0, -1,
                    naturalRevision);
        long center = SectionPos.asLong(known.centerChunkX(), known.centerSectionY(), known.centerChunkZ());
        return new FHRequestInfraredViewDataSyncPacket(-1, false, known.generation(), center, known.infraredEpoch(),
                known.presence().length == 0 ? new long[12] : known.presence(), known.readable(), known.fieldPages(),
                known.storedEpoch(), known.storedSampleTick(), -1, naturalRevision);
    }
}
