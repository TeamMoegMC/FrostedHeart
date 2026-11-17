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
package com.teammoeg.frostedresearch.knowledge.client;

import com.teammoeg.frostedresearch.knowledge.client.observation.ObservationPresentation;
import com.teammoeg.frostedresearch.knowledge.model.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.*;

/**
 * Decodes one visible snapshot once. No world lookup or codec work belongs in rendering.
 */
public final class KnowledgeJournalData {
    public record Entry(KnowledgeKey key, KnowledgeElement element, String status, boolean active, String subtype,
                        String source,
                        String group, Component title, Component body, List<Component> summary, List<Component> details,
                        ItemStack icon, String search) {
    }

    public record Relation(String identity, Component title, List<KnowledgeKey> inputs, List<KnowledgeKey> outputs,
                           boolean active, boolean research, String project, boolean abandoned) {
        public Relation(String identity, Component title, List<KnowledgeKey> inputs, List<KnowledgeKey> outputs, boolean active, boolean research) {
            this(identity, title, inputs, outputs, active, research, "", false);
        }
    }

    public record Project(String id, Component title, List<KnowledgeKey> ideas, List<Component> outputTitles,
                          boolean active, boolean completed, Component body, List<Requirement> requirements,
                          KnowledgeKey origin, String startStatus, String openRun) {
        public Project(String id, Component title, List<KnowledgeKey> ideas, List<Component> outputTitles, boolean active, boolean completed) {
            this(id, title, ideas, outputTitles, active, completed, Component.empty(), List.of(), null, "", "");
        }
    }

    /**
     * One requirement as the server judged it. {@code slots} counts the team's records that fit each unsatisfied
     * evidence slot; {@code ready} says a full set can be submitted now. {@code done} counts the player's games or
     * experiments together with labor shares standing in for them; {@code playerRemaining} is what is still to be done
     * at a desk or table. {@code labor} is null when town researchers take no part.
     */
    public record Requirement(String id, String type, Component title, int level, int count, int done, boolean satisfied,
                              boolean ready, List<Integer> slots, List<KnowledgeKey> bound, int duration,
                              int playerDone, int playerRemaining, Labor labor) {
        public Requirement(String id, String type, Component title, int level, int count, int done, boolean satisfied,
                           boolean ready, List<Integer> slots, List<KnowledgeKey> bound, int duration) {
            this(id, type, title, level, count, done, satisfied, ready, slots, bound, duration, done, Math.max(0, count - done), null);
        }

        public boolean evidence() {
            return type.equals("evidence");
        }

        public boolean experiment() {
            return type.equals("experiment");
        }
    }

    /**
     * Town labor a requirement asks for: researchers per day of at least {@code education}, days per share, the daily
     * items and, once a run exists, the worker-days put in, still useful and the finished optional shares.
     */
    public record Labor(boolean required, int workers, int days, int education, List<Component> items,
                        double done, double remaining, int shares) {
        public double share() {
            return (double) workers * days;
        }
    }

    /** Observation records held and allowed; the base is the configured part of each limit. */
    public record Capacity(int archiveUsed, int archiveLimit, int archiveBase, int inboxUsed, int inboxLimit, int inboxBase) {
        public static final Capacity UNKNOWN = new Capacity(0, 0, 0, 0, 0, 0);
    }

    /**
     * The research institutes' latest day: labor by education level, what each requirement received, the labor left
     * over and the association.
     */
    public record LaborDay(long day, List<Double> labor, List<Double> idle, List<Allocation> allocations,
                           String outcome, double associationLabor, double chance, KnowledgeKey subject,
                           Component subjectTitle) {
        public double total() {
            return labor.stream().mapToDouble(Double::doubleValue).sum();
        }

        public double idleTotal() {
            return idle.stream().mapToDouble(Double::doubleValue).sum();
        }

        public Allocation allocation(String run, String requirement) {
            return allocations.stream().filter(a -> a.run().equals(run) && a.requirement().equals(requirement)).findFirst().orElse(null);
        }
    }

