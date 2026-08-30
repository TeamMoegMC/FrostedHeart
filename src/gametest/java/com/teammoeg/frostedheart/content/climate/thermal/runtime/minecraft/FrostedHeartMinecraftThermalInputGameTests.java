/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.content.climate.thermal.geometry.ConservativeAirGeometry;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialBoundaryRegistry;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.PagePublication;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.PageSignatures;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalBrickCellLayout;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ResolvedThermalSignature;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureRegistry;
import com.teammoeg.frostedheart.content.climate.thermal.query.QueryPublication;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.engine.ThermalDimensionEngine;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.engine.ThermalDimensionLimits;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ResolvedGeometryBatch;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalCompletion;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputBatch;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget;
import com.teammoeg.frostedheart.content.climate.thermal.solver.BuoyancyConductance;
import com.teammoeg.frostedheart.content.climate.thermal.solver.PhaseTransitionRuntime;
import com.teammoeg.frostedheart.content.climate.thermal.source.EmissionPort;
import com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.MinecraftPhysicalSourceProfile;
import com.teammoeg.frostedheart.content.climate.thermal.source.NodePowerAccumulatorArena;
import com.teammoeg.frostedheart.content.climate.thermal.source.SourceBinding;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceBatch;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceLedger;
import com.teammoeg.frostedheart.content.climate.thermal.topology.FarFieldSettings;
import com.teammoeg.frostedheart.content.climate.thermal.topology.ThermalTopologyParameters;
import com.teammoeg.frostedheart.FHMain;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.Arrays;
import java.util.List;

@GameTestHolder(FHMain.MODID)
@PrefixGameTestTemplate(false)
public final class FrostedHeartMinecraftThermalInputGameTests {
    private static final String TEMPLATE = "phase0a_empty";
    private static final String BATCH = "frostedheart_thermal_async";

    private FrostedHeartMinecraftThermalInputGameTests() {
    }

    @GameTest(template = TEMPLATE, batch = BATCH + "_empty", timeoutTicks = 40)
    public static void emptyCutPublishesAnEmptyQueryEnvelope(GameTestHelper helper) {
        Fixture fixture = fixture();
        try {
            ThermalCompletion completion = fixture.engine.process(batch(1L, 0L));
            helper.assertTrue(
                    completion.status() == ThermalCompletion.Status.COMPLETED,
                    "empty cut must complete");
            helper.assertTrue(
                    !fixture.query.tryRead(0, 1, 0L, new QueryPublication.MutableSample()),
                    "empty arena has no query slot");
            helper.succeed();
        } finally {
            fixture.engine.close();
        }
    }

    @GameTest(template = TEMPLATE, batch = BATCH + "_admission", timeoutTicks = 40)
    public static void admissionPublishesAResolvedPage(GameTestHelper helper) {
        Fixture fixture = fixture();
        try {
            ThermalCompletion completion = fixture.engine.process(admissionBatch(
                    1L, 20L, fixture.page, fixture.signatures));
            PagePublication publication = fixture.page.currentPublication();
            helper.assertTrue(
                    completion.status() == ThermalCompletion.Status.COMPLETED
                            && publication != null
                            && fixture.arena.liveCellCount() == 64,
                    "admission must compile 64 regular Air cells");
            helper.succeed();
        } finally {
            fixture.engine.close();
        }
    }

    @GameTest(template = TEMPLATE, batch = BATCH + "_continuation", timeoutTicks = 40)
    public static void continuationCarriesExactPageIdentity(GameTestHelper helper) {
        Fixture fixture = fixture();
        try {
            ThermalCompletion completion = fixture.engine.process(admissionBatch(
                    1L, 20L, fixture.page, fixture.signatures));
            ThermalCompletion.PageContinuation continuation =
                    completion.continuations()[0];
            helper.assertTrue(
                    continuation.sectionKey() == fixture.page.sectionKey()
                            && continuation.lifecycleGeneration()
                                    == fixture.page.lifecycleGeneration()
                            && Byte.toUnsignedInt(continuation.faceMask()) == 0x3f,
                    "continuation must carry the committed Page identity and mask");
            helper.succeed();
        } finally {
            fixture.engine.close();
        }
    }

