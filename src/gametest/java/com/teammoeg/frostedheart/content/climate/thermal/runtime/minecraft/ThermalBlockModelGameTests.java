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
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import static com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.ThermalLoadedWorldGameTests.*;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.PagePublication;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.DormantChunkThermalState;
import com.teammoeg.frostedheart.content.climate.thermal.profile.minecraft.MinecraftThermalProfiles;
import com.teammoeg.frostedheart.content.climate.thermal.query.QueryPublication;
import com.teammoeg.frostedheart.util.mixin.ICampfireExtra;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Real world capture, source binding, worker publication and chunk checkpoint. */
@GameTestHolder("frostedheart_block_runtime")
@PrefixGameTestTemplate(false)
public final class ThermalBlockModelGameTests {

    private static void fill(ServerLevel level, BlockPos origin, net.minecraft.world.level.block.state.BlockState state) {
        for (int x = 0; x < 8; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++)
            level.setBlockAndUpdate(origin.offset(x, y, z), state);
    }
    private static int brickIndex(BlockPos pos) { return (pos.getX() & 15) >>> 2 | ((pos.getZ() & 15) >>> 2) << 2 | ((pos.getY() & 15) >>> 2) << 4; }
    private static int pageBlock(BlockPos pos) { return (pos.getX() & 15) | (pos.getZ() & 15) << 4 | (pos.getY() & 15) << 8; }
    private static int airSlotAt(ThermalPageHandle page, BlockPos pos) { return page.currentPublication().resolveAirPoint(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15); }
    private static int materialSlot(ThermalPageHandle page, BlockPos pos) {
        var brick = page.currentPublication().brick(brickIndex(pos));
        return brick.firstSlot() + brick.blockLayout().nodeAt((pos.getX() & 3) | (pos.getZ() & 3) << 2 | (pos.getY() & 3) << 4);
    }

    private static void near(GameTestHelper helper, double expected, double actual, String message) {
        helper.assertTrue(Math.abs(expected - actual) < 1e-6, message + ": expected=" + expected + ", actual=" + actual);
    }

