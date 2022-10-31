/*
 * Copyright (c) 2022 TeamMoeg
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
 *
 */

package com.teammoeg.frostedheart.util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TemperatureMapper {
    // Oh, no, this file will be left open.
    // But it doesn't matter. It will get killed after JVM exits.
    // Just want to reuse the file handle throughout the whole lifespan of the game
    // while don't want to bother with any stuff related to the game's lifecycle.
    private static BufferedWriter w;

    static private void init() {
        try {
            Path tempFile = Files.createTempFile("FHTemperature", "log");
            w = new BufferedWriter(new FileWriter(tempFile.toFile()));
        } catch (IOException e) {
            System.out.println("TemperatureMapper: log file initialization failed");
            e.printStackTrace();
        }
    }

    static void logTemperature(float temperature, float posX, float posY, float posZ) {
        if (w == null) init();
        try {
            w.write(
                    String.format("%f@<%f,%f,%f>", temperature, posX, posY, posZ)
            );
        } catch (IOException e) {
            System.out.println("TemperatureMapper: log file write failed");
        }
    }
}
