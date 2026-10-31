/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialSample;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialThermalLaw;
import com.teammoeg.frostedheart.content.climate.thermal.query.QueryPublication;
import com.teammoeg.frostedheart.content.climate.thermal.query.ThermalEnvironmentSample;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Actual queries retained across real construction and shutdown; no injected heat or owned engine. */
@GameTestHolder("frostedheart_regional_lifecycle")
@PrefixGameTestTemplate(false)
public final class ContinuousAirRegionalLifecycleProductionGameTests {
    @GameTest(template = "phase0a_empty", batch = "regional_material_controls", timeoutTicks = 1600)
    public static void gameplayPhaseControlsKeepHeatAndRequestAcrossRealConstruction(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos fire = ContinuousAirProductionGameTests.room(helper, helper.absolutePos(BlockPos.ZERO));
        BlockPos[] bodies = {fire.above(), fire.above().east()};
        for (var body : bodies) level.setBlockAndUpdate(body, Blocks.PACKED_ICE.defaultBlockState());
        ContinuousAirProductionGameTests.useItem(helper, fire, Items.FLINT_AND_STEEL);
        int randomTicks = level.getGameRules().getInt(GameRules.RULE_RANDOMTICKING);
        level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(0, level.getServer());
        var sample = new MaterialSample();
        long[] requests = new long[bodies.length];
        double[] energy = new double[bodies.length];
        long[] constructionTick = {0};
        helper.startSequence().thenWaitUntil(() -> {
            for (var body : bodies) helper.assertTrue(MinecraftThermalInput.sampleMaterial(level, body, sample)
                            && sample.source() == MaterialSample.Source.LIVE,
                    "Actual campfire capture must own both ice bodies before gameplay controls");
        }).thenExecute(() -> {
            // Exercise the real gameplay boundary, not a fabricated source or numerical-state write.
            for (var body : bodies) helper.assertTrue(MinecraftThermalInput.requestGameplayPhase(level, body,
                            level.getBlockState(body), MaterialThermalLaw.HEATING),
                    "The production gameplay entry must accept each owned material control");
        }).thenWaitUntil(() -> {
            for (int index = 0; index < bodies.length; index++) {
                helper.assertTrue(MinecraftThermalInput.sampleMaterial(level, bodies[index], sample)
                                && sample.source() == MaterialSample.Source.LIVE && sample.requestSequence() > 0
                                && sample.branch() == MaterialThermalLaw.HEATING
                                && sample.law().heating().complete(sample.enthalpyJ()),
                        "Both controls settle heat and publish their real phase requests");
                requests[index] = sample.requestSequence();
                energy[index] = sample.enthalpyJ();
            }
        }).thenExecute(() -> {
            constructionTick[0] = level.getGameTime();
            level.setBlockAndUpdate(bodies[0].south(), Blocks.STONE.defaultBlockState());
        }).thenWaitUntil(() -> {
            for (int index = 0; index < bodies.length; index++) helper.assertTrue(
                    MinecraftThermalInput.sampleMaterial(level, bodies[index], sample)
                            && sample.source() == MaterialSample.Source.LIVE
                            && sample.sampleTick() > constructionTick[0]
                            && sample.requestSequence() == requests[index] && sample.enthalpyJ() == energy[index],
                    "Real same-Brick construction preserves settled phase heat and request identity");
        }).thenExecute(() -> {
            // Retire one pending body while the other proceeds through the normal world ACK.
            level.setBlockAndUpdate(bodies[0], Blocks.STONE.defaultBlockState());
            level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(randomTicks, level.getServer());
        }).thenWaitUntil(() -> {
            helper.assertTrue(level.getBlockState(bodies[0]).is(Blocks.STONE),
                    "A pending old request must not convert its replacement stone");
            helper.assertTrue(!level.getBlockState(bodies[1]).is(Blocks.PACKED_ICE),
                    "The surviving real material completes its world transition");
            helper.assertTrue(MinecraftThermalInput.sampleMaterial(level, bodies[0], sample)
                            && sample.source() == MaterialSample.Source.LIVE && sample.requestSequence() == 0,
                    "Replacement material must not inherit the retired request");
        }).thenExecute(() -> {
            ContinuousAirProductionGameTests.useItem(helper, fire, Items.IRON_SHOVEL);
            FHMain.LOGGER.info("THERMAL_REGIONAL_MATERIAL_CONTROLS,bodies={},firstRequest={},secondRequest={},constructionTick={},replacement={},survivor={}",
                    bodies.length, requests[0], requests[1], constructionTick[0],
                    level.getBlockState(bodies[0]), level.getBlockState(bodies[1]));
        }).thenSucceed();
    }

