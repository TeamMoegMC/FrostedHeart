/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.profile.minecraft;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.thermal.geometry.ConservativeAirGeometry;
import com.teammoeg.frostedheart.content.climate.thermal.profile.DependencyOffsetMask;
import com.teammoeg.frostedheart.content.climate.thermal.profile.LocalAirRegionPattern;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ResolvedThermalSignature;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ResolverBlockView;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalResolution;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureRegistry;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureResolver;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** One-pass dispatcher census plus a second immutable-registry reload prototype. */
@GameTestHolder(FHMain.MODID)
@PrefixGameTestTemplate(false)
public final class FrostedHeartPhaseAResolverCensusGameTests {
    public static final String REPORT_PROPERTY = "frostedheart.phaseAResolverCensusReport";
    private static final String BATCH = "frostedheart_phase_a_resolver_census";
    private static final String TEMPLATE = "phase0a_empty";
    private static final int CENSUS_MAXIMUM_REGIONS =
            ConservativeAirGeometry.MICROCELL_COUNT;
    private static final DependencyOffsetMask.Offset EAST =
            new DependencyOffsetMask.Offset(1, 0, 0);

    private FrostedHeartPhaseAResolverCensusGameTests() {
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void activeRegistryResolverDispatchAndReloadCensus(GameTestHelper helper) {
        ThermalSignatureResolverDispatcher dispatcher = createDispatcher();
        List<Block> blocks = new ArrayList<>(ForgeRegistries.BLOCKS.getValues());
        blocks.sort(Comparator.comparing(
                FrostedHeartPhaseAResolverCensusGameTests::registryId));

        CensusPass first = runCensus(blocks, dispatcher);
        CensusPass reload = runCensus(blocks, dispatcher);

        helper.assertTrue(first.stateCount() > 0
                        && first.accountedStateCount() == first.stateCount(),
                "resolver census must account for every active BlockState");
        helper.assertTrue(first.routeCount(
                        ThermalSignatureResolverDispatcher.Route.GENERIC_STATE_STATIC) > 0,
                "resolver census must exercise the automatic generic-static route");
        helper.assertTrue(first.explicitResolvedStateCount() > 0,
                "resolver census must exercise a physical explicit profile");
        helper.assertTrue(first.contextualResolvedStateCount() > 0
                        && first.contextualOutputCount() == 2,
                "resolver census must exercise both bounded contextual fixture outputs");
        helper.assertTrue(first.movingPistonClassified(),
                "moving piston must remain UNRESOLVED_DYNAMIC during the census");
        helper.assertTrue(registriesEqual(first.registry(), reload.registry()),
                "reloading identical resolver inputs must produce deterministic signature IDs");

        writeReport(blocks.size(), dispatcher, first, reload);
        helper.succeed();
    }

    private static ThermalSignatureResolverDispatcher createDispatcher() {
        return ThermalSignatureResolverDispatcher.builder(
                        StateStaticThermalResolver.geometryOnly(CENSUS_MAXIMUM_REGIONS))
                .registerExplicitProfile(
                        Blocks.STONE,
                        "frostedheart:phase_a_stone_physical_fixture",
                        explicitStoneSignature())
                .registerContextual(Blocks.BAMBOO, new BambooContextualFixtureResolver())
                .build();
    }

    private static CensusPass runCensus(
            List<Block> blocks,
            ThermalSignatureResolverDispatcher dispatcher
    ) {
        ThermalSignatureRegistry.Builder signatures = ThermalSignatureRegistry.builder();
        Map<ThermalResolution.Status, Integer> statusCounts =
                new EnumMap<>(ThermalResolution.Status.class);
        Map<ThermalResolution.Reason, Integer> reasonCounts =
                new EnumMap<>(ThermalResolution.Reason.class);
        Map<ThermalSignatureResolverDispatcher.Route, Integer> routeCounts =
                new EnumMap<>(ThermalSignatureResolverDispatcher.Route.class);
        Map<Integer, Integer> regionHistogram = new TreeMap<>();
        Set<ResolvedThermalSignature> contextualOutputs = new HashSet<>();
        Set<List<LocalAirRegionPattern>> geometryOutputs = new HashSet<>();

        int stateCount = 0;
        int stateStaticCount = 0;
        int dynamicStateCount = 0;
        int explicitResolvedStateCount = 0;
        int contextualResolvedStateCount = 0;
        int maximumObservedRegions = 0;
        long registeredDeclaredSnapshotCells = 0L;
        boolean movingPistonClassified = false;

        long started = System.nanoTime();
        for (Block block : blocks) {
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                stateCount++;
                if (block.hasDynamicShape()) {
                    dynamicStateCount++;
                } else {
                    stateStaticCount++;
                }

                ThermalSignatureResolverDispatcher.DispatchPlan plan = dispatcher.plan(state);
                routeCounts.merge(plan.route(), 1, Integer::sum);
                if (plan.isRegistered()) {
                    registeredDeclaredSnapshotCells += plan.dependencyMask().offsetCount();
                }
                ThermalResolution<ResolvedThermalSignature> resolution = plan.resolve(
                        fixtureSnapshot(state, plan.dependencyMask(),
                                Blocks.AIR.defaultBlockState()));
                statusCounts.merge(resolution.status(), 1, Integer::sum);
                reasonCounts.merge(resolution.reason(), 1, Integer::sum);
                if (resolution.isResolved()) {
                    ResolvedThermalSignature signature = resolution.value().orElseThrow();
                    signatures.intern(signature);
                    geometryOutputs.add(signature.airRegions());
                    int regions = signature.localAirRegionCount();
                    regionHistogram.merge(regions, 1, Integer::sum);
                    maximumObservedRegions = Math.max(maximumObservedRegions, regions);
                    if (plan.route()
                            == ThermalSignatureResolverDispatcher.Route.EXPLICIT_OVERRIDE) {
                        explicitResolvedStateCount++;
                    } else if (plan.route()
                            == ThermalSignatureResolverDispatcher.Route.CONTEXTUAL) {
                        contextualResolvedStateCount++;
                        contextualOutputs.add(signature);
                    }
                }

                if (state.is(Blocks.MOVING_PISTON)) {
                    movingPistonClassified = resolution.status()
                            == ThermalResolution.Status.UNRESOLVED
                            && resolution.reason()
                            == ThermalResolution.Reason.UNRESOLVED_DYNAMIC;
                }
            }
        }

        // The active-state pass uses an air east neighbor. Add the second
        // declared context output so reload sizing includes the full fixture domain.
        ThermalSignatureResolverDispatcher.DispatchPlan contextual =
                dispatcher.plan(Blocks.BAMBOO.defaultBlockState());
        ThermalResolution<ResolvedThermalSignature> eastStone = contextual.resolve(
                fixtureSnapshot(
                        Blocks.BAMBOO.defaultBlockState(),
                        contextual.dependencyMask(),
                        Blocks.STONE.defaultBlockState()));
        if (eastStone.isResolved()) {
            ResolvedThermalSignature signature = eastStone.value().orElseThrow();
            contextualOutputs.add(signature);
            signatures.intern(signature);
            geometryOutputs.add(signature.airRegions());
        }
        long elapsedNanos = System.nanoTime() - started;

        return new CensusPass(
                signatures.build(),
                stateCount,
                stateStaticCount,
                dynamicStateCount,
                explicitResolvedStateCount,
                contextualResolvedStateCount,
                contextualOutputs.size(),
                geometryOutputs.size(),
                maximumObservedRegions,
                registeredDeclaredSnapshotCells,
                elapsedNanos,
                movingPistonClassified,
                statusCounts,
                reasonCounts,
                routeCounts,
                regionHistogram
        );
    }

