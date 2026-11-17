/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureTable;
import com.teammoeg.frostedheart.content.climate.thermal.profile.minecraft.MinecraftThermalProfiles;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget;
import com.teammoeg.frostedheart.content.climate.thermal.solver.TiledThermalField;
import com.teammoeg.frostedheart.content.climate.thermal.source.SpatialEnergyBanks;
import com.teammoeg.frostedheart.content.climate.thermal.source.SpatialPowerTimeline;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Captured-world core screening. This does not run network players or qualify the world integration. */
@GameTestHolder("frostedheart_tiled_scale")
@PrefixGameTestTemplate(false)
public final class TiledThermalScaleGameTests {
    @GameTest(template = "phase0a_empty", batch = "thermal_tiled_scale", timeoutTicks = 2400)
    public static void hundredSeparateBasesWithSixteenSourcesEach(GameTestHelper helper) {
        screen(helper, "100_separate_bases", 100, 12, new int[] {2, 4, 7, 9}, 32768);
    }

    @GameTest(template = "phase0a_empty", batch = "thermal_tiled_scale", timeoutTicks = 2400)
    public static void twentyFiveSharedBasesWithSixtyFourSourcesEach(GameTestHelper helper) {
        screen(helper, "25_shared_bases", 25, 24, new int[] {2, 5, 8, 11, 14, 17, 20, 22}, 40960);
    }