    @GameTest(template = "phase0a_empty", batch = "regional_readers", timeoutTicks = 1800)
    public static void borrowedSnapshotsRemainChargedAcrossConstructionAndClose(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos fire = ContinuousAirProductionGameTests.room(helper, helper.absolutePos(BlockPos.ZERO));
        ContinuousAirProductionGameTests.useItem(helper, fire, Items.FLINT_AND_STEEL);
        var sample = new ThermalEnvironmentSample();
        var readers = new QueryPublication.ReadCursor[] {
                new QueryPublication.ReadCursor(), new QueryPublication.ReadCursor(), new QueryPublication.ReadCursor()};
        QueryPublication[] publication = {null};
        long[] chargedAtClose = {0};
        long[] reopenedAt = {0};
        var sequence = helper.startSequence().thenIdle(240).thenWaitUntil(() -> {
            MinecraftThermalInput.gameplayPlacedReservoirEnvironment(level, fire.above(),
                    WorldTemperature.naturalAir(level, fire.above()), sample);
            helper.assertTrue(ContinuousAirProductionTiming.sourceField(sample),
                    "The actual ignited source must establish its published detailed field");
        }).thenExecute(() -> {
            publication[0] = MinecraftThermalInput.queryPublicationForObservation(level);
            var player = FakePlayerFactory.getMinecraft(level);
            player.setPos(fire.getX() + .5, fire.getY() + 1, fire.getZ() + .5);
            var infrared = MinecraftThermalInput.gameplayInfraredSnapshot(player, true, 0, 0, 0,
                    new long[12], false, new long[0], 0, 0);
            helper.assertTrue(infrared != null && infrared.readable(),
                    "A real infrared request must allocate and read the actual change-epoch arrays");
        });
        for (int index = 0; index < readers.length; index++) {
            int read = index;
            sequence.thenExecute(() -> {
                helper.assertTrue(publication[0].beginRead(readers[read]) && readers[read].valid(),
                        "Borrow the actual publication for construction observation");
                helper.assertTrue(readers[read].airAggregationSpanAt(fire.getX() + .5,
                        fire.getY() + 1.5, fire.getZ() + .5) > 0,
                        "Read and pin the actual local Page and field before construction");
                helper.assertTrue(publication[0].retainedReaderPayloadBytes() > 0,
                        "Borrowed immutable payload has an existing budget reservation");
                // Actual pillar changes replace the captured geometry and numerical layout.
                BlockPos pillar = fire.east(2 + read);
                for (int dy = 0; dy < 3; dy++) level.setBlockAndUpdate(pillar.above(dy), Blocks.STONE.defaultBlockState());
            }).thenIdle(100).thenWaitUntil(() -> {
                helper.assertTrue(!readers[read].isCurrent(), "Construction must retire the borrowed publication version");
                helper.assertTrue(publication[0].retiredReaderAirBytes() > 0,
                        "The retired reader layout remains charged while the cursor owns it");
                helper.assertTrue(publication[0].retainedReaderCount() == read + 1,
                        "Each real borrowed envelope retains one reader");
            });
        }
        sequence.thenExecute(() -> {
            ContinuousAirProductionGameTests.useItem(helper, fire, Items.IRON_SHOVEL);
            MinecraftThermalInput.closeActiveLevel(level);
        }).thenWaitUntil(() -> helper.assertTrue(publication[0].closedReaderAirBackingBytes() > 0,
                "The real worker close must transfer its retained Air charge to the readers"))
        .thenExecute(() -> {
            chargedAtClose[0] = MinecraftThermalInput.thermalBudgetUsedBytes();
            long payload = publication[0].retainedReaderPayloadBytes();
            long air = publication[0].retiredReaderAirBytes();
            long epochs = publication[0].closedReaderPageBackingBytes();
            FHMain.LOGGER.info("THERMAL_REGIONAL_READER_CLOSE,readers={},payloadBytes={},airBytes={},epochBytes={},rootBytes={}",
                    publication[0].retainedReaderCount(), payload, air, epochs, chargedAtClose[0]);
            try {
                helper.assertTrue(publication[0].retainedReaderCount() == readers.length,
                        "Shutdown preserves borrowed snapshots until their readers release them");
                helper.assertTrue(payload > 0 && air > 0 && epochs > 0 && chargedAtClose[0] >= payload + air + epochs,
                        "Closed runtime must retain actual reader payload and Air backing charges");
            } finally {
                for (var reader : readers) reader.clear();
            }
            helper.assertTrue(publication[0].retainedReaderCount() == 0
                            && publication[0].retainedReaderPayloadBytes() == 0 && publication[0].retiredReaderAirBytes() == 0,
                    "The final reader releases the retained data and its reservations");
            helper.assertTrue(MinecraftThermalInput.thermalBudgetUsedBytes() < chargedAtClose[0],
                    "Reader release reduces the real shared root budget");
            FHMain.LOGGER.info("THERMAL_REGIONAL_READER_RELEASE,rootBeforeBytes={},rootAfterBytes={}",
                    chargedAtClose[0], MinecraftThermalInput.thermalBudgetUsedBytes());
            reopenedAt[0] = level.getGameTime();
            ContinuousAirProductionGameTests.useItem(helper, fire, Items.FLINT_AND_STEEL);
            ContinuousAirProductionTiming.eachTick(helper, 240, elapsed -> {
                if (elapsed % 40 != 0) return;
                MinecraftThermalInput.gameplayPlacedReservoirEnvironment(level, fire.above(),
                        WorldTemperature.naturalAir(level, fire.above()), sample);
                FHMain.LOGGER.info("THERMAL_REGIONAL_REOPEN,elapsedTicks={},sampleTick={},airC={},basis={},read={},worker={}",
                        elapsed, sample.airSampleTick(), sample.airTemperatureC(), sample.airBasisTerms(),
                        MinecraftThermalInput.describeAirRead(level, fire.above()), MinecraftThermalInput.describeWorkerProgress(level));
            });
        }).thenIdle(240).thenWaitUntil(() -> {
            MinecraftThermalInput.gameplayPlacedReservoirEnvironment(level, fire.above(),
                    WorldTemperature.naturalAir(level, fire.above()), sample);
            helper.assertTrue(ContinuousAirProductionTiming.sourceField(sample)
                            && sample.airSampleTick() >= reopenedAt[0] + 200
                            && MinecraftThermalInput.describeAirRead(level, fire.above()).startsWith("geometry_current"),
                    "A new runtime in the same JVM must keep advancing after readers release the previous generation");
        }).thenExecute(() -> {
            ContinuousAirProductionGameTests.useItem(helper, fire, Items.IRON_SHOVEL);
            MinecraftThermalInput.closeActiveLevel(level);
        }).thenWaitUntil(() -> helper.assertTrue(MinecraftThermalInput.thermalBudgetUsedBytes() == 0,
                "The reopened runtime also releases its complete backing")).thenSucceed();
    }