    private static ResolverBlockView<BlockState, FluidState> fixtureSnapshot(
            BlockState self,
            DependencyOffsetMask mask,
            BlockState neighbor
    ) {
        Map<DependencyOffsetMask.Offset,
                ResolverBlockView.SnapshotCell<BlockState, FluidState>> cells = new HashMap<>();
        for (DependencyOffsetMask.Offset offset : mask.offsets()) {
            BlockState state = offset.equals(DependencyOffsetMask.SELF) ? self : neighbor;
            cells.put(offset, ResolverBlockView.SnapshotCell.present(
                    state, state.getFluidState()));
        }
        return ResolverBlockView.snapshot(mask, cells);
    }

    private static ResolvedThermalSignature explicitStoneSignature() {
        return new ResolvedThermalSignature(
                0,
                1,
                List.of(),
                1,
                1,
                0,
                0,
                1
        );
    }

    private static ResolvedThermalSignature contextualBambooSignature(boolean eastIsStone) {
        return new ResolvedThermalSignature(
                0,
                eastIsStone ? 3 : 2,
                List.of(),
                2,
                2,
                0,
                0,
                eastIsStone ? 3 : 2
        );
    }

    private static String registryId(Block block) {
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(block);
        return id == null ? "" : id.toString();
    }

    private static boolean registriesEqual(
            ThermalSignatureRegistry first,
            ThermalSignatureRegistry second
    ) {
        if (first.signatureCount() != second.signatureCount()) {
            return false;
        }
        for (int signatureId = 0; signatureId < first.signatureCount(); signatureId++) {
            if (!first.signature(signatureId).equals(second.signature(signatureId))) {
                return false;
            }
        }
        return true;
    }