    public record Allocation(String run, String requirement, Component project, Component title, String type,
                             double wanted, double given, boolean itemsShort) {
    }

    /**
     * A research run: {@code status} is open, completed or abandoned; open runs carry their requirement progress.
     */
    public record Run(String id, String project, Component title, String status, KnowledgeKey idea, List<Requirement> requirements) {
        public boolean open() {
            return status.equals("open");
        }
    }

    public final List<Entry> inbox;
    public final List<Entry> archive;
    public final List<Relation> relations;
    /** 研究所想到每个想法的城镇日；旧存档里想到的没有记录。 / The town day an institute found each idea on; ideas from older saves have none. */
    public final Map<KnowledgeKey, Long> instituteDays;
    public final List<Component> hints;
    public final List<Project> projects;
    public final List<Run> runs;
    public final Capacity capacity;
    /** Null before any institute has worked. */
    public final LaborDay laborDay;
    private final Map<KnowledgeKey, Entry> entries;

    public KnowledgeJournalData(CompoundTag snapshot) {
        inbox = read(snapshot.getList("inbox", Tag.TAG_COMPOUND));
        archive = read(snapshot.getList("archive", Tag.TAG_COMPOUND));
        Map<KnowledgeKey, Entry> index = new LinkedHashMap<>();
        inbox.forEach(e -> index.put(e.key, e));
        archive.forEach(e -> index.put(e.key, e));
        entries = Map.copyOf(index);
        Map<KnowledgeKey, Long> days = new LinkedHashMap<>();
        for (Tag raw : snapshot.getList("acquisitions", Tag.TAG_COMPOUND)) {
            CompoundTag acquisition = (CompoundTag) raw;
            if (!acquisition.contains("town_day")) continue;
            KnowledgeKey found = key(acquisition.get("key"));
            if (found != null) days.put(found, acquisition.getLong("town_day"));
        }
        instituteDays = Map.copyOf(days);
        List<Relation> relationships = new ArrayList<>();
        for (Tag raw : snapshot.getList("links", Tag.TAG_COMPOUND)) {
            CompoundTag link = (CompoundTag) raw;
            KnowledgeKey output = key(link.get("output"));
            if (output == null) continue;
            List<KnowledgeKey> inputs = new ArrayList<>();
            for (Tag input : link.getList("inputs", Tag.TAG_COMPOUND)) {
                KnowledgeKey k = key(((CompoundTag) input).get("key"));
                if (k != null) inputs.add(k);
            }
            relationships.add(new Relation(link.getString("rule"), title(output), List.copyOf(inputs), List.of(output), link.getBoolean("active"), false));
        }
        List<Run> runs = new ArrayList<>();
        for (Tag raw : snapshot.getList("research", Tag.TAG_COMPOUND)) {
            CompoundTag r = (CompoundTag) raw;
            KnowledgeKey idea = key(r.get("idea"));
            if (idea == null) continue;
            List<KnowledgeKey> results = new ArrayList<>();
            for (Tag result : r.getList("results", Tag.TAG_COMPOUND)) {
                var k = key(result);
                if (k != null) results.add(k);
            }
            Component title = r.getString("title").isBlank() ? KnowledgePresentation.t("research") : KnowledgePresentation.text(r.getString("title"));
            boolean abandoned = r.getString("status").equals("abandoned");
            relationships.add(new Relation(r.getString("id"), title, List.of(idea), List.copyOf(results),
                    !abandoned && entry(idea) != null && entry(idea).active(), true, r.getString("project"), abandoned));
            String status = r.getString("status").isBlank() ? r.getBoolean("completed") ? "completed" : "open" : r.getString("status");
            runs.add(new Run(r.getString("id"), r.getString("project"), title, status, idea, requirements(r.getList("requirements", Tag.TAG_COMPOUND))));
        }
        relations = List.copyOf(relationships);
        this.runs = List.copyOf(runs);
        List<Project> projects = new ArrayList<>();
        for (Tag raw : snapshot.getList("projects", Tag.TAG_COMPOUND)) {
            CompoundTag project = (CompoundTag) raw;
            List<KnowledgeKey> origins = new ArrayList<>();
            for (Tag origin : project.getList("ideas", Tag.TAG_COMPOUND)) {
                var key = key(origin);
                if (key != null) origins.add(key);
            }
            List<Component> outputs = new ArrayList<>();
            for (Tag output : project.getList("outputs", Tag.TAG_COMPOUND))
                outputs.add(KnowledgePresentation.text(((CompoundTag) output).getString("title")));
            projects.add(new Project(project.getString("id"), KnowledgePresentation.text(project.getString("title")), List.copyOf(origins),
                    List.copyOf(outputs), project.getBoolean("active"), project.getBoolean("completed"),
                    KnowledgePresentation.text(project.getString("body")), requirements(project.getList("requirements", Tag.TAG_COMPOUND)),
                    key(project.get("origin")), project.getString("start_status"), project.getString("open_run")));
        }
        this.projects = List.copyOf(projects);
        Map<String, Component> hints = new LinkedHashMap<>();
        for (Tag raw : snapshot.getList("hints", Tag.TAG_COMPOUND)) {
            Component text = KnowledgePresentation.text(((CompoundTag) raw).getString("text"));
            hints.putIfAbsent(text.getString().strip(), text);
        }
        this.hints = List.copyOf(hints.values());
        CompoundTag c = snapshot.getCompound("capacity");
        capacity = c.isEmpty() ? Capacity.UNKNOWN : new Capacity(c.getInt("archive_used"), c.getInt("archive_limit"), c.getInt("archive_base"),
                c.getInt("inbox_used"), c.getInt("inbox_limit"), c.getInt("inbox_base"));
        laborDay = snapshot.contains("labor_report", Tag.TAG_COMPOUND) ? laborDay(snapshot.getCompound("labor_report")) : null;
    }

