/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.profile.minecraft;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.thermal.geometry.ConservativeAirGeometry;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ResolvedThermalSignature;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalResolution;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureRegistry;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** One-pass geometry-only census over the active Forge block-state registry. */
@GameTestHolder(FHMain.MODID)
@PrefixGameTestTemplate(false)
public final class FrostedHeartPhaseAResolverCensusGameTests {
    public static final String REPORT_PROPERTY = "frostedheart.phaseAResolverCensusReport";
    private static final String BATCH = "frostedheart_phase_a_resolver_census";
    private static final String TEMPLATE = "phase0a_empty";
    private static final int CENSUS_MAXIMUM_REGIONS =
            ConservativeAirGeometry.MICROCELL_COUNT;

    private FrostedHeartPhaseAResolverCensusGameTests() {
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void activeRegistryStateStaticCensus(GameTestHelper helper) {
        StateStaticThermalResolver resolver =
                StateStaticThermalResolver.geometryOnly(CENSUS_MAXIMUM_REGIONS);
        ThermalSignatureRegistry.Builder signatures = ThermalSignatureRegistry.builder();
        Map<ThermalResolution.Status, Integer> statusCounts =
                new EnumMap<>(ThermalResolution.Status.class);
        Map<ThermalResolution.Reason, Integer> reasonCounts =
                new EnumMap<>(ThermalResolution.Reason.class);
        Map<Integer, Integer> regionHistogram = new TreeMap<>();

        List<Block> blocks = new ArrayList<>(ForgeRegistries.BLOCKS.getValues());
        blocks.sort(Comparator.comparing(FrostedHeartPhaseAResolverCensusGameTests::registryId));

        int stateCount = 0;
        int stateStaticCount = 0;
        int dynamicStateCount = 0;
        int maximumObservedRegions = 0;
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

                ThermalResolution<ResolvedThermalSignature> resolution =
                        resolver.resolve(state, state.getFluidState());
                statusCounts.merge(resolution.status(), 1, Integer::sum);
                reasonCounts.merge(resolution.reason(), 1, Integer::sum);
                if (resolution.isResolved()) {
                    ResolvedThermalSignature signature = resolution.value().orElseThrow();
                    signatures.intern(signature);
                    int regions = signature.localAirRegionCount();
                    regionHistogram.merge(regions, 1, Integer::sum);
                    maximumObservedRegions = Math.max(maximumObservedRegions, regions);
                }

                if (state.is(Blocks.MOVING_PISTON)) {
                    movingPistonClassified = resolution.status()
                            == ThermalResolution.Status.UNRESOLVED
                            && resolution.reason() == ThermalResolution.Reason.UNRESOLVED_DYNAMIC;
                }
            }
        }
        long elapsedNanos = System.nanoTime() - started;

        int accounted = statusCounts.values().stream().mapToInt(Integer::intValue).sum();
        helper.assertTrue(stateCount > 0 && accounted == stateCount,
                "resolver census must account for every active BlockState");
        helper.assertTrue(statusCounts.getOrDefault(ThermalResolution.Status.RESOLVED, 0) > 0,
                "resolver census must produce resolved generic-static signatures");
        helper.assertTrue(movingPistonClassified,
                "moving piston must remain UNRESOLVED_DYNAMIC during the census");

        writeReport(
                blocks.size(),
                stateCount,
                stateStaticCount,
                dynamicStateCount,
                signatures.build().signatureCount(),
                maximumObservedRegions,
                elapsedNanos,
                statusCounts,
                reasonCounts,
                regionHistogram
        );
        helper.succeed();
    }

    private static String registryId(Block block) {
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(block);
        return id == null ? "" : id.toString();
    }

    private static void writeReport(
            int blockCount,
            int stateCount,
            int stateStaticCount,
            int dynamicStateCount,
            int signatureCount,
            int maximumObservedRegions,
            long elapsedNanos,
            Map<ThermalResolution.Status, Integer> statusCounts,
            Map<ThermalResolution.Reason, Integer> reasonCounts,
            Map<Integer, Integer> regionHistogram
    ) {
        JsonObject report = new JsonObject();
        report.addProperty("schemaVersion", 1);
        report.addProperty("evidenceScope", "phase-a-geometry-only-generic-static-census");
        report.addProperty("generatedAt", OffsetDateTime.now().toString());
        report.addProperty("blockCount", blockCount);
        report.addProperty("stateCount", stateCount);
        report.addProperty("stateStaticCount", stateStaticCount);
        report.addProperty("dynamicStateCount", dynamicStateCount);
        report.addProperty("uniqueGeometrySignatureCount", signatureCount);
        report.addProperty("maximumObservedLocalAirRegions", maximumObservedRegions);
        report.addProperty("maximumRegionGuard", CENSUS_MAXIMUM_REGIONS);
        report.addProperty("contextualOutputCount", 0);
        report.addProperty("elapsedNanos", elapsedNanos);
        report.addProperty("metadataMode", "neutral-zero-ids-not-physical-profiles");

        JsonObject statuses = new JsonObject();
        for (ThermalResolution.Status status : ThermalResolution.Status.values()) {
            statuses.addProperty(status.name(), statusCounts.getOrDefault(status, 0));
        }
        report.add("statusCounts", statuses);

        JsonObject reasons = new JsonObject();
        for (Map.Entry<ThermalResolution.Reason, Integer> entry : reasonCounts.entrySet()) {
            reasons.addProperty(entry.getKey().name(), entry.getValue());
        }
        report.add("reasonCounts", reasons);

        JsonObject regions = new JsonObject();
        for (Map.Entry<Integer, Integer> entry : regionHistogram.entrySet()) {
            regions.addProperty(Integer.toString(entry.getKey()), entry.getValue());
        }
        report.add("localAirRegionHistogram", regions);

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
                    new GsonBuilder().setPrettyPrinting().create().toJson(report)
                            + System.lineSeparator(),
                    StandardCharsets.UTF_8
            );
        } catch (IOException exception) {
            throw new IllegalStateException("could not write Phase A resolver census to " + output,
                    exception);
        }
    }
}
