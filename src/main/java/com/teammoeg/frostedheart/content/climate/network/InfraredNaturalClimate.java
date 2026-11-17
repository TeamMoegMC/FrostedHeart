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
 *
 */

package com.teammoeg.frostedheart.content.climate.network;

import java.util.Arrays;

import net.minecraft.network.FriendlyByteBuf;

/**
 * 客户端为服务端没有发送温度的方块计算自然温度所需的数据：9×9 窗口内每个区块、每个覆盖厚度的气候，超平坦标志和下限。
 * 维度、群系、海拔和覆盖层由客户端自己的世界和同步数据给出。
 * <p>
 * What a client needs to compute the natural temperature of blocks the server sends no value for: the climate reaching
 * every cover in each chunk of the 9×9 window, the superflat flag and the lower clamp. The dimension, biome, altitude
 * and cover terms come from the client's own world and synced data.
 *
 * @param revision      content hash; a client that already holds this revision needs nothing new
 * @param coverLevels   values per table, cover 0 (open air) through the cover limit
 * @param climates      tables back to back; table 0 is the climate of chunks no white curtain reaches
 * @param chunkTables   table of each window chunk, indexed {@code x + 9 * z} from the window's north-west chunk
 */
public record InfraredNaturalClimate(long revision, boolean noUnderground, float absoluteZeroC, int coverLevels,
        float[] climates, byte[] chunkTables) {
    public static final int WINDOW_CHUNKS = InfraredWindow.WIDTH * InfraredWindow.WIDTH;
    /** Known revision of a caller that does not track natural climate: nothing is resent because of it. */
    public static final long UNTRACKED = Long.MIN_VALUE;
    private static final int MAXIMUM_COVER_LEVELS = 65, MAXIMUM_TABLES = WINDOW_CHUNKS + 1;

    public InfraredNaturalClimate {
        int tables = coverLevels <= 0 ? 0 : climates.length / coverLevels;
        if (coverLevels <= 0 || coverLevels > MAXIMUM_COVER_LEVELS || climates.length % coverLevels != 0
                || tables == 0 || tables > MAXIMUM_TABLES || chunkTables.length != WINDOW_CHUNKS)
            throw new IllegalArgumentException("infrared natural climate shape");
        for (byte table : chunkTables)
            if (Byte.toUnsignedInt(table) >= tables) throw new IllegalArgumentException("infrared natural climate table");
    }

    /** A record whose revision identifies its content. */
    public static InfraredNaturalClimate of(boolean noUnderground, float absoluteZeroC, int coverLevels,
            float[] climates, byte[] chunkTables) {
        long hash = 0x9E3779B97F4A7C15L;
        hash = mix(hash, noUnderground ? 1 : 0);
        hash = mix(hash, Float.floatToIntBits(absoluteZeroC));
        hash = mix(hash, coverLevels);
        for (float climate : climates) hash = mix(hash, Float.floatToIntBits(climate));
        for (byte table : chunkTables) hash = mix(hash, table);
        // Zero is what a client that holds nothing reports, and UNTRACKED is reserved.
        return new InfraredNaturalClimate(hash == 0 || hash == UNTRACKED ? 1 : hash, noUnderground, absoluteZeroC,
                coverLevels, climates, chunkTables);
    }

    private static long mix(long hash, int value) {
        hash = (hash ^ value) * 0x100000001B3L;
        return hash ^ hash >>> 29;
    }

    /** Climate reaching {@code cover} opaque blocks deep in window chunk {@code windowChunk}. */
    public float climate(int windowChunk, int cover) {
        int table = Byte.toUnsignedInt(chunkTables[windowChunk]);
        return climates[table * coverLevels + Math.min(cover, coverLevels - 1)];
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeLong(revision);
        buffer.writeBoolean(noUnderground);
        buffer.writeFloat(absoluteZeroC);
        buffer.writeVarInt(coverLevels);
        buffer.writeVarInt(climates.length / coverLevels);
        for (float climate : climates) buffer.writeFloat(climate);
        buffer.writeBytes(chunkTables);
    }

    public static InfraredNaturalClimate read(FriendlyByteBuf buffer) {
        long revision = buffer.readLong();
        boolean noUnderground = buffer.readBoolean();
        float absoluteZeroC = buffer.readFloat();
        int coverLevels = buffer.readVarInt(), tables = buffer.readVarInt();
        if (coverLevels <= 0 || coverLevels > MAXIMUM_COVER_LEVELS || tables <= 0 || tables > MAXIMUM_TABLES)
            throw new IllegalArgumentException("infrared natural climate shape");
        float[] climates = new float[coverLevels * tables];
        for (int index = 0; index < climates.length; index++) climates[index] = buffer.readFloat();
        byte[] chunkTables = new byte[WINDOW_CHUNKS];
        buffer.readBytes(chunkTables);
        return new InfraredNaturalClimate(revision, noUnderground, absoluteZeroC, coverLevels, climates, chunkTables);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof InfraredNaturalClimate that && revision == that.revision
                && noUnderground == that.noUnderground && absoluteZeroC == that.absoluteZeroC
                && coverLevels == that.coverLevels && Arrays.equals(climates, that.climates)
                && Arrays.equals(chunkTables, that.chunkTables);
    }

    @Override
    public int hashCode() {
        return Long.hashCode(revision);
    }
}