    @GameTest(template = TEMPLATE, batch = BATCH + "_mutation", timeoutTicks = 40)
    public static void localMutationRebuildsOnlyTheChangedBrick(GameTestHelper helper) {
        Fixture fixture = fixture();
        try {
            fixture.engine.process(admissionBatch(1L, 20L, fixture.page, fixture.signatures));
            long revision = fixture.page.beginGeometryMutation();
            ThermalCompletion completion = fixture.engine.process(
                    mutationBatch(2L, 40L, fixture.page, revision, fixture.solidId));
            helper.assertTrue(
                    completion.status() == ThermalCompletion.Status.COMPLETED
                            && fixture.page.currentPublication() != null
                            && fixture.page.currentPublication().geometryRevision() == revision,
                    "local geometry must publish its exact revision");
            helper.succeed();
        } finally {
            fixture.engine.close();
        }
    }

    @GameTest(template = TEMPLATE, batch = BATCH + "_retirement", timeoutTicks = 40)
    public static void retirementClearsThePageAndArenaCells(GameTestHelper helper) {
        Fixture fixture = fixture();
        try {
            fixture.engine.process(admissionBatch(1L, 20L, fixture.page, fixture.signatures));
            ThermalInputBatch retirement = new ThermalInputBatch(
                    1L, 2L, 40L,
                    ThermalInputBatch.NO_ADMISSIONS,
                    new ThermalInputBatch.PageRetirement[]{
                            new ThermalInputBatch.PageRetirement(fixture.page)},
                    ResolvedGeometryBatch.EMPTY,
                    ThermalSourceBatch.EMPTY,
                    ThermalInputBatch.NO_ENVIRONMENT_UPDATES,
                    ThermalInputBatch.NO_PHASE_ACKS,
                    Double.NaN);
            fixture.engine.process(retirement);
            helper.assertTrue(
                    fixture.page.currentPublication() == null
                            && fixture.arena.liveCellCount() == 0,
                    "retirement must remove the Page publication and cells");
            helper.succeed();
        } finally {
            fixture.engine.close();
        }
    }

    @GameTest(template = TEMPLATE, batch = BATCH + "_query", timeoutTicks = 40)
    public static void flatQueryPublicationValidatesTheArenaGeneration(GameTestHelper helper) {
        Fixture fixture = fixture();
        try {
            fixture.engine.process(admissionBatch(1L, 20L, fixture.page, fixture.signatures));
            PagePublication.Brick brick = fixture.page.currentPublication()
                    .brickAt(0, 0, 0);
            QueryPublication.MutableSample sample =
                    new QueryPublication.MutableSample();
            helper.assertTrue(
                    fixture.query.tryRead(
                            brick.coverageSlot(), brick.arenaGeneration(),
                            1L, sample),
                    "query publication must read the live slot");
            helper.assertTrue(
                    !fixture.query.tryRead(
                            brick.coverageSlot(), brick.arenaGeneration() + 1,
                            1L, sample),
                    "stale arena generation must be rejected");
            helper.succeed();
        } finally {
            fixture.engine.close();
        }
    }

    @GameTest(template = TEMPLATE, batch = BATCH + "_source", timeoutTicks = 40)
    public static void sourceLedgerDeliversPowerAtTheExactCut(GameTestHelper helper) {
        ThermalCellArena arena = new ThermalCellArena(1);
        ThermalCellArena.BrickAllocation allocation = regular(arena, 0, 1, 0);
        ThermalSourceLedger ledger = new ThermalSourceLedger(
                0L, 1, 1, 8,
                new NodePowerAccumulatorArena(1, 8), arena);
        ThermalSourceBatch.Builder events = new ThermalSourceBatch.Builder(0L);
        events.addRegister(
                0L, 1, com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceMode.POWER_SOURCE,
                20.0D, true, 0L, 0, 0, 0, 1,
                new EmissionPort[]{EmissionPort.of(
                        0, 1.0D, SourceBinding.thermalNode(
                                allocation.cellSpan().firstSlot(), 1))});
        ledger.acceptAndAdvance(
                events.buildAndReset(), 20L, (batch, index, current) -> { });
        helper.assertTrue(
                Math.abs(arena.enthalpyJ(allocation.cellSpan().firstSlot()) - 20.0D) < 1.0e-9D,
                "source energy must integrate for one second");
        ledger.close();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH + "_phase", timeoutTicks = 40)
    public static void phaseRequestAcknowledgementIsExactlyOnce(GameTestHelper helper) {
        ThermalCellArena arena = new ThermalCellArena(2);
        ThermalCellArena.BrickAllocation allocation = phase(arena);
        PhaseTransitionRuntime phases = new PhaseTransitionRuntime(arena, 2);
        int phaseSlot = allocation.phaseReservoirSlots()[0];
        int airSlot = allocation.cellSpan().firstSlot();
        phases.registerReservoir(phaseSlot);
        arena.setEnthalpyJ(airSlot, 10_000.0D);
        phases.applyContact(airSlot, phaseSlot, 100.0D, 0.0D, 1.0D);
        PhaseTransitionRuntime.Request[] requests = phases.drainRequests(2);
        helper.assertTrue(
                requests.length == 1
                        && phases.applyAck(
                                requests[0], PhaseTransitionRuntime.AckOutcome.APPLIED)
                        && !phases.applyAck(
                                requests[0], PhaseTransitionRuntime.AckOutcome.APPLIED),
                "phase ACK must mutate one request once");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH + "_sequence", timeoutTicks = 40)
    public static void staleBatchIdentityIsRejected(GameTestHelper helper) {
        Fixture fixture = fixture();
        try {
            boolean rejected = false;
            try {
                fixture.engine.process(batch(2L, 20L));
            } catch (IllegalArgumentException expected) {
                rejected = true;
            }
            helper.assertTrue(rejected, "sequence must start at one");
            helper.succeed();
        } finally {
            fixture.engine.close();
        }
    }

