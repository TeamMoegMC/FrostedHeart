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
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.content.climate.player.SurroundingTemperatureSimulatorBenchmarkFixture;
import org.openjdk.jol.info.GraphLayout;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class LegacyPlayerSamplingRetainedSize {
    private LegacyPlayerSamplingRetainedSize() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            throw new IllegalArgumentException("expected one output JSON path");
        }
        JsonArray fixtures = new JsonArray();
        for (String pattern : new String[]{"all_air", "room"}) {
            Object firstSnapshot = SurroundingTemperatureSimulatorBenchmarkFixture
                    .source(pattern)
                    .capture()
                    .retainedGraphRoot();
            Object secondSnapshot = SurroundingTemperatureSimulatorBenchmarkFixture
                    .source(pattern)
                    .capture()
                    .retainedGraphRoot();
            GraphLayout both = GraphLayout.parseInstance(firstSnapshot, secondSnapshot);
            GraphLayout exclusive = both.subtract(GraphLayout.parseInstance(firstSnapshot));
            JsonObject fixture = new JsonObject();
            fixture.addProperty("pattern", pattern);
            fixture.addProperty("snapshotExclusiveBytes", exclusive.totalSize());
            fixture.addProperty("snapshotExclusiveObjects", exclusive.totalCount());
            fixtures.add(fixture);
        }

        JsonObject report = new JsonObject();
        report.addProperty("evidenceScope", "legacy-player-sampling-synthetic-snapshot");
        report.addProperty("measurement", "JOL incremental graph of a second equivalent snapshot");
        report.add("fixtures", fixtures);
        report.addProperty("caveat",
                "Synthetic isolated snapshots; incremental graph excludes objects shared by two equivalent snapshots.");

        Path output = Path.of(args[0]).toAbsolutePath().normalize();
        if (output.getParent() != null) {
            Files.createDirectories(output.getParent());
        }
        Files.writeString(output,
                new GsonBuilder().setPrettyPrinting().create().toJson(report)
                        + System.lineSeparator(),
                StandardCharsets.UTF_8);
    }
}
