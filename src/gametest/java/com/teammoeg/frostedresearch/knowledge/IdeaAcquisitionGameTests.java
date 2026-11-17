package com.teammoeg.frostedresearch.knowledge;

import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.*;
import com.teammoeg.frostedresearch.knowledge.definition.*;
import com.teammoeg.frostedresearch.knowledge.link.*;
import com.teammoeg.frostedresearch.knowledge.model.*;
import com.teammoeg.frostedresearch.knowledge.state.*;
import com.teammoeg.chorda.dataholders.team.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.gametest.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@GameTestHolder("frostedresearch_m2")
@PrefixGameTestTemplate(false)
public final class IdeaAcquisitionGameTests {
    private static ResourceLocation id(String name) { return new ResourceLocation("m2", name); }
    private static KnowledgeCondition idea(String name) { return new KnowledgeCondition(KnowledgeKey.Kind.IDEA, Optional.of(id(name)), Optional.empty(), List.of(), "", ""); }
    private static KnowledgeCondition item(String name) { return new KnowledgeCondition(KnowledgeKey.Kind.OBSERVATION, Optional.empty(), Optional.of(Observation.Type.ITEM),
        List.of(new FieldCondition("object", FieldCondition.Operator.EQ, Optional.of(ObservationValue.known("minecraft:" + name)), Optional.empty())), name, ""); }
    private static final AcquisitionSource SOURCE = new AcquisitionSource("external", Optional.empty(), 42);
    private static IdeaDefinition definition(IdeaAcquisition acquisition) { return new IdeaDefinition("Idea", "", List.of(), List.of(), true, acquisition); }
    private static IdeaAcquisition acquisition(List<IdeaAcquisition.Trigger> triggers, List<KnowledgeCondition> required, List<KnowledgeCondition> optional) {
        return new IdeaAcquisition(triggers, required, optional, Optional.of(1.0));
    }
    private static IdeaAcquisition.Trigger trigger(ResourceLocation event, KnowledgeCondition condition) { return new IdeaAcquisition.Trigger(event, Optional.ofNullable(condition)); }
    private static KnowledgeService service() { return new KnowledgeService(UUID.randomUUID(), new KnowledgeState(), 42, () -> {}); }
    private static void install(Map<ResourceLocation, IdeaDefinition> ideas, Map<ResourceLocation, LinkRule> links) {
        var snapshot = new KnowledgeDefinitions.Snapshot(0, ideas, Map.of(), links, Map.of(), Map.of(), List.of());
        if (!KnowledgeDefinitions.validate(snapshot).isEmpty()) throw new IllegalStateException(KnowledgeDefinitions.validate(snapshot).toString());
        KnowledgeDefinitions.install(snapshot);
    }
    private static void learn(KnowledgeService s, String name) { s.learn(KnowledgeElement.idea(id(name)), SOURCE, KnowledgeService.Grant.NORMAL); }
    private static Observation paper() { return new Observation(UUID.randomUUID(), Observation.Type.ITEM, new ResourceLocation("minecraft:paper"), Map.of(), Observation.Source.command(), Optional.empty()); }

    private static <T> T fixture(String path, Codec<T> codec) {
        try (var reader = new java.io.InputStreamReader(Objects.requireNonNull(IdeaAcquisitionGameTests.class.getResourceAsStream("/m2-fixtures/" + path + ".json")), java.nio.charset.StandardCharsets.UTF_8)) {
            return codec.parse(JsonOps.INSTANCE, JsonParser.parseReader(reader)).getOrThrow(false, error -> {});
        } catch (java.io.IOException e) { throw new IllegalStateException(e); }
    }

