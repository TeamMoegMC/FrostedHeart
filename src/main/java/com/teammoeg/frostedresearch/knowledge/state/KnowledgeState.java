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

/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge.state;

import com.mojang.serialization.Codec;
import com.teammoeg.chorda.dataholders.SpecialData;
import com.teammoeg.frostedresearch.FRMain;
import com.teammoeg.frostedresearch.knowledge.model.KnowledgeKey;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * 知识系统的持久化队伍状态。 / Persistent team archive, inbox and actual history.
 */
public class KnowledgeState implements SpecialData {
    /**
     * Layout written by {@link #write()}; bump it together with a step in {@link #upgrade}.
     */
    public static final int SCHEMA_VERSION = 2;
    public static final Codec<KnowledgeState> CODEC = CompoundTag.CODEC.xmap(KnowledgeState::read, KnowledgeState::write);
    private final KnowledgeArchive archive = new KnowledgeArchive();
    private final KnowledgeInbox inbox = new KnowledgeInbox();
    private final Map<ResourceLocation, LinkDiscovery> links = new LinkedHashMap<>();
    private final Map<UUID, ResearchHistory> research = new LinkedHashMap<>();
    private final Map<ResourceLocation, List<String>> hints = new LinkedHashMap<>();
    private final Map<String, Long> daily = new LinkedHashMap<>();
    private final Map<UUID, KnowledgeKey> dreamTopics = new LinkedHashMap<>();
    private final List<KnowledgeEntry> acquisitions = new ArrayList<>();
    /**
     * Saved entries this version cannot decode, in their saved layout. They take no part in play and are written back
     * unchanged, so a later version that can read them again finds them in place.
     */
    private CompoundTag unreadable = new CompoundTag();
    private boolean initialized;
    private long revision;

    public KnowledgeArchive archive() {
        return archive;
    }

    public KnowledgeInbox inbox() {
        return inbox;
    }

    public Map<ResourceLocation, LinkDiscovery> links() {
        return Collections.unmodifiableMap(links);
    }

    public Map<UUID, ResearchHistory> research() {
        return Collections.unmodifiableMap(research);
    }

    public Map<ResourceLocation, List<String>> hints() {
        return Collections.unmodifiableMap(hints);
    }

    public List<KnowledgeEntry> acquisitions() {
        return List.copyOf(acquisitions);
    }

    public long mutationRevision() {
        return revision;
    }

    public boolean initialized() {
        return initialized;
    }

    public void initialize() {
        initialized = true;
        revision++;
    }

    public void putArchive(KnowledgeEntry entry) {
        archive.entries.put(entry.element().key(), entry);
        revision++;
    }

    public void putInbox(KnowledgeEntry entry) {
        inbox.entries.put(entry.element().key(), entry);
        revision++;
    }

    public boolean removeArchive(KnowledgeKey key) {
        boolean changed = archive.entries.remove(key) != null;
        if (changed) revision++;
        return changed;
    }

    public boolean removeInbox(KnowledgeKey key) {
        boolean changed = inbox.entries.remove(key) != null;
        if (changed) revision++;
        return changed;
    }

    public void recordAcquisition(KnowledgeEntry entry) {
        acquisitions.add(entry);
        revision++;
    }

    public boolean discover(LinkDiscovery link) {
        if (links.putIfAbsent(link.rule(), link) != null) return false;
        revision++;
        return true;
    }

    public void recordResearch(ResearchHistory record) {
        research.put(record.id(), record);
        revision++;
    }

    public boolean hasHint(String text) {
        return hints.values().stream().anyMatch(values -> values.contains(text));
    }

    public void addHint(ResourceLocation rule, String text) {
        if (hasHint(text)) return;
        List<String> values = new ArrayList<>(hints.getOrDefault(rule, List.of()));
        values.add(text);
        hints.put(rule, List.copyOf(values));
        revision++;
    }

    /**
     * Keep revealed stages unique when authors replace literal wording with translation keys.
     */
    public boolean reconcileHints(Map<ResourceLocation, com.teammoeg.frostedresearch.knowledge.link.LinkRule> rules) {
        boolean changed = false;
        Set<String> seen = new LinkedHashSet<>();
        for (var entry : hints.entrySet()) {
            var rule = rules.get(entry.getKey());
            List<String> current = rule == null ? List.of() : rule.hints();
            List<String> resolved = new ArrayList<>();
            List<String> previous = entry.getValue();
            for (int i = 0; i < previous.size(); i++) {
                String text = previous.get(i);
                if (!current.contains(text) && i < current.size()) text = current.get(i);
                if (seen.add(text)) resolved.add(text);
            }
            if (!resolved.equals(previous)) {
                entry.setValue(List.copyOf(resolved));
                changed = true;
            }
        }
        if (changed) revision++;
        return changed;
    }

