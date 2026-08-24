/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.transport.device;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.GlobalPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Persistent bounded town cache of terminal-owned filter summaries. */
public final class P2PFilterSummaryState {
    public static final int MAX_TERMINALS = 4096;
    public static final Comparator<GlobalPos> GLOBAL_POS_COMPARATOR = Comparator
            .comparing((GlobalPos pos) -> pos.dimension().location().toString())
            .thenComparingInt(pos -> pos.pos().getX())
            .thenComparingInt(pos -> pos.pos().getY())
            .thenComparingInt(pos -> pos.pos().getZ());
    public static final P2PFilterSummaryState EMPTY = new P2PFilterSummaryState(List.of());

    private static final Codec<List<Entry>> ENTRIES_CODEC = Entry.CODEC.listOf().flatXmap(
            P2PFilterSummaryState::validateEntries, P2PFilterSummaryState::validateEntries);
    public static final Codec<P2PFilterSummaryState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ENTRIES_CODEC.optionalFieldOf("terminals", List.of())
                    .forGetter(P2PFilterSummaryState::entries)
    ).apply(instance, P2PFilterSummaryState::new));

    private final Map<GlobalPos, Entry> summaries;
    private final List<Entry> entries;

    public P2PFilterSummaryState(List<Entry> entries) {
        List<Entry> sorted = new ArrayList<>(entries == null ? List.of() : entries);
        sorted.sort(Comparator.comparing(Entry::endpoint, GLOBAL_POS_COMPARATOR));
        if (sorted.size() > MAX_TERMINALS) {
            throw new IllegalArgumentException("P2P filter summary state exceeds its size limit.");
        }
        Map<GlobalPos, Entry> indexed = new LinkedHashMap<>();
        for (Entry entry : sorted) {
            if (entry == null || indexed.putIfAbsent(entry.endpoint(), entry) != null) {
                throw new IllegalArgumentException(
                        "P2P filter summary state contains a null or duplicate endpoint.");
            }
        }
        this.summaries = Collections.unmodifiableMap(indexed);
        this.entries = List.copyOf(indexed.values());
    }

    public List<Entry> entries() {
        return entries;
    }

    public Optional<Entry> get(GlobalPos endpoint) {
        return Optional.ofNullable(summaries.get(endpoint));
    }

    public P2PFilterSummaryState with(
            GlobalPos endpoint,
            P2PFilterSnapshot sendFilter,
            P2PFilterSnapshot receiveFilter
    ) {
        Entry replacement = new Entry(endpoint, sendFilter, receiveFilter);
        if (replacement.equals(summaries.get(endpoint))) {
            return this;
        }
        List<Entry> changed = new ArrayList<>(entries);
        changed.removeIf(entry -> entry.endpoint().equals(endpoint));
        changed.add(replacement);
        return new P2PFilterSummaryState(changed);
    }

    public P2PFilterSummaryState without(GlobalPos endpoint) {
        if (endpoint == null || !summaries.containsKey(endpoint)) {
            return this;
        }
        return new P2PFilterSummaryState(entries.stream()
                .filter(entry -> !entry.endpoint().equals(endpoint)).toList());
    }

    private static DataResult<List<Entry>> validateEntries(List<Entry> entries) {
        if (entries == null || entries.size() > MAX_TERMINALS) {
            return DataResult.error(() -> "P2P filter summaries exceed "
                    + MAX_TERMINALS + " terminals.");
        }
        try {
            new P2PFilterSummaryState(entries);
            return DataResult.success(entries);
        } catch (IllegalArgumentException exception) {
            return DataResult.error(exception::getMessage);
        }
    }

    public record Entry(
            GlobalPos endpoint,
            P2PFilterSnapshot sendFilter,
            P2PFilterSnapshot receiveFilter
    ) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                GlobalPos.CODEC.fieldOf("endpoint").forGetter(Entry::endpoint),
                P2PFilterSnapshot.CODEC.fieldOf("sendFilter").forGetter(Entry::sendFilter),
                P2PFilterSnapshot.CODEC.fieldOf("receiveFilter").forGetter(Entry::receiveFilter)
        ).apply(instance, Entry::new));

        public Entry {
            if (endpoint == null || sendFilter == null || receiveFilter == null) {
                throw new IllegalArgumentException("P2P filter summary entry is incomplete.");
            }
        }
    }
}
