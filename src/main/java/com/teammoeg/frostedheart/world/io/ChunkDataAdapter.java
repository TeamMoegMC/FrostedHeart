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

package com.teammoeg.frostedheart.world.io;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.teammoeg.frostedheart.world.chunkdata.ChunkData;
import com.teammoeg.frostedheart.world.chunkdata.ChunkDataCache;
import com.teammoeg.frostedheart.world.chunkdata.ChunkMatrix;
import net.minecraft.util.math.ChunkPos;

import java.io.IOException;

@Deprecated
public class ChunkDataAdapter extends TypeAdapter<ChunkData> {

    @Override
    public ChunkData read(JsonReader in) throws IOException {
        while (in.hasNext()) {
            in.beginObject();
            while (in.hasNext()) {
                String chunkName = in.nextName();
                int posX = Integer.parseInt(chunkName.substring(chunkName.indexOf('x')+2, chunkName.indexOf('z')-1));
                int posZ = Integer.parseInt(chunkName.substring(chunkName.indexOf('z')+2));
                ChunkPos chunkPos = new ChunkPos(posX, posZ);
                ChunkData chunkData = new ChunkData(chunkPos);
                in.beginArray();
                in.beginObject();
                if (in.nextName().startsWith("block")) {
                    String blockName = in.nextName();
                    int x = Integer.parseInt(blockName.substring(blockName.indexOf('x') + 2, blockName.indexOf('y') - 1));
                    int y = Integer.parseInt(blockName.substring(blockName.indexOf('y') + 2, blockName.indexOf('z') - 1));
                    int z = Integer.parseInt(blockName.substring(blockName.indexOf('z') + 2));
                    chunkData.getChunkMatrix().setValue(x, y, z, (byte) in.nextInt());
                }
                ChunkDataCache.SERVER.putChunkDataToCache(chunkPos, chunkData);
                in.endObject();
                in.endArray();
            }
            in.endObject();
        }
        return null;
    }

    @Override
    public void write(JsonWriter writer, ChunkData value) throws IOException {
        if (value == null) {
            writer.nullValue();
            return;
        }
        writer.setIndent(" ");
        writer.beginObject();
        writer.name("chunk_x="+value.getPos().x+",z="+value.getPos().z).beginArray().beginObject();
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) for (int y = 0; y < 256; y++) {
            writer.name("block_x="+x+",y="+y+",z="+z).value(value.getChunkMatrix().getValue(x, y, z));
        }
        writer.endObject().endArray();
        writer.endObject();
    }
}