    public long lastDay(String key) {
        return daily.getOrDefault(key, Long.MIN_VALUE);
    }

    public void markDay(String key, long day) {
        daily.put(key, day);
        revision++;
    }

    public KnowledgeKey dreamTopic(UUID player) {
        return dreamTopics.get(player);
    }

    public void dreamTopic(UUID player, KnowledgeKey key) {
        dreamTopics.put(player, key);
        revision++;
    }

    public void replaceState(KnowledgeState replacement) {
        archive.entries.clear();
        archive.entries.putAll(replacement.archive.entries);
        inbox.entries.clear();
        inbox.entries.putAll(replacement.inbox.entries);
        links.clear();
        links.putAll(replacement.links);
        research.clear();
        research.putAll(replacement.research);
        hints.clear();
        hints.putAll(replacement.hints);
        daily.clear();
        daily.putAll(replacement.daily);
        dreamTopics.clear();
        dreamTopics.putAll(replacement.dreamTopics);
        acquisitions.clear();
        acquisitions.addAll(replacement.acquisitions);
        unreadable = replacement.unreadable.copy();
        initialized = replacement.initialized;
        revision++;
    }

    /**
     * Explicit team merge preserves all distinct records, even above configured capacity.
     */
    public void merge(KnowledgeState source) {
        source.archive.entries.forEach(archive.entries::putIfAbsent);
        source.inbox.entries.forEach((key, entry) -> {
            if (!archive.contains(key)) inbox.entries.putIfAbsent(key, entry);
        });
        archive.entries.keySet().forEach(inbox.entries::remove);
        source.links.forEach(links::putIfAbsent);
        source.research.forEach(research::putIfAbsent);
        source.hints.forEach((key, values) -> {
            LinkedHashSet<String> all = new LinkedHashSet<>(hints.getOrDefault(key, List.of()));
            all.addAll(values);
            hints.put(key, List.copyOf(all));
        });
        source.daily.forEach((key, day) -> daily.merge(key, day, Math::max));
        source.dreamTopics.forEach(dreamTopics::putIfAbsent);
        for (KnowledgeEntry entry : source.acquisitions) if (!acquisitions.contains(entry)) acquisitions.add(entry);
        // Unreadable entries move with the team as well; identical ones are kept once.
        for (String section : source.unreadable.getAllKeys()) {
            Tag own = unreadable.get(section);
            if (source.unreadable.get(section) instanceof ListTag raw) {
                ListTag list = own instanceof ListTag existing ? existing : new ListTag();
                Set<Tag> present = new HashSet<>(list);
                for (Tag entry : raw) if (present.add(entry)) list.addTag(list.size(), entry.copy());
                unreadable.put(section, list);
            } else if (source.unreadable.get(section) instanceof CompoundTag raw) {
                CompoundTag map = own instanceof CompoundTag existing ? existing : new CompoundTag();
                for (String key : raw.getAllKeys()) if (!map.contains(key)) map.put(key, raw.get(key).copy());
                unreadable.put(section, map);
            }
        }
        initialized |= source.initialized;
        revision++;
    }

