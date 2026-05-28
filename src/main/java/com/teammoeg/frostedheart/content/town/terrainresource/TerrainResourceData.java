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

package com.teammoeg.frostedheart.content.town.terrainresource;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TerrainResourceData {
    public static final Codec<TerrainResourceData> CODEC = RecordCodecBuilder.create(ins -> ins.group(
    Codec.DOUBLE.optionalFieldOf("extracted", 0.0).forGetter(o -> o.extracted),

    ChunkResourceTracker.CODEC.optionalFieldOf("chunkTracker", null).forGetter(o -> o.chunkResourceTracker)

    ).apply(ins, TerrainResourceData::new));
    public static final int DEFAULT_MAX_RADIUS = 3200;
	private static final double PI=3.0;
	@Getter
	int radius;
	@Getter
	double extracted;
	double total;

    @Nullable
    private ChunkResourceTracker chunkResourceTracker;

	public TerrainResourceData() {
	}

	public TerrainResourceData(double extracted) {
		this.extracted=extracted;
	}

    public TerrainResourceData(double extracted, ChunkResourceTracker tracker) {
        this.extracted = extracted;
        this.chunkResourceTracker = tracker;
    }

	public void recalculateRadius(double resoucePerSquare,int maxradius) {
		if(resoucePerSquare<=0)return;
		double convertedRadius=Math.sqrt(extracted/PI/resoucePerSquare);//use 3 as pi
		total=(PI*resoucePerSquare*maxradius*maxradius);
		
		radius=Mth.floor(convertedRadius)+1;
	}

    public void recoverResource(double number) {
        if (chunkResourceTracker != null) {
            return;
        }
        extracted -= number;
        if (extracted < 0) {
            extracted = 0;
        }
    }

	public void costResource(double number) {
		extracted+=number;
	}
	public double getRemainResource() {
		return total-extracted;
	}
	public double getSize() {
		return PI*radius*radius;
	}
	public double mayCostResource(double d) {
		return Math.min(total-extracted, d);
		
	}

    public double mayCostResource(ChunkPos chunk, double amount, double perChunkLimit) {
        if (chunkResourceTracker != null) {
            return chunkResourceTracker.mayCostResource(chunk, amount, perChunkLimit);
        }
        return Math.min(amount, total - extracted);
    }

    public void costChunkResource(ChunkPos chunk, double amount) {
        if (chunkResourceTracker != null) {
            chunkResourceTracker.cost(chunk, amount);
        }
        this.extracted += amount;
    }

    @Nullable
    public ChunkResourceTracker getChunkResourceTracker() {
        return chunkResourceTracker;
    }

    public void setChunkTracker(ChunkResourceTracker tracker) {
        this.chunkResourceTracker = tracker;
    }

    public static class ChunkResourceTracker {
        public static final Codec<ChunkResourceTracker> CODEC = RecordCodecBuilder.create(ins -> ins.group(
        Codec.unboundedMap(
                Codec.LONG.xmap(val -> new ChunkPos((int)(val >> 32), (int)(val & 0xFFFFFFFFL)), pos -> ((long)pos.x << 32) | (pos.z & 0xFFFFFFFFL)), Codec.DOUBLE)
        .optionalFieldOf("extractedResources", Map.of())
        .forGetter(o -> o.extractedResources)
        ).apply(ins, ChunkResourceTracker::new));

        private Map<ChunkPos, Double> extractedResources = new HashMap<>();

        private transient Set<ChunkPos> activeChunks = new HashSet<>(); // 临时计算，不持久化

        public ChunkResourceTracker() {}

        public ChunkResourceTracker(Map<ChunkPos, Double> extractedResources) {
            this.extractedResources = extractedResources;
        }

        public double mayCostResource(ChunkPos chunk, double amount, double perChunkTotal) {
            if (!activeChunks.contains(chunk)) return 0;

            double cur = extractedResources.getOrDefault(chunk, 0.0);
            return Math.min(amount, perChunkTotal - cur);
        }

        public void cost(ChunkPos chunk, double amount) {
            extractedResources.merge(chunk, amount, Double::sum);
        }

        public void setActiveChunks(Set<ChunkPos> activeChunks) {
            this.activeChunks = activeChunks;
        }

        public void clearActiveChunks() {
            activeChunks.clear();
        }

    }
}
