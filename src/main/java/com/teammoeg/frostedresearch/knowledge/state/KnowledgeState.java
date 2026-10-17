/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge.state;

import com.mojang.serialization.Codec;
import com.teammoeg.chorda.dataholders.SpecialData;
import com.teammoeg.frostedresearch.knowledge.model.KnowledgeKey;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

/**
 * 知识系统的持久化队伍状态。 / Persistent team archive, inbox and actual history.
 */
public class KnowledgeState implements SpecialData {
    public static final Codec<KnowledgeState> CODEC = CompoundTag.CODEC.xmap(KnowledgeState::read, KnowledgeState::write);
    private final KnowledgeArchive archive = new KnowledgeArchive();
    private final KnowledgeInbox inbox = new KnowledgeInbox();
    private final Map<ResourceLocation, LinkDiscovery> links = new LinkedHashMap<>();
    private final Map<UUID, ResearchHistory> research = new LinkedHashMap<>();
    private final Map<ResourceLocation, List<String>> hints = new LinkedHashMap<>();
    private final Map<String, Long> daily = new LinkedHashMap<>();
    private final Map<UUID, KnowledgeKey> dreamTopics = new LinkedHashMap<>();
    private final List<KnowledgeEntry> acquisitions = new ArrayList<>();
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
        initialized |= source.initialized;
        revision++;
    }

    public CompoundTag write() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("schema_version", 2);
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
        return tag;
    }

    public static KnowledgeState read(CompoundTag tag) {
        KnowledgeState state = new KnowledgeState();
        decodeList(tag, "archive", KnowledgeEntry.CODEC).forEach(e -> state.archive.entries.put(e.element().key(), e));
        decodeList(tag, "inbox", KnowledgeEntry.CODEC).forEach(e -> state.inbox.entries.put(e.element().key(), e));
        decodeList(tag, "links", LinkDiscovery.CODEC).forEach(e -> state.links.put(e.rule(), e));
        decodeList(tag, "research", ResearchHistory.CODEC).forEach(e -> state.research.put(e.id(), e));
        state.acquisitions.addAll(decodeList(tag, "acquisitions", KnowledgeEntry.CODEC));
        CompoundTag hints = tag.getCompound("hints");
        hints.getAllKeys().forEach(k -> state.hints.put(new ResourceLocation(k), decode(Codec.STRING.listOf(), hints.get(k))));
        CompoundTag daily = tag.getCompound("daily");
        daily.getAllKeys().forEach(k -> state.daily.put(k, daily.getLong(k)));
        CompoundTag topics = tag.getCompound("dream_topics");
        topics.getAllKeys().forEach(k -> state.dreamTopics.put(UUID.fromString(k), decode(KnowledgeKey.CODEC, topics.get(k))));
        state.initialized = tag.getBoolean("initialized");
        state.revision = tag.getLong("revision");
        return state;
    }

    public static <T> Tag encode(Codec<T> codec, T value) {
        return codec.encodeStart(NbtOps.INSTANCE, value).getOrThrow(false, s -> {
        });
    }

    public static <T> T decode(Codec<T> codec, Tag tag) {
        return codec.parse(NbtOps.INSTANCE, tag).getOrThrow(false, s -> {
        });
    }

    private static <T> List<T> decodeList(CompoundTag tag, String key, Codec<T> codec) {
        return tag.contains(key) ? decode(codec.listOf(), tag.get(key)) : List.of();
    }
}