    public CompoundTag write() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("schema_version", SCHEMA_VERSION);
        tag.putBoolean("initialized", initialized);
        tag.putLong("revision", revision);
        tag.put("archive", encode(KnowledgeEntry.CODEC.listOf(), List.copyOf(archive.entries.values())));
        tag.put("inbox", encode(KnowledgeEntry.CODEC.listOf(), List.copyOf(inbox.entries.values())));
        tag.put("links", encode(LinkDiscovery.CODEC.listOf(), List.copyOf(links.values())));
        tag.put("research", encode(ResearchHistory.CODEC.listOf(), List.copyOf(research.values())));
        tag.put("acquisitions", encode(KnowledgeEntry.CODEC.listOf(), acquisitions));
        CompoundTag h = new CompoundTag();
        hints.forEach((k, v) -> h.put(k.toString(), encode(Codec.STRING.listOf(), v)));
        tag.put("hints", h);
        CompoundTag d = new CompoundTag();
        daily.forEach(d::putLong);
        tag.put("daily", d);
        CompoundTag t = new CompoundTag();
        dreamTopics.forEach((k, v) -> t.put(k.toString(), encode(KnowledgeKey.CODEC, v)));
        tag.put("dream_topics", t);
        // Unreadable entries go back into their own sections unchanged; a live entry keeps its key.
        for (String section : unreadable.getAllKeys()) {
            if (unreadable.get(section) instanceof ListTag raw && tag.get(section) instanceof ListTag list) {
                // An NBT list holds one tag type; only hand-edited entries that are not compounds can be refused.
                for (Tag entry : raw) list.addTag(list.size(), entry.copy());
            } else if (unreadable.get(section) instanceof CompoundTag raw && tag.get(section) instanceof CompoundTag map) {
                for (String key : raw.getAllKeys()) if (!map.contains(key)) map.put(key, raw.get(key).copy());
            }
        }
        return tag;
    }

    /**
     * Never fails: each entry is decoded on its own, and one this version cannot read is set aside in
     * {@link #unreadable} instead of costing the team its whole data file.
     */
    public static KnowledgeState read(CompoundTag tag) {
        KnowledgeState state = new KnowledgeState();
        CompoundTag data = upgrade(tag, tag.getInt("schema_version"));
        Reader reader = new Reader(data, state.unreadable);
        reader.list("archive", KnowledgeEntry.CODEC, e -> state.archive.entries.put(e.element().key(), e));
        reader.list("inbox", KnowledgeEntry.CODEC, e -> state.inbox.entries.put(e.element().key(), e));
        reader.list("links", LinkDiscovery.CODEC, e -> state.links.put(e.rule(), e));
        reader.list("research", ResearchHistory.CODEC, e -> state.research.put(e.id(), e));
        reader.list("acquisitions", KnowledgeEntry.CODEC, state.acquisitions::add);
        reader.keyed("hints", (k, v) -> state.hints.put(new ResourceLocation(k), decode(Codec.STRING.listOf(), v)));
        reader.keyed("dream_topics", (k, v) -> state.dreamTopics.put(UUID.fromString(k), decode(KnowledgeKey.CODEC, v)));
        CompoundTag daily = data.getCompound("daily");
        daily.getAllKeys().forEach(k -> state.daily.put(k, daily.getLong(k)));
        state.initialized = data.getBoolean("initialized");
        state.revision = data.getLong("revision");
        reader.log();
        return state;
    }

    /**
     * The one place for stored-layout migrations: add steps here, oldest first, that rewrite a copy of {@code tag}
     * from {@code version} up to {@link #SCHEMA_VERSION}. Version 2 is the first knowledge-system layout, so there are
     * none yet. A missing or newer version is decoded as it is; whatever the current codecs cannot read is set aside.
     */
    private static CompoundTag upgrade(CompoundTag tag, int version) {
        return tag;
    }

    public static <T> Tag encode(Codec<T> codec, T value) {
        return codec.encodeStart(NbtOps.INSTANCE, value).getOrThrow(false, s -> {
        });
    }

    public static <T> T decode(Codec<T> codec, Tag tag) {
        return codec.parse(NbtOps.INSTANCE, tag).getOrThrow(false, s -> {
        });
    }

    /**
     * Decodes one saved entry at a time and keeps a copy of every entry that fails, in the section it came from.
     */
    private static final class Reader {
        private final CompoundTag data;
        private final CompoundTag unreadable;
        private int count;
        private String first;

        Reader(CompoundTag data, CompoundTag unreadable) {
            this.data = data;
            this.unreadable = unreadable;
        }

        <T> void list(String section, Codec<T> codec, Consumer<T> sink) {
            Tag stored = data.get(section);
            if (stored == null) return;
            // A section that is not a list at all (hand-edited) is kept as one entry rather than dropped.
            List<Tag> entries = stored instanceof ListTag list ? list : List.of(stored);
            ListTag kept = new ListTag();
            for (int i = 0; i < entries.size(); i++) {
                try {
                    sink.accept(decode(codec, entries.get(i)));
                } catch (RuntimeException e) {
                    keep(section + "[" + i + "]", e);
                    kept.addTag(kept.size(), entries.get(i).copy());
                }
            }
            if (!kept.isEmpty()) unreadable.put(section, kept);
        }

        void keyed(String section, BiConsumer<String, Tag> sink) {
            CompoundTag stored = data.getCompound(section);
            CompoundTag kept = new CompoundTag();
            for (String key : stored.getAllKeys()) {
                try {
                    sink.accept(key, stored.get(key));
                } catch (RuntimeException e) {
                    keep(section + "[" + key + "]", e);
                    kept.put(key, stored.get(key).copy());
                }
            }
            if (!kept.isEmpty()) unreadable.put(section, kept);
        }

        private void keep(String where, RuntimeException e) {
            if (count++ == 0) first = where + ": " + e.getMessage();
        }

        void log() {
            if (count > 0)
                FRMain.LOGGER.warn("Knowledge data: set aside {} unreadable entries, which are kept out of play and saved back unchanged. First: {}", count, first);
        }
    }
}