    private static void screen(GameTestHelper helper, String scene, int bases, int width, int[] sourceCoordinates, int worldOrigin) {
        var level = helper.getLevel();
        var profiles = MinecraftThermalProfiles.prepare();
        var budget = new ThermalMemoryBudget(128L << 20);
        var fires = new ArrayList<BlockPos>();
        var origins = new ArrayList<BlockPos>();
        var chunks = new LinkedHashSet<ChunkPos>();
        MinecraftThermalInput.closeActiveLevel(level);
        try (var banks = new SpatialEnergyBanks(budget, 2048, 2048, 0);
                var powers = new SpatialPowerTimeline(budget, banks, 2048, 1, 0);
                var field = new TiledThermalField(budget, profiles.signatures(), profiles.materials(),
                        profiles.tuning().airHeatCapacityJPerBlockK(), profiles.tuning().airMixingWPerBlockK(), 0, 256, 0)) {
            var profile = profiles.tuning().campfire();
            for (int base = 0; base < bases; base++) {
                var origin = new BlockPos(worldOrigin + base % 10 * 48, 192, worldOrigin + base / 10 * 48);
                origins.add(origin);
                for (int x = origin.getX() >> 4; x <= (origin.getX() + width - 1) >> 4; x++)
                    for (int z = origin.getZ() >> 4; z <= (origin.getZ() + width - 1) >> 4; z++) {
                        var chunk = new ChunkPos(x, z);
                        if (chunks.add(chunk)) level.setChunkForced(x, z, true);
                    }
                for (int x = 0; x < width; x++) for (int y = 0; y < 8; y++) for (int z = 0; z < width; z++) {
                    boolean wall = x == 0 || x == width - 1 || y == 0 || y == 7 || z == 0 || z == width - 1;
                    level.setBlockAndUpdate(origin.offset(x, y, z), (wall ? Blocks.STONE : Blocks.AIR).defaultBlockState());
                }
                var localFires = new ArrayList<BlockPos>();
                for (int x : sourceCoordinates) for (int z : sourceCoordinates) {
                    var position = origin.offset(x, 0, z);
                    fires.add(position); localFires.add(position);
                    level.setBlockAndUpdate(position, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
                }
                var captures = new LinkedHashMap<Long, int[]>();
                var masks = new LinkedHashMap<Long, Long>();
                for (int x = 0; x < width; x++) for (int y = 0; y < 8; y++) for (int z = 0; z < width; z++) {
                    var position = origin.offset(x, y, z);
                    long section = SectionPos.asLong(position);
                    var captured = captures.computeIfAbsent(section, ignored -> {
                        int[] values = new int[4096]; Arrays.fill(values, ThermalSignatureTable.UNRESOLVED); return values;
                    });
                    captured[local(position)] = profiles.states().signatureId(level.getBlockState(position));
                    int brick = ((position.getX() & 15) >>> 2) | ((position.getZ() & 15) >>> 2) << 2 | ((position.getY() & 15) >>> 2) << 4;
                    masks.merge(section, 1L << brick, (first, second) -> first | second);
                }
                for (var entry : captures.entrySet())
                    helper.assertTrue(field.admit(entry.getKey(), entry.getValue(), masks.get(entry.getKey()), 0), "Admit every base within the common 128MiB budget");
                for (var position : localFires) {
                    var air = new SpatialEnergyBanks.Address(position.above().asLong(), -1, profile.port(0).targetFace().ordinal());
                    helper.assertTrue(powers.tryRegister(position.asLong(), 1, new SpatialEnergyBanks.Address[] {air, null},
                                    new double[] {profile.port(0).powerShare(), profile.port(1).powerShare()}, profile.ratedPowerW(), true, 0)
                                    && field.updateMixing(position.above().asLong(), 1),
                            "Accept each source, declared radiation loss and mixing footprint before producing J");
                }
            }
            helper.assertTrue(fires.size() == 1600 && powers.sourceCount() == 1600, "Both distributions contain 1600 admitted physical-profile sources");
            var bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
            bean.setThreadCpuTimeEnabled(true); bean.setThreadAllocatedMemoryEnabled(true);
            long thread = Thread.currentThread().getId();
            long cpuNs = 0, allocated = 0, wallNs = 0, maximumStepNs = 0;
            var sample = new TiledThermalField.Sample();
            double initialJ = field.totalEnthalpyJ() + field.externalEnergyJ();
            int reads = 0;
            for (int tick = 20; tick <= 2400; tick += 20) {
                long cpuStart = bean.getCurrentThreadCpuTime(), allocationStart = bean.getThreadAllocatedBytes(thread);
                long started = System.nanoTime();
                if (tick % 200 == 0) {
                    var changed = origins.get((tick / 200 - 1) % bases).offset(width - 1, 2, width / 2);
                    var state = level.getBlockState(changed).is(Blocks.IRON_BLOCK) ? Blocks.STONE.defaultBlockState() : Blocks.IRON_BLOCK.defaultBlockState();
                    helper.assertTrue(level.setBlockAndUpdate(changed, state), "The construction probe must actually change its world block");
                    helper.assertTrue(field.changeCell(changed.asLong(), profiles.states().signatureId(level.getBlockState(changed)),
                            TiledThermalField.Change.REPLACEMENT), "Update one actual construction cell without expanding its build scope");
                }
                if (tick % 400 == 0) {
                    boolean enabled = (tick / 400 & 1) == 0;
                    for (int index = 0; index < fires.size(); index += 97) {
                        var position = fires.get(index);
                        helper.assertTrue(powers.update(position.asLong(), 1, profile.ratedPowerW(), enabled, tick - 20)
                                        && field.updateMixing(position.above().asLong(), enabled ? 1 : -1), "Repeatedly switch source membership without retaining history");
                    }
                }
                helper.assertTrue(field.begin(powers.seal(tick)), "Begin the next bounded numerical epoch");
                long deadline = started + 10_000_000_000L;
                while (!field.advance(System.nanoTime() + 1_000_000L))
                    helper.assertTrue(System.nanoTime() < deadline, "Core epoch completes without building an unbounded connected region");
                helper.assertTrue(field.commit(), "Commit the exact consumed input bank");
                for (int base = 0; base < bases; base++) {
                    var probe = fires.get(base * sourceCoordinates.length * sourceCoordinates.length).above();
                    helper.assertTrue(field.read(probe.asLong(), false, sample) && sample.sampleTick() == tick
                                    && Double.isFinite(sample.temperatureC()) && sample.temperatureC() > 0,
                            "Every base publishes a current, heated Air reading");
                    reads++;
                }
                long elapsed = System.nanoTime() - started;
                if (tick > 600) {
                    wallNs += elapsed; maximumStepNs = Math.max(maximumStepNs, elapsed);
                    cpuNs += bean.getCurrentThreadCpuTime() - cpuStart;
                    allocated += bean.getThreadAllocatedBytes(thread) - allocationStart;
                }
            }
            double ownedJ = field.totalEnthalpyJ() + field.externalEnergyJ() - initialJ;
            double acceptedJ = powers.producedJ() - powers.declaredLossJ();
            helper.assertTrue(powers.unavailableReceiverJ() == 0 && Math.abs(ownedJ - acceptedJ) <= 1e-6 * acceptedJ,
                    "All accepted source J remain owned through local changes and control churn");
            helper.assertTrue(budget.admissionRefusals() == 0 && budget.peakUsedBytes() <= (128L << 20),
                    "Ordinary scale screening admits all bases and sources within the declared core budget");
            FHMain.LOGGER.info("THERMAL_TILED_SCALE_RESULT,scene={},bases={},sources={},sections={},capturedCells={},simulatedTicks=2400,warmupTicks=600,measuredSeconds=90,cpuNs={},wallNs={},allocatedBytes={},maxStepNs={},peakReservedBytes={},refusals={},reads={},geometryCells={},ownedJ={},acceptedJ={},networkPlayers=0,scope=captured_world_core",
                    scene, bases, fires.size(), field.tileCount(), bases * width * width * 8, cpuNs, wallNs, allocated,
                    maximumStepNs, budget.peakUsedBytes(), budget.admissionRefusals(), reads, field.changedCells(), ownedJ, acceptedJ);
        } finally {
            for (var position : fires) level.setBlockAndUpdate(position, Blocks.AIR.defaultBlockState());
            MinecraftThermalInput.closeActiveLevel(level);
            for (var chunk : chunks) level.setChunkForced(chunk.x, chunk.z, false);
        }
        helper.assertTrue(budget.usedBytes() == 0, "The screened source/core storage releases all reservations");
        helper.succeed();
    }

    private static int local(BlockPos position) {
        return (position.getX() & 15) | (position.getZ() & 15) << 4 | (position.getY() & 15) << 8;
    }
}
