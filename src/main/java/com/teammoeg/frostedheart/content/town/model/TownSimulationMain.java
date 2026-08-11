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
        if (args.length == 0 || !"audit".equals(args[0])) {
            printUsage();
            throw new IllegalArgumentException("Stage 0 only implements the audit command.");
        }
        Map<String, String> options = parseOptions(args);
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
        System.err.println("Usage: TownSimulationMain audit --pack-root <TWR .minecraft> "
                + "[--project-root <FH root>] [--output <directory>]");
    }
}
