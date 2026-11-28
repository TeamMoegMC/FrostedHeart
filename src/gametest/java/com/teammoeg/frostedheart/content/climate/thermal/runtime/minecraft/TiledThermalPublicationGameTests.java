/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.network.FHResponseInfraredViewDataSyncPacket;
import com.teammoeg.frostedheart.content.climate.network.InfraredBrickCodec;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialSample;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialStateAccess;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialThermalLaw;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.PagePublication;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureTable;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.MaterialSectionState;
import com.teammoeg.frostedheart.content.climate.thermal.query.QueryPublication;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.input.MinecraftPageManager;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.input.MinecraftPhaseController;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ResolvedGeometryBatch;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputBatch;
import com.teammoeg.frostedheart.content.climate.thermal.solver.TiledThermalField;
import com.teammoeg.frostedheart.content.climate.thermal.solver.PhaseTransitionRuntime;
import com.teammoeg.frostedheart.content.climate.thermal.source.SpatialEnergyBanks;
import java.util.Arrays;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.common.util.FakePlayerFactory;

/** Existing query API reads the fixed core's sole Accepted state; geometry headers carry no numerical arrays. */
@GameTestHolder("frostedheart_tiled_publication")
@PrefixGameTestTemplate(false)
public final class TiledThermalPublicationGameTests {
    private static final QueryPublication.SectionPublication[] NO_GEOMETRY = new QueryPublication.SectionPublication[0];