    private static void writeReport(
            int blockCount,
            ThermalSignatureResolverDispatcher dispatcher,
            CensusPass first,
            CensusPass reload
    ) {
        JsonObject report = new JsonObject();
        report.addProperty("schemaVersion", 2);
        report.addProperty("evidenceScope", "phase-a-full-resolver-dispatch-census");
        report.addProperty("generatedAt", OffsetDateTime.now().toString());
        report.addProperty("blockCount", blockCount);
        report.addProperty("stateCount", first.stateCount());
        report.addProperty("stateStaticCount", first.stateStaticCount());
        report.addProperty("dynamicStateCount", first.dynamicStateCount());
        report.addProperty("uniqueResolvedSignatureCount", first.registry().signatureCount());
        report.addProperty("uniqueGeometrySignatureCount",
                first.uniqueGeometrySignatureCount());
        report.addProperty("maximumObservedLocalAirRegions",
                first.maximumObservedRegions());
        report.addProperty("maximumRegionGuard", CENSUS_MAXIMUM_REGIONS);
        report.addProperty("explicitProfileOutputCount", first.explicitResolvedStateCount());
        report.addProperty("contextualResolvedStateCount",
                first.contextualResolvedStateCount());
        report.addProperty("contextualOutputCount", first.contextualOutputCount());
        report.addProperty("registeredDeclaredSnapshotCellCount",
                first.registeredDeclaredSnapshotCells());
        report.addProperty("elapsedNanos", first.elapsedNanos());
        report.addProperty("metadataMode",
                "neutral generic metadata plus bounded explicit/contextual fixtures");

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        report.add("resolverIds", gson.toJsonTree(dispatcher.resolverIds()));
        report.add("statusCounts", enumCounts(first.statusCounts(),
                ThermalResolution.Status.values()));
        report.add("reasonCounts", enumCounts(first.reasonCounts(),
                ThermalResolution.Reason.values()));
        report.add("routeCounts", enumCounts(first.routeCounts(),
                ThermalSignatureResolverDispatcher.Route.values()));

        JsonObject regions = new JsonObject();
        for (Map.Entry<Integer, Integer> entry : first.regionHistogram().entrySet()) {
            regions.addProperty(Integer.toString(entry.getKey()), entry.getValue());
        }
        report.add("localAirRegionHistogram", regions);

        JsonObject reloadEvidence = new JsonObject();
        reloadEvidence.addProperty("model",
                "immutable old/new registry overlap during generation swap");
        reloadEvidence.addProperty("oldSignatureCount", first.registry().signatureCount());
        reloadEvidence.addProperty("newSignatureCount", reload.registry().signatureCount());
        reloadEvidence.addProperty("simultaneousSignatureCount",
                first.registry().signatureCount() + reload.registry().signatureCount());
        reloadEvidence.addProperty("reloadElapsedNanos", reload.elapsedNanos());
        reloadEvidence.addProperty("deterministicIds",
                registriesEqual(first.registry(), reload.registry()));
        reloadEvidence.addProperty("retainedBytesArtifact", "retained-size.json");
        reloadEvidence.addProperty("productionDatapackListenerWired", false);
        report.add("reloadPrototype", reloadEvidence);

        Path output = Path.of(System.getProperty(
                        REPORT_PROPERTY,
                        "build/reports/thermal-phase-a/resolver-census.json"))
                .toAbsolutePath()
                .normalize();
        try {
            if (output.getParent() != null) {
                Files.createDirectories(output.getParent());
            }
            Files.writeString(
                    output,
                    gson.toJson(report) + System.lineSeparator(),
                    StandardCharsets.UTF_8
            );
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "could not write Phase A resolver census to " + output,
                    exception);
        }
    }

    private static <E extends Enum<E>> JsonObject enumCounts(
            Map<E, Integer> counts,
            E[] values
    ) {
        JsonObject result = new JsonObject();
        for (E value : values) {
            result.addProperty(value.name(), counts.getOrDefault(value, 0));
        }
        return result;
    }

    private record CensusPass(
            ThermalSignatureRegistry registry,
            int stateCount,
            int stateStaticCount,
            int dynamicStateCount,
            int explicitResolvedStateCount,
            int contextualResolvedStateCount,
            int contextualOutputCount,
            int uniqueGeometrySignatureCount,
            int maximumObservedRegions,
            long registeredDeclaredSnapshotCells,
            long elapsedNanos,
            boolean movingPistonClassified,
            Map<ThermalResolution.Status, Integer> statusCounts,
            Map<ThermalResolution.Reason, Integer> reasonCounts,
            Map<ThermalSignatureResolverDispatcher.Route, Integer> routeCounts,
            Map<Integer, Integer> regionHistogram
    ) {
        private CensusPass {
            statusCounts = Map.copyOf(statusCounts);
            reasonCounts = Map.copyOf(reasonCounts);
            routeCounts = Map.copyOf(routeCounts);
            regionHistogram = Map.copyOf(regionHistogram);
        }

        private int accountedStateCount() {
            return statusCounts.values().stream().mapToInt(Integer::intValue).sum();
        }

        private int routeCount(ThermalSignatureResolverDispatcher.Route route) {
            return routeCounts.getOrDefault(route, 0);
        }
    }

    private static final class BambooContextualFixtureResolver
            implements ThermalSignatureResolver<BlockState, FluidState> {
        private static final String RESOLVER_ID =
                "frostedheart:phase_a_bamboo_context_fixture";
        private static final DependencyOffsetMask MASK = DependencyOffsetMask.explicit(EAST);

        @Override
        public String resolverId() {
            return RESOLVER_ID;
        }

        @Override
        public DependencyOffsetMask dependencyMask() {
            return MASK;
        }

        @Override
        public int maxOutputRegions() {
            return 0;
        }

        @Override
        public ThermalResolution<ResolvedThermalSignature> resolve(
                ResolverBlockView.Access<BlockState, FluidState> view
        ) {
            ThermalResolution<ResolverBlockView.StateAndFluid<BlockState, FluidState>> self =
                    view.lookup(DependencyOffsetMask.SELF).asResolution();
            if (!self.isResolved()) {
                return ThermalResolution.failure(self.reason());
            }
            ThermalResolution<ResolverBlockView.StateAndFluid<BlockState, FluidState>> east =
                    view.lookup(EAST).asResolution();
            if (!east.isResolved()) {
                return ThermalResolution.failure(east.reason());
            }
            boolean eastIsStone = east.value().orElseThrow().blockState().is(Blocks.STONE);
            return ThermalResolution.resolved(contextualBambooSignature(eastIsStone));
        }
    }
}