    @GameTest(batch = "formal_material_links", template = "empty", timeoutTicks = 40)
    public static void formalMaterialCombinationsReplaceEmptyDesk(GameTestHelper h) {
        Map<String, LinkRule> rules = new HashMap<>();
        for (String name : List.of("earthen_smelting", "iron_smelting", "refractory_firing")) {
            try (var reader = new java.io.InputStreamReader(Objects.requireNonNull(
                    IdeaAcquisitionGameTests.class.getResourceAsStream("/formal-link-fixtures/" + name + ".json")), java.nio.charset.StandardCharsets.UTF_8)) {
                var rule = LinkRule.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(reader)).getOrThrow(false, error -> {});
                h.assertTrue(rule.validate().isEmpty(), name + " formal link validates");
                h.assertTrue(rule.output().equals(new ResourceLocation("the_winter_rescue", name)), "output remains the authored idea");
                h.assertTrue(LinkMatcher.bindExact(List.of(), rule.pattern(), TagLookup.NONE).isEmpty(), "empty desk must not match " + name);
                rules.put(name, rule);
            } catch (java.io.IOException e) { throw new IllegalStateException(e); }
        }
        java.util.function.Function<String, KnowledgeElement> observe = name -> KnowledgeElement.observation(new Observation(
                UUID.randomUUID(), Observation.Type.ITEM, new ResourceLocation(name), Map.of(), Observation.Source.command(), Optional.empty()));
        var clay = observe.apply("minecraft:clay_ball");
        var sand = observe.apply("minecraft:sand");
        var copper = observe.apply("minecraft:raw_copper");
        var earthen = rules.get("earthen_smelting");
        h.assertTrue(LinkMatcher.bindExact(List.of(copper), earthen.pattern(), TagLookup.NONE).isPresent(), "raw copper alone is sufficient for linking");
        h.assertTrue(LinkMatcher.bindExact(List.of(clay, sand), earthen.pattern(), TagLookup.NONE).isEmpty(), "auxiliary records cannot replace raw copper");
        h.assertTrue(LinkMatcher.bindExact(List.of(copper, clay), earthen.pattern(), TagLookup.NONE).isEmpty(), "auxiliaries belong in the archive, not the exact tray inputs");
        // Inject real authored tag members: this tests predicates, not full-pack registration.
        TagLookup tags = (type, field, value, tag) -> type == Observation.Type.ITEM && field.equals("object") && switch (tag.toString()) {
            case "the_winter_rescue:research/iron_carbon_fuel" -> Set.of("minecraft:coal", "minecraft:charcoal", "immersiveengineering:coal_coke").contains(value.toString());
            case "the_winter_rescue:research/sandy_bricks" -> Set.of("charcoal_pit:sandy_brick_item", "charcoal_pit:sandy_brick").contains(value.toString());
            default -> false;
        };
        var iron = rules.get("iron_smelting");
        for (String fuel : List.of("minecraft:coal", "minecraft:charcoal", "immersiveengineering:coal_coke")) {
            var rawIron = observe.apply("minecraft:raw_iron"); var carbon = observe.apply(fuel);
            h.assertTrue(LinkMatcher.bindExact(List.of(rawIron, carbon), iron.pattern(), tags).isPresent(), "raw iron and each fuel alternative match");
            h.assertTrue(LinkMatcher.bindExact(List.of(carbon, rawIron), iron.pattern(), tags).isPresent(), "desk order is irrelevant");
        }
        h.assertTrue(LinkMatcher.bindExact(List.of(observe.apply("minecraft:raw_iron")), iron.pattern(), tags).isEmpty(), "iron link still needs fuel");
        var refractory = rules.get("refractory_firing");
        for (String brick : List.of("charcoal_pit:sandy_brick_item", "charcoal_pit:sandy_brick"))
            h.assertTrue(LinkMatcher.bindExact(List.of(observe.apply(brick)), refractory.pattern(), tags).isPresent(), "either sandy brick form matches");
        h.assertTrue(LinkMatcher.bindExact(List.of(observe.apply("frostedheart:kaolin"), observe.apply("frostedheart:bauxite"), clay), refractory.pattern(), tags).isEmpty(), "raw materials are auxiliary, not the mandatory brick");
        var previous = KnowledgeDefinitions.current();
        try {
            install(Map.of(earthen.output(), definition(IdeaAcquisition.NONE), iron.output(), definition(IdeaAcquisition.NONE),
                refractory.output(), definition(IdeaAcquisition.NONE)), Map.of(id("formal_earthen"), earthen,
                    id("formal_iron"), iron, id("formal_refractory"), refractory));
            var service = service(); service.learn(copper, SOURCE, KnowledgeService.Grant.NORMAL);
            h.assertTrue(service.matchLink(id("formal_earthen"), List.of(copper.key())).isPresent()
                && KnowledgeLinkSessions.numbers(service, earthen) == 2, "mandatory-only link works at two numbers");
            service.receive(clay, SOURCE);
            h.assertTrue(KnowledgeLinkSessions.numbers(service, earthen) == 2, "inbox auxiliary does not reduce cost");
            service.learnInbox(clay.key());
            h.assertTrue(KnowledgeLinkSessions.numbers(service, earthen) == 1, "one learned auxiliary removes one ordered number");
            service.learn(sand, SOURCE, KnowledgeService.Grant.NORMAL);
            h.assertTrue(KnowledgeLinkSessions.numbers(service, earthen) == 0, "both auxiliaries give a zero-number puzzle");
            var bare = service();
            h.assertTrue(KnowledgeLinkSessions.numbers(bare, refractory) == 3, "refractory starts at three numbers");
            for (String material : List.of("frostedheart:kaolin", "frostedheart:bauxite", "minecraft:clay_ball"))
                bare.learn(observe.apply(material), SOURCE, KnowledgeService.Grant.NORMAL);
            h.assertTrue(KnowledgeLinkSessions.numbers(bare, refractory) == 0, "all three refractory auxiliaries give zero numbers");
        } finally { KnowledgeDefinitions.install(previous); }
        h.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void linkKnowledgeOverridesBehaviorWithoutChangingLegacyRules(GameTestHelper h) {
        var previous = KnowledgeDefinitions.current();
        try {
            var behavior = acquisition(List.of(trigger(id("signal"), null)), List.of(idea("event_gate")), List.of(idea("event_bonus")));
            var explicit = new LinkRule(List.of(), List.of(), id("target"), List.of(),
                Optional.of(List.of(idea("link_gate"))), Optional.of(List.of(idea("link_bonus"), item("glass"))));
            var noGate = new LinkRule(List.of(), List.of(), id("target"), List.of(), Optional.of(List.of()), Optional.of(List.of()));
            var legacy = new LinkRule(List.of(), List.of(), id("target"));
            install(Map.of(id("target"), definition(behavior), id("event_gate"), definition(IdeaAcquisition.NONE),
                id("event_bonus"), definition(IdeaAcquisition.NONE), id("link_gate"), definition(IdeaAcquisition.NONE),
                id("link_bonus"), definition(IdeaAcquisition.NONE)), Map.of(id("explicit"), explicit, id("none"), noGate, id("legacy"), legacy));
            var s = service(); UUID player = UUID.randomUUID();
            IdeaAcquisitionService.fire(s, List.of(new IdeaAcquisitionService.Signal(id("signal"), Optional.empty())), () -> 0);
            h.assertTrue(!s.isActive(KnowledgeKey.idea(id("target"))), "behavior is gated by its own required knowledge");
            h.assertTrue(s.matchLink(id("none"), List.of()).isPresent() && s.matchLink(id("legacy"), List.of()).isEmpty(), "explicit empty gate overrides while legacy inherits");
            learn(s, "link_gate"); learn(s, "event_bonus");
            h.assertTrue(s.matchLink(id("explicit"), List.of()).isPresent(), "link can run without the event prerequisite");
            h.assertTrue(KnowledgeLinkSessions.numbers(s, explicit) == 2, "event bonus does not reduce explicit link difficulty");
            learn(s, "link_bonus");
            h.assertTrue(KnowledgeLinkSessions.numbers(s, explicit) == 1 && KnowledgeLinkSessions.numbers(s, noGate) == 0, "route auxiliary and explicit no-auxiliary override are independent");
            var glass = new Observation(UUID.randomUUID(), Observation.Type.ITEM, new ResourceLocation("minecraft:glass"), Map.of(), Observation.Source.command(), Optional.empty());
            h.assertTrue(KnowledgeDefinitions.current().hasObservationUse(glass), "link-only auxiliary observation is indexed as useful");
            KnowledgeLinkSessions.start(player, 1, BlockPos.ZERO, s, id("explicit"), List.of());
            h.assertTrue(KnowledgeLinkSessions.session(player).orElseThrow().numbers() == 1, "actual session uses rule-specific difficulty");
            s.forget(KnowledgeKey.idea(id("link_gate")));
            h.assertTrue(KnowledgeLinkSessions.check(player, s).state().equals("cancelled"), "link gate is rechecked during the puzzle");
            learn(s, "event_gate");
            IdeaAcquisitionService.fire(s, List.of(new IdeaAcquisitionService.Signal(id("signal"), Optional.empty())), () -> 0);
            h.assertTrue(s.isActive(KnowledgeKey.idea(id("target"))), "event works without link prerequisites");
            for (String field : List.of("required_knowledge", "optional_knowledge"))
                h.assertTrue(LinkRule.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"inputs\":[],\"output\":\"m2:target\",\"" + field + "\":true}")).error().isPresent(), "invalid route knowledge is not silently ignored");
            h.assertTrue(LinkRule.CODEC.parse(JsonOps.INSTANCE, LinkRule.CODEC.encodeStart(JsonOps.INSTANCE, explicit).result().orElseThrow()).result().orElseThrow().equals(explicit), "route prerequisites survive codec round trip");
            h.succeed();
        } finally { KnowledgeLinkSessions.clear(); KnowledgeDefinitions.install(previous); }
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void authoredExamplesAndStrictAcquisitionFields(GameTestHelper h) {
        Map<ResourceLocation, IdeaDefinition> ideas = new HashMap<>();
        for (String name : List.of("field_idea", "learned_hint")) ideas.put(new ResourceLocation("example", name), fixture("ideas/" + name, IdeaDefinition.CODEC));
        ideas.put(new ResourceLocation("example:i"), definition(IdeaAcquisition.NONE));
        Map<ResourceLocation, ResultDefinition> results = new HashMap<>();
        for (String name : List.of("smelting_finding", "joint_finding")) {
            String json = "{\"title\":\"Finding\",\"result\":{\"type\":\"finding\",\"id\":\"example:" + name + "\"}}";
            results.put(new ResourceLocation("example", name), ResultDefinition.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow(false, error -> {}));
        }
        Map<ResourceLocation, LinkRule> links = new HashMap<>();
        for (String name : List.of("field_zero", "field_one", "field_legacy")) links.put(new ResourceLocation("example", name), fixture("links/" + name, LinkRule.CODEC));
        var snapshot = new KnowledgeDefinitions.Snapshot(0, ideas, results, links, Map.of(),
            Map.of(new ResourceLocation("example:behavior_study"), fixture("projects/behavior_study", ProjectDefinition.CODEC)), List.of());
        h.assertTrue(KnowledgeDefinitions.validate(snapshot).isEmpty(), "authored M2 fixture resolves all definitions and explicit 0/1/2 rules");
        for (String field : List.of("required", "optional", "triggers", "chance")) {
            var json = JsonParser.parseString("{\"title\":\"Bad\",\"acquisition\":{\"" + field + "\":true}}");
            h.assertTrue(IdeaDefinition.CODEC.parse(JsonOps.INSTANCE, json).error().isPresent(), "bad acquisition " + field + " must not default");
        }
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void knownIdeaManualLoopAddsHistoryWithoutReacquisition(GameTestHelper h) {
        var previous = KnowledgeDefinitions.current();
        java.util.concurrent.atomic.AtomicInteger acquisitionChecks = new java.util.concurrent.atomic.AtomicInteger();
        var target = KnowledgeKey.idea(new ResourceLocation("knowledge_example:materials"));
        java.util.function.Consumer<com.teammoeg.frostedresearch.knowledge.event.KnowledgeOperationEvent.Check> listener = event -> {
            if (event.element().key().equals(target) && event.operation().startsWith("learn")) acquisitionChecks.incrementAndGet();
        };
        try {
            var loop = fixture("legacy-loop/links/materials_revisited", LinkRule.CODEC);
            Map<ResourceLocation, ResultDefinition> results = new HashMap<>();
            for (String name : List.of("metallurgy_finding", "climbing_finding"))
                results.put(new ResourceLocation("knowledge_example", name), fixture("legacy-loop/results/" + name, ResultDefinition.CODEC));
            var rule = new ResourceLocation("knowledge_example:materials_revisited"); var alternative = id("another_materials_path");
            var snapshot = new KnowledgeDefinitions.Snapshot(0, Map.of(target.definitionId(), definition(IdeaAcquisition.NONE)), results,
                Map.of(rule, loop, alternative, loop), Map.of(), Map.of(), List.of());
            h.assertTrue(KnowledgeDefinitions.validate(snapshot).isEmpty(), "original playthrough loop fixture resolves");
            KnowledgeDefinitions.install(snapshot);
            var service = service();
            service.learn(KnowledgeElement.idea(target.definitionId()), SOURCE, KnowledgeService.Grant.NORMAL);
            var keys = results.keySet().stream().sorted(Comparator.comparing(Object::toString)).map(KnowledgeKey::result).toList();
            for (var key : keys) service.learn(KnowledgeElement.result(key.definitionId()), SOURCE, KnowledgeService.Grant.NORMAL);
            int entries = service.state().archive().entries().size(), acquisitions = service.state().acquisitions().size();
            MinecraftForge.EVENT_BUS.addListener(listener);
            h.assertTrue(service.previewLinks(keys).isEmpty() && service.candidateLinks(10).isEmpty(), "archived outputs are hidden through both candidate APIs");
            UUID player = UUID.randomUUID();
            h.assertTrue(KnowledgeLinkSessions.start(player, 1, BlockPos.ZERO, service, rule, keys) == KnowledgeService.Status.ALREADY_OWNED, "known output cannot start a redundant UI puzzle");
            h.assertTrue(service.link(rule, keys).status() == KnowledgeService.Status.ALREADY_OWNED, "internal history API retains an additional manual path without a puzzle");
            h.assertTrue(service.state().links().size() == 1 && service.state().links().get(rule).output().equals(target), "actual loop binding retained");
            h.assertTrue(service.link(alternative, keys).status() == KnowledgeService.Status.ALREADY_OWNED && service.state().links().size() == 2, "another rule adds a distinct historical path");
            var firstBinding = service.state().links().get(rule); long revision = service.state().mutationRevision();
            service.link(rule, keys);
            h.assertTrue(service.state().links().size() == 2 && service.state().links().get(rule).equals(firstBinding)
                && service.state().mutationRevision() == revision, "repeating same rule preserves its first binding with no further mutation");
            h.assertTrue(service.state().archive().entries().size() == entries && service.state().acquisitions().size() == acquisitions
                && acquisitionChecks.get() == 0, "known manual output never reacquires knowledge or posts another learning attempt");
            var inbox = service(); inbox.receive(KnowledgeElement.idea(target.definitionId()), SOURCE);
            for (var key : keys) inbox.learn(KnowledgeElement.result(key.definitionId()), SOURCE, KnowledgeService.Grant.NORMAL);
            int inboxAcquisitions = inbox.state().acquisitions().size();
            h.assertTrue(inbox.previewLinks(keys).isEmpty() && inbox.candidateLinks(10).isEmpty(), "received outputs are hidden before learning too");
            h.assertTrue(inbox.link(rule, keys).status() == KnowledgeService.Status.ALREADY_RECEIVED && inbox.state().links().containsKey(rule), "received output also permits a manual path");
            h.assertTrue(inbox.query(target).inInbox() && !inbox.query(target).inArchive() && inbox.state().acquisitions().size() == inboxAcquisitions,
                "received output is not silently relearned or duplicated by a manual path");
            h.succeed();
        } finally { MinecraftForge.EVENT_BUS.unregister(listener); KnowledgeLinkSessions.clear(); KnowledgeDefinitions.install(previous); }
    }

    @GameTest(template = "empty", timeoutTicks = 180)
    public static void repeatedRealObservationStillFiresAction(GameTestHelper h) {
        var previous = KnowledgeDefinitions.current(); var server = h.getLevel().getServer();
        var player = new ServerPlayer(server, h.getLevel(), new GameProfile(UUID.randomUUID(), "m2-observation"));
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        var channel = new io.netty.channel.embedded.EmbeddedChannel(connection); connection.setProtocol(net.minecraft.network.ConnectionProtocol.PLAY);
        AtomicInteger actions = new AtomicInteger();
        java.util.function.Consumer<com.teammoeg.frostedresearch.knowledge.event.KnowledgeBehaviorEvent> listener = event -> {
            if (event.id().equals(IdeaAcquisitionService.OBSERVATION_COMPLETED) && event.actor().filter(player.getUUID()::equals).isPresent()) actions.incrementAndGet();
        };
        Runnable cleanup = () -> {
            MinecraftForge.EVENT_BUS.unregister(listener);
            com.teammoeg.frostedresearch.knowledge.observation.ObservationSessions.handle(player,
                new com.teammoeg.frostedresearch.knowledge.observation.ObservationActionPacket(com.teammoeg.frostedresearch.knowledge.observation.ObservationActionPacket.Action.DISCARD));
            if (server.getPlayerList().getPlayers().contains(player)) server.getPlayerList().remove(player);
            channel.finishAndReleaseAll(); KnowledgeDefinitions.install(previous);
        };
        try {
            install(Map.of(id("observed"), definition(acquisition(List.of(trigger(IdeaAcquisitionService.OBSERVATION_COMPLETED, item("paper"))), List.of(), List.of()))), Map.of());
            server.getPlayerList().placeNewPlayer(connection, player);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.PAPER)); player.setXRot(-90);
            var service = KnowledgeService.forPlayer(player);
            service.learn(com.teammoeg.frostedresearch.knowledge.item.ItemRetrieval.observe(player, player.getMainHandItem()), SOURCE, KnowledgeService.Grant.NORMAL);
            h.assertTrue(!service.isActive(KnowledgeKey.idea(id("observed"))), "learning is independent of observing now");
            MinecraftForge.EVENT_BUS.addListener(listener);
            Runnable start = () -> com.teammoeg.frostedresearch.knowledge.observation.ObservationSessions.handle(player,
                new com.teammoeg.frostedresearch.knowledge.observation.ObservationActionPacket(
                    com.teammoeg.frostedresearch.knowledge.observation.ObservationActionPacket.Action.HELD_ITEM, BlockPos.ZERO, 0, player.containerMenu.containerId,
                    com.teammoeg.frostedresearch.knowledge.observation.ObservationActionPacket.Method.KEY));
            start.run();
            h.onEachTick(() -> com.teammoeg.frostedresearch.knowledge.observation.ObservationSessions.tick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.END, player)));
            h.runAfterDelay(65, () -> {
                try {
                    h.assertTrue(actions.get() == 1, "first real observation action");
                    // Close the first record preview, as a real player must, before holding observe again.
                    com.teammoeg.frostedresearch.knowledge.observation.ObservationSessions.handle(player,
                        new com.teammoeg.frostedresearch.knowledge.observation.ObservationActionPacket(com.teammoeg.frostedresearch.knowledge.observation.ObservationActionPacket.Action.DISCARD));
                    start.run();
                }
                catch (Throwable failure) { cleanup.run(); throw failure; }
            });
            h.runAfterDelay(132, () -> {
                try {
                    h.assertTrue(actions.get() == 2, "second equivalent observation still emits its action");
                    h.assertTrue(service.state().archive().observationCount() == 1 && service.isActive(KnowledgeKey.idea(id("observed"))), "duplicate record not learned; behavior idea still acquired once");
                    h.succeed();
                } finally { cleanup.run(); }
            });
        } catch (Throwable failure) { cleanup.run(); throw failure; }
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void realPickupCraftSmeltContainerUseAndInternalMove(GameTestHelper h) {
        var previous = KnowledgeDefinitions.current();
        var server = h.getLevel().getServer();
        ServerPlayer player = new ServerPlayer(server, h.getLevel(), new GameProfile(UUID.randomUUID(), "m2-bridge"));
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        var channel = new io.netty.channel.embedded.EmbeddedChannel(connection);
        connection.setProtocol(net.minecraft.network.ConnectionProtocol.PLAY);
        try {
            Map<ResourceLocation, IdeaDefinition> ideas = new HashMap<>();
            for (String[] pair : List.of(new String[]{"pickup", "paper"}, new String[]{"craft", "oak_planks"},
                    new String[]{"smelt", "iron_ingot"}, new String[]{"container", "diamond"}, new String[]{"move", "cobblestone"}))
                ideas.put(id(pair[0]), definition(acquisition(List.of(trigger(IdeaAcquisitionService.ITEM_OBTAINED, item(pair[1]))), List.of(), List.of())));
            ideas.put(id("use"), definition(acquisition(List.of(trigger(IdeaAcquisitionService.ITEM_USED, item("apple"))), List.of(), List.of())));
            ideas.put(id("use_on"), definition(acquisition(List.of(trigger(IdeaAcquisitionService.ITEM_USED, item("torch"))), List.of(), List.of())));
            var lever = new KnowledgeCondition(KnowledgeKey.Kind.OBSERVATION, Optional.empty(), Optional.of(Observation.Type.BLOCK),
                List.of(new FieldCondition("object", FieldCondition.Operator.EQ, Optional.of(ObservationValue.known("minecraft:lever")), Optional.empty())), "Lever", "");
            ideas.put(id("block"), definition(acquisition(List.of(trigger(IdeaAcquisitionService.BLOCK_INTERACTED, lever)), List.of(), List.of())));
            ideas.put(id("wait_target"), definition(IdeaAcquisition.NONE));
            install(ideas, Map.of(id("wait_rule"), new LinkRule(List.of(), List.of(), id("wait_target"))));
            server.getPlayerList().placeNewPlayer(connection, player);
            player.teleportTo(h.getLevel(), h.absolutePos(BlockPos.ZERO).getX(), h.absolutePos(BlockPos.ZERO).getY() + 1, h.absolutePos(BlockPos.ZERO).getZ(), Set.of(), 0, 0);
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            var s = KnowledgeService.forPlayer(player);
            var note = com.teammoeg.frostedresearch.FRContents.Items.RESEARCH_NOTE.get().getDefaultInstance();
            h.assertTrue(!com.teammoeg.frostedresearch.knowledge.item.ItemRetrieval.canObserve(note), "notes retain separate reading semantics");
            var droppedNote = new ItemEntity(h.getLevel(), player.getX(), player.getY(), player.getZ(), note);
            droppedNote.setNoPickUpDelay(); h.getLevel().addFreshEntity(droppedNote); droppedNote.playerTouch(player);
            KnowledgeBehaviorBridge.itemUsed(player, note);
            h.assertTrue(com.teammoeg.frostedresearch.knowledge.item.ItemRetrieval.behaviorSample(player, note).observation().orElseThrow().object().toString().equals("frostedresearch:research_note"), "behavior payload can describe note item without reading its content");
            ItemEntity dropped = new ItemEntity(h.getLevel(), player.getX(), player.getY(), player.getZ(), new ItemStack(Items.PAPER));
            dropped.setNoPickUpDelay(); h.getLevel().addFreshEntity(dropped); dropped.playerTouch(player);
            h.assertTrue(s.isActive(KnowledgeKey.idea(id("pickup"))), "actual ItemEntity post-pickup bridge");
            player.inventoryMenu.getSlot(1).set(new ItemStack(Items.OAK_LOG));
            player.inventoryMenu.clicked(0, 0, ClickType.PICKUP, player);
            h.assertTrue(s.isActive(KnowledgeKey.idea(id("craft"))), "actual crafting result click bridge");
            player.inventoryMenu.setCarried(ItemStack.EMPTY);
            h.setBlock(new BlockPos(1, 1, 1), Blocks.FURNACE);
            var furnace = (net.minecraft.world.level.block.entity.FurnaceBlockEntity) h.getBlockEntity(new BlockPos(1, 1, 1));
            furnace.setItem(2, new ItemStack(Items.IRON_INGOT));
            var furnaceMenu = new FurnaceMenu(7, player.getInventory(), furnace, new SimpleContainerData(4));
            player.containerMenu = furnaceMenu; furnaceMenu.clicked(2, 0, ClickType.PICKUP, player);
            h.assertTrue(s.isActive(KnowledgeKey.idea(id("smelt"))), "actual furnace output extraction bridge");
            furnaceMenu.setCarried(ItemStack.EMPTY);
            var chestItems = new net.minecraft.world.SimpleContainer(27); chestItems.setItem(0, new ItemStack(Items.DIAMOND));
            var chest = ChestMenu.threeRows(8, player.getInventory(), chestItems); player.containerMenu = chest;
            chest.clicked(0, 0, ClickType.QUICK_MOVE, player);
            h.assertTrue(s.isActive(KnowledgeKey.idea(id("container"))), "external container shift extraction bridge");
            player.containerMenu = player.inventoryMenu; player.getInventory().setItem(0, new ItemStack(Items.COBBLESTONE));
            player.inventoryMenu.clicked(36, 0, ClickType.PICKUP, player);
            h.assertTrue(!s.isActive(KnowledgeKey.idea(id("move"))), "inventory to cursor is ownership-preserving movement");
            player.inventoryMenu.setCarried(ItemStack.EMPTY);
            player.inventoryMenu.getSlot(1).set(new ItemStack(Items.COBBLESTONE));
            player.inventoryMenu.clicked(1, 0, ClickType.PICKUP, player);
            h.assertTrue(!s.isActive(KnowledgeKey.idea(id("move"))), "returning owned crafting inputs is not new acquisition");
            player.inventoryMenu.setCarried(ItemStack.EMPTY);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.APPLE)); player.getFoodData().setFoodLevel(10);
            player.gameMode.useItem(player, h.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND);
            h.assertTrue(s.isActive(KnowledgeKey.idea(id("use"))), "successful real item use bridge");
            player.stopUsingItem();
            BlockPos support = h.absolutePos(new BlockPos(2, 1, 2)); h.getLevel().setBlockAndUpdate(support, Blocks.STONE.defaultBlockState());
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.TORCH));
            player.gameMode.useItemOn(player, h.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(support), net.minecraft.core.Direction.UP, support, false));
            h.assertTrue(s.isActive(KnowledgeKey.idea(id("use_on"))), "actual useOn counts as item use");
            BlockPos leverPos = support.above(); h.getLevel().setBlockAndUpdate(leverPos, Blocks.LEVER.defaultBlockState().setValue(net.minecraft.world.level.block.LeverBlock.FACE, net.minecraft.world.level.block.state.properties.AttachFace.FLOOR));
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            player.gameMode.useItemOn(player, h.getLevel(), ItemStack.EMPTY, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(leverPos), net.minecraft.core.Direction.UP, leverPos, false));
            h.assertTrue(s.isActive(KnowledgeKey.idea(id("block"))), "actual successful block interaction bridge");
            h.setBlock(new BlockPos(3, 1, 1), com.teammoeg.frostedresearch.FRContents.Blocks.DRAWING_DESK.get());
            var deskTile = (com.teammoeg.frostedresearch.blocks.DrawingDeskTileEntity) h.getBlockEntity(new BlockPos(3, 1, 1));
            var deskMenu = new com.teammoeg.frostedresearch.gui.drawdesk.DrawDeskContainer(12, player.getInventory(), deskTile);
            player.containerMenu = deskMenu;
            h.assertTrue(KnowledgeLinkSessions.start(player.getUUID(), 12, deskTile.getBlockPos(), s, id("wait_rule"), List.of()) == KnowledgeService.Status.SUCCESS, "real desk starts a link puzzle");
            KnowledgeLinkSessions.tick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.END, player));
            h.assertTrue(KnowledgeLinkSessions.session(player.getUUID()).isPresent(), "valid desk keeps its puzzle");
            player.closeContainer();
            KnowledgeLinkSessions.tick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.END, player));
            h.assertTrue(KnowledgeLinkSessions.session(player.getUUID()).isEmpty() && !s.isActive(KnowledgeKey.idea(id("wait_target"))), "actual container close cancels without output");
            player.containerMenu = new com.teammoeg.frostedresearch.gui.drawdesk.DrawDeskContainer(13, player.getInventory(), deskTile);
            KnowledgeLinkSessions.start(player.getUUID(), 13, deskTile.getBlockPos(), s, id("wait_rule"), List.of());
            server.getPlayerList().remove(player);
            h.assertTrue(KnowledgeLinkSessions.session(player.getUUID()).isEmpty(), "actual logout cancels the transient puzzle");
            h.succeed();
        } finally {
            if (server.getPlayerList().getPlayers().contains(player)) server.getPlayerList().remove(player);
            channel.finishAndReleaseAll(); KnowledgeDefinitions.install(previous);
        }
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void gatesOrEntrancesOptionalProbabilityAndExternalGrants(GameTestHelper h) {
        var previous = KnowledgeDefinitions.current();
        try {
            var acquisition = acquisition(List.of(trigger(id("custom_a"), null), trigger(id("custom_b"), null)), List.of(idea("required")), List.of(idea("bonus")));
            install(Map.of(id("target"), definition(acquisition), id("required"), definition(IdeaAcquisition.NONE), id("bonus"), definition(IdeaAcquisition.NONE)), Map.of());
            var s = service(); AtomicInteger rolls = new AtomicInteger();
            var signal = new IdeaAcquisitionService.Signal(id("custom_a"), Optional.empty());
            IdeaAcquisitionService.fire(s, List.of(signal), () -> { rolls.incrementAndGet(); return 0; });
            h.assertTrue(!s.query(KnowledgeKey.idea(id("target"))).inArchive() && rolls.get() == 0, "missing necessary knowledge never rolls behavior");
            s.receive(KnowledgeElement.idea(id("target")), SOURCE);
            h.assertTrue(s.learnInbox(KnowledgeKey.idea(id("target"))).succeeded(), "external receive/learn bypass acquisition gates");
            var external = service(); h.assertTrue(external.produce(KnowledgeElement.idea(id("target")), SOURCE).succeeded(), "external produce also bypasses acquisition gates");
            for (var event : List.of(id("custom_a"), id("custom_b"))) {
                var one = service(); learn(one, "required");
                IdeaAcquisitionService.fire(one, List.of(new IdeaAcquisitionService.Signal(event, Optional.empty())), () -> 0);
                h.assertTrue(one.isActive(KnowledgeKey.idea(id("target"))), "each OR entrance works alone");
            }
            var batch = service(); learn(batch, "required");
            IdeaAcquisitionService.fire(batch, List.of(signal, new IdeaAcquisitionService.Signal(id("custom_b"), Optional.empty())), () -> { rolls.incrementAndGet(); return 0; });
            IdeaAcquisitionService.fire(batch, List.of(signal), () -> { rolls.incrementAndGet(); return 0; });
            h.assertTrue(rolls.get() == 1, "one operation rolls an idea once; owning it never rolls again");
            var optional = new IdeaAcquisition(acquisition.triggers(), List.of(), List.of(idea("bonus")), Optional.of(0.2));
            install(Map.of(id("target"), definition(optional), id("bonus"), definition(IdeaAcquisition.NONE)), Map.of());
            var rich = service(); double before = IdeaAcquisitionService.chance(rich, id("target")); learn(rich, "bonus");
            h.assertTrue(IdeaAcquisitionService.chance(rich, id("target")) > before, "optional archive knowledge increases probability");
            h.succeed();
        } finally { KnowledgeDefinitions.install(previous); }
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void everyMissingAuxiliaryAddsOneNumberAndAllLevelsAwardOnWin(GameTestHelper h) {
        var previous = KnowledgeDefinitions.current();
        try {
            var conditions = new ArrayList<KnowledgeCondition>();
            var ideas = new HashMap<ResourceLocation, IdeaDefinition>();
            for (int n = 0; n < 7; n++) {
                conditions.add(idea("aux" + n));
                ideas.put(id("aux" + n), definition(IdeaAcquisition.NONE));
            }
            ideas.put(id("target"), definition(new IdeaAcquisition(List.of(), List.of(), conditions, Optional.empty())));
            var rule = new LinkRule(List.of(), List.of(), id("target"));
            install(ideas, Map.of(id("all_levels"), rule));
            for (int known = 0; known <= 7; known++) {
                var service = service(); var player = UUID.randomUUID();
                for (int n = 0; n < known; n++) learn(service, "aux" + n);
                h.assertTrue(KnowledgeLinkSessions.start(player, 1, BlockPos.ZERO, service, id("all_levels"), List.of())
                        == KnowledgeService.Status.SUCCESS, "every supported cost can start");
                var session = KnowledgeLinkSessions.session(player).orElseThrow();
                h.assertTrue(session.missing() == 7 - known && session.numbers() == 7 - known,
                        "one missing auxiliary maps to one ordered number");
                var payload = KnowledgeLinkSessions.check(player, service).payload();
                h.assertTrue(!payload.contains("link_duration") && !payload.contains("link_elapsed")
                        && payload.getCompound("link_game").getInt("lvl") == session.game().getLvl(),
                        "UI receives a real board and difficulty, with no timer");
                winLink(player, service, h);
                h.assertTrue(service.isActive(KnowledgeKey.idea(id("target"))), "all eight difficulties produce the idea on an ordinary win");
            }
            h.succeed();
        } finally { KnowledgeLinkSessions.clear(); KnowledgeDefinitions.install(previous); }
    }

    private static void winLink(UUID player, KnowledgeService service, GameTestHelper h) {
        var session = KnowledgeLinkSessions.session(player).orElseThrow();
        int level = com.teammoeg.frostedresearch.gui.drawdesk.game.GenerateInfo.levelForNumbers(session.numbers());
        var moves = session.game().initWithSolution(com.teammoeg.frostedresearch.gui.drawdesk.game.GenerateInfo.all[level], new Random(17));
        session.game().setLvl(level);
        var before = session.game().serialize();
        h.assertTrue(KnowledgeLinkSessions.check(player, service).session().game().serialize().equals(before), "checking never advances or resolves a board by time");
        for (var pair : moves)
            h.assertTrue(KnowledgeLinkSessions.play(player, service, pair.getFirst(), pair.getSecond()).state().equals("running"), "ordinary pairs do not grant the idea early");
        h.assertTrue(!service.query(KnowledgeKey.idea(session.output())).inArchive() && !service.query(KnowledgeKey.idea(session.output())).inInbox(), "all symbol/number pairs still require the final tile");
        h.assertTrue(KnowledgeLinkSessions.play(player, service, com.teammoeg.frostedresearch.gui.drawdesk.game.ResearchGame.FINAL_CARD, null).state().equals("completed"), "final tile grants the idea immediately");
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void puzzlesReplaceWaitingAndRecheckKnowledge(GameTestHelper h) {
        var previous = KnowledgeDefinitions.current();
        try {
            var acquisition = acquisition(List.of(), List.of(idea("required")), List.of(idea("bonus")));
            var single = item("paper").slot(); var second = new LinkRule.Slot("second", single.kind(), single.definition(), single.observationType(), single.conditions());
            install(Map.of(id("target"), definition(acquisition), id("required"), definition(IdeaAcquisition.NONE), id("bonus"), definition(IdeaAcquisition.NONE)),
                Map.of(id("zero"), new LinkRule(List.of(), List.of(), id("target")), id("one"), new LinkRule(List.of(single), List.of(), id("target")), id("old"), new LinkRule(List.of(single, second), List.of(), id("target"))));
            var s = service(); UUID player = UUID.randomUUID();
            h.assertTrue(s.previewLinks(List.of()).isEmpty(), "missing required knowledge blocks the puzzle");
            learn(s, "required");
            var rule = KnowledgeDefinitions.current().links().get(id("zero"));
            h.assertTrue(s.previewLinks(List.of()).size() == 1 && KnowledgeLinkSessions.numbers(s, rule) == 1, "one absent auxiliary means one number");
            KnowledgeLinkSessions.start(player, 1, BlockPos.ZERO, s, id("zero"), List.of());
            var original = KnowledgeLinkSessions.session(player).orElseThrow();
            h.assertTrue(KnowledgeLinkSessions.start(player, 1, BlockPos.ZERO, s, id("zero"), List.of()) == KnowledgeService.Status.ACTIVE
                && original == KnowledgeLinkSessions.session(player).orElseThrow(), "repeat start preserves the current board");
            learn(s, "bonus");
            h.assertTrue(KnowledgeLinkSessions.check(player, s).session().numbers() == 1, "knowledge gained mid-game keeps this board fixed");
            h.assertTrue(KnowledgeLinkSessions.reset(player, s).session().numbers() == 0, "reset recalculates difficulty from current learned auxiliaries");
            winLink(player, s, h);
            h.assertTrue(s.isActive(KnowledgeKey.idea(id("target"))) && KnowledgeLinkSessions.check(player, s).state().equals("idle"), "zero-number puzzle grants once, not on start or by timer");
            var rich = service(); learn(rich, "required"); learn(rich, "bonus");
            KnowledgeLinkSessions.start(player, 1, BlockPos.ZERO, rich, id("zero"), List.of());
            KnowledgeLinkSessions.cancel(player);
            h.assertTrue(KnowledgeLinkSessions.check(player, rich).state().equals("idle") && !rich.isActive(KnowledgeKey.idea(id("target"))), "cancel leaves no output");
            KnowledgeLinkSessions.start(player, 1, BlockPos.ZERO, rich, id("zero"), List.of());
            rich.forget(KnowledgeKey.idea(id("required")));
            h.assertTrue(KnowledgeLinkSessions.check(player, rich).state().equals("cancelled"), "required knowledge loss ends the puzzle");
            learn(rich, "required"); Observation a = paper(), b = paper();
            rich.learn(KnowledgeElement.observation(a), SOURCE, KnowledgeService.Grant.NORMAL);
            rich.learn(KnowledgeElement.observation(b), SOURCE, KnowledgeService.Grant.NORMAL);
            h.assertTrue(rich.previewLinks(List.of(a.key())).size() == 1, "one-input rules remain independent of auxiliaries");
            h.assertTrue(rich.previewLinks(List.of(a.key(), b.key())).size() == 1, "two-slot rules still need distinct records");
            KnowledgeLinkSessions.start(player, 1, BlockPos.ZERO, rich, id("zero"), List.of());
            rich.receive(KnowledgeElement.idea(id("target")), SOURCE);
            h.assertTrue(KnowledgeLinkSessions.check(player, rich).state().equals("cancelled"), "a teammate's received idea ends the redundant puzzle");
            h.succeed();
        } finally { KnowledgeLinkSessions.clear(); KnowledgeDefinitions.install(previous); }
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void learnedCascadeAndObservationUsesRemainIndependent(GameTestHelper h) {
        var previous = KnowledgeDefinitions.current();
        try {
            install(Map.of(id("a"), definition(acquisition(List.of(trigger(IdeaAcquisitionService.KNOWLEDGE_LEARNED, idea("b"))), List.of(), List.of())),
                id("b"), definition(acquisition(List.of(trigger(IdeaAcquisitionService.KNOWLEDGE_LEARNED, idea("a"))), List.of(), List.of())),
                id("observed"), definition(acquisition(List.of(trigger(IdeaAcquisitionService.OBSERVATION_COMPLETED, item("paper"))), List.of(), List.of()))), Map.of());
            var s = service(); learn(s, "a");
            h.assertTrue(s.isActive(KnowledgeKey.idea(id("a"))) && s.isActive(KnowledgeKey.idea(id("b"))) && s.state().archive().entries().size() == 2, "cyclic learned events terminate with one copy per idea");
            Observation record = paper();
            h.assertTrue(KnowledgeDefinitions.current().hasObservationUse(record), "trigger-only observation counts as a potential use");
            s.learn(KnowledgeElement.observation(record), SOURCE, KnowledgeService.Grant.NORMAL);
            h.assertTrue(!s.isActive(KnowledgeKey.idea(id("observed"))), "learning an old observation does not replay the action");
            AtomicInteger actions = new AtomicInteger();
            java.util.function.Consumer<com.teammoeg.frostedresearch.knowledge.event.KnowledgeBehaviorEvent> listener = e -> { if (e.id().equals(IdeaAcquisitionService.OBSERVATION_COMPLETED)) actions.incrementAndGet(); };
            MinecraftForge.EVENT_BUS.addListener(listener);
            try {
                for (int i = 0; i < 2; i++) IdeaAcquisitionService.fire(s, List.of(new IdeaAcquisitionService.Signal(IdeaAcquisitionService.OBSERVATION_COMPLETED, KnowledgeElement.observation(record))), () -> 0);
            } finally { MinecraftForge.EVENT_BUS.unregister(listener); }
            h.assertTrue(actions.get() == 2 && s.isActive(KnowledgeKey.idea(id("observed"))), "duplicate observation still notifies action consumers independently of archive admission");
            h.succeed();
        } finally { KnowledgeDefinitions.install(previous); }
    }
}
