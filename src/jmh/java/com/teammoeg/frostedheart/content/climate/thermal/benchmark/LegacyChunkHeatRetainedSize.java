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
import com.teammoeg.frostedheart.content.climate.gamedata.chunkheat.ChunkHeatData;
import org.openjdk.jol.info.ClassLayout;
import org.openjdk.jol.info.GraphLayout;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class LegacyChunkHeatRetainedSize {
    private LegacyChunkHeatRetainedSize() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            throw new IllegalArgumentException("expected one output JSON path");
        }
        Path output = Path.of(args[0]).toAbsolutePath().normalize();
        JsonObject report = new JsonObject();
        report.addProperty("evidenceScope", "legacy-chunk-heat-object-graph");
        report.addProperty("measurement", "JOL GraphLayout retained-size estimate");
        report.addProperty("compressedOopsMode",
                System.getProperty("java.vm.compressedOopsMode", "unknown"));
        JsonArray fixtures = new JsonArray();
        for (int adjusterCount : new int[]{0, 1, 10, 100}) {
            ChunkHeatData data = LegacyChunkHeatFixtures.data(adjusterCount);
            GraphLayout graph = GraphLayout.parseInstance(data);
            JsonObject fixture = new JsonObject();
            fixture.addProperty("adjusterCount", adjusterCount);
            fixture.addProperty("chunkHeatDataShallowBytes",
                    ClassLayout.parseInstance(data).instanceSize());
            fixture.addProperty("retainedBytes", graph.totalSize());
            fixture.addProperty("retainedObjects", graph.totalCount());
            fixtures.add(fixture);
        }
        report.add("fixtures", fixtures);
        report.addProperty("caveat",
                "Object-graph estimate for isolated fixtures; excludes class metadata and shared statics.");

        if (output.getParent() != null) {
            Files.createDirectories(output.getParent());
        }
        Files.writeString(output,
                new GsonBuilder().setPrettyPrinting().create().toJson(report) + System.lineSeparator(),
                StandardCharsets.UTF_8);
    }
}