    @GameTest(template = TEMPLATE, batch = BATCH + "_resync", timeoutTicks = 40)
    public static void resyncTokenRejectsAChangedLiveRevision(GameTestHelper helper) {
        ThermalPageHandle page = new ThermalPageHandle(0L, 1L);
        page.requireFullGeometryResync(
                ThermalPageHandle.GeometryResyncReason.SECTION_REPLACED);
        ThermalPageHandle.GeometryResyncToken token =
                page.pendingFullGeometryResync();
        page.beginGeometryMutation();
        helper.assertTrue(
                !page.acknowledgeFullGeometryResync(token),
                "a changed live revision must reject the stale token");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH + "_signature", timeoutTicks = 40)
    public static void signatureDirectoryKeepsUnchangedBricksShared(GameTestHelper helper) {
        ThermalSignatureRegistry.Builder registry = ThermalSignatureRegistry.builder();
        int air = registry.intern(fullAir());
        PageSignatures original = page(air);
        int[] changed = new int[64];
        Arrays.fill(changed, air);
        PageSignatures next = original.withBricks(new int[]{0}, new int[][]{changed});
        helper.assertTrue(
                original.brickPayload(1) == next.brickPayload(1),
                "unchanged Brick payload must be shared");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH + "_pool", timeoutTicks = 40)
    public static void engineCloseIsIdempotent(GameTestHelper helper) {
        Fixture fixture = fixture();
        fixture.engine.close();
        fixture.engine.close();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH + "_fixed", timeoutTicks = 40)
    public static void fixedTwentyTickBatchesAdvanceWithoutLegacyScheduler(GameTestHelper helper) {
        Fixture fixture = fixture();
        try {
            ThermalCompletion first = fixture.engine.process(batch(1L, 0L));
            ThermalCompletion second = fixture.engine.process(batch(2L, 20L));
            helper.assertTrue(
                    first.status() == ThermalCompletion.Status.COMPLETED
                            && second.status() == ThermalCompletion.Status.COMPLETED,
                    "fixed 20-tick batches must use the direct engine pipeline");
            helper.succeed();
        } finally {
            fixture.engine.close();
        }
    }

    private static ThermalInputBatch admissionBatch(
            long sequence,
            long tick,
            ThermalPageHandle page,
            ThermalSignatureRegistry signatures
    ) {
        return new ThermalInputBatch(
                1L, sequence, tick,
                new ThermalInputBatch.PageAdmission[]{
                        new ThermalInputBatch.PageAdmission(
                                page, page.liveGeometryRevision(),
                                page(signatures, 0), 0.0D, sky(), null)},
                ThermalInputBatch.NO_RETIREMENTS,
                ResolvedGeometryBatch.EMPTY,
                ThermalSourceBatch.EMPTY,
                ThermalInputBatch.NO_ENVIRONMENT_UPDATES,
                ThermalInputBatch.NO_PHASE_ACKS,
                Double.NaN);
    }

    private static ThermalInputBatch mutationBatch(
            long sequence,
            long tick,
            ThermalPageHandle page,
            long revision,
            int signatureId
    ) {
        ResolvedGeometryBatch.Builder geometry =
                new ResolvedGeometryBatch.Builder();
        geometry.addResolvedCenter(page, revision, 0, signatureId);
        return new ThermalInputBatch(
                1L, sequence, tick,
                ThermalInputBatch.NO_ADMISSIONS,
                ThermalInputBatch.NO_RETIREMENTS,
                geometry.buildAndReset(),
                ThermalSourceBatch.EMPTY,
                ThermalInputBatch.NO_ENVIRONMENT_UPDATES,
                ThermalInputBatch.NO_PHASE_ACKS,
                Double.NaN);
    }