    private static LaborDay laborDay(CompoundTag tag) {
        List<Allocation> allocations = new ArrayList<>();
        for (Tag raw : tag.getList("allocations", Tag.TAG_COMPOUND)) {
            CompoundTag a = (CompoundTag) raw;
            allocations.add(new Allocation(a.getString("run"), a.getString("requirement"), KnowledgePresentation.text(a.getString("project_title")),
                    KnowledgePresentation.text(a.getString("title")), a.getString("type"), a.getDouble("wanted"), a.getDouble("given"),
                    a.getBoolean("items_short")));
        }
        CompoundTag association = tag.getCompound("association");
        return new LaborDay(tag.getLong("day"), doubles(tag.getList("labor", Tag.TAG_DOUBLE)), doubles(tag.getList("idle", Tag.TAG_DOUBLE)),
                List.copyOf(allocations), association.getString("outcome"), association.getDouble("labor"), association.getDouble("chance"),
                key(association.get("subject")), KnowledgePresentation.text(association.getString("subject_title")));
    }

    private static List<Double> doubles(ListTag list) {
        List<Double> values = new ArrayList<>();
        for (Tag raw : list) values.add(((NumericTag) raw).getAsDouble());
        return List.copyOf(values);
    }

    private static Labor labor(CompoundTag tag) {
        List<Component> items = new ArrayList<>();
        for (Tag raw : tag.getList("items", Tag.TAG_COMPOUND)) {
            CompoundTag item = (CompoundTag) raw;
            Component name = item.contains("item")
                    ? BuiltInRegistries.ITEM.getOptional(ResourceLocation.tryParse(item.getString("item"))).map(i -> i.getDescription())
                    .orElse(Component.literal(item.getString("item")))
                    : tagName(item.getString("tag"));
            items.add(KnowledgePresentation.t("research.labor.item", name, item.getInt("count")));
        }
        return new Labor(tag.getBoolean("required"), tag.getInt("workers"), tag.getInt("days"), tag.getInt("education"), List.copyOf(items),
                tag.getDouble("done"), tag.getDouble("remaining"), tag.getInt("shares"));
    }

