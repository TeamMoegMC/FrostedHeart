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
        defs.projects().forEach((id, p) -> p.legacyReplacement().ifPresent(m -> {
            String problem = problem(p, m);
            if (problem != null) {
                if (revision != defs.revision()) FRMain.LOGGER.warn("Legacy replacement {} disabled: {}", id, problem);
            } else if (next.putIfAbsent(m.research(), id) != null) duplicates.add(m.research());
        }));
        duplicates.forEach(key -> { next.remove(key); if (revision != defs.revision()) FRMain.LOGGER.warn("Duplicate legacy replacement: {}", key); });
        active = Map.copyOf(next); revision = defs.revision();
    }
    private static String problem(ProjectDefinition p, LegacyReplacement m) {
        if (m.research().isBlank() || m.effects().keySet().stream().anyMatch(String::isBlank) || m.effects().size() != 2 || new HashSet<>(m.effects().values()).size() != 2 || !p.results().containsAll(m.effects().values())) return "requires two distinct mapped project results";
        var idea = KnowledgeDefinitions.current().ideas().get(m.partialIdea());
        if (idea == null || !idea.enabled()) return "partial idea unavailable: " + m.partialIdea();
        boolean design = false, construction = false;
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return "recipes not ready";
        for (var id : m.effects().values()) {
            var d = KnowledgeDefinitions.current().results().get(id);
            if (d == null || !d.enabled() || !d.understanding().isEmpty()) return "mapped result unavailable or requires understanding: " + id;
            if (d.result() instanceof ResearchResult.Design r) {
                design = !r.recipes().isEmpty();
                if (!design || r.recipes().stream().anyMatch(t -> server.getRecipeManager().byKey(t).isEmpty())) return "recipe target unavailable: " + id;
            } else if (d.result() instanceof ResearchResult.Construction r) {
                construction = !r.multiblocks().isEmpty();
                if (!construction || r.multiblocks().stream().anyMatch(t -> MultiblockHandler.getByUniqueName(t) == null)) return "multiblock target unavailable: " + id;
            } else return "requires Design and Construction: " + id;
        }
        return design && construction ? null : "requires Design and Construction";
    }
    public static Map<String, ResourceLocation> replacements() {
        if (revision != KnowledgeDefinitions.current().revision()) refreshDefinitions();
        return active;
    }
    public static boolean isReplaced(String id) { return replacements().containsKey(id); }
    public static boolean completedCredit(TeamResearchData data, ResourceLocation project) {
        return replacements().containsValue(project) && data.getVariants().getCompound(RECEIPTS).getCompound(project.toString()).getBoolean("completed_credit");
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
            var m = KnowledgeDefinitions.current().projects().get(e.getValue()).legacyReplacement().orElseThrow();
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
            if (raw.isCompleted()) tag.putBoolean("completed_credit", true);
            if (!raw.isCompleted() && !tag.getBoolean("partial_idea_granted") && (raw.isActive() || raw.getCommitted() > 0 || raw.getClueData().values().stream().anyMatch(ClueData::isCompleted))) {
                service.produce(KnowledgeElement.idea(m.partialIdea()), source);
                var query = service.query(KnowledgeKey.idea(m.partialIdea()));
                if (query.inArchive() || query.inInbox()) tag.putBoolean("partial_idea_granted", true);
            }
            if (!tag.equals(before)) { root.put(e.getValue().toString(), tag); changed = true; }
        }
        if (changed) { legacy.getVariants().put(RECEIPTS, root); TeamResearchService.sync(team); }
    }
    public static boolean isPrerequisiteCompleted(TeamDataHolder team, String id) {
        var data = team.getData(FRSpecialDataTypes.RESEARCH_DATA);
        if (data.getExistingData(id).map(d -> d.isCompleted()).orElse(false)) return true;
        var project = replacements().get(id);
        if (project == null) return false;
        var service = KnowledgeService.forTeam(team, 0);
        var m = KnowledgeDefinitions.current().projects().get(project).legacyReplacement().orElseThrow();
        return service.state().research().values().stream().anyMatch(run -> run.completed() && run.project().equals(project)) && m.effects().values().stream().allMatch(r -> service.isActive(KnowledgeKey.result(r)));
    }
    public static ListTag projection(TeamDataHolder team) {
        ListTag list = new ListTag();
        replacements().forEach((old, project) -> {
            CompoundTag tag = new CompoundTag(); tag.putString("research", old); tag.putString("project", project.toString());
            tag.putBoolean("effective_completed", isPrerequisiteCompleted(team, old));
            var receipt = team.getData(FRSpecialDataTypes.RESEARCH_DATA).getVariants().getCompound(RECEIPTS).getCompound(project.toString());
            tag.putBoolean("completed_credit", receipt.getBoolean("completed_credit"));
            tag.putBoolean("partial_idea_granted", receipt.getBoolean("partial_idea_granted")); list.add(tag);
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
            root.put(project, a);
        }
        target.getVariants().put(RECEIPTS, root);
    }
}
