package com.teammoeg.frostedresearch.knowledge;

import com.teammoeg.chorda.dataholders.team.TeamDataHolder;
import com.teammoeg.frostedresearch.FRSpecialDataTypes;
import com.teammoeg.frostedresearch.FRMain;
import com.teammoeg.frostedresearch.api.TeamResearchService;
import com.teammoeg.frostedresearch.data.TeamResearchData;
import com.teammoeg.frostedresearch.data.ClueData;
import com.teammoeg.frostedresearch.knowledge.definition.*;
import com.teammoeg.frostedresearch.knowledge.model.*;
import com.teammoeg.frostedresearch.knowledge.state.AcquisitionSource;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.server.ServerLifecycleHooks;
import blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler;
import java.util.*;

/** Import recorded facts without rewriting legacy progress or manufacturing experiment runs. */
public final class LegacyResearchBridge {
    public static final String RECEIPTS = "frostedresearch:legacy_replacements";
    private static Map<String, ResourceLocation> active = Map.of();
    private static long revision = -1;
    private LegacyResearchBridge() {}
    public static void clear() { active = Map.of(); revision = -1; }
    /** Refresh at final recipes/tags and load/login; incomplete content restores the old route. */
    public static void refreshDefinitions() {
        var defs = KnowledgeDefinitions.current();
        Map<String, ResourceLocation> next = new LinkedHashMap<>();
        Set<String> duplicates = new HashSet<>();
        defs.projects().forEach((id, p) -> p.legacyReplacements().forEach(m -> {
            String problem = problem(p, m);
            if (problem != null) {
                if (revision != defs.revision()) FRMain.LOGGER.warn("Legacy replacement {} disabled: {}", id, problem);
            } else if (next.putIfAbsent(m.research(), id) != null) duplicates.add(m.research());
        }));
        duplicates.forEach(key -> { next.remove(key); if (revision != defs.revision()) FRMain.LOGGER.warn("Duplicate legacy replacement: {}", key); });
        active = Map.copyOf(next); revision = defs.revision();
    }
    private static String problem(ProjectDefinition p, LegacyReplacement m) {
        if (m.research().isBlank() || m.effects().keySet().stream().anyMatch(String::isBlank) || m.effects().isEmpty() || !p.results().containsAll(m.effects().values())) return "requires nonempty mapped project results";
        var idea = KnowledgeDefinitions.current().ideas().get(m.partialIdea());
        if (idea == null || !idea.enabled()) return "partial idea unavailable: " + m.partialIdea();
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return "recipes not ready";
        for (var id : m.effects().values()) {
            var d = KnowledgeDefinitions.current().results().get(id);
            if (d == null || !d.enabled() || !d.understanding().isEmpty()) return "mapped result unavailable or requires understanding: " + id;
            if (d.result() instanceof ResearchResult.Design r) {
                if (r.recipes().isEmpty() || r.recipes().stream().anyMatch(t -> server.getRecipeManager().byKey(t).isEmpty())) return "recipe target unavailable: " + id;
            } else if (d.result() instanceof ResearchResult.Construction r) {
                if (r.multiblocks().isEmpty() || r.multiblocks().stream().anyMatch(t -> MultiblockHandler.getByUniqueName(t) == null)) return "multiblock target unavailable: " + id;
            } else if (d.result() instanceof ResearchResult.Procedure r) {
                if (r.toolActions().isEmpty() || r.toolActions().stream().anyMatch(String::isBlank))
                    return "requires a Procedure with a tool action: " + id;
            } else return "requires Design, Construction or a tool Procedure: " + id;
        }
        return null;
    }
    public static Map<String, ResourceLocation> replacements() {
        if (revision != KnowledgeDefinitions.current().revision()) refreshDefinitions();
        return active;
    }
    public static boolean isReplaced(String id) { return replacements().containsKey(id); }
    public static boolean completedCredit(TeamResearchData data, ResourceLocation project) {
        var definition = KnowledgeDefinitions.current().projects().get(project);
        if (definition == null || definition.legacyReplacements().isEmpty()) return false;
        var receipt = data.getVariants().getCompound(RECEIPTS).getCompound(project.toString());
        boolean single = definition.legacyReplacements().size() == 1;
        return definition.legacyReplacements().stream().allMatch(mapping ->
                project.equals(replacements().get(mapping.research())) && hasCredit(receipt, mapping.research(), single));
    }

    private static boolean hasCredit(CompoundTag receipt, String old, boolean single) {
        return receipt.getCompound("completed_researches").getBoolean(old)
                || (single && receipt.getBoolean("completed_credit"));
    }

    private static LegacyReplacement mapping(ProjectDefinition definition, String old) {
        return definition.legacyReplacements().stream().filter(m -> m.research().equals(old)).findFirst().orElseThrow();
    }