    private static Component tagName(String id) {
        ResourceLocation location = ResourceLocation.tryParse(id);
        if (location == null) return Component.literal("#" + id);
        return Component.translatableWithFallback("tag.item." + location.getNamespace() + "." + location.getPath().replace('/', '.'), "#" + id);
    }

    private static List<Requirement> requirements(ListTag list) {
        List<Requirement> result = new ArrayList<>();
        for (Tag raw : list) {
            CompoundTag tag = (CompoundTag) raw;
            List<Integer> slots = new ArrayList<>();
            for (Tag slot : tag.getList("slots", Tag.TAG_INT)) slots.add(((IntTag) slot).getAsInt());
            List<KnowledgeKey> bound = new ArrayList<>();
            for (Tag key : tag.getList("bound", Tag.TAG_COMPOUND)) {
                var k = key(key);
                if (k != null) bound.add(k);
            }
            int done = tag.getInt("done");
            int playerDone = tag.contains("player_done") ? tag.getInt("player_done") : done;
            int playerRemaining = tag.contains("player_remaining") ? tag.getInt("player_remaining") : Math.max(0, tag.getInt("count") - done);
            result.add(new Requirement(tag.getString("id"), tag.getString("type"), KnowledgePresentation.text(tag.getString("title")),
                    tag.getInt("level"), tag.getInt("count"), done, tag.getBoolean("satisfied"), tag.getBoolean("ready"),
                    List.copyOf(slots), List.copyOf(bound), tag.getInt("duration"), playerDone, playerRemaining,
                    tag.contains("labor", Tag.TAG_COMPOUND) ? labor(tag.getCompound("labor")) : null));
        }
        return List.copyOf(result);
    }

    public Run run(String id) {
        return runs.stream().filter(run -> run.id().equals(id)).findFirst().orElse(null);
    }

    public Run openRun(String project) {
        return runs.stream().filter(run -> run.open() && run.project().equals(project)).findFirst().orElse(null);
    }

    public Entry entry(KnowledgeKey key) {
        return key == null ? null : entries.get(key);
    }

    public Component title(KnowledgeKey key) {
        var entry = entry(key);
        return entry == null ? KnowledgePresentation.t("history.record") : entry.title;
    }

    public static KnowledgeKey key(Tag tag) {
        return tag == null ? null : KnowledgeKey.CODEC.parse(NbtOps.INSTANCE, tag).result().orElse(null);
    }

    private static List<Entry> read(ListTag records) {
        List<Entry> result = new ArrayList<>();
        for (Tag raw : records) {
            CompoundTag tag = (CompoundTag) raw;
            KnowledgeElement element = KnowledgeElement.CODEC.parse(NbtOps.INSTANCE, tag.getCompound("element")).result().orElse(null);
            if (element == null) continue;
            Component title = KnowledgePresentation.title(element), body = KnowledgePresentation.body(element);
            List<Component> summary = element.observation().map(ObservationPresentation::summary).orElse(List.of());
            List<Component> details = element.observation().map(ObservationPresentation::details).orElse(List.of());
            String searchable = (title.getString() + " " + body.getString() + " " + String.join(" ", summary.stream().map(Component::getString).toList())).toLowerCase(Locale.ROOT);
            result.add(new Entry(element.key(), element, tag.getString("status"), tag.getBoolean("active"), tag.getString("subtype"), tag.getString("source"), tag.getString("group"), title, body, summary, details, KnowledgePresentation.icon(element), searchable));
        }
        return List.copyOf(result);
    }
}