    @GameTest(template = "phase0a_empty", timeoutTicks = 300)
    public static void admittingAnAdjacentSectionDoesNotChangePublishedCornerCapacityBeforeCommit(GameTestHelper helper) {
        var level = helper.getLevel(); var origin = new BlockPos(131084, 192, 131072); var probe = origin.offset(3, 1, 1);
        for (int x = 0; x < 8; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++)
            level.setBlockAndUpdate(origin.offset(x, y, z), Blocks.AIR.defaultBlockState());
        MinecraftThermalInput.closeActiveLevel(level);
        var budget = new ThermalMemoryBudget(4L << 20);
        try (var geometry = new ThermalMaterialBehaviorGameTests.Simulation(helper, origin, 4, 4, 4, 0);
                var banks = new SpatialEnergyBanks(budget, 8, 8, 0);
                var field = new TiledThermalField(budget, geometry.profiles.signatures(), geometry.profiles.materials(), 1200, 0, 0, 8, 0);
                var query = QueryPublication.createFixed(budget, 8, field)) {
            int[] left = new int[4096], right = new int[4096];
            Arrays.fill(left, ThermalSignatureTable.UNRESOLVED); Arrays.fill(right, ThermalSignatureTable.UNRESOLVED);
            int airSignature = geometry.profiles.states().signatureId(Blocks.AIR.defaultBlockState());
            for (int x = 0; x < 4; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++) {
                left[local(origin.offset(x, y, z))] = airSignature; right[local(origin.offset(x + 4, y, z))] = airSignature;
            }
            helper.assertTrue(field.admit(SectionPos.asLong(origin), left, 1L << 3, 0), "Admit the original edge Brick");
            var handle = geometry.page(origin);
            begin(helper, field, banks, 0); finish(helper, field);
            helper.assertTrue(query.publishFixed(new QueryPublication.SectionPublication[] {
                    new QueryPublication.SectionPublication(handle, handle.currentPublication())}), "Publish only the established Section");
            long receiver = banks.retain(new SpatialEnergyBanks.Address(probe.asLong(), -1));
            banks.add(receiver, 6400); begin(helper, field, banks, 20); finish(helper, field);
            helper.assertTrue(query.publishFixed(NO_GEOMETRY), "Publish heat next to the uncaptured neighbour");
            var sample = new QueryPublication.MutableSample();
            helper.assertTrue(query.tryReadAir(probe.getX() + .875, probe.getY() + .5, probe.getZ() + .5, sample)
                            && sample.temperatureC() > 0, "Read a heated fractional position at the Section edge");
            double before = sample.temperatureC(), ownedJ = field.totalEnthalpyJ();
            helper.assertTrue(field.admit(SectionPos.asLong(probe.east()), right, 1, 0), "Prepare an adjacent Section without committing geometry");
            helper.assertTrue(query.tryReadAir(probe.getX() + .875, probe.getY() + .5, probe.getZ() + .5, sample)
                            && sample.temperatureC() == before && sample.sampleTick() == 20,
                    "Unpublished geometry must not dilute a published Q1 corner; before=" + before + ",observed=" + sample.temperatureC());
            begin(helper, field, banks, 20); finish(helper, field);
            helper.assertTrue(query.publishFixed(NO_GEOMETRY)
                            && query.tryReadAir(probe.getX() + .875, probe.getY() + .5, probe.getZ() + .5, sample)
                            && sample.temperatureC() < before && Math.abs(field.totalEnthalpyJ() - ownedJ) < 1e-7,
                    "Only the atomic commit joins new corner capacity, conserving all previously owned J");
            FHMain.LOGGER.info("THERMAL_FIXED_ADMISSION_PUBLICATION,beforeC={},afterCommitC={},beforeCommitUnchanged=true,ownedJ={}", before, sample.temperatureC(), ownedJ);
        } finally { MinecraftThermalInput.closeActiveLevel(level); }
        helper.assertTrue(budget.usedBytes() == 0, "Admission/publication fixture releases all state");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 200)
    public static void retiringFrozenSectionDoesNotClearAnotherSectionThatReusesItsPageSlot(GameTestHelper helper) {
        var level = helper.getLevel();
        var first = new BlockPos(61440, 192, 61440); var second = first.east(16);
        for (var origin : new BlockPos[] {first, second}) {
            level.setChunkForced(origin.getX() >> 4, origin.getZ() >> 4, true);
            for (int x = 0; x < 4; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++)
                level.setBlockAndUpdate(origin.offset(x, y, z), Blocks.STONE.defaultBlockState());
        }
        MinecraftThermalInput.closeActiveLevel(level);
        var budget = new ThermalMemoryBudget(4L << 20);
        try (var geometry = new ThermalMaterialBehaviorGameTests.Simulation(helper, first, 4, 4, 4, 0);
                var banks = new SpatialEnergyBanks(budget, 8, 8, 0);
                var field = new TiledThermalField(budget, geometry.profiles.signatures(), geometry.profiles.materials(), 1200, 96, 0, 8, 0);
                var query = QueryPublication.createFixed(budget, 8, field)) {
            int[] captured = new int[4096]; Arrays.fill(captured, ThermalSignatureTable.UNRESOLVED);
            int signature = geometry.profiles.states().signatureId(Blocks.STONE.defaultBlockState());
            for (int x = 0; x < 4; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++) captured[x | z << 4 | y << 8] = signature;
            long a = SectionPos.asLong(first), b = SectionPos.asLong(second);
            helper.assertTrue(field.admit(a, captured, 1, 0), "Admit the first material Section");
            var ownerA = geometry.page(first); var pageA = ownerA.currentPublication();
            begin(helper, field, banks, 0); finish(helper, field);
            helper.assertTrue(query.publishFixed(new QueryPublication.SectionPublication[] {new QueryPublication.SectionPublication(ownerA, pageA)}), "Publish the original slot owner");
            helper.assertTrue(field.freeze(a) && field.admit(b, captured, 1, 0), "Keep the old values frozen while admitting another actual Section");
            ownerA.publish(PagePublication.EMPTY);
            var ownerB = new ThermalPageHandle(b, 1);
            var pageB = PagePublication.owned(pageA.workerPageSlot(), 0, pageA.topologyGeneration() + 1, pageA.copyBricks());
            ownerB.publish(pageB);
            helper.assertTrue(pageA.workerPageSlot() == pageB.workerPageSlot(), "Exercise actual numeric page-slot reuse in the published headers");
            begin(helper, field, banks, 20); finish(helper, field);
            helper.assertTrue(query.publishFixed(new QueryPublication.SectionPublication[] {new QueryPublication.SectionPublication(ownerB, pageB)}), "Publish the newer slot owner");
            var material = new MaterialSample();
            helper.assertTrue(query.tryReadMaterial(pageB, 0, Block.getId(Blocks.STONE.defaultBlockState()), material), "Read the new Section before old writeback retires");
            helper.assertTrue(query.tryReadMaterial(pageA, 0, Block.getId(Blocks.STONE.defaultBlockState()), material)
                            && material.sampleTick() == 0,
                    "The frozen old header retains its own values while another Section reuses its slot");
            helper.assertTrue(field.retire(a), "Finish the old frozen Section's ownership");
            begin(helper, field, banks, 40); finish(helper, field);
            helper.assertTrue(query.publishFixed(NO_GEOMETRY)
                            && query.tryReadMaterial(pageB, 0, Block.getId(Blocks.STONE.defaultBlockState()), material)
                            && material.sampleTick() == 40 && !query.tryReadMaterial(pageA, 0, Block.getId(Blocks.STONE.defaultBlockState()), material),
                    "Old retirement removes only its own header, preserving the live reused slot");
            query.close();
            helper.assertTrue(field.hasSection(b), "Closing the read facade does not close its engine-owned numerical state");
            FHMain.LOGGER.info("THERMAL_TILED_QUERY_SLOT_REUSE,slot={},oldFrozenSectionRetired=true,newSectionReadable=true", pageB.workerPageSlot());
        } finally {
            MinecraftThermalInput.closeActiveLevel(level);
            for (var origin : new BlockPos[] {first, second}) level.setChunkForced(origin.getX() >> 4, origin.getZ() >> 4, false);
        }
        helper.assertTrue(budget.usedBytes() == 0, "Reused query-slot ownership releases all storage");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 200)
    public static void queryPublishesAcceptedGeometryAndInputReceiptTogether(GameTestHelper helper) {
        var level = helper.getLevel();
        var origin = new BlockPos(57344, 192, 57344);
        var airPosition = origin.offset(1, 1, 1); var materialPosition = origin.offset(2, 1, 1);
        level.setChunkForced(origin.getX() >> 4, origin.getZ() >> 4, true);
        try {
            for (int x = 0; x < 4; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++)
                level.setBlockAndUpdate(origin.offset(x, y, z), Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(materialPosition, Blocks.STONE.defaultBlockState());
            MinecraftThermalInput.closeActiveLevel(level);
            var budget = new ThermalMemoryBudget(4L << 20);
            try (var geometry = new ThermalMaterialBehaviorGameTests.Simulation(helper, origin, 4, 4, 4, 0);
                    var banks = new SpatialEnergyBanks(budget, 8, 8, 0);
                    var field = new TiledThermalField(budget, geometry.profiles.signatures(), geometry.profiles.materials(), 1200, 96, 0, 8, 0);
                    var query = QueryPublication.createFixed(budget, 8, field)) {
                int[] captured = new int[4096]; Arrays.fill(captured, ThermalSignatureTable.UNRESOLVED);
                for (int x = 0; x < 4; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++) {
                    var position = origin.offset(x, y, z);
                    captured[local(position)] = geometry.profiles.states().signatureId(level.getBlockState(position));
                }
                long section = SectionPos.asLong(origin);
                helper.assertTrue(field.admit(section, captured, 1, 0), "Capture the actual Page in the fixed field");
                var handle = geometry.page(origin); PagePublication page = handle.currentPublication();
                var header = new QueryPublication.SectionPublication(handle, page);
                var air = new QueryPublication.MutableSample(); var matter = new MaterialSample(); var direct = new TiledThermalField.Sample();
                helper.assertTrue(!query.tryReadAir(airPosition.getX() + .5, airPosition.getY() + .5, airPosition.getZ() + .5, air), "An uncommitted query has no published values");
                begin(helper, field, banks, 0); finish(helper, field);
                helper.assertTrue(query.publishFixed(new QueryPublication.SectionPublication[] {header}), "Publish initial geometry and its accepted bank atomically");
                helper.assertTrue(read(query, airPosition, air) && air.temperatureC() == 0 && air.sampleTick() == 0, "Read the actual initial accepted state through the existing query API");
                long receiver = banks.retain(new SpatialEnergyBanks.Address(airPosition.asLong(), -1));
                banks.add(receiver, 6400); begin(helper, field, banks, 20);
                helper.assertTrue(!query.publishFixed(NO_GEOMETRY) && banks.hasSealedBank(), "Unfinished work cannot publish or acknowledge its input");
                helper.assertTrue(read(query, airPosition, air) && air.temperatureC() == 0, "Readers retain Accepted while numerical work is pending");
                finish(helper, field);
                helper.assertTrue(read(query, airPosition, air) && air.temperatureC() == 0 && banks.hasSealedBank(), "A finished uncommitted trial is still invisible");
                helper.assertTrue(query.publishFixed(NO_GEOMETRY) && !banks.hasSealedBank(), "Move the field and its receipt through one publication point");
                helper.assertTrue(read(query, airPosition, air) && field.sampleAir(airPosition.getX() + .5, airPosition.getY() + .5, airPosition.getZ() + .5, direct)
                                && air.temperatureC() == direct.temperatureC() && air.sampleTick() == 20 && air.airBasisTerms() == 8 && !air.minimumQuality(),
                        "The published query reads the same reconstructed view and its eight corner terms");
                helper.assertTrue(query.tryReadMaterial(page, local(materialPosition), Block.getId(Blocks.STONE.defaultBlockState()), matter)
                                && field.read(materialPosition.asLong(), true, direct) && matter.enthalpyJ() == direct.enthalpyJ(),
                        "Material queries address the same accepted body by Page and world position");
                var changed = geometry.change(materialPosition, Blocks.IRON_BLOCK.defaultBlockState(), ResolvedGeometryBatch.MaterialChanges.REPLACE);
                geometry.tick = 20; geometry.publish(changed, ThermalInputBatch.NO_PHASE_ACKS);
                PagePublication next = handle.currentPublication();
                helper.assertTrue(!read(query, airPosition, air), "A real geometry change invalidates the strict old Air header");
                helper.assertTrue(query.tryReadCapturedAir(airPosition.getX() + .5, airPosition.getY() + .5, airPosition.getZ() + .5, 20, 40, air)
                                && air.sampleTick() == 20, "The bounded captured fallback keeps its real sampling time");
                helper.assertTrue(!query.tryReadMaterial(next, local(materialPosition), Block.getId(Blocks.IRON_BLOCK.defaultBlockState()), matter),
                        "New world matter cannot read the old committed body through newer geometry");
                helper.assertTrue(field.changeCell(materialPosition.asLong(), geometry.profiles.states().signatureId(Blocks.IRON_BLOCK.defaultBlockState()),
                        TiledThermalField.Change.REPLACEMENT), "Capture the actual replacement");
                begin(helper, field, banks, 40); finish(helper, field);
                helper.assertTrue(query.publishFixed(new QueryPublication.SectionPublication[] {new QueryPublication.SectionPublication(handle, next)})
                                && read(query, airPosition, air) && air.sampleTick() == 40,
                        "Publish the new material identity and matching Air geometry together");
                helper.assertTrue(query.tryReadMaterial(next, local(materialPosition), Block.getId(Blocks.IRON_BLOCK.defaultBlockState()), matter)
                                && !query.tryReadMaterial(next, local(materialPosition), Block.getId(Blocks.STONE.defaultBlockState()), matter),
                        "World-coordinate material reads require the committed state identity");
                helper.assertTrue(field.retire(section), "Retire the numerical Section");
                begin(helper, field, banks, 40); finish(helper, field);
                helper.assertTrue(query.publishFixed(NO_GEOMETRY) && !read(query, airPosition, air)
                                && query.regionAt(airPosition.getX() + .5, airPosition.getY() + .5, airPosition.getZ() + .5) == -1,
                        "Retirement removes the same geometry-only query header");
                helper.assertTrue(java.util.Arrays.stream(query.getClass().getDeclaredFields()).noneMatch(member -> member.getName().equals("regionalIndex"))
                                && query.retainedReaderCount() == 0,
                        "The fixed query holds no regional index or copied numerical frame");
                FHMain.LOGGER.info("THERMAL_TILED_PUBLICATION_RESULT,acceptedTicks=0:20:40,numericalCopies=0,unfinishedReceiptRejected=true,materialIdentity=true,retirement=true");
            }
            helper.assertTrue(budget.usedBytes() == 0, "Query metadata, core and banks release their owned storage");
        } finally {
            MinecraftThermalInput.closeActiveLevel(level);
            level.setChunkForced(origin.getX() >> 4, origin.getZ() >> 4, false);
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 200)
    public static void bulkReaderCapturesAcceptedValuesAndInvalidatesAtTheNextCommit(GameTestHelper helper) {
        try (var f = new Fixture(helper, 65536)) {
            f.heat(6400, 32);
            f.query.noteInfraredRequest(f.tick, 80);
            var cursor = new QueryPublication.ReadCursor();
            try {
                var sample = new QueryPublication.MutableSample(); var material = new MaterialSample();
                var page = f.handle.currentPublication();
                helper.assertTrue(f.query.beginRead(cursor) && cursor.valid() && f.query.retainedReaderCount() == 1,
                        "Borrow one direct accepted reader without pinning a numerical copy");
                helper.assertTrue(cursor.tryRead(page, local(f.air), page.topologyGeneration(), sample)
                                && sample.temperatureC() == 6400.0 / 1200 && sample.sampleTick() == f.tick,
                        "Bulk Air reads retain the same actual cell and time");
                helper.assertTrue(cursor.tryReadMaterial(page, local(f.matter), Block.getId(Blocks.STONE.defaultBlockState()), material)
                                && material.temperatureC() == 32 && cursor.freshCell(page, local(f.matter), page.topologyGeneration(), f.tick + 40, 40)
                                && !cursor.freshCell(page, local(f.matter), page.topologyGeneration(), f.tick + 41, 40),
                        "Material and freshness use the actual accepted sample tick");
                helper.assertTrue(cursor.isCurrent(), "The reader belongs to the current committed cell publication");
                var changes = f.geometry.change(f.air, Blocks.STONE.defaultBlockState(), ResolvedGeometryBatch.MaterialChanges.REPLACE);
                f.geometry.publish(changes, ThermalInputBatch.NO_PHASE_ACKS);
                helper.assertTrue(f.field.changeCell(f.air.asLong(), f.geometry.profiles.states().signatureId(Blocks.STONE.defaultBlockState()),
                        TiledThermalField.Change.REPLACEMENT), "Stage a real captured world replacement");
                var saved = cursor.captureValidatedAir(page, 0);
                helper.assertTrue(saved != null && saved.sampleC(f.air.getX() + .5, f.air.getY() + .5, f.air.getZ() + .5) == 6400.0 / 1200
                                && saved.sampleTickAt(f.air.getX() + .5, f.air.getY() + .5, f.air.getZ() + .5, -1) == f.tick,
                        "Air capture exports Accepted rather than the pending material overlay");
                var captured = MaterialSectionState.capture(page, f.query, f.geometry.profiles.signatures(),
                        new MaterialSectionState.CaptureScratch(), false, cursor);
                helper.assertTrue(captured.valid() && captured.state() != null && captured.state().find(local(f.matter)) >= 0,
                        "The existing material capture consumes the same Page-based reader");
                int epoch = cursor.brickChangeEpoch(page.workerPageSlot(), 0);
                begin(helper, f.field, f.banks, f.tick + 20); finish(helper, f.field);
                helper.assertTrue(cursor.valid() && cursor.tryReadMaterial(page, local(f.matter), Block.getId(Blocks.STONE.defaultBlockState()), material),
                        "A completed Working frame is invisible until publication");
                var next = f.handle.currentPublication();
                helper.assertTrue(f.query.publishFixed(new QueryPublication.SectionPublication[] {
                        new QueryPublication.SectionPublication(f.handle, next)}), "Commit values, geometry and input receipt");
                helper.assertTrue(!cursor.valid() && !cursor.tryReadMaterial(page, local(f.matter), Block.getId(Blocks.STONE.defaultBlockState()), material)
                                && cursor.captureValidatedAir(page, 0) == null,
                        "A batch cannot mix values from successive accepted cuts");
                helper.assertTrue(!MaterialSectionState.capture(PagePublication.EMPTY, f.query, f.geometry.profiles.signatures(),
                                new MaterialSectionState.CaptureScratch(), false, cursor).valid(),
                        "An invalidated reader cannot turn an empty traversal into a valid material deletion");
                helper.assertTrue(f.query.beginRead(cursor) && f.query.retainedReaderCount() == 1
                                && cursor.pageAirSampleTick(next) == f.tick + 20
                                && cursor.brickChangeEpoch(next.workerPageSlot(), 0) > epoch,
                        "Reacquiring replaces the lease and observes the new time and dirty epoch");
                helper.assertTrue(!cursor.tryRead(page, local(f.matter), 0, sample)
                                && cursor.tryReadMaterial(next, local(f.air), Block.getId(Blocks.STONE.defaultBlockState()), material),
                        "Only the matching committed geometry identifies the replacement material");
                FHMain.LOGGER.info("THERMAL_TILED_BULK_READER,acceptedCapture=true,workingInvisible=true,commitInvalidation=true,materialCapture=true");
            } finally { cursor.clear(); }
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 200)
    public static void frozenSampleAgeAndReaderCreditFollowTheirActualOwners(GameTestHelper helper) {
        try (var f = new Fixture(helper, 69632)) {
            f.heat(0.49 * 1200, 0);
            var cursor = new QueryPublication.ReadCursor();
            try {
                var page = f.handle.currentPublication();
                helper.assertTrue(f.query.beginRead(cursor) && cursor.captureValidatedAir(page, 0) == null,
                        "Save filtering keeps the existing 0.5 C threshold");
                f.banks.add(f.airReceiver, .02 * 1200); f.publish(f.tick);
                helper.assertTrue(f.query.beginRead(cursor) && cursor.captureValidatedAir(page, 0) != null,
                        "Meaningful residual Air remains saveable through the original representation");
                f.query.noteInfraredRequest(f.tick, 80); f.query.beginRead(cursor);
                int epoch = cursor.brickChangeEpoch(page.workerPageSlot(), 0);
                helper.assertTrue(f.field.freeze(SectionPos.asLong(f.origin)), "Freeze the actual residual frame");
                f.publish(f.tick + 41);
                f.query.noteInfraredRequest(f.tick + 41, 80); f.query.beginRead(cursor);
                helper.assertTrue(cursor.pageAirSampleTick(page) == f.tick && !cursor.hasFreshSample(f.tick + 41, 40)
                                && cursor.brickChangeEpoch(page.workerPageSlot(), 0) > epoch,
                        "Another publication never makes frozen values appear freshly simulated");
                helper.assertTrue(cursor.captureValidatedAir(page, 0) != null, "Stale frozen values remain valid save data");
                long retained = f.budget.usedBytes();
                f.query.close();
                helper.assertTrue(!cursor.valid() && f.budget.usedBytes() == retained && f.field.hasSection(SectionPos.asLong(f.origin)),
                        "Closing the query invalidates its reader and retains borrowed arrays' credit without closing the core");
                cursor.clear();
                helper.assertTrue(f.budget.usedBytes() < retained && f.query.retainedReaderCount() == 0,
                        "The last cursor releases the deferred directory credit");
                FHMain.LOGGER.info("THERMAL_TILED_BULK_LIFETIME,frozenAgeRetained=true,saveThresholdC=0.5,deferredReaderCreditReleased=true");
            } finally { cursor.clear(); }
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 200)
    public static void originalInfraredCaptureEncodesFixedMaterialsAndTheirChanges(GameTestHelper helper) throws Exception {
        try (var f = new Fixture(helper, 73728);
                var pages = readOnlyPages(f);
                var infrared = new InfraredCapture()) {
            var player = FakePlayerFactory.getMinecraft(helper.getLevel()); var previousPosition = player.position();
            try {
                player.setPos(f.air.getX() + .5, f.air.getY(), f.air.getZ() + .5);
                f.heat(6400, 42.125);
                var full = infrared.capture(pages, f.query, 1, player, true, 0, 0, 0, new long[12], false, new long[0], 0, 0, -1, -1);
                helper.assertTrue(full != null && full.full() && full.readable(), "Original infrared capture reads the fixed publication");
                expectInfrared(helper, full, f.matter, 42.125);
                f.heat(0, 57.625);
                long center = SectionPos.asLong(full.centerChunkX(), full.centerSectionY(), full.centerChunkZ());
                var delta = infrared.capture(pages, f.query, 1, player, false, full.generation(), center, full.infraredEpoch(),
                        full.presence(), full.readable(), full.fieldPages(), full.storedEpoch(), full.storedSampleTick(), -1, -1);
                helper.assertTrue(delta != null && !delta.full(), "A new accepted material value sends an incremental infrared record");
                expectInfrared(helper, delta, f.matter, 57.625);
                var wire = new FriendlyByteBuf(Unpooled.buffer());
                try {
                    new FHResponseInfraredViewDataSyncPacket(17, delta, 0).encode(wire);
                    var decoded = new FHResponseInfraredViewDataSyncPacket(wire);
                    helper.assertTrue(!wire.isReadable(), "The original response codec consumes the complete packet");
                    expectInfrared(helper, decoded.snapshot(), f.matter, 57.625);
                } finally { wire.release(); }
                helper.assertTrue(f.query.retainedReaderCount() == 0, "Infrared capture releases its read cursor");
                FHMain.LOGGER.info("THERMAL_TILED_INFRARED,fullAndDelta=true,originalCapture=true,responseCodec=true,readerReleased=true");
            } finally { player.setPos(previousPosition.x, previousPosition.y, previousPosition.z); }
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 200)
    public static void originalPhaseRequestsPublishTheirSequenceWithoutCopyingMaterialState(GameTestHelper helper) throws Exception {
        try (var f = new Fixture(helper, 77824, Blocks.ICE.defaultBlockState()); var pages = readOnlyPages(f)) {
            int slot = f.geometry.slot(f.matter);
            var states = new MaterialStateAccess() {
                @Override public void readMaterial(int address, MaterialSample out) {
                    out.clear();
                    helper.assertTrue(address == slot && f.query.tryReadMaterial(f.handle.currentPublication(), local(f.matter),
                            Block.getId(Blocks.ICE.defaultBlockState()), out), "The original phase runtime reads the fixed material owner");
                }
                @Override public void applyMaterialControl(int address, double energyJ, byte branch) { f.control(address, energyJ, branch); }
            };
            var phases = new PhaseTransitionRuntime(f.geometry.arena, states, 8);
            phases.onMaterialControlChanged(address -> {
                var sample = new MaterialSample(); states.readMaterial(address, sample);
                f.control(address, sample.enthalpyJ(), sample.branch());
            });
            phases.registerMaterial(slot);
            var cursor = new QueryPublication.ReadCursor();
            try {
                f.query.noteInfraredRequest(f.tick, 80); f.query.beginRead(cursor);
                int baseline = cursor.brickChangeEpoch(f.handle.currentPublication().workerPageSlot(), 0);
                double accountedJ = f.field.totalEnthalpyJ() + f.field.externalEnergyJ();
                helper.assertTrue(phases.forceMaterialTransition(slot, MaterialThermalLaw.HEATING), "Use the existing gameplay phase control");
                phases.advanceMaterialRequests(8);
                var requests = phases.drainRequests(8);
                helper.assertTrue(requests.length == 1 && requests[0].requestSequence() != 0 && !cursor.valid(),
                        "The existing phase owner emits one request and atomically invalidates the old batch");
                var request = requests[0]; var sample = new MaterialSample(); states.readMaterial(slot, sample);
                double completedJ = sample.enthalpyJ();
                helper.assertTrue(sample.requestSequence() == request.requestSequence() && sample.sampleTick() == f.tick
                                && sample.law().heating().complete(completedJ)
                                && Math.abs(f.field.totalEnthalpyJ() + f.field.externalEnergyJ() - accountedJ) < 1e-6,
                        "Request metadata preserves the numerical clock and records controlled external energy once");
                f.query.beginRead(cursor);
                helper.assertTrue(cursor.brickChangeEpoch(f.handle.currentPublication().workerPageSlot(), 0) > baseline,
                        "Material controls dirty the original infrared Brick");
                var main = new MinecraftPhaseController(helper.getLevel(), pages, f.geometry.profiles, null, 8);
                var matches = MinecraftPhaseController.class.getDeclaredMethod("matchesMaterialRequest", ThermalPageHandle.class,
                        PhaseTransitionRuntime.Request.class, QueryPublication.class); matches.setAccessible(true);
                helper.assertTrue((Boolean) matches.invoke(main, f.handle, request, f.query),
                        "The original main-thread matcher accepts the fixed query's real outstanding request");
                var coreSample = new TiledThermalField.Sample(); f.field.read(f.matter.asLong(), true, coreSample);
                begin(helper, f.field, f.banks, f.tick + 20);
                helper.assertTrue(!f.query.publishFixedMaterialControl(f.handle.currentPublication(), local(f.matter), coreSample.materialGeneration(),
                                completedJ, sample.branch(), true, request.requestSequence()),
                        "A control cannot mutate Accepted while numerical work owns its next frame");
                finish(helper, f.field);
                helper.assertTrue(f.query.publishFixed(NO_GEOMETRY), "Publish the numerical interval");
                states.readMaterial(slot, sample);
                helper.assertTrue(sample.requestSequence() == request.requestSequence() && sample.enthalpyJ() == completedJ,
                        "The request and completed latent energy survive a numerical buffer swap");
                helper.assertTrue(phases.applyAck(request, PhaseTransitionRuntime.AckOutcome.RETRY), "Retry through the original acknowledgement owner");
                phases.advanceMaterialRequests(8);
                var retried = phases.drainRequests(8);
                helper.assertTrue(retried.length == 1 && retried[0].requestSequence() == request.requestSequence(),
                        "Retry retains the same outstanding request");
                helper.assertTrue(phases.applyAck(request, PhaseTransitionRuntime.AckOutcome.REJECTED), "Resolve a rejected world application");
                phases.advanceMaterialRequests(8);
                var newer = phases.drainRequests(8);
                helper.assertTrue(newer.length == 1 && newer[0].requestSequence() > request.requestSequence()
                                && !phases.applyAck(request, PhaseTransitionRuntime.AckOutcome.APPLIED)
                                && !(Boolean) matches.invoke(main, f.handle, request, f.query)
                                && (Boolean) matches.invoke(main, f.handle, newer[0], f.query),
                        "A late old acknowledgement cannot consume or impersonate the next request");
                FHMain.LOGGER.info("THERMAL_TILED_PHASE_PUBLICATION,originalPhaseRuntime=true,mainMatcher=true,retryIdentity=true,lateAckRejected=true,controlledEnergyConserved=true");
            } finally { cursor.clear(); }
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 200)
    public static void requestMetadataSurvivesLocalGrowthAndFreezeButNotMaterialReplacement(GameTestHelper helper) {
        try (var f = new Fixture(helper, 81920, Blocks.ICE.defaultBlockState())) {
            var sample = new TiledThermalField.Sample(); f.field.read(f.matter.asLong(), true, sample);
            int incarnation = sample.materialGeneration(); double completedJ = sample.law().heating().targetEnthalpyJ();
            helper.assertTrue(f.query.publishFixedMaterialControl(f.handle.currentPublication(), local(f.matter), incarnation,
                    completedJ, MaterialThermalLaw.HEATING, true, 71), "Publish a completed outstanding material control");
            int[] captured = new int[4096]; Arrays.fill(captured, ThermalSignatureTable.UNRESOLVED);
            for (int x = 0; x < 8; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++) {
                var position = f.origin.offset(x, y, z);
                captured[local(position)] = f.geometry.profiles.states().signatureId(helper.getLevel().getBlockState(position));
            }
            helper.assertTrue(f.field.grow(f.handle.sectionKey(), captured, 2) && f.field.freeze(f.handle.sectionKey()),
                    "Grow one actual local Brick and freeze the retained state");
            f.publish(f.tick + 80);
            helper.assertTrue(f.field.thaw(f.handle.sectionKey(), -20, 30) && f.field.read(f.matter.asLong(), true, sample)
                            && sample.enthalpyJ() == completedJ && sample.requestSequence() == 71 && sample.materialGeneration() == incarnation,
                    "Pending world application keeps its completed H and request through growth, freeze and thaw");
            f.publish(f.tick + 80);
            helper.assertTrue(f.field.read(f.matter.asLong(), true, sample) && sample.requestSequence() == 71,
                    "Both numerical buffers retain the request after thaw");
            int signature = f.geometry.profiles.states().signatureId(Blocks.ICE.defaultBlockState());
            helper.assertTrue(f.field.changeCell(f.matter.asLong(), signature, TiledThermalField.Change.REPLACEMENT),
                    "Capture a same-state replacement as a new physical incarnation");
            f.publish(f.tick + 80);
            helper.assertTrue(f.field.read(f.matter.asLong(), true, sample) && sample.materialGeneration() != incarnation && sample.requestSequence() == 0
                            && !f.query.publishFixedMaterialControl(f.handle.currentPublication(), local(f.matter), incarnation,
                                    completedJ, MaterialThermalLaw.HEATING, true, 71),
                    "The replacement cannot inherit or accept the old body's request");
            FHMain.LOGGER.info("THERMAL_TILED_PHASE_LIFETIME,growth=true,freezeThaw=true,completedLatentPreserved=true,replacementClearsRequest=true");
        }
        helper.succeed();
    }

    private static MinecraftPageManager readOnlyPages(Fixture f) throws Exception {
        var pages = new MinecraftPageManager(null, f.helper.getLevel(), null, null, null);
        // Read-only fixture registration uses the original directory; it never starts another world runtime.
        var method = MinecraftPageManager.class.getDeclaredMethod("entry", long.class); method.setAccessible(true);
        var entry = method.invoke(pages, f.handle.sectionKey());
        var handle = entry.getClass().getDeclaredField("handle"); handle.setAccessible(true); handle.set(entry, f.handle);
        return pages;
    }

    private static void expectInfrared(GameTestHelper helper, MinecraftThermalInput.InfraredSnapshot snapshot, BlockPos position, double temperature) {
        int page = ((position.getY() >> 4) - snapshot.centerSectionY() + 4) * 81
                + ((position.getZ() >> 4) - snapshot.centerChunkZ() + 4) * 9 + (position.getX() >> 4) - snapshot.centerChunkX() + 4;
        int brick = (position.getX() & 15) >>> 2 | ((position.getZ() & 15) >>> 2) << 2 | ((position.getY() & 15) >>> 2) << 4;
        int block = (position.getX() & 3) | (position.getZ() & 3) << 2 | (position.getY() & 3) << 4;
        var decoder = new InfraredBrickCodec.Decoder(); var values = new short[64];
        for (byte[] part : snapshot.brickRecords()) {
            var bytes = new FriendlyByteBuf(Unpooled.wrappedBuffer(part));
            try {
                int address;
                while ((address = decoder.readRecord(bytes, values)) >= 0) if (address == page * 64 + brick) {
                    helper.assertTrue(values[block] == InfraredBrickCodec.quantize(temperature), "The wired material cell equals its Accepted temperature");
                    return;
                }
            } finally { bytes.release(); }
        }
        helper.fail("Missing infrared record for the captured material cell");
    }

    private static final class Fixture implements AutoCloseable {
        final GameTestHelper helper;
        final BlockPos origin, air, matter;
        final ThermalMemoryBudget budget = new ThermalMemoryBudget(4L << 20);
        final ThermalMaterialBehaviorGameTests.Simulation geometry;
        final SpatialEnergyBanks banks;
        final TiledThermalField field;
        final QueryPublication query;
        final ThermalPageHandle handle;
        final long tick, airReceiver, materialReceiver;
        Fixture(GameTestHelper helper, int coordinate) {
            this(helper, coordinate, Blocks.STONE.defaultBlockState());
        }
        Fixture(GameTestHelper helper, int coordinate, BlockState material) {
            this.helper = helper; origin = new BlockPos(coordinate, 192, coordinate); air = origin.offset(1, 1, 1); matter = origin.offset(2, 1, 1);
            var level = helper.getLevel(); tick = level.getGameTime();
            level.setChunkForced(origin.getX() >> 4, origin.getZ() >> 4, true);
            for (int x = 0; x < 4; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++)
                level.setBlockAndUpdate(origin.offset(x, y, z), Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(air, Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(matter, material);
            MinecraftThermalInput.closeActiveLevel(level);
            geometry = new ThermalMaterialBehaviorGameTests.Simulation(helper, origin, 4, 4, 4, 0);
            banks = new SpatialEnergyBanks(budget, 8, 8, tick);
            field = new TiledThermalField(budget, geometry.profiles.signatures(), geometry.profiles.materials(), 1200, 0, 0, 8, tick);
            query = QueryPublication.createFixed(budget, 8, field);
            int[] captured = new int[4096]; Arrays.fill(captured, ThermalSignatureTable.UNRESOLVED);
            for (int x = 0; x < 4; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++) {
                var position = origin.offset(x, y, z);
                captured[local(position)] = geometry.profiles.states().signatureId(level.getBlockState(position));
            }
            helper.assertTrue(field.admit(SectionPos.asLong(origin), captured, 1, 0), "Admit captured fixture values");
            handle = geometry.page(origin);
            begin(helper, field, banks, tick); finish(helper, field);
            helper.assertTrue(query.publishFixed(new QueryPublication.SectionPublication[] {
                    new QueryPublication.SectionPublication(handle, handle.currentPublication())}), "Publish the initial fixture");
            airReceiver = banks.retain(new SpatialEnergyBanks.Address(air.asLong(), -1));
            var sample = new TiledThermalField.Sample(); field.read(matter.asLong(), true, sample);
            materialReceiver = banks.retain(new SpatialEnergyBanks.Address(matter.asLong(), sample.materialGeneration()));
        }
        void heat(double airJ, double materialTemperature) {
            var sample = new TiledThermalField.Sample(); field.read(matter.asLong(), true, sample);
            helper.assertTrue(banks.add(airReceiver, airJ)
                    && banks.add(materialReceiver, sample.law().enthalpyAtTemperature(materialTemperature) - sample.enthalpyJ()), "Capture fixture input");
            publish(tick);
        }
        void publish(long target) {
            begin(helper, field, banks, target); finish(helper, field);
            helper.assertTrue(query.publishFixed(NO_GEOMETRY), "Publish the next fixture state");
        }
        void control(int slot, double energyJ, byte branch) {
            var sample = new TiledThermalField.Sample();
            helper.assertTrue(field.read(matter.asLong(), true, sample) && query.publishFixedMaterialControl(handle.currentPublication(),
                    local(matter), sample.materialGeneration(), energyJ, branch, geometry.arena.materialTransitionWaiting(slot),
                    geometry.arena.phaseRequestSequence(slot)), "Publish the existing phase owner's material control");
        }
        @Override public void close() {
            query.close(); field.close(); banks.close(); geometry.close();
            MinecraftThermalInput.closeActiveLevel(helper.getLevel());
            helper.getLevel().setChunkForced(origin.getX() >> 4, origin.getZ() >> 4, false);
            helper.assertTrue(budget.usedBytes() == 0, "Release fixture query, core and input ownership");
        }
    }

    private static boolean read(QueryPublication query, BlockPos position, QueryPublication.MutableSample out) {
        return query.tryReadAir(position.getX() + .5, position.getY() + .5, position.getZ() + .5, out);
    }
    private static int local(BlockPos position) { return (position.getX() & 15) | (position.getZ() & 15) << 4 | (position.getY() & 15) << 8; }
    private static void begin(GameTestHelper helper, TiledThermalField field, SpatialEnergyBanks banks, long tick) {
        helper.assertTrue(field.begin(banks.seal(tick)), "Begin the next core interval");
    }
    private static void finish(GameTestHelper helper, TiledThermalField field) {
        long deadline = System.nanoTime() + 10_000_000_000L;
        while (!field.advance(System.nanoTime() + 1_000_000L)) helper.assertTrue(System.nanoTime() < deadline, "Complete the bounded core step");
    }
}