    public static void reconcile(TeamDataHolder team, KnowledgeService service) {
        refreshDefinitions();
        var legacy = team.getData(FRSpecialDataTypes.RESEARCH_DATA);
        legacy.clearReplacedSelection(team);
        var root = legacy.getVariants().getCompound(RECEIPTS).copy();
        boolean changed = false;
        for (var e : replacements().entrySet()) {
            var raw = legacy.getExistingData(e.getKey()).orElse(null);
            if (raw == null) continue;
            var definition = KnowledgeDefinitions.current().projects().get(e.getValue());
            var m = mapping(definition, e.getKey());
            boolean single = definition.legacyReplacements().size() == 1;
            var tag = root.getCompound(e.getValue().toString()).copy(); var before = tag.copy();
            Set<String> imported = new TreeSet<>();
            tag.getList("imported_results", Tag.TAG_STRING).forEach(t -> imported.add(t.getAsString()));
            var source = new AcquisitionSource("legacy_research:" + e.getKey(), Optional.empty(), service.time());
            m.effects().forEach((nonce, id) -> {
                if (!imported.contains(id.toString()) && (raw.isCompleted() || raw.isEffectGranted(nonce))) {
                    var result = service.learn(KnowledgeElement.result(id), source, KnowledgeService.Grant.NORMAL);
                    if (result.succeeded() || service.query(KnowledgeKey.result(id)).inArchive()) imported.add(id.toString());
                }
            });
            ListTag list = new ListTag(); imported.forEach(id -> list.add(StringTag.valueOf(id))); tag.put("imported_results", list);
            if (raw.isCompleted()) {
                if (single) {
                    tag.putBoolean("completed_credit", true);
                } else {
                    var completed = tag.getCompound("completed_researches");
                    completed.putBoolean(e.getKey(), true);
                    tag.put("completed_researches", completed);
                }
            }
            boolean invested = raw.isActive() || raw.getCommitted() > 0 || raw.getClueData().values().stream().anyMatch(ClueData::isCompleted);
            if (!tag.getBoolean("partial_idea_granted") && (raw.isCompleted() ? !single : invested)) {
                service.produce(KnowledgeElement.idea(m.partialIdea()), source);
                var query = service.query(KnowledgeKey.idea(m.partialIdea()));
                if (query.inArchive() || query.inInbox()) tag.putBoolean("partial_idea_granted", true);
            }
            if (!tag.equals(before)) { root.put(e.getValue().toString(), tag); changed = true; }
        }
        if (changed) { legacy.getVariants().put(RECEIPTS, root); TeamResearchService.sync(team); }
        if (reconcileExpandedProjects(service)) TeamResearchService.sync(team);
    }

    private static boolean reconcileExpandedProjects(KnowledgeService service) {
        var completed = ResearchRuns.sorted(service.state()).stream().filter(run -> run.completed())
                .collect(java.util.stream.Collectors.groupingBy(run -> run.project()));
        boolean changed = false;
        for (var entry : completed.entrySet()) {
            var project = entry.getKey();
            var runs = entry.getValue();
            var definition = KnowledgeDefinitions.current().projects().get(project);
            if (definition == null || definition.legacyReplacements().size() < 2) continue;
            var missing = new LinkedHashSet<KnowledgeKey>();
            for (var mapping : definition.legacyReplacements()) {
                if (project.equals(replacements().get(mapping.research()))) {
                    mapping.effects().values().stream().map(KnowledgeKey::result).forEach(missing::add);
                }
            }
            runs.forEach(run -> missing.removeAll(run.results()));
            if (missing.isEmpty()) continue;
            var run = runs.get(0);
            var results = new ArrayList<>(run.results());
            results.addAll(missing);
            service.state().recordResearch(run.completedWith(results));
            var source = new AcquisitionSource("research:" + run.id(), Optional.empty(), service.time());
            for (var key : missing) {
                service.produce(KnowledgeElement.result(new ResourceLocation(key.id())), source);
            }
            changed = true;
        }
        return changed;
    }
    public static boolean isPrerequisiteCompleted(TeamDataHolder team, String id) {
        var data = team.getData(FRSpecialDataTypes.RESEARCH_DATA);
        if (data.getExistingData(id).map(d -> d.isCompleted()).orElse(false)) return true;
        var project = replacements().get(id);
        if (project == null) return false;
        var service = KnowledgeService.forTeam(team, 0);
        var m = mapping(KnowledgeDefinitions.current().projects().get(project), id);
        return service.state().research().values().stream().anyMatch(run -> run.completed() && run.project().equals(project)) && m.effects().values().stream().allMatch(r -> service.isActive(KnowledgeKey.result(r)));
    }
    public static ListTag projection(TeamDataHolder team) {
        ListTag list = new ListTag();
        replacements().forEach((old, project) -> {
            CompoundTag tag = new CompoundTag(); tag.putString("research", old); tag.putString("project", project.toString());
            tag.putBoolean("effective_completed", isPrerequisiteCompleted(team, old));
            var receipt = team.getData(FRSpecialDataTypes.RESEARCH_DATA).getVariants().getCompound(RECEIPTS).getCompound(project.toString());
            tag.putBoolean("completed_credit", hasCredit(receipt, old, KnowledgeDefinitions.current().projects().get(project).legacyReplacements().size() == 1));
            tag.putBoolean("partial_idea_granted", receipt.getBoolean("partial_idea_granted"));
            list.add(tag);
        }); return list;
    }
    /** Union this subkey alone, keeping unrelated variables and raw legacy progress. */
    public static void mergeReceipts(TeamResearchData target, TeamResearchData source) {
        CompoundTag root = target.getVariants().getCompound(RECEIPTS).copy(), incoming = source.getVariants().getCompound(RECEIPTS);
        for (String project : incoming.getAllKeys()) {
            CompoundTag a = root.getCompound(project).copy(), b = incoming.getCompound(project);
            Set<String> ids = new TreeSet<>();
            for (CompoundTag tag : List.of(a, b)) tag.getList("imported_results", Tag.TAG_STRING).forEach(t -> ids.add(t.getAsString()));
            ListTag list = new ListTag(); ids.forEach(id -> list.add(StringTag.valueOf(id))); a.put("imported_results", list);
            for (String flag : List.of("completed_credit", "partial_idea_granted")) a.putBoolean(flag, a.getBoolean(flag) || b.getBoolean(flag));
            var completed = a.getCompound("completed_researches");
            completed.merge(b.getCompound("completed_researches"));
            if (!completed.isEmpty()) a.put("completed_researches", completed);
            root.put(project, a);
        }
        target.getVariants().put(RECEIPTS, root);
    }
}
