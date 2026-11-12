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
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
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

    public record Project(String id, Component title, List<KnowledgeKey> sources, List<Component> outputTitles,
                          boolean active, boolean completed, Component body, List<Requirement> requirements,
                          String startStatus, String openRun, List<AccessCondition> accessConditions, List<AccessGroup> groups, boolean staged) {
        public Project(String id,Component title,List<KnowledgeKey> sources,List<Component> outputTitles,boolean active,boolean completed,
                Component body,List<Requirement> requirements,String startStatus,String openRun,List<AccessCondition> accessConditions,List<AccessGroup> groups) {
            this(id,title,sources,outputTitles,active,completed,body,requirements,startStatus,openRun,accessConditions,groups,false);
        }
        public Project(String id, Component title, List<KnowledgeKey> sources, List<Component> outputTitles, boolean active, boolean completed) {
            this(id, title, sources, outputTitles, active, completed, Component.empty(), List.of(), "", "", List.of(), List.of());
        }
    }

    /**
     * One requirement as the server judged it. {@code slots} counts the team's records that fit each unsatisfied
     * evidence slot; {@code ready} says a full set can be submitted now.
     */
    public record Requirement(String id, String type, Component title, int level, int count, int done, boolean satisfied,
                              boolean ready, List<Integer> slots, List<KnowledgeKey> bound, int duration, boolean optional, Component method) {
        public boolean sample() { return type.equals("sample"); }

        public boolean evidence() {
            return type.equals("evidence");
        }

        public boolean experiment() {
            return type.equals("experiment");
        }
    }

    /**
     * A research run: {@code status} is open, completed or abandoned; open runs carry their requirement progress.
     */
    public record Stage(String id,Component title,Component objective,Component method,boolean ready,boolean last,boolean available,List<Component> conclusions,List<String> recipes) {
        static Stage read(CompoundTag tag) {
            return new Stage(tag.getString("id"),KnowledgePresentation.text(tag.getString("title")),KnowledgePresentation.text(tag.getString("objective")),
                KnowledgePresentation.text(tag.getString("method")),tag.getBoolean("ready"),tag.getBoolean("last"),tag.getBoolean("available"),
                tag.getList("conclusions",Tag.TAG_STRING).stream().map(t -> KnowledgePresentation.text(t.getAsString())).toList(),
                tag.getList("temporary_recipes",Tag.TAG_STRING).stream().map(Tag::getAsString).toList());
        }
    }
    public record Run(String id, String project, Component title, String status, List<KnowledgeKey> origins, List<Requirement> requirements, Stage stage) {
        public boolean open() {
            return status.equals("open");
        }
    }

    public final List<Entry> inbox;
    public final List<Entry> archive;
    public final List<Relation> relations;
    public final List<Component> hints;
    public final List<Project> projects;
    public final List<Run> runs;
    private final Map<KnowledgeKey, Entry> entries;

    public KnowledgeJournalData(CompoundTag snapshot) {
        inbox = read(snapshot.getList("inbox", Tag.TAG_COMPOUND));
        archive = read(snapshot.getList("archive", Tag.TAG_COMPOUND));
        Map<KnowledgeKey, Entry> index = new LinkedHashMap<>();
        inbox.forEach(e -> index.put(e.key, e));
        archive.forEach(e -> index.put(e.key, e));
        entries = Map.copyOf(index);
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
            List<KnowledgeKey> origins = keys(r.getList("origins", Tag.TAG_COMPOUND));
            List<KnowledgeKey> results = new ArrayList<>();
            for (Tag result : r.getList("results", Tag.TAG_COMPOUND)) {
                var k = key(result);
                if (k != null) results.add(k);
            }
            Component title = r.getString("title").isBlank() ? KnowledgePresentation.t("research") : KnowledgePresentation.text(r.getString("title"));
            boolean abandoned = r.getString("status").equals("abandoned");
            relationships.add(new Relation(r.getString("id"), title, origins, List.copyOf(results),
                    !abandoned && r.getBoolean("active"), true, r.getString("project"), abandoned));
            String status = r.getString("status").isBlank() ? r.getBoolean("completed") ? "completed" : "open" : r.getString("status");
            runs.add(new Run(r.getString("id"), r.getString("project"), title, status, origins, requirements(r.getList("requirements", Tag.TAG_COMPOUND)),Stage.read(r.getCompound("stage"))));
        }
        relations = List.copyOf(relationships);
        this.runs = List.copyOf(runs);
        List<Project> projects = new ArrayList<>();
        for (Tag raw : snapshot.getList("projects", Tag.TAG_COMPOUND)) {
            CompoundTag project = (CompoundTag) raw;
            List<KnowledgeKey> origins = new ArrayList<>();
            for (Tag origin : project.getList("sources", Tag.TAG_COMPOUND)) {
                var key = key(origin);
                if (key != null) origins.add(key);
            }
            List<Component> outputs = new ArrayList<>();
            for (Tag output : project.getList("outputs", Tag.TAG_COMPOUND))
                outputs.add(KnowledgePresentation.text(((CompoundTag) output).getString("title")));
            projects.add(new Project(project.getString("id"), KnowledgePresentation.text(project.getString("title")), List.copyOf(origins),
                    List.copyOf(outputs), project.getBoolean("active"), project.getBoolean("completed"),
                    KnowledgePresentation.text(project.getString("body")), requirements(project.getList("requirements", Tag.TAG_COMPOUND)),
                    project.getString("start_status"), project.getString("open_run"), accessConditions(project), accessGroups(project),project.getBoolean("staged")));
        }
        this.projects = List.copyOf(projects);
        Map<String, Component> hints = new LinkedHashMap<>();
        for (Tag raw : snapshot.getList("hints", Tag.TAG_COMPOUND)) {
            Component text = KnowledgePresentation.text(((CompoundTag) raw).getString("text"));
            hints.putIfAbsent(text.getString().strip(), text);
        }
        this.hints = List.copyOf(hints.values());
    }

    public record AccessCondition(String id, boolean required, boolean hidden, Component title, Component hint,
            boolean met, KnowledgeKey key, List<KnowledgeKey> inboxKeys) {}
    public record AccessGroup(String id, boolean hidden, Component hint, List<String> conditions) {}
    private static List<KnowledgeKey> keys(ListTag list) {
        return list.stream().map(KnowledgeJournalData::key).filter(Objects::nonNull).toList();
    }
    private static List<AccessCondition> accessConditions(CompoundTag project) {
        List<AccessCondition> rows = new ArrayList<>();
        for (Tag raw : project.getList("access_conditions", Tag.TAG_COMPOUND)) {
            CompoundTag t = (CompoundTag) raw;
            rows.add(new AccessCondition(t.getString("id"), t.getBoolean("required"), t.getBoolean("hidden"),
                KnowledgePresentation.text(t.getString("title")), KnowledgePresentation.text(t.getString("hint")),
                t.getBoolean("met"), key(t.get("key")), keys(t.getList("inbox_keys", Tag.TAG_COMPOUND))));
        }
        return List.copyOf(rows);
    }
    private static List<AccessGroup> accessGroups(CompoundTag project) {
        List<AccessGroup> groups = new ArrayList<>();
        for (Tag raw : project.getList("groups", Tag.TAG_COMPOUND)) {
            CompoundTag t = (CompoundTag) raw;
            groups.add(new AccessGroup(t.getString("id"), t.getBoolean("hidden"), KnowledgePresentation.text(t.getString("unknown_hint")),
                t.getList("conditions", Tag.TAG_STRING).stream().map(Tag::getAsString).toList()));
        }
        return List.copyOf(groups);
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
            result.add(new Requirement(tag.getString("id"), tag.getString("type"), KnowledgePresentation.text(tag.getString("title")),
                    tag.getInt("level"), tag.getInt("count"), tag.getInt("done"), tag.getBoolean("satisfied"), tag.getBoolean("ready"),
                    List.copyOf(slots), List.copyOf(bound), tag.getInt("duration"),tag.getBoolean("optional"),KnowledgePresentation.text(tag.getString("method"))));
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
