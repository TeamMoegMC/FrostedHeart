/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialSample;
import com.teammoeg.frostedheart.content.climate.thermal.query.ThermalEnvironmentSample;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import com.mojang.authlib.GameProfile;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Two real occupied storeys, partition walls and normal player/source actions. */
@GameTestHolder("frostedheart_largebase_128")
@PrefixGameTestTemplate(false)
public final class ContinuousAirLargeBaseProductionGameTests {
    private static final int END_TICK = 2400;

    private ContinuousAirLargeBaseProductionGameTests() {}

    @GameTest(template = "phase0a_empty", timeoutTicks = 2500)
    public static void twoStoreyBaseKeepsCurrentAirThroughSourceAndPassageChanges(GameTestHelper helper) {
        run(helper, END_TICK, false);
    }

    static void run(GameTestHelper helper, int endTick, boolean receiverPressure) {
        helper.assertTrue(true,
                "Large-base production run requires continuous Air enabled");
        var level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(5, 0, 7));
        BuiltBase built = buildBase(helper, origin);
        BlockPos[] fires = built.fires(), probes = built.probes();
        ContinuousAirProductionTiming.observe(helper, receiverPressure ? "largebase128_receivers" : "largebase128", probes, endTick);
        ContinuousAirProductionTiming.observeTemperatureEnvelope(helper, fires, probes, endTick);
        FakePlayer[] receivers = new FakePlayer[receiverPressure ? 100 : 0];
        for (int index = 0; index < receivers.length; index++) {
            String name = "thBase" + index;
            receivers[index] = FakePlayerFactory.get(level, new GameProfile(UUID.nameUUIDFromBytes(
                    name.getBytes(StandardCharsets.UTF_8)), name));
        }
        var receiverSample = new ThermalEnvironmentSample();
        long[] receiverQueries = {0};

