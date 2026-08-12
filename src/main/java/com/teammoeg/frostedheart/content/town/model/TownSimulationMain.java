/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 */

package com.teammoeg.frostedheart.content.town.model;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Command-line entry point for the incrementally implemented town model. */
public final class TownSimulationMain {
    private TownSimulationMain() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            printUsage();
            throw new IllegalArgumentException("A command is required.");
        }
        Map<String, String> options = parseOptions(args);
        switch (args[0]) {
            case "audit" -> runAudit(options);
            case "simulate" -> runSimulation(options);
            default -> {
                printUsage();
                throw new IllegalArgumentException("Unknown command: " + args[0]);
            }
        }
    }

    private static void runAudit(Map<String, String> options) throws Exception {
        String packRootValue = options.get("pack-root");
        if (packRootValue == null || packRootValue.isBlank()) {
            printUsage();
            throw new IllegalArgumentException("--pack-root is required.");
        }
        Path projectRoot = Path.of(options.getOrDefault("project-root", System.getProperty("user.dir")));
        Path packRoot = Path.of(packRootValue);
        Path output = options.containsKey("output") ? Path.of(options.get("output")) : null;
        TownStageZeroAudit.AuditRun run = TownStageZeroAudit.run(projectRoot, packRoot, output);
        TownStageZeroAudit.printSummary(run);
    }

    private static void runSimulation(Map<String, String> options) throws Exception {
        String packRootValue = requireOption(options, "pack-root");
        String scenarioValue = requireOption(options, "scenario");
        Path projectRoot = Path.of(options.getOrDefault("project-root", System.getProperty("user.dir")));
        Path packRoot = Path.of(packRootValue);
        Path scenario = Path.of(scenarioValue);
        Path output = options.containsKey("output") ? Path.of(options.get("output")) : null;
        Integer runs = options.containsKey("runs") ? Integer.valueOf(options.get("runs")) : null;
        Long seed = options.containsKey("seed") ? Long.valueOf(options.get("seed")) : null;
        TownStageOneTwoSimulator.SimulationRun run = TownStageOneTwoSimulator.run(
                projectRoot, packRoot, scenario, output, runs, seed);
        TownStageOneTwoSimulator.printSummary(run);
    }

    private static String requireOption(Map<String, String> options, String name) {
        String value = options.get(name);
        if (value == null || value.isBlank()) {
            printUsage();
            throw new IllegalArgumentException("--" + name + " is required.");
        }
        return value;
    }

    static Map<String, String> parseOptions(String[] args) {
        Map<String, String> result = new LinkedHashMap<>();
        for (int index = 1; index < args.length; index++) {
            String argument = args[index];
            if (!argument.startsWith("--")) {
                throw new IllegalArgumentException("Unexpected argument: " + argument);
            }
            int equals = argument.indexOf('=');
            if (equals > 2) {
                result.put(argument.substring(2, equals), argument.substring(equals + 1));
                continue;
            }
            if (index + 1 >= args.length || args[index + 1].startsWith("--")) {
                throw new IllegalArgumentException("Missing value for " + argument);
            }
            result.put(argument.substring(2), args[++index]);
        }
        return result;
    }

    private static void printUsage() {
        System.err.println("Usage:");
        System.err.println("  TownSimulationMain audit --pack-root <TWR .minecraft> "
                + "[--project-root <FH root>] [--output <directory>]");
        System.err.println("  TownSimulationMain simulate --pack-root <TWR .minecraft> "
                + "--scenario <json> [--project-root <FH root>] [--output <directory>] "
                + "[--runs <N>] [--seed <S>]");
    }
}