    @GameTest(template = "phase0a_empty", batch = "thermal_block_ventilation", timeoutTicks = 900)
    public static void stairAndUnknownBlockAcceptHeatWithoutInternalSubdivision(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftThermalInput.closeActiveLevel(level);
        BlockPos anchor = helper.absolutePos(new BlockPos(2, 4, 2));
        BlockPos target = new BlockPos((anchor.getX() & ~3) + 1,
                (anchor.getY() & ~3) + 2, (anchor.getZ() & ~3) + 1);
        for (Direction direction : Direction.values()) {
            level.setBlockAndUpdate(target.relative(direction), Blocks.STONE.defaultBlockState());
        }
        BlockPos fire = target.below();
        level.setBlockAndUpdate(fire, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        ((ICampfireExtra) level.getBlockEntity(fire)).setLifeTime(20_000);
        level.setBlockAndUpdate(target, Blocks.OAK_STAIRS.defaultBlockState());
        BlockPos unknown = target.east();
        level.setBlockAndUpdate(unknown, Blocks.MOVING_PISTON.defaultBlockState());
        MinecraftThermalInput input = start(level, target);
        var profiles = MinecraftThermalProfiles.prepare();
        helper.assertTrue(profiles.signatures().ventilation(profiles.states().signatureId(level.getBlockState(target))) == 75,
                "stair must use numeric ventilation 75");
        helper.assertTrue(profiles.signatures().ventilation(profiles.states().signatureId(level.getBlockState(unknown))) == 75,
                "unsupported moving-piston shape must use numeric ventilation 75");
        QueryPublication queries = read(input, "queryPublication");
        QueryPublication.MutableSample sample = new QueryPublication.MutableSample();
        long[] previousRevision = {-1};
        double[] initialStairTemperature = {Double.NaN};
        helper.startSequence().thenWaitUntil(() -> {
            PagePublication publication = publication(helper, level, target);
            PagePublication.Brick brick = publication.brickAt(target.getX() & 15, target.getY() & 15, target.getZ() & 15);
            int stairSlot = materialSlot(owner(level, target).page(), target);
            int unknownSlot = materialSlot(owner(level, unknown).page(), unknown);
            helper.assertTrue(stairSlot >= 0 && unknownSlot >= 0 && stairSlot != unknownSlot,
                    "each ventilation-75 body must have its own material state");
            helper.assertTrue(publication.resolveAirPoint(target.getX() & 15, target.getY() & 15, target.getZ() & 15) < 0,
                    "stair temperature is not an air transport temperature");
            var reader = new QueryPublication.ReadCursor();
            boolean read;
            try { read = queries.beginRead(reader) && reader.tryRead(publication,
                    (target.getX() & 15) | (target.getZ() & 15) << 4 | (target.getY() & 15) << 8, publication.topologyGeneration(), sample); }
            finally { reader.clear(); }
            helper.assertTrue(read, "stair temperature must be published");
            if (Double.isNaN(initialStairTemperature[0])) initialStairTemperature[0] = sample.temperatureC();
            helper.assertTrue(sample.temperatureC() > initialStairTemperature[0] + 1e-4,
                    "normal campfire source must heat the stair body: " + sample.temperatureC());
        }).thenExecute(() -> {
            PagePublication publication = publication(helper, level, target);
            previousRevision[0] = publication.geometryRevision();
            captureRoundTrip(helper, level, target, publication, queries);
            // Two centers in one Brick exercise grouped capture; ordinary Air may
            // merge again only after the ventilation restrictions are removed.
            level.setBlockAndUpdate(target, Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(unknown, Blocks.AIR.defaultBlockState());
        }).thenWaitUntil(() -> {
            PagePublication publication = publication(helper, level, target);
            helper.assertTrue(publication.geometryRevision() > previousRevision[0], "both block changes must reach the worker");
            int a = publication.resolveAirPoint(target.getX() & 15, target.getY() & 15, target.getZ() & 15);
            int b = publication.resolveAirPoint(unknown.getX() & 15, unknown.getY() & 15, unknown.getZ() & 15);
            helper.assertTrue(a >= 0 && a == b, "adjacent ordinary Air must merge after the actual world mutation");
        }).thenExecute(() -> {
            level.setBlockAndUpdate(fire, Blocks.AIR.defaultBlockState());
            MinecraftThermalInput.closeActiveLevel(level);
        }).thenSucceed();
    }

    private static PagePublication publication(GameTestHelper helper, ServerLevel level, BlockPos position) {
        var handle = owner(level, position).page();
        helper.assertTrue(handle != null, "source must admit the target Page");
        var publication = handle.currentPublication();
        helper.assertTrue(publication != null, "latest world mutation must have a coherent publication");
        return publication;
    }

    private static void captureRoundTrip(GameTestHelper helper, ServerLevel level, BlockPos position,
            PagePublication publication, QueryPublication queries) {
        int sectionY = position.getY() >> 4;
        var state = new DormantChunkThermalState(level.getMinSection(), level.getSectionsCount());
        var cursor = new QueryPublication.ReadCursor();
        try {
            helper.assertTrue(queries.beginRead(cursor), "heated production publication must be readable");
            long tick = cursor.pageAirSampleTick(publication);
            helper.assertTrue(tick >= 0, "checkpoint must match the published Air identities");
            var air = cursor.captureValidatedAir(publication, 0);
            helper.assertTrue(air != null && cursor.isCurrent(), "heated production field must be checkpointed");
            state.replace(sectionY, new DormantChunkThermalState.SectionEntry(tick, 0, air));
        } finally { cursor.clear(); }
        CompoundTag tag = new CompoundTag();
        state.encode(tag);
        var restored = DormantChunkThermalState.decode(tag, level.getMinSection(), level.getSectionsCount());
        CompoundTag encodedAgain = new CompoundTag();
        restored.encode(encodedAgain);
        helper.assertTrue(tag.equals(encodedAgain), "new chunk format must round-trip spatial masks and residuals");
    }
}
