/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialSample;
import com.teammoeg.frostedheart.content.climate.thermal.query.ThermalEnvironmentSample;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Real construction at A while B remains occupied; no delays or thermal state injection. */
@GameTestHolder("frostedheart_regional_steady")
@PrefixGameTestTemplate(false)
public final class ContinuousAirRegionalSteadyProductionGameTests {
    @GameTest(template = "phase0a_empty", timeoutTicks = 2500)
    public static void stableBaseAdvancesDuringOtherBaseConstruction(GameTestHelper helper) throws java.io.IOException {
        ContinuousAirPrecisionProductionGameTests.restoreReferenceClimate(helper, true);
        var level = helper.getLevel();
        var origins = new BlockPos[] {helper.absolutePos(new BlockPos(5, 0, 7)), helper.absolutePos(new BlockPos(165, 0, 7))};
        var bases = new ContinuousAirLargeBaseProductionGameTests.BuiltBase[2];
        for (int base = 0; base < 2; base++) {
            var origin = origins[base];
            for (int x = (origin.getX() - 16) >> 4; x <= (origin.getX() + 66) >> 4; x++)
                for (int z = (origin.getZ() - 16) >> 4; z <= (origin.getZ() + 30) >> 4; z++) level.setChunkForced(x, z, true);
        }
        bases[1] = ContinuousAirLargeBaseProductionGameTests.buildBase(helper, origins[1]);
        helper.runAfterDelay(601, () -> bases[0] = ContinuousAirLargeBaseProductionGameTests.buildBase(helper, origins[0]));
        var player = FakePlayerFactory.getMinecraft(level);
        var probe = bases[1].probes()[0];
        player.setPos(probe.getX() + .5, probe.getY(), probe.getZ() + .5);
        var air = new ThermalEnvironmentSample();
        var material = new MaterialSample();
        int[] violations = {0}, geometryViolations = {0}, monitored = {0}, infraredFailures = {0};
        boolean[] bAhead = {false}, describedWorldChange = {false};
        var observedSections = new it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<BlockState[]>();
        helper.runAfterDelay(800, () -> {
            var position = new BlockPos.MutableBlockPos();
            for (var p : bases[1].probes()) for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
                long key = SectionPos.asLong((p.getX() >> 4) + dx, (p.getY() >> 4) + dy, (p.getZ() >> 4) + dz);
                if (observedSections.containsKey(key)) continue;
                var states = new BlockState[4096];
                for (int block = 0; block < states.length; block++) states[block] = level.getBlockState(position.set(
                        (SectionPos.x(key) << 4) + (block & 15), (SectionPos.y(key) << 4) + (block >>> 8), (SectionPos.z(key) << 4) + (block >>> 4 & 15)));
                observedSections.put(key, states);
            }
        });
        for (int delay = 1; delay <= 2400; delay++) {
            int elapsed = delay;
            helper.runAfterDelay(delay, () -> {
                if (elapsed >= 800) for (var position : bases[1].probes()) {
                    MinecraftThermalInput.gameplayPlacedReservoirEnvironment(level, position, WorldTemperature.naturalAir(level, position), air);
                    monitored[0]++;
                    bAhead[0] |= MinecraftThermalInput.pointRegionCommittedIndependently(level, position);
                    if (!air.airAvailable() || air.airSampleTick() < 0 || level.getGameTime() - air.airSampleTick() > 40) violations[0]++;
                    String geometryRead = MinecraftThermalInput.describeAirRead(level, position);
                    if (!geometryRead.startsWith("geometry_current")) {
                        geometryViolations[0]++;
                        if (geometryViolations[0] <= 3) FHMain.LOGGER.info("THERMAL_REGIONAL_STEADY_GEOMETRY,elapsedTicks={},probe={},read={},invalidations={}",
                                elapsed, position, geometryRead, MinecraftThermalInput.describeGeometryInvalidations(level));
                        if (!describedWorldChange[0] && !observedSections.isEmpty()) {
                            describedWorldChange[0] = true;
                            var match = java.util.regex.Pattern.compile("section=(-?[0-9]+)").matcher(geometryRead);
                            if (match.find()) {
                                long key = Long.parseLong(match.group(1));
                                var states = observedSections.get(key);
                                var pos = new BlockPos.MutableBlockPos();
                                int changes = 0;
                                if (states != null) for (int block = 0; block < states.length; block++) {
                                    pos.set((SectionPos.x(key) << 4) + (block & 15), (SectionPos.y(key) << 4) + (block >>> 8), (SectionPos.z(key) << 4) + (block >>> 4 & 15));
                                    var current = level.getBlockState(pos);
                                    if (current != states[block]) {
                                        if (changes < 12) FHMain.LOGGER.info("THERMAL_REGIONAL_STEADY_WORLD_CHANGE,elapsedTicks={},block={},old={},current={}", elapsed, pos, states[block], current);
                                        changes++;
                                    }
                                }
                                FHMain.LOGGER.info("THERMAL_REGIONAL_STEADY_WORLD_CHANGE_SUMMARY,elapsedTicks={},section={},observed={},changes={}", elapsed, key, states != null, changes);
                            }
                        }
                    }
                }
                if (elapsed >= 800 && elapsed % 40 == 0) {
                    var snapshot = MinecraftThermalInput.gameplayInfraredSnapshot(player, true, 0, 0, 0, new long[12], false, new long[0], 0, 0);
                    if (snapshot == null || !snapshot.readable()) infraredFailures[0]++;
                }
                if (elapsed % 200 == 0) FHMain.LOGGER.info("THERMAL_REGIONAL_STEADY,elapsedTicks={},monitored={},ageViolations={},geometryViolations={},infraredFailures={},aheadAdvances={},worker={}",
                        elapsed, monitored[0], violations[0], geometryViolations[0], infraredFailures[0], MinecraftThermalInput.independentRegionCommits(level), MinecraftThermalInput.describeWorkerProgress(level));
                if (elapsed != 2400) return;
                helper.assertTrue(bAhead[0], "B's observed Air region must actually advance a later cut before A's construction finishes");
                helper.assertTrue(violations[0] == 0 && geometryViolations[0] == 0 && infraredFailures[0] == 0,
                        "Stable B must retain its real geometry and <=40tick Air/infrared while A constructs");
                helper.assertTrue(MinecraftThermalInput.enabledPhysicalSourceCount(level) == 256, "The real scene must retain both128-source bases");
                for (var position : bases[1].probes()) helper.assertTrue(MinecraftThermalInput.sampleMaterial(level, position.below(2), material)
                        && material.source() == MaterialSample.Source.LIVE, "B's actual floor material must remain live");
                helper.succeed();
            });
        }
        for (int delay = 400; delay < 2400; delay += 400) helper.runAfterDelay(delay, () -> {
            for (var base : bases) if (base != null) for (var fire : base.fires()) level.getBlockEntity(fire).getCapability(ForgeCapabilities.ITEM_HANDLER)
                    .orElseThrow(() -> new IllegalStateException("Actual campfire fuel capability disappeared"))
                    .insertItem(0, new ItemStack(Items.COAL), false);
        });
        for (int cycle = 0; cycle < 6; cycle++) {
            int iteration = cycle;
            int delay = 1001 + cycle * 160;
            helper.runAfterDelay(delay, () -> {
                boolean closing = (iteration & 1) == 0;
                // Place/remove real pillars beside every A fire, keeping its fuel,
                // supporting floor and emission face intact.
                for (var fire : bases[0].fires()) for (int dy = 1; dy <= 4; dy++)
                    level.setBlockAndUpdate(fire.west().above(dy), closing ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
                FHMain.LOGGER.info("THERMAL_REGIONAL_STEADY_ACTION,elapsedTicks={},pillars={},base=A", delay, closing ? "placed" : "removed");
            });
        }
    }
}
