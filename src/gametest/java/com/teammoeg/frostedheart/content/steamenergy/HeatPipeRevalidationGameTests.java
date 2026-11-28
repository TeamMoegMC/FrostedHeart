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

package com.teammoeg.frostedheart.content.steamenergy;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.content.steamenergy.pipe.CPipeBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Counts real revalidator calls without losing coverage of the native topology paths. */
@GameTestHolder("frostedheart_heat_pipe_revalidation")
@PrefixGameTestTemplate(false)
public final class HeatPipeRevalidationGameTests {
    private static final String TEMPLATE = "phase0a_empty";
    // Keep a controlled pipe face for the fallback tests; notify it explicitly in the event test.
    private static final int FIXTURE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    private HeatPipeRevalidationGameTests() {}

    private static final class ObservedNetwork extends HeatNetwork {
        int fallbackScans;
        int directConnections;

        @Override
        public void startConnectionFromBlock(BlockEntity block) {
            fallbackScans++;
            super.startConnectionFromBlock(block);
        }

        @Override
        public void startConnectionFromBlock(BlockEntity block, Direction face) {
            directConnections++;
            super.startConnectionFromBlock(block, face);
        }
    }

    @GameTest(template = TEMPLATE)
    public static void fallbackCadenceDoesNotDelayMergeOrRemoval(GameTestHelper h) {
        var level = h.getLevel();
        var root = h.absolutePos(new BlockPos(3, 3, 3));
        var leftEnd = root.east(2);
        var bridge = root.east(3);
        var rightEnd = root.east(4);
        put(level, root);
        put(level, root.east());
        HeatPipeTileEntity left = put(level, leftEnd);
        HeatPipeTileEntity right = put(level, rightEnd);
        ObservedNetwork larger = new ObservedNetwork();
        ObservedNetwork smaller = new ObservedNetwork();
        larger.setReconnector(() -> connectFrom(larger, level, root, Direction.WEST));
        smaller.setReconnector(() -> connectFrom(smaller, level, rightEnd, Direction.EAST));
        larger.tick(level);
        smaller.tick(level);
        h.assertTrue(left.getNetwork() == larger && larger.getNetworkSize() == 3 && smaller.getNetworkSize() == 1,
                "start with two separately owned networks");
        for (int block = 0; block < 2; block++) {
            int before = larger.fallbackScans;
            for (int i = 0; i < 100; i++) left.tick();
            h.assertTrue(larger.fallbackScans - before == 10, "ten fallback scans per 100 ticks, window " + block);
        }
        FHMain.LOGGER.info("HEAT_PIPE_REVALIDATION two100Windows={} total", larger.fallbackScans);
        int steadyScans = larger.fallbackScans;
        for (int i = 0; i < 9; i++) left.tick();
        h.assertTrue(larger.fallbackScans == steadyScans, "no fallback before the next ten-tick interval");

        var middle = put(level, bridge);
        h.assertTrue(middle.getNetwork() == null, "addition in fixture suppresses automatic neighbor notification");
        left.onFaceChange(Direction.EAST, true);
        h.assertTrue(larger.directConnections == 1 && middle.getNetwork() == larger && right.getNetwork() == larger,
                "the face event joins both networks immediately, before the periodic scan");
        h.assertTrue(smaller.isUpdateRequested(), "old owner queued existing cleanup");
        h.assertTrue(larger.fallbackScans == steadyScans, "direct change is not throttled by the fallback counter");

        // A real block-entity removal, with the external face updates suppressed.
        level.setBlock(bridge, Blocks.AIR.defaultBlockState(), FIXTURE_FLAGS);
        h.assertTrue(larger.isUpdateRequested(), "native pipe removal queued a rebuild");
        for (int i = 0; i <= 10; i++) larger.tick(level);
        h.assertTrue(left.getNetwork() == larger && right.getNetwork() == null && larger.getNetworkSize() == 3,
                "rebuild keeps only the still-reachable branch");

        middle = put(level, bridge);
        left.onFaceChange(Direction.EAST, true);
        h.assertTrue(middle.getNetwork() == larger && right.getNetwork() == larger && larger.directConnections == 2,
                "reconnection through the real face event remains immediate");
        left.onFaceChange(Direction.EAST, false);
        h.assertTrue(larger.isUpdateRequested(), "closing a face still schedules topology refresh");
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = "heat_pipe_unloaded_boundary", timeoutTicks = 600)
    public static void fallbackDoesNotLoadAnAbsentNeighborAndRejoinsAfterReload(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        var anchor = h.absolutePos(new BlockPos(1, 3, 1)).east(1024);
        var target = new BlockPos((anchor.getX() >> 4) << 4, anchor.getY(), anchor.getZ());
        int x = target.getX() >> 4, z = target.getZ() >> 4;
        level.setChunkForced(x, z, true);
        HeatPipeTileEntity original = put(level, target);
        // Drive an actual connector tick explicitly, so the surrounding simulation does not
        // keep the target loaded just to tick the test's source connector.
        var source = new HeatPipeTileEntity(target.west(), FHBlocks.HEAT_PIPE.get().defaultBlockState()
                .setValue(CPipeBlock.EAST, true));
        source.setLevel(level);
        ObservedNetwork network = new ObservedNetwork();
        source.setNetwork(network);
        h.runAfterDelay(5, () -> level.setChunkForced(x, z, false));

        h.startSequence().thenWaitUntil(() -> h.assertTrue(original.isRemoved()
                && level.getChunkSource().getChunkNow(x, z) == null
                && !level.hasChunkAt(target), "target chunk and block entity not unloaded yet")).thenExecute(() -> {
            for (int i = 0; i < 10; i++) source.tick();
            h.assertTrue(network.fallbackScans == 1 && network.getNetworkSize() == 0
                    && level.getChunkSource().getChunkNow(x, z) == null,
                    "periodic probe ran across the border without loading the chunk");
            level.setChunkForced(x, z, true);
            try {
                var fresh = (HeatPipeTileEntity) level.getBlockEntity(target);
                h.assertTrue(fresh != null && fresh != original && fresh.getNetwork() == null,
                        "reload created a fresh pipe; the face did not change");
                for (int i = 0; i < 9; i++) source.tick();
                h.assertTrue(fresh.getNetwork() == null, "no fallback before ten more ticks");
                source.tick();
                h.assertTrue(network.fallbackScans == 2 && fresh.getNetwork() == network,
                        "next periodic scan finds the reloaded same-face connector");
            } finally {
                level.setChunkForced(x, z, false);
            }
        }).thenSucceed();
    }

    private static HeatPipeTileEntity put(ServerLevel level, BlockPos at) {
        level.setBlock(at, FHBlocks.HEAT_PIPE.get().getAxisState(Direction.Axis.X), FIXTURE_FLAGS);
        return (HeatPipeTileEntity) level.getBlockEntity(at);
    }

    private static void connectFrom(HeatNetwork network, ServerLevel level, BlockPos root, Direction inlet) {
        network.connectTo(level, root, root.relative(inlet), inlet);
    }
}
