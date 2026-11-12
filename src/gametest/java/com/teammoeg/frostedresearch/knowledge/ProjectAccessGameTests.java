package com.teammoeg.frostedresearch.knowledge;

import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.teammoeg.frostedresearch.knowledge.definition.*;
import com.teammoeg.frostedresearch.knowledge.link.*;
import com.teammoeg.frostedresearch.knowledge.model.*;
import com.teammoeg.frostedresearch.knowledge.state.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.gametest.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@GameTestHolder("frostedresearch_m1")
@PrefixGameTestTemplate(false)
public final class ProjectAccessGameTests {
    private static ResourceLocation id(String name) { return new ResourceLocation("example", name); }
    private static final AcquisitionSource SOURCE = new AcquisitionSource("external", Optional.empty(), 42);
    private static <T> T fixture(String path, Codec<T> codec) {
        try (var reader = new InputStreamReader(Objects.requireNonNull(ProjectAccessGameTests.class.getResourceAsStream("/m1-fixtures/" + path + ".json")), StandardCharsets.UTF_8)) {
            return codec.parse(JsonOps.INSTANCE, JsonParser.parseReader(reader)).getOrThrow(false, s -> {});
        } catch (IOException e) { throw new IllegalStateException(e); }
    }
    private static KnowledgeDefinitions.Snapshot install(ProjectDefinition joint) {
        Map<ResourceLocation, IdeaDefinition> ideas = new HashMap<>();
        for (String name : List.of("i", "j")) ideas.put(id(name), fixture("ideas/" + name, IdeaDefinition.CODEC));
        Map<ResourceLocation, ResultDefinition> results = new HashMap<>();
        for (String name : List.of("joint_finding", "smelting_finding", "followup_finding")) results.put(id(name), fixture("results/" + name, ResultDefinition.CODEC));
        var snapshot = new KnowledgeDefinitions.Snapshot(0, ideas, results, Map.of(), Map.of(),
            Map.of(id("joint_study"), joint, id("followup"), fixture("projects/followup", ProjectDefinition.CODEC)), List.of());
        if (!KnowledgeDefinitions.validate(snapshot).isEmpty()) throw new IllegalStateException(KnowledgeDefinitions.validate(snapshot).toString());
        return KnowledgeDefinitions.install(snapshot);
    }
    private static KnowledgeService service() { return new KnowledgeService(UUID.randomUUID(), new KnowledgeState(), 42, () -> {}); }
    private static void learn(KnowledgeService service, KnowledgeElement element) {
        if (!service.learn(element, SOURCE, KnowledgeService.Grant.NORMAL).succeeded()) throw new IllegalStateException("Could not learn " + element.key());
    }
    private static Observation item(String object) { return new Observation(UUID.randomUUID(), Observation.Type.ITEM,
        new ResourceLocation("minecraft", object), Map.of(), Observation.Source.command(), Optional.empty()); }
    private static CompoundTag project(KnowledgeService s, String name) {
        return s.snapshot().getList("projects", Tag.TAG_COMPOUND).stream().map(t -> (CompoundTag)t)
            .filter(t -> t.getString("id").equals(id(name).toString())).findFirst().orElseThrow();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void typedObservationsHiddenDifferencesAndFutureUses(GameTestHelper h) {
        var previous = KnowledgeDefinitions.current(); var useId = id("future_branch");
        try {
            var joint = fixture("projects/joint_study", ProjectDefinition.CODEC);
            var threshold = new FieldCondition("temperature", FieldCondition.Operator.GTE, Optional.of(ObservationValue.known(10)), Optional.empty());
            var condition = new KnowledgeCondition(KnowledgeKey.Kind.OBSERVATION, Optional.empty(), Optional.of(Observation.Type.BLOCK), List.of(threshold), "Warm sample", "");
            var tag = new KnowledgeCondition(KnowledgeKey.Kind.OBSERVATION, Optional.empty(), Optional.of(Observation.Type.ITEM),
                List.of(new FieldCondition("object", FieldCondition.Operator.TAG, Optional.empty(), Optional.of(new ResourceLocation("minecraft:planks")))), "Planks", "");
            var access = new ProjectAccess(Map.of("warm", condition, "planks", tag), List.of("warm"), List.of("warm"), List.of());
            install(new ProjectDefinition(joint.title(), joint.body(), joint.results(), joint.requirements(), Optional.of(access)));
            var cold = new Observation(UUID.randomUUID(), Observation.Type.BLOCK, new ResourceLocation("minecraft:stone"), Map.of("temperature", ObservationValue.known(9)), Observation.Source.command(), Optional.empty());
            var warm = new Observation(UUID.randomUUID(), Observation.Type.BLOCK, cold.object(), Map.of("temperature", ObservationValue.known(10)), cold.source(), Optional.empty());
            var warmer = new Observation(UUID.randomUUID(), warm.type(), warm.object(), Map.of("temperature", ObservationValue.known(11)), warm.source(), Optional.empty());
            h.assertTrue(!condition.matches(KnowledgeElement.observation(cold)), "threshold uses typed values");
            h.assertTrue(!condition.matches(KnowledgeElement.observation(new Observation(UUID.randomUUID(), warm.type(), warm.object(), Map.of(), warm.source(), Optional.empty()))), "unknown does not satisfy comparison");
            h.assertTrue(tag.matches(KnowledgeElement.observation(item("oak_planks"))), "item registry tags match");
            h.assertTrue(!tag.matches(KnowledgeElement.observation(new Observation(UUID.randomUUID(), Observation.Type.BLOCK, new ResourceLocation("minecraft:oak_planks"), Map.of(), warm.source(), Optional.empty()))), "placed block is not an item observation");
            var entityCondition = new KnowledgeCondition(KnowledgeKey.Kind.OBSERVATION, Optional.empty(), Optional.of(Observation.Type.ENTITY), List.of(), "Entity", "");
            h.assertTrue(entityCondition.matches(KnowledgeElement.observation(new Observation(UUID.randomUUID(), Observation.Type.ENTITY, new ResourceLocation("minecraft:pig"), Map.of(), warm.source(), Optional.empty()))), "entity type supported");
            var s = service(); s.receive(KnowledgeElement.observation(warm), SOURCE);
            h.assertTrue(!s.projectAccess(id("joint_study")).discovered(), "observation inbox is not discovery");
            s.learnInbox(warm.key()); h.assertTrue(s.startResearch(id("joint_study")).isPresent(), "observation-only discovery and start");
            h.assertTrue(!KnowledgeDefinitions.current().addsObservationDifference(warmer, List.of(warm)), "10/11 same truth vector need no duplicate");
            KnowledgeDefinitions.registerObservationUse(useId, new ObservationUse() {
                public boolean matches(Observation o) { return o.type() == Observation.Type.BLOCK; }
                public Optional<Boolean> equivalent(Observation a, Observation b) { return Optional.of(a.value("temperature").equals(b.value("temperature"))); }
            });
            h.assertTrue(KnowledgeDefinitions.current().addsObservationDifference(warmer, List.of(warm)), "registered future branch retains useful difference before discovery");
            h.assertTrue(!KnowledgeDefinitions.current().addsObservationDifference(warm, List.of(warm)), "copied UUID never adds independent record");
            var left = new LinkRule.Slot("left", KnowledgeKey.Kind.OBSERVATION, Optional.empty(), Optional.of(Observation.Type.BLOCK), List.of());
            var right = new LinkRule.Slot("right", left.kind(), left.definition(), left.observationType(), left.conditions());
            var evidence = ProjectRequirement.evidence("locations", "Different locations", List.of(left, right), List.of(), List.of(new ProjectRequirement.Distinct("x", List.of("left", "right"))));
            var evidenceSnapshot = new KnowledgeDefinitions.Snapshot(0, Map.of(), Map.of(), Map.of(), Map.of(),
                Map.of(id("evidence"), new ProjectDefinition("Evidence", "", joint.results(), List.of(evidence))), List.of());
            var x1 = new Observation(UUID.randomUUID(), warm.type(), warm.object(), Map.of("x", ObservationValue.known(1)), warm.source(), Optional.empty());
            var x1copy = new Observation(UUID.randomUUID(), warm.type(), warm.object(), x1.values(), warm.source(), Optional.empty());
            var x2 = new Observation(UUID.randomUUID(), warm.type(), warm.object(), Map.of("x", ObservationValue.known(2)), warm.source(), Optional.empty());
            h.assertTrue(evidenceSnapshot.addsObservationDifference(x1copy, List.of(x1)), "existing two-slot evidence still admits a second independent record");
            h.assertTrue(LinkMatcher.bindExact(List.of(KnowledgeElement.observation(x1), KnowledgeElement.observation(x1)), evidence.pattern(), TagLookup.SERVER).isEmpty(), "copied UUID cannot fill two slots");
            h.assertTrue(LinkMatcher.bindExact(List.of(KnowledgeElement.observation(x1), KnowledgeElement.observation(x1copy)), evidence.pattern(), TagLookup.SERVER).isEmpty(), "distinct location still required");
            h.assertTrue(LinkMatcher.bindExact(List.of(KnowledgeElement.observation(x1), KnowledgeElement.observation(x2)), evidence.pattern(), TagLookup.SERVER).isPresent(), "independent locations satisfy existing evidence");
            h.succeed();
        } finally { KnowledgeDefinitions.unregisterObservationUse(useId); KnowledgeDefinitions.install(previous); }
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void authorErrorsStableRowsAndHistoricalVisibility(GameTestHelper h) {
        var previous = KnowledgeDefinitions.current();
        try {
            for (String field : List.of("conditions", "start_all", "discover_any", "groups")) {
                var json = JsonParser.parseString("{\"access\":{\"" + field + "\":true},\"results\":[\"example:joint_finding\"]}");
                h.assertTrue(ProjectDefinition.CODEC.parse(JsonOps.INSTANCE, json).error().isPresent(), "malformed " + field + " must not become empty defaults");
            }
            h.assertTrue(ProjectDefinition.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"results\":[\"example:joint_finding\"],\"access\":true}")).error().isPresent(), "bad explicit access never becomes legacy");
            var joint = fixture("projects/joint_study", ProjectDefinition.CODEC); var access = joint.access().orElseThrow();
            var groups = List.of(new ProjectAccess.Group("default", List.of("i"), List.of("i", "a"), ""),
                new ProjectAccess.Group("other", List.of("j"), List.of("j", "b", "c"), ""));
            install(new ProjectDefinition(joint.title(), joint.body(), joint.results(), joint.requirements(), Optional.of(
                new ProjectAccess(access.conditions(), List.of("i", "j", "b", "c"), access.discoverAny(), groups))));
            var s = service(); learn(s, KnowledgeElement.idea(id("j")));
            var before = project(s, "joint_study").getList("access_conditions", Tag.TAG_COMPOUND).stream().map(t -> (CompoundTag)t)
                .filter(t -> t.getString("title").equals("检视沙")).findFirst().orElseThrow().getString("id");
            learn(s, KnowledgeElement.idea(id("i")));
            var after = project(s, "joint_study").getList("access_conditions", Tag.TAG_COMPOUND).stream().map(t -> (CompoundTag)t)
                .filter(t -> t.getString("title").equals("检视沙")).findFirst().orElseThrow().getString("id");
            h.assertTrue(before.equals(after), "revealed optional A cannot change B row ID");
            learn(s, KnowledgeElement.observation(item("sand"))); learn(s, KnowledgeElement.observation(item("charcoal")));
            UUID run = s.startResearch(id("joint_study")).orElseThrow();
            s.abandonResearch(run); s.forget(KnowledgeKey.idea(id("i"))); s.forget(KnowledgeKey.idea(id("j")));
            h.assertTrue(!project(s, "joint_study").getBoolean("active"), "abandoned undiscovered project is historical only");
            var repaired = new ProjectAccess(access.conditions(), access.startAll(), List.of(), List.of(new ProjectAccess.Group("default", List.of("i"), List.of("i"), "")));
            h.assertTrue(repaired.validate().isEmpty() && repaired.groups().size() == 2, "fallback group avoids existing default ID");
            h.succeed();
        } finally { KnowledgeDefinitions.install(previous); }
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void disclosureAndStartUseLearnedKnowledge(GameTestHelper h) {
        var previous = KnowledgeDefinitions.current();
        try {
            var joint = fixture("projects/joint_study", ProjectDefinition.CODEC); install(joint);
            var s = service();
            s.receive(KnowledgeElement.idea(id("i")), SOURCE);
            h.assertTrue(!s.projectAccess(id("joint_study")).discovered(), "inbox idea cannot discover");
            s.learnInbox(KnowledgeKey.idea(id("i")));
            learn(s, KnowledgeElement.observation(item("charcoal")));
            var rows = project(s, "joint_study").getList("access_conditions", Tag.TAG_COMPOUND);
            h.assertTrue(rows.size() == 5, "five distinct required rows including shared B once");
            h.assertTrue(rows.stream().filter(t -> ((CompoundTag)t).getBoolean("hidden")).count() == 2, "J and C each have a placeholder even when C owned");
            for (Tag raw : rows) if (((CompoundTag)raw).getBoolean("hidden"))
                h.assertTrue(((CompoundTag)raw).getAllKeys().equals(Set.of("id", "required", "hidden")), "hidden row has no identity or match data");
            h.assertTrue(s.startResearch(id("joint_study"), KnowledgeKey.idea(id("i"))).isEmpty(), "old selected-idea API cannot bypass explicit access");
            var a = item("clay_ball"); var b = item("sand");
            s.receive(KnowledgeElement.observation(a), SOURCE); s.receive(KnowledgeElement.observation(b), SOURCE);
            learn(s, KnowledgeElement.idea(id("j")));
            h.assertTrue(s.startResearch(id("joint_study")).isEmpty(), "received observations are not mastered");
            s.learnInbox(a.key()); s.learnInbox(b.key());
            h.assertTrue(project(s, "joint_study").getList("access_conditions", Tag.TAG_COMPOUND).stream().noneMatch(t -> ((CompoundTag)t).getBoolean("hidden")), "J reveals remaining rows");
            UUID run = s.startResearch(id("joint_study")).orElseThrow();
            h.assertTrue(s.state().research().get(run).origins().size() == 5, "all witnesses retained");
            h.assertTrue(s.startResearch(id("joint_study")).isEmpty(), "second member cannot duplicate open run");
            s.forget(KnowledgeKey.idea(id("i"))); s.forget(KnowledgeKey.idea(id("j")));
            h.assertTrue(s.openRun(id("joint_study")).isPresent(), "forgetting sources retains run");
            h.assertTrue(s.snapshot().getList("research", Tag.TAG_COMPOUND).getCompound(0).getList("origins", Tag.TAG_COMPOUND).isEmpty(), "history also clips hidden relations");
            h.succeed();
        } finally { KnowledgeDefinitions.install(previous); }
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void authorExceptionResultEntryAndLegacyCompatibility(GameTestHelper h) {
        var previous = KnowledgeDefinitions.current();
        try {
            var joint = fixture("projects/joint_study", ProjectDefinition.CODEC);
            var access = joint.access().orElseThrow();
            install(new ProjectDefinition(joint.title(), joint.body(), joint.results(), joint.requirements(), Optional.of(
                new ProjectAccess(access.conditions(), List.of("i", "j", "b", "c"), access.discoverAny(), access.groups()))));
            var s = service(); learn(s, KnowledgeElement.idea(id("i"))); learn(s, KnowledgeElement.idea(id("j")));
            learn(s, KnowledgeElement.observation(item("sand"))); learn(s, KnowledgeElement.observation(item("charcoal")));
            h.assertTrue(s.startResearch(id("joint_study")).isPresent(), "external ideas permit omitted A prerequisite");
            h.assertTrue(project(s, "joint_study").getList("access_conditions", Tag.TAG_COMPOUND).stream().anyMatch(t -> !((CompoundTag)t).getBoolean("required")), "A is reference only");
            s.receive(KnowledgeElement.result(id("smelting_finding")), SOURCE);
            h.assertTrue(!s.projectAccess(id("followup")).discovered(), "inbox result cannot discover");
            s.learnInbox(KnowledgeKey.result(id("smelting_finding")));
            UUID run = s.startResearch(id("followup")).orElseThrow();
            h.assertTrue(s.state().research().get(run).origins().equals(List.of(KnowledgeKey.result(id("smelting_finding")))), "pure result origin");
            s.forget(KnowledgeKey.result(id("smelting_finding")));
            h.assertTrue(!s.completeResearch(run).isEmpty(), "developer completion retains open run after source forgetting");
            h.assertTrue(s.completeResearch(run).isEmpty(), "repeated completion produces nothing");
            var snapshot = KnowledgeDefinitions.current();
            Map<ResourceLocation, IdeaDefinition> ideas = new HashMap<>();
            snapshot.ideas().forEach((id, i) -> ideas.put(id, new IdeaDefinition(i.title(), i.body(), i.understanding(), List.of(id("joint_study")), true)));
            KnowledgeDefinitions.install(new KnowledgeDefinitions.Snapshot(0, ideas, snapshot.results(), Map.of(), Map.of(),
                Map.of(id("joint_study"), new ProjectDefinition("legacy", joint.results())), List.of()));
            var legacy = service(); learn(legacy, KnowledgeElement.idea(id("j")));
            h.assertTrue(legacy.startResearch(id("joint_study")).isPresent(), "either legacy idea can start alone");
            h.succeed();
        } finally { KnowledgeDefinitions.install(previous); }
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void observationUsesAndVersionThreeRoundTrip(GameTestHelper h) {
        var previous = KnowledgeDefinitions.current();
        try {
            install(fixture("projects/joint_study", ProjectDefinition.CODEC));
            var snapshot = KnowledgeDefinitions.current(); var a = item("clay_ball");
            h.assertTrue(snapshot.hasObservationUse(a), "hidden access alone makes an observation useful");
            h.assertTrue(!snapshot.addsObservationDifference(item("clay_ball"), List.of(a)), "independent existence conditions do not demand duplicates");
            var s = service(); learn(s, KnowledgeElement.observation(a));
            UUID id = UUID.randomUUID();
            ResearchHistory run = new ResearchHistory(id, id("joint_study"), KnowledgeKey.idea(id("i")), 77, false, List.of())
                .withCalculation("tabulate", 2).withEvidence("sample", Map.of("a", a)).withExperiment("trial");
            s.state().recordResearch(run);
            CompoundTag saved = s.state().write(); saved.putInt("schema_version", 2);
            CompoundTag old = saved.getList("research", Tag.TAG_COMPOUND).getCompound(0);
            old.remove("origins"); old.put("idea", KnowledgeState.encode(KnowledgeKey.CODEC, KnowledgeKey.idea(id("i"))));
            var restored = KnowledgeState.read(saved);
            h.assertTrue(restored.research().get(id).equals(run), "v2 migration preserves UUID/time/all progress");
            h.assertTrue(KnowledgeState.read(restored.write()).research().get(id).equals(run), "v3 second round trip never duplicates origins");
            h.assertTrue(saved.getList("research", Tag.TAG_COMPOUND).getCompound(0).contains("idea"), "migration leaves caller input intact");
            var restoredService = new KnowledgeService(UUID.randomUUID(), restored, 100, () -> {});
            h.assertTrue(restoredService.snapshot().getList("research", Tag.TAG_COMPOUND).getCompound(0).getList("origins", Tag.TAG_COMPOUND).isEmpty(), "legacy history obeys current explicit access disclosure");
            old.put("origins", KnowledgeState.encode(KnowledgeKey.CODEC.listOf(), List.of(KnowledgeKey.result(id("smelting_finding")))));
            h.assertTrue(KnowledgeState.read(saved).research().get(id).origins().equals(List.of(KnowledgeKey.result(id("smelting_finding")))), "new origins take precedence over old idea");
            old.remove("origins"); saved.remove("schema_version");
            h.assertTrue(KnowledgeState.read(saved).research().get(id).equals(run), "unversioned legacy conversion");
            old.remove("idea");
            h.assertTrue(KnowledgeState.read(saved).research().get(id).calculations().equals(run.calculations()), "missing origin retains progress");
            h.succeed();
        } finally { KnowledgeDefinitions.install(previous); }
    }
}
