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

package com.teammoeg.frostedheart.world;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.teammoeg.frostedheart.world.chunkdata.ChunkData;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

public class ChunkDataJsonWriter {
    public static void writeJson() {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(ChunkData.class, new ChunkDataAdapter())
                .create();
        try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(ChunkDataJsonReader.SAVE_ELT_FOLDER_PATH,
                "ResearchProgress.json")), StandardCharsets.UTF_8))) {

            //gson.toJson(, out);

            out.flush();
        } catch (Exception e) {


        }
    }
}