        var air = new ThermalEnvironmentSample();
        var material = new MaterialSample();
        boolean[] monitoring = {false};
        int[] monitoringSince = {-1};
        String[] firstViolation = {null};
        long[] workLimitedAtMonitoring = {-1};
        int[] currentTicks = {0};
        int[] ageViolationTicks = {0}, overdueGeometryTicks = {0}, receiverAgeViolationReads = {0};
        for (int delay = 1; delay <= endTick; delay++) {
            int elapsed = delay;
            helper.runAfterDelay(delay, () -> {
                boolean allCurrent = true;
                boolean ageViolation = false, overdueGeometry = false;
                for (int index = 0; index < probes.length; index++) {
                    BlockPos probe = probes[index];
                    MinecraftThermalInput.gameplayPlacedReservoirEnvironment(level, probe,
                            WorldTemperature.naturalAir(level, probe), air);
                    boolean live = air.airAvailable() && air.airSampleTick() >= 0 && air.airBasisTerms() >= 8;
                    boolean fresh = live && level.getGameTime() - air.airSampleTick() <= 40;
                    String read = MinecraftThermalInput.describeAirRead(level, probe);
                    boolean current = read.startsWith("geometry_current");
                    if (monitoring[0]) {
                        ageViolation |= !fresh;
                        overdueGeometry |= !current && level.getGameTime()
                                - MinecraftThermalInput.lastGeometryInvalidationTick(level) > 40;
                    }
                    allCurrent &= fresh && current && (monitoring[0] || ContinuousAirProductionTiming.sourceField(air));
                    if (monitoring[0] && firstViolation[0] == null
                            && (!fresh || !current && level.getGameTime()
                                    - MinecraftThermalInput.lastGeometryInvalidationTick(level) > 40)) {
                        firstViolation[0] = "Large-base Air invalid at " + probe + ", elapsed=" + elapsed
                                + ", read=" + read + ", worker=" + MinecraftThermalInput.describeWorkerProgress(level);
                        FHMain.LOGGER.info("THERMAL_LARGEBASE_FIRST_VIOLATION,{}", firstViolation[0]);
                    }
                }
                if (ageViolation) ageViolationTicks[0]++;
                if (overdueGeometry) overdueGeometryTicks[0]++;
                if (!monitoring[0] && allCurrent) {
                    monitoring[0] = true;
                    monitoringSince[0] = elapsed;
                    workLimitedAtMonitoring[0] = MinecraftThermalInput.workLimitedCompletions(level);
                    FHMain.LOGGER.info("THERMAL_LARGEBASE_MONITORING,elapsedTicks={}", elapsed);
                }
                if (allCurrent) currentTicks[0]++;
                int receiverCount = elapsed < 2000 ? Math.min(10, receivers.length)
                        : elapsed < 4000 ? Math.min(30, receivers.length)
                        : elapsed < 6000 ? Math.min(60, receivers.length) : receivers.length;
                for (int index = 0; index < receiverCount; index++) {
                    if (elapsed % 20 != index % 20) continue;
                    FakePlayer player = receivers[index];
                    BlockPos near = fires[index % fires.length];
                    player.setPos(near.getX() + .5 + .15 * Math.sin(elapsed * .02 + index),
                            near.getY(), near.getZ() + .5);
                    MinecraftThermalInput.gameplayPlayerEnvironment(player, WorldTemperature.naturalAir(level, near), receiverSample);
                    receiverQueries[0]++;
                    boolean receiverExpired = monitoring[0] && (!receiverSample.airAvailable()
                            || receiverSample.airSampleTick() < 0
                            || level.getGameTime() - receiverSample.airSampleTick() > 40);
                    if (receiverExpired) receiverAgeViolationReads[0]++;
                    if (receiverExpired && firstViolation[0] == null)
                        {
                            firstViolation[0] = "Player receiver " + index + " lost live Air at elapsed " + elapsed;
                            FHMain.LOGGER.info("THERMAL_LARGEBASE_FIRST_VIOLATION,{}", firstViolation[0]);
                        }
                }
                if (elapsed % 200 == 0) {
                    for (int probe = 0; probe < probes.length; probe++) for (int distance = 0; distance <= 2; distance += 2) {
                        BlockPos point = probes[probe].east(distance);
                        MinecraftThermalInput.gameplayPlacedReservoirEnvironment(level, point,
                                WorldTemperature.naturalAir(level, point), air);
                        MinecraftThermalInput.sampleMaterial(level, point.below(2), material);
                        FHMain.LOGGER.info("THERMAL_LONG_SCALE_POINT,elapsedTicks={},probe={},distance={},sampleTick={},naturalC={},airC={},materialH={},branch={},current={}",
                                elapsed, probe, distance, air.airSampleTick(), WorldTemperature.naturalAir(level, point),
                                air.airTemperatureC(), material.enthalpyJ(), material.branch(), MinecraftThermalInput.describeAirRead(level, point));
                    }
                    FHMain.LOGGER.info("THERMAL_LARGEBASE_VALIDATION,elapsedTicks={},ageViolationTicks={},overdueGeometryTicks={},receiverAgeViolationReads={}",
                            elapsed, ageViolationTicks[0], overdueGeometryTicks[0], receiverAgeViolationReads[0]);
                    int liveMaterials = 0;
                    for (BlockPos probe : probes)
                        if (MinecraftThermalInput.sampleMaterial(level, probe.below(2), material)
                                && material.source() == MaterialSample.Source.LIVE) liveMaterials++;
                    FHMain.LOGGER.info("THERMAL_LARGEBASE_STATE,elapsedTicks={},currentTicks={},liveMaterials={},workLimited={},rootChargedBytes={},worker={}",
                            elapsed, currentTicks[0], liveMaterials,
                            MinecraftThermalInput.workLimitedCompletions(level), MinecraftThermalInput.thermalBudgetUsedBytes(),
                            MinecraftThermalInput.describeWorkerProgress(level));
                    if (receiverPressure)
                        FHMain.LOGGER.info("THERMAL_LARGEBASE_RECEIVERS,elapsedTicks={},receivers={},queries={},previousTickProcessingUs={}",
                                elapsed, receiverCount, receiverQueries[0], ContinuousAirGameTestPacing.lastProcessingNanos() / 1000);
                    if (elapsed == endTick) helper.assertTrue(liveMaterials == probes.length,
                            "Both real stone floors remain LIVE after source and passage changes");
                }
                if (elapsed == endTick) {
                    helper.assertTrue(firstViolation[0] == null, firstViolation[0]);
                    helper.assertTrue(monitoring[0] && monitoringSince[0] <= endTick - 800 && allCurrent,
                            "128-source enclosed base must provide current Air over a substantial window");
                    helper.assertTrue(MinecraftThermalInput.workLimitedCompletions(level) == workLimitedAtMonitoring[0],
                            "Large-base backing must remain admitted after the first complete field");
                    FHMain.LOGGER.info("THERMAL_LARGEBASE_RESULT,monitoringSince={},currentTicks={},rootPeakChargedBytes={},worker={}",
                            monitoringSince[0], currentTicks[0], MinecraftThermalInput.thermalBudgetPeakBytes(),
                            MinecraftThermalInput.describeWorkerProgress(level));
                    helper.succeed();
                }
            });
        }
        for (int cycle = 0; cycle + 2400 <= endTick; cycle += 4000) {
        int cycleStart = cycle;
        helper.runAfterDelay(cycleStart + 601, () -> {
            for (int y = 1; y <= 2; y++) level.setBlockAndUpdate(origin.offset(24, y, 6), Blocks.STONE.defaultBlockState());
            FHMain.LOGGER.info("THERMAL_LARGEBASE_ACTION,elapsedTicks={},action=close_lower_passage", cycleStart + 601);
        });
        helper.runAfterDelay(cycleStart + 901, () -> {
            for (int y = 1; y <= 2; y++) level.setBlockAndUpdate(origin.offset(24, y, 6), Blocks.AIR.defaultBlockState());
            FHMain.LOGGER.info("THERMAL_LARGEBASE_ACTION,elapsedTicks={},action=open_lower_passage", cycleStart + 901);
        });
        helper.runAfterDelay(cycleStart + 1201, () -> {
            for (int index = 0; index < fires.length; index += 2)
                ContinuousAirProductionGameTests.useItem(helper, fires[index], Items.IRON_SHOVEL);
            FHMain.LOGGER.info("THERMAL_LARGEBASE_ACTION,elapsedTicks={},action=extinguish_64", cycleStart + 1201);
        });
        helper.runAfterDelay(cycleStart + 1501, () -> {
            for (int index = 0; index < fires.length; index += 2)
                ContinuousAirProductionGameTests.useItem(helper, fires[index], Items.FLINT_AND_STEEL);
            FHMain.LOGGER.info("THERMAL_LARGEBASE_ACTION,elapsedTicks={},action=reignite_64", cycleStart + 1501);
        });
        if (receiverPressure) {
            helper.runAfterDelay(cycleStart + 1801, () -> {
                for (BlockPos fire : fires) ContinuousAirProductionGameTests.useItem(helper, fire, Items.IRON_SHOVEL);
                FHMain.LOGGER.info("THERMAL_LARGEBASE_ACTION,elapsedTicks={},action=extinguish_all", cycleStart + 1801);
            });
            helper.runAfterDelay(cycleStart + 2201, () -> {
                for (BlockPos fire : fires) ContinuousAirProductionGameTests.useItem(helper, fire, Items.FLINT_AND_STEEL);
                FHMain.LOGGER.info("THERMAL_LARGEBASE_ACTION,elapsedTicks={},action=reignite_all", cycleStart + 2201);
            });
        }
        }
        if (receiverPressure) for (int delay = 400; delay < endTick; delay += 400) {
            int elapsed = delay;
            helper.runAfterDelay(delay, () -> {
                int accepted = 0;
                for (BlockPos fire : fires) {
                    var fuel = level.getBlockEntity(fire).getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(
                            () -> new IllegalStateException("Real campfire lost its fuel capability"));
                    if (fuel.insertItem(0, new ItemStack(Items.COAL), false).isEmpty()) accepted++;
                }
                FHMain.LOGGER.info("THERMAL_LARGEBASE_REFUEL,elapsedTicks={},acceptedCampfires={}", elapsed, accepted);
            });
        }
    }

    record BuiltBase(BlockPos[] fires, BlockPos[] probes) {}

    static BuiltBase buildBase(GameTestHelper helper, BlockPos origin) {
        return buildBase(helper.getLevel(), origin, helper::assertTrue);
    }

    static BuiltBase buildBase(net.minecraft.server.level.ServerLevel level, BlockPos origin) {
        return buildBase(level, origin, (valid, message) -> {
            if (!valid) throw new IllegalStateException(message);
        });
    }

    private static BuiltBase buildBase(net.minecraft.server.level.ServerLevel level, BlockPos origin,
                                      java.util.function.BiConsumer<Boolean, String> check) {
        int width = 50, depth = 14;
        for (int y = 0; y <= 12; y++) for (int z = 0; z < depth; z++) for (int x = 0; x < width; x++) {
            boolean floor = y % 6 == 0;
            boolean wall = x == 0 || x == width - 1 || z == 0 || z == depth - 1;
            boolean partition = x == 12 || x == 24 || x == 36;
            boolean passage = z == 6 && y % 6 >= 1 && y % 6 <= 2;
            level.setBlockAndUpdate(origin.offset(x, y, z),
                    floor || wall || partition && !passage ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
        }
        // A real open shaft connects the floors; a half-slab preserves a partial outlet.
        level.setBlockAndUpdate(origin.offset(25, 6, 6), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(origin.offset(26, 6, 6), Blocks.STONE_SLAB.defaultBlockState());
        BlockPos[] fires = new BlockPos[128];
        for (int storey = 0; storey < 2; storey++) for (int z = 0; z < 4; z++) for (int x = 0; x < 16; x++) {
            BlockPos fire = origin.offset(2 + x * 3, 1 + storey * 6, 2 + z * 3);
            fires[storey * 64 + z * 16 + x] = fire;
            placeAndIgnite(level, fire, check);
        }
        BlockPos[] probes = new BlockPos[8];
        for (int storey = 0; storey < 2; storey++) for (int room = 0; room < 4; room++)
            probes[storey * 4 + room] = fires[storey * 64 + room * 4 + 1].above();
        return new BuiltBase(fires, probes);
    }

    private static void placeAndIgnite(net.minecraft.server.level.ServerLevel level, BlockPos fire,
                                      java.util.function.BiConsumer<Boolean, String> check) {
        level.setBlockAndUpdate(fire, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
        var fuel = level.getBlockEntity(fire).getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(
                () -> new IllegalStateException("Real campfire has no fuel capability"));
        check.accept(fuel.insertItem(0, new ItemStack(Items.COAL), false).isEmpty(),
                "Real campfire accepts coal");
        ContinuousAirProductionGameTests.useItem(level, fire, Items.FLINT_AND_STEEL);
        check.accept(level.getBlockState(fire).getValue(CampfireBlock.LIT), "Actual campfire ignites");
    }
}