    private static ThermalInputBatch batch(long sequence, long tick) {
        return new ThermalInputBatch(
                1L, sequence, tick,
                ThermalInputBatch.NO_ADMISSIONS,
                ThermalInputBatch.NO_RETIREMENTS,
                ResolvedGeometryBatch.EMPTY,
                ThermalSourceBatch.EMPTY,
                ThermalInputBatch.NO_ENVIRONMENT_UPDATES,
                ThermalInputBatch.NO_PHASE_ACKS,
                Double.NaN);
    }

    private static Fixture fixture() {
        ThermalSignatureRegistry.Builder builder = ThermalSignatureRegistry.builder();
        int airId = builder.intern(fullAir());
        int solidId = builder.intern(new ResolvedThermalSignature(
                new ConservativeAirGeometry.Resolution(
                        ConservativeAirGeometry.Status.RESOLVED, List.of()),
                0, 0));
        ThermalSignatureRegistry signatures = builder.build();
        ThermalCellArena arena = new ThermalCellArena(256);
        QueryPublication query = QueryPublication.tryCreate(
                new ThermalMemoryBudget(8L * 1024L * 1024L)
                        .createDimensionBudget(8L * 1024L * 1024L),
                256);
        ThermalDimensionEngine engine = new ThermalDimensionEngine(
                1L, 0L, arena, signatures,
                new MaterialBoundaryRegistry(List.of(), List.of()),
                new ThermalTopologyParameters(
                        64, 1_200.0D, 0.0D, 1.0D, 0.25D,
                        new BuoyancyConductance.Parameters(0.25D, 4.0D, 10.0D),
                        8, 4),
                new FarFieldSettings(1.0D, 1.0D, 16.0D),
                MinecraftPhysicalSourceProfile.CAMPFIRE,
                new ThermalDimensionLimits(
                        16, 128, 256,
                        4_096, 2_048, 4_096, 4_096, 4_096,
                        2, 1.0e-6D),
                query);
        return new Fixture(
                engine,
                arena,
                query,
                new ThermalPageHandle(0L, 1L),
                signatures,
                airId,
                solidId);
    }

    private static PageSignatures page(
            ThermalSignatureRegistry signatures,
            int ignored
    ) {
        int id = signatures.idOrDefault(fullAir(), 0);
        return page(id);
    }

    private static PageSignatures page(int id) {
        PageSignatures.Builder builder = new PageSignatures.Builder();
        for (int index = 0; index < PageSignatures.ENTRY_COUNT; index++) {
            builder.set(index, id);
        }
        return builder.build();
    }

    private static byte[] sky() {
        byte[] result = new byte[256];
        Arrays.fill(result, (byte) 16);
        return result;
    }

    private static ResolvedThermalSignature fullAir() {
        return new ResolvedThermalSignature(
                new ConservativeAirGeometry.Resolution(
                        ConservativeAirGeometry.Status.RESOLVED,
                        List.of(new ConservativeAirGeometry.AirComponent(
                                0, -1L, 0xffff, 0xffff, 0xffff,
                                0xffff, 0xffff, 0xffff))),
                0, 0);
    }

    private static ThermalCellArena.BrickAllocation regular(
            ThermalCellArena arena,
            int pageSlot,
            int generation,
            int minX
    ) {
        ThermalBrickCellLayout layout = new ThermalBrickCellLayout();
        layout.reset(minX, 0, 0);
        layout.setRegularAir(100.0D / 64.0D);
        ThermalCellArena.BrickAllocation allocation = arena.stageBrickCells(
                pageSlot, generation, layout, 0.0D, 0.0D, 4_096);
        arena.commitStagedCells(allocation.cellSpan());
        return allocation;
    }

    private static ThermalCellArena.BrickAllocation phase(
            ThermalCellArena arena
    ) {
        ThermalBrickCellLayout layout = new ThermalBrickCellLayout();
        layout.reset(0, 0, 0);
        layout.setRegularAir(100.0D / 64.0D);
        layout.addPhaseReservoir(
                0, 0, 0, 1, 1L, 0.0D, 10.0D);
        ThermalCellArena.BrickAllocation allocation = arena.stageBrickCells(
                0, 1, layout, 0.0D, 0.0D, 4_096);
        arena.commitStagedCells(allocation.cellSpan());
        return allocation;
    }

    private record Fixture(
            ThermalDimensionEngine engine,
            ThermalCellArena arena,
            QueryPublication query,
            ThermalPageHandle page,
            ThermalSignatureRegistry signatures,
            int airId,
            int solidId
    ) {
    }
}