    @GameTest(template = "phase0a_empty", batch = "regional_material_phase", timeoutTicks = 9000)
    public static void actualDenseSourcesMeltPowderSnowAndRetireItsMaterialIdentity(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos fire = ContinuousAirProductionGameTests.room(helper, helper.absolutePos(BlockPos.ZERO));
        BlockPos powder = fire.above();
        // Supply real heat in a sealed two-storey chamber. Nine fires in the
        // original large room only kept the snow near its transition plateau;
        // that fixture could not finish the latent heat within its timeout.
        for (int dy = -1; dy <= 5; dy++) for (int dz = -4; dz <= 5; dz++) for (int dx = -4; dx <= 5; dx++) {
            boolean wall = dy == -1 || dy == 2 || dy == 5 || dx == -4 || dx == 5 || dz == -4 || dz == 5;
            level.setBlockAndUpdate(fire.offset(dx, dy, dz), (wall ? Blocks.STONE : Blocks.AIR).defaultBlockState());
        }
        var fires = new java.util.ArrayList<BlockPos>();
        for (int layer = 0; layer < 2; layer++) for (int dx = -3; dx <= 4; dx++) for (int dz = -3; dz <= 4; dz++) {
            BlockPos adjacent = fire.offset(dx, 3 * layer, dz);
            level.setBlockAndUpdate(adjacent, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
            level.getBlockEntity(adjacent).getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(
                    () -> new IllegalStateException("Actual dense campfire lost its fuel capability"))
                    .insertItem(0, new ItemStack(Items.COAL, 4), false);
            ContinuousAirProductionGameTests.useItem(helper, adjacent, Items.FLINT_AND_STEEL);
            fires.add(adjacent);
        }
        for (int delay = 1000; delay < 9000; delay += 1000) helper.runAfterDelay(delay, () -> {
            if (level.getBlockState(powder).isAir()) return;
            for (var source : fires) level.getBlockEntity(source).getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(
                    fuel -> fuel.insertItem(0, new ItemStack(Items.COAL, 4), false));
        });
        var air = new ThermalEnvironmentSample();
        var material = new com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialSample();
        double[] firstH = {Double.NaN}, maximumH = {Double.NEGATIVE_INFINITY};
        long[] placedTick = {0};
        for (int delay = 300; delay < 9000; delay += 500) {
            int elapsed = delay;
            helper.runAfterDelay(delay, () -> {
                if (placedTick[0] == 0) {
                    FHMain.LOGGER.info("THERMAL_REGIONAL_PHASE_WAITING,elapsedTicks={},worker={}", elapsed,
                            MinecraftThermalInput.describeWorkerProgress(level));
                    return;
                }
                MinecraftThermalInput.sampleMaterial(level, powder, material);
                if (material.source() == com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialSample.Source.LIVE)
                    maximumH[0] = Math.max(maximumH[0], material.enthalpyJ());
                double progress = material.law() == null || material.law().transition(material.branch()) == null ? -1
                        : material.law().transition(material.branch()).progress(material.enthalpyJ());
                FHMain.LOGGER.info("THERMAL_REGIONAL_PHASE_OBSERVATION,elapsedTicks={},state={},H={},temperatureC={},branch={},progress={},requestSequence={},source={},nearAirC={},worker={}",
                        elapsed, level.getBlockState(powder), material.enthalpyJ(), material.temperatureC(), material.branch(), progress,
                        material.requestSequence(), material.source(), MinecraftThermalInput.gameplayPassiveEnvironment(level,
                                powder.east(), WorldTemperature.naturalAir(level, powder.east())), MinecraftThermalInput.describeWorkerProgress(level));
            });
        }
        helper.startSequence().thenIdle(240).thenWaitUntil(() -> {
            MinecraftThermalInput.gameplayPlacedReservoirEnvironment(level, powder,
                    WorldTemperature.naturalAir(level, powder), air);
            helper.assertTrue(ContinuousAirProductionTiming.sourceField(air),
                    "The actual source establishes Air before the material replacement");
        }).thenExecute(() -> {
            level.setBlockAndUpdate(powder, Blocks.POWDER_SNOW.defaultBlockState());
            placedTick[0] = level.getGameTime();
        }).thenWaitUntil(() -> {
            helper.assertTrue(MinecraftThermalInput.sampleMaterial(level, powder, material)
                            && material.source() == com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialSample.Source.LIVE,
                    "Actual output-face powder snow enters material publication");
            firstH[0] = material.enthalpyJ();
            maximumH[0] = firstH[0];
        }).thenWaitUntil(() -> {
            boolean live = MinecraftThermalInput.sampleMaterial(level, powder, material)
                    && material.source() == com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialSample.Source.LIVE;
            if (live) maximumH[0] = Math.max(maximumH[0], material.enthalpyJ());
            helper.assertTrue(maximumH[0] > firstH[0] + 10,
                    "Real source delivery raises stored material H before conversion");
        }).thenWaitUntil(() -> helper.assertTrue(level.getBlockState(powder).isAir(),
                "The original phase controller converts actual heated powder snow into Air"))
        .thenWaitUntil(() -> {
            MinecraftThermalInput.gameplayPlacedReservoirEnvironment(level, powder,
                    WorldTemperature.naturalAir(level, powder), air);
            helper.assertTrue(air.airSampleTick() >= 0 && level.getGameTime() - air.airSampleTick() <= 40
                            && MinecraftThermalInput.describeAirRead(level, powder).startsWith("geometry_current"),
                    "The phase ACK replaces the old material identity with current Air");
            helper.assertTrue(!MinecraftThermalInput.sampleMaterial(level, powder, material)
                            || material.source() != com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialSample.Source.LIVE,
                    "Converted Air must not retain the retired powder-snow material identity");
        }).thenExecute(() -> {
            FHMain.LOGGER.info("THERMAL_REGIONAL_PHASE_OUTPUT,placedTick={},convertedTick={},initialH={},maximumH={},airSampleTick={}",
                    placedTick[0], level.getGameTime(), firstH[0], maximumH[0], air.airSampleTick());
            for (var source : fires) ContinuousAirProductionGameTests.useItem(helper, source, Items.IRON_SHOVEL);
        }).thenSucceed();
    }
}
