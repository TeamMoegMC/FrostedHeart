/*
 *  Copyright (c) 2021. TeamMoeg
 *
 *  This file is part of Energy Level Transition.
 *
 *  Energy Level Transition is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, version 3.
 *
 *  Energy Level Transition is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Energy Level Transition.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.teammoeg.frostedheart.world.unused.io;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.teammoeg.frostedheart.world.chunkdata.ChunkData;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Deprecated
public class ChunkDataJsonReader {
    public static File FH_SAVE_PATH;

    public static ChunkData readFile() {
        ChunkData chunkData = null;

        if (!FH_SAVE_PATH.exists()) {
            try {
                FH_SAVE_PATH.mkdir();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(ChunkData.class, new ChunkDataAdapter());
        Gson gson = gsonBuilder.create();

        try (BufferedReader rd = new BufferedReader(new InputStreamReader(new FileInputStream(new File(FH_SAVE_PATH, "temperature.json")), StandardCharsets.UTF_8))) {
            chunkData = gson.fromJson(rd, ChunkData.class);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return chunkData;
    }
}

