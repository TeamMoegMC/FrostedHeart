/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.thermal.benchmark;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.teammoeg.frostedheart.content.climate.thermal.geometry.ComponentBrickCompiler;
import com.teammoeg.frostedheart.content.climate.thermal.geometry.ConservativeAirGeometry;
import com.teammoeg.frostedheart.content.climate.thermal.profile.LocalAirRegionPattern;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ResolvedThermalSignature;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureRegistry;
import org.openjdk.jol.info.GraphLayout;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Writes retained-object evidence for the Phase A correctness layouts. */
public final class PhaseARetainedSize {
    private PhaseARetainedSize() {
    }

    public static void main(String[] arguments) throws IOException {
        if (arguments.length != 2) {
            throw new IllegalArgumentException("expected output and resolver-census paths");
        }

        Path censusPath = Path.of(arguments[1]).toAbsolutePath().normalize();
        JsonObject census = JsonParser.parseString(Files.readString(
                censusPath, StandardCharsets.UTF_8)).getAsJsonObject();
        int censusSignatureCount = census.get("uniqueResolvedSignatureCount").getAsInt();
        if (censusSignatureCount <= 0) {
            throw new IllegalArgumentException("resolver census has no resolved signatures");
        }

        ThermalSignatureRegistry oldRegistry = syntheticRegistry(0, censusSignatureCount);
        ThermalSignatureRegistry newRegistry = syntheticRegistry(
                censusSignatureCount, censusSignatureCount);
        ComponentBrickCompiler.CompiledBrick allAir = compileBrick(BrickFixture.ALL_AIR);
        ComponentBrickCompiler.CompiledBrick solidWall = compileBrick(BrickFixture.SOLID_WALL);
        ComponentBrickCompiler.CompiledBrick splitRegions =
                compileBrick(BrickFixture.SPLIT_REGIONS);

        long oldRegistryBytes = retainedBytes(oldRegistry);
        long newRegistryBytes = retainedBytes(newRegistry);
        long reloadOverlapBytes = retainedBytes(oldRegistry, newRegistry);

        JsonObject report = new JsonObject();
        report.addProperty("schemaVersion", 1);
        report.addProperty("evidenceScope", "phase-a-synthetic-correctness-layout-retained-size");
        report.addProperty("generatedAt", OffsetDateTime.now().toString());
        report.addProperty("censusSignatureCount", censusSignatureCount);
        report.addProperty("censusArtifact", censusPath.getFileName().toString());
        report.addProperty("oldRegistryBytes", oldRegistryBytes);
        report.addProperty("newRegistryBytes", newRegistryBytes);
        report.addProperty("reloadOverlapBytes", reloadOverlapBytes);
        report.addProperty("reloadTransientOverheadBytes",
                reloadOverlapBytes - Math.max(oldRegistryBytes, newRegistryBytes));
        report.addProperty("allAirBrickBytes", retainedBytes(allAir));
        report.addProperty("solidWallBrickBytes", retainedBytes(solidWall));
        report.addProperty("splitRegionsBrickBytes", retainedBytes(splitRegions));
        report.addProperty("layout", "object-heavy-correctness-prototype-not-production-packing");
        report.addProperty("limitation",
                "Synthetic registry mirrors the current census cardinality but not its "
                        + "exact region distribution; "
                        + "it is not a whole-server retained-heap measurement.");

        Path output = Path.of(arguments[0]).toAbsolutePath().normalize();
        if (output.getParent() != null) {
            Files.createDirectories(output.getParent());
        }
        Files.writeString(output,
                new GsonBuilder().setPrettyPrinting().create().toJson(report)
                        + System.lineSeparator(),
                StandardCharsets.UTF_8);
    }

    private static ThermalSignatureRegistry syntheticRegistry(
            int flagsBase,
            int signatureCount
    ) {
        ThermalSignatureRegistry.Builder builder = ThermalSignatureRegistry.builder();
        for (int index = 0; index < signatureCount; index++) {
            List<LocalAirRegionPattern> regions = List.of(new LocalAirRegionPattern(
                    0,
                    -1L,
                    0xffff,
                    0xffff,
                    0xffff,
                    0xffff,
                    0xffff,
                    0xffff
            ));
            builder.intern(new ResolvedThermalSignature(
                    0,
                    0,
                    regions,
                    0,
                    0,
                    0,
                    0,
                    flagsBase + index
            ));
        }
        return builder.build();
    }

    private static ComponentBrickCompiler.CompiledBrick compileBrick(BrickFixture fixture) {
        ConservativeAirGeometry.Resolution air =
                ConservativeAirGeometry.resolve(List.of(), 4);
        List<ConservativeAirGeometry.Resolution> blocks = new ArrayList<>(
                Collections.nCopies(ComponentBrickCompiler.BLOCK_COUNT, air));
        if (fixture == BrickFixture.SOLID_WALL) {
            ConservativeAirGeometry.Resolution solid = ConservativeAirGeometry.resolve(
                    List.of(ConservativeAirGeometry.UnitBox.fullBlock()), 4);
            for (int y = 0; y < ComponentBrickCompiler.BLOCKS_PER_AXIS; y++) {
                for (int z = 0; z < ComponentBrickCompiler.BLOCKS_PER_AXIS; z++) {
                    blocks.set(ComponentBrickCompiler.blockIndex(1, y, z), solid);
                }
            }
        } else if (fixture == BrickFixture.SPLIT_REGIONS) {
            ConservativeAirGeometry.Resolution split = ConservativeAirGeometry.resolve(
                    List.of(new ConservativeAirGeometry.UnitBox(
                            0.49D, 0.0D, 0.0D, 0.51D, 1.0D, 1.0D)),
                    4);
            Collections.fill(blocks, split);
        }
        return ComponentBrickCompiler.compile(blocks, 4, 1).brick().orElseThrow();
    }

    private static long retainedBytes(Object... roots) {
        return GraphLayout.parseInstance(roots).totalSize();
    }

    private enum BrickFixture {
        ALL_AIR,
        SOLID_WALL,
        SPLIT_REGIONS
    }
}
