/*
 * Copyright (c) 2021 TeamMoeg
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

package com.teammoeg.frostedheart.world.unused.io;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.teammoeg.frostedheart.climate.chunkdata.ChunkData;
import com.teammoeg.frostedheart.climate.chunkdata.ChunkDataCache;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

@Deprecated
public class ChunkDataJsonWriter {
    public static void writeJson() {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(ChunkData.class, new ChunkDataAdapter())
                .create();
        try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(ChunkDataJsonReader.FH_SAVE_PATH,
                "temperature.json")), StandardCharsets.UTF_8))) {

            gson.toJson(ChunkDataCache.SERVER.getCache().values().toArray(), out);

            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
