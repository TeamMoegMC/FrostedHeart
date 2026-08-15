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
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import lombok.Getter;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class TerrainResourceData {
    public static final Codec<TerrainResourceData> CODEC = RecordCodecBuilder.create(ins -> ins.group(
    Codec.DOUBLE.optionalFieldOf("extracted", 0.0).forGetter(o -> o.extracted),

    ChunkResourceTracker.CODEC.optionalFieldOf("chunkTracker")
            .forGetter(o -> Optional.ofNullable(o.chunkResourceTracker))

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

    private TerrainResourceData(double extracted, Optional<ChunkResourceTracker> tracker) {
        this(extracted, tracker.orElse(null));
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

    /**
     * Returns the remaining global resource without relying on the transient
     * radius/total cache, which is not part of the town codec.
     */
    public double getRemainResource(double resourcePerSquare, int maxRadius) {
        return Math.max(0.0, calculateTotalResource(resourcePerSquare, maxRadius) - extracted);
    }

    public static double calculateTotalResource(double resourcePerSquare, int maxRadius) {
        return Math.max(0.0, PI * resourcePerSquare * maxRadius * maxRadius);
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
        private static final Codec<ChunkPos> CHUNK_POS_KEY_CODEC = Codec.STRING.xmap(value -> {
            long packed = Long.parseLong(value);
            return new ChunkPos((int) (packed >> 32), (int) (packed & 0xFFFFFFFFL));
        }, pos -> Long.toString(((long) pos.x << 32) | (pos.z & 0xFFFFFFFFL)));

        public static final Codec<ChunkResourceTracker> CODEC = RecordCodecBuilder.create(ins -> ins.group(
        Codec.unboundedMap(
                CHUNK_POS_KEY_CODEC, Codec.DOUBLE)
        .optionalFieldOf("extractedResources", Map.of())
        .forGetter(o -> o.extractedResources)
        ).apply(ins, ChunkResourceTracker::new));

        private Object2DoubleOpenHashMap<ChunkPos> extractedResources = new Object2DoubleOpenHashMap<>();

        private transient Set<ChunkPos> activeChunks = new HashSet<>(); // 临时计算，不持久化

        public ChunkResourceTracker() {}

        public ChunkResourceTracker(Map<ChunkPos, Double> extractedResources) {
            // Codec map decoders may return an immutable map. The tracker updates this
            // collection every time a mine extracts resources, so keep an owned,
            // mutable copy after loading.
            this.extractedResources = new Object2DoubleOpenHashMap<>(extractedResources);
        }

        public double mayCostResource(ChunkPos chunk, double amount, double perChunkTotal) {
            if (!activeChunks.contains(chunk)) return 0;

            double cur = extractedResources.getOrDefault(chunk, 0.0);
            return Math.min(amount, perChunkTotal - cur);
        }

        public void cost(ChunkPos chunk, double amount) {
            extractedResources.addTo(chunk, amount);
        }

        public void setActiveChunks(Set<ChunkPos> activeChunks) {
            this.activeChunks = new HashSet<>(activeChunks);
        }

        public void clearActiveChunks() {
            activeChunks.clear();
        }

        /**
         * Read-only query used by client-facing mine information. This does not
         * require the chunk to be in the transient active set.
         */
        public double getExtracted(ChunkPos chunk) {
            return Math.max(0.0, extractedResources.getOrDefault(chunk, 0.0));
        }

    }
}
