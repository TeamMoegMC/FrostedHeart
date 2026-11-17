/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ResolvedGeometryBatch;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputBatch;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputEnvelope;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.TopologyInbox;
import com.teammoeg.frostedheart.content.climate.thermal.source.SpatialSourceTimeline;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("frostedheart_geometry_input_order")
@PrefixGameTestTemplate(false)
public final class ThermalGeometryInputOrderGameTests {
    @GameTest(template = "phase0a_empty", batch = "thermal_geometry_input_order", timeoutTicks = 200)
    public static void adjacentAdmissionsRespectEarlierInputWithoutExistingRegions(GameTestHelper helper) throws ReflectiveOperationException {
        runOrder(helper, false);
    }

    @GameTest(template = "phase0a_empty", batch = "thermal_geometry_input_order", timeoutTicks = 200)
    public static void uninstalledConnectingPagesKeepLaterIslandBehindEarlierGeometry(GameTestHelper helper) throws ReflectiveOperationException {
        runOrder(helper, true);
    }

    private static void runOrder(GameTestHelper helper, boolean connectingPages) throws ReflectiveOperationException {
        var level = helper.getLevel();
        MinecraftThermalInput.closeActiveLevel(level);
        var anchor = helper.absolutePos(new BlockPos(8, 8, 8));
        var origin = new BlockPos(anchor.getX() & ~15,
                anchor.getY() & (connectingPages ? ~15 : ~3), anchor.getZ() & (connectingPages ? ~15 : ~3));
        var positions = connectingPages ? new BlockPos[] {origin, origin.east(48), origin.east(16), origin.east(32)}
                : new BlockPos[] {origin, origin.east(16)};
        for (int index = 0; index < positions.length; index++) {
            var first = positions[index];
            int size = connectingPages && index == 0 ? 16 : 4;
            for (int x = 0; x < size; x++) for (int y = 0; y < size; y++) for (int z = 0; z < size; z++)
            level.setBlockAndUpdate(first.offset(x, y, z),
                    (x == 0 || y == 0 || z == 0 || x == size - 1 || y == size - 1 || z == size - 1 ? Blocks.STONE : Blocks.AIR).defaultBlockState());
        }
        try (var simulation = new ThermalMaterialBehaviorGameTests.Simulation(helper, origin, 0, 0, 0, 0)) {
            var handles = new ThermalPageHandle[positions.length];
            byte[] sky = new byte[256]; Arrays.fill(sky, (byte) 16);
            for (int index = 0; index < handles.length; index++) {
                var position = positions[index];
                var handle = handles[index] = new ThermalPageHandle(SectionPos.asLong(position), 1);
                long mask = connectingPages && index == 0 ? -1L
                        : 1L << ((position.getX() & 15) >>> 2 | ((position.getZ() & 15) >>> 2) << 2 | ((position.getY() & 15) >>> 2) << 4);
                var admission = new ThermalInputBatch.PageAdmission(handle, 0, mask, mask,
                        simulation.capture.captureBricks(handle.sectionKey(), null, simulation.capture.unresolvedPage(), mask), 0, sky, null);
                var input = new ThermalInputBatch(1, ++simulation.sequence, index == 0 ? 0 : 20,
                        new ThermalInputBatch.PageAdmission[] {admission}, ThermalInputBatch.NO_RETIREMENTS,
                        ThermalInputBatch.NO_RESIDENCY_UPDATES, ResolvedGeometryBatch.EMPTY,
                        ThermalInputBatch.NO_ENVIRONMENT_UPDATES, ThermalInputBatch.NO_PHASE_ACKS, Double.NaN);
                simulation.engine.offerInput(new ThermalInputEnvelope(input, new SpatialSourceTimeline.Window[0]));
            }
            TopologyInbox inbox = ThermalLoadedWorldGameTests.read(simulation.engine, "topologyInputs");
            var intakeLane = simulation.engine.getClass().getDeclaredField("nextWorkLane");
            intakeLane.setAccessible(true);
            for (int round = 0; round < positions.length + 2 && inbox.pendingSectionCount() < positions.length; round++) {
                intakeLane.setInt(simulation.engine, 0);
                simulation.engine.processWorkRound();
            }
            helper.assertTrue(inbox.pendingSectionCount() == positions.length, "All real Page admissions must reach the coordinator before selection");
            helper.assertTrue(handles[0].currentPublication() == null && handles[1].currentPublication() == null,
                    "Neither actual Page may have published during intake");
            // Fair visitation can resume at any Page after an earlier readiness wait.
            // Select the later admission first without changing input timestamps.
            helper.assertTrue(inbox.nextPendingSection() == handles[0].sectionKey(), "Start from the earlier input's real queue position");
            long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(15);
            while (handles[1].currentPublication() == null && System.nanoTime() < deadline)
                if (simulation.engine.hasReadyWork()) simulation.engine.processWorkRound(); else Thread.yield();
            helper.assertTrue(handles[1].currentPublication() != null, "The later real Page must eventually publish");
            helper.assertTrue(handles[0].currentPublication() != null,
                    "A later adjacent admission must not publish ahead of its earlier influencing geometry, even with no installed region");
            simulation.tick = 20;
            simulation.publish(ResolvedGeometryBatch.EMPTY, ThermalInputBatch.NO_PHASE_ACKS);
            FHMain.LOGGER.info("THERMAL_GEOMETRY_INPUT_ORDER_RESULT,oldTick=0,newTick=20,laterPageVisitedFirst=true,earlierPagePublishedFirst=true,uninstalledConnectingPages={}",
                    positions.length - 2);
        }
        helper.succeed();
    }
}
