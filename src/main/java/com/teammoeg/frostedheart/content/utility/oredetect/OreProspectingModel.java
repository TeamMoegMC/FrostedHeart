/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.utility.oredetect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.bootstrap.reference.FHTags;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Shared, read-only ore scan used by tools and sealed field observations. */
public final class OreProspectingModel {
    private OreProspectingModel() {
    }

    /** Preserves the historical half-open scan volume: [-h,h) x [-v,v) x [-h,h). */
    public static Snapshot scan(BlockGetter level, BlockPos origin, int horizontalRange, int verticalRange) {
        Map<ResourceLocation, Integer> counts = new LinkedHashMap<>();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -horizontalRange; dx < horizontalRange; dx++) {
            for (int dy = -verticalRange; dy < verticalRange; dy++) {
                for (int dz = -horizontalRange; dz < horizontalRange; dz++) {
                    BlockState state = level.getBlockState(cursor.setWithOffset(origin, dx, dy, dz));
                    if (!state.is(FHTags.Blocks.ORES.tag)) continue;
                    ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(state.getBlock());
                    if (blockId != null) counts.merge(blockId, 1, Integer::sum);
                }
            }
        }
        return new Snapshot(counts);
    }

    public record Snapshot(Map<ResourceLocation, Integer> mineralCounts) {
        private static final Codec<Entry> ENTRY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("block").forGetter(Entry::block),
                Codec.INT.fieldOf("count").forGetter(Entry::count)
        ).apply(instance, Entry::new));
        public static final Codec<Snapshot> CODEC = ENTRY_CODEC.listOf().xmap(Snapshot::fromEntries, Snapshot::entries);
        public static final Snapshot EMPTY = new Snapshot(Map.of());

        public Snapshot {
            List<Map.Entry<ResourceLocation, Integer>> sorted = new ArrayList<>(mineralCounts.entrySet());
            sorted.removeIf(entry -> entry.getValue() == null || entry.getValue() <= 0);
            sorted.sort(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)));
            Map<ResourceLocation, Integer> normalized = new LinkedHashMap<>();
            sorted.forEach(entry -> normalized.put(entry.getKey(), entry.getValue()));
            mineralCounts = Collections.unmodifiableMap(normalized);
        }

        public int totalCount() {
            return mineralCounts.values().stream().mapToInt(Integer::intValue).sum();
        }

        private List<Entry> entries() {
            return mineralCounts.entrySet().stream().map(entry -> new Entry(entry.getKey(), entry.getValue())).toList();
        }

        private static Snapshot fromEntries(List<Entry> entries) {
            Map<ResourceLocation, Integer> values = new LinkedHashMap<>();
            entries.forEach(entry -> values.merge(entry.block(), entry.count(), Integer::sum));
            return new Snapshot(values);
        }

        private record Entry(ResourceLocation block, int count) {
        }
    }
}
