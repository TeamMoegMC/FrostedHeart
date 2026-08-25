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
import com.teammoeg.frostedheart.content.climate.thermal.runtime.DimensionThermalRuntime;
import org.openjdk.jol.info.GraphLayout;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ThermalPublishedAirQueryRetainedSize {
    private ThermalPublishedAirQueryRetainedSize() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            throw new IllegalArgumentException("expected one output JSON path");
        }
        JsonArray fixtures = new JsonArray();
        for (String layout : new String[]{"shared_page", "distributed_pages"}) {
            for (int receiverCount : new int[]{1, 10, 50, 100}) {
                try (ThermalPublishedAirQueryFixtures.Fixture fixture =
                             ThermalPublishedAirQueryFixtures.create(receiverCount, layout)) {
                    double checksum = fixture.queryBatch();
                    GraphLayout graph = GraphLayout.parseInstance(fixture.retainedGraphRoot());
                    DimensionThermalRuntime.Diagnostics diagnostics = fixture.diagnostics();
                    JsonObject entry = new JsonObject();
                    entry.addProperty("workloadId", "outdoor-players-" + receiverCount);
                    entry.addProperty("layout", fixture.layoutId());
                    entry.addProperty("syntheticReceiverCount", fixture.receiverCount());
                    entry.addProperty("pageCount", fixture.pageCount());
                    entry.addProperty("liveCellCount", diagnostics.liveCellCount());
                    entry.addProperty("publicationReservedBytes",
                            diagnostics.publicationReservedBytes());
                    entry.addProperty("retainedBytes", graph.totalSize());
                    entry.addProperty("retainedObjects", graph.totalCount());
                    entry.addProperty("queryChecksum", checksum);
                    fixtures.add(entry);
                }
            }
        }

        JsonObject report = new JsonObject();
        report.addProperty("schemaVersion", 1);
        report.addProperty("evidenceScope",
                "synthetic-published-air-query-retained-diagnostic-not-phase-l-acceptance");
        report.addProperty("measurement",
                "JOL object graph of production ThermalPage, arena, runtime, and publication owners");
        report.add("fixtures", fixtures);
        report.addProperty("caveat",
                "No ServerLevel, real players, radiation receiver cache, modpack workload, TPS, or chunk lifecycle; receiver counts label sequential query batches only.");

        Path output = Path.of(args[0]).toAbsolutePath().normalize();
        if (output.getParent() != null) {
            Files.createDirectories(output.getParent());
        }
        Files.writeString(
                output,
                new GsonBuilder().setPrettyPrinting().create().toJson(report)
                        + System.lineSeparator(),
                StandardCharsets.UTF_8);
    }
}
