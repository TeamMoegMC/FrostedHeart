/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge;

import com.teammoeg.frostedresearch.knowledge.definition.KnowledgeDefinitions;
import com.teammoeg.frostedresearch.knowledge.model.*;
import com.teammoeg.frostedresearch.knowledge.state.*;
import net.minecraft.nbt.*;

import java.util.*;

/**
 * 只包含队伍已经接触的内容和实际关系，不同步未发现规则。 / Team-visible snapshot only.
 */
public final class KnowledgeView {
    private KnowledgeView() {
    }

    public static CompoundTag create(KnowledgeService service) {
        CompoundTag view = new CompoundTag();
        view.putLong("revision", service.state().mutationRevision());
        view.putLong("definitions_revision", KnowledgeDefinitions.current().revision());
        view.put("archive", entries(service, service.state().archive().entries().values()));
        view.put("inbox", entries(service, service.state().inbox().entries().values()));
        ListTag links = new ListTag();
        service.state().links().values().forEach(link -> {
            CompoundTag tag = new CompoundTag();
            tag.putString("rule", link.rule().toString());
            tag.put("output", KnowledgeState.encode(KnowledgeKey.CODEC, link.output()));
            tag.putLong("time", link.time());
            tag.putBoolean("active", activeLink(service, link));
            ListTag inputs = new ListTag();
            link.inputs().forEach((slot, key) -> {
                CompoundTag input = new CompoundTag();
                input.putString("slot", slot);
                input.put("key", KnowledgeState.encode(KnowledgeKey.CODEC, key));
                inputs.add(input);
            });
            tag.put("inputs", inputs);
            links.add(tag);
        });
        view.put("links", links);
        ListTag hints = new ListTag();
        service.state().hints().forEach((rule, texts) -> {
            for (int i = 0; i < texts.size(); i++) {
                CompoundTag hint = new CompoundTag();
                hint.putString("rule", rule.toString());
                hint.putString("text", texts.get(i));
                hint.putInt("level", i + 1);
                hints.add(hint);
            }
        });
        view.put("hints", hints);
        ListTag research = new ListTag();
        service.state().research().values().forEach(record -> {
            CompoundTag tag = new CompoundTag();
            tag.putString("id", record.id().toString());
            tag.putString("project", record.project().toString());
            var project = KnowledgeDefinitions.current().projects().get(record.project());
            if (project != null) tag.putString("title", project.title());
            tag.put("idea", KnowledgeState.encode(KnowledgeKey.CODEC, record.idea()));
            tag.putLong("time", record.time());
            tag.putBoolean("completed", record.completed());
            tag.put("results", KnowledgeState.encode(KnowledgeKey.CODEC.listOf(), record.results()));
            research.add(tag);
        });
        view.put("research", research);
        // Only projects disclosed by learned ideas are visible before a research run exists.
        Map<net.minecraft.resources.ResourceLocation, List<KnowledgeKey>> disclosed = new LinkedHashMap<>();
        for (KnowledgeKey key : service.state().archive().entries().keySet()) {
            if (key.kind() != KnowledgeKey.Kind.IDEA) continue;
            var idea = KnowledgeDefinitions.current().ideas().get(key.definitionId());
            if (idea != null) for (var project : idea.projects())
                disclosed.computeIfAbsent(project, ignored -> new ArrayList<>()).add(key);
        }
        ListTag projects = new ListTag();
        Set<net.minecraft.resources.ResourceLocation> completedProjects = new HashSet<>();
        service.state().research().values().stream().filter(ResearchHistory::completed)
                .forEach(run -> completedProjects.add(run.project()));
        disclosed.forEach((id, origins) -> {
            var definition = KnowledgeDefinitions.current().projects().get(id);
            if (definition == null) return;
            CompoundTag project = new CompoundTag();
            project.putString("id", id.toString());
            project.putString("title", definition.title());
            project.put("ideas", KnowledgeState.encode(KnowledgeKey.CODEC.listOf(), origins));
            project.putBoolean("active", origins.stream().anyMatch(service::isActive));
            project.putBoolean("completed", completedProjects.contains(id));
            ListTag outputs = new ListTag();
            for (var result : definition.results()) {
                KnowledgeElement element = service.describe(KnowledgeKey.result(result));
                CompoundTag output = new CompoundTag();
                output.put("key", KnowledgeState.encode(KnowledgeKey.CODEC, element.key()));
                output.putString("title", element.title());
                outputs.add(output);
            }
            project.put("outputs", outputs);
            projects.add(project);
        });
        view.put("projects", projects);
        ListTag history = new ListTag();
        service.state().acquisitions().forEach(entry -> {
            CompoundTag tag = new CompoundTag();
            tag.put("key", KnowledgeState.encode(KnowledgeKey.CODEC, entry.element().key()));
            tag.putString("source", entry.source().mechanism());
            tag.putLong("time", entry.source().time());
            history.add(tag);
        });
        view.put("acquisitions", history);
        return view;
    }

    private static boolean activeLink(KnowledgeService service, LinkDiscovery link) {
        var rule = KnowledgeDefinitions.current().links().get(link.rule());
        if (rule == null || !KnowledgeKey.idea(rule.output()).equals(link.output()) || !service.isActive(link.output()) || rule.inputs().size() != link.inputs().size())
            return false;
        Map<String, KnowledgeElement> binding = new HashMap<>();
        for (var slot : rule.inputs()) {
            var key = link.inputs().get(slot.name());
            if (key == null || !service.isActive(key)) return false;
            var element = service.entry(key).element();
            if (!slot.matches(element, com.teammoeg.frostedresearch.knowledge.link.TagLookup.SERVER)) return false;
            binding.put(slot.name(), element);
        }
        return rule.conditions().stream().allMatch(c -> c.matches(binding));
    }

    private static ListTag entries(KnowledgeService service, Collection<KnowledgeEntry> entries) {
        ListTag list = new ListTag();
        for (KnowledgeEntry entry : entries) {
            KnowledgeElement element = service.describe(entry.element().key());
            KnowledgeKey key = element.key();
            CompoundTag tag = new CompoundTag();
            tag.put("key", KnowledgeState.encode(KnowledgeKey.CODEC, key));
            tag.put("element", KnowledgeState.encode(KnowledgeElement.CODEC, element));
            String title = element.title().isBlank() ? element.observation().map(o -> o.object().toString()).orElse(key.id()) : element.title();
            tag.putString("title", title);
            tag.putString("body", element.body());
            tag.putString("kind", key.kind().name().toLowerCase(Locale.ROOT));
            var query = service.query(key);
            tag.putString("status", query.status().name());
            tag.putBoolean("active", query.active());
            tag.putString("subtype", element.observation().map(o -> o.type().name().toLowerCase(Locale.ROOT)).orElse(entry.resultType()));
            tag.putString("source", entry.source().mechanism());
            tag.putLong("received_at", entry.source().time());
            tag.putLong("learned_at", entry.learnedAt());
            String group = element.observation().map(o -> o.type() + ":" + o.object() + ":" + o.value("biome")).orElse(key.id());
            tag.putString("group", group);
            list.add(tag);
        }
        return list;
    }
}
