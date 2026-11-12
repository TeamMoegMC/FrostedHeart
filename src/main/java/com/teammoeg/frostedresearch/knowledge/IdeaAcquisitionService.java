package com.teammoeg.frostedresearch.knowledge;

import com.teammoeg.frostedresearch.FRConfig;
import com.teammoeg.frostedresearch.knowledge.definition.*;
import com.teammoeg.frostedresearch.knowledge.event.KnowledgeBehaviorEvent;
import com.teammoeg.frostedresearch.knowledge.model.*;
import com.teammoeg.frostedresearch.knowledge.state.AcquisitionSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import java.util.*;
import java.util.function.DoubleSupplier;

public final class IdeaAcquisitionService {
    public static final ResourceLocation ITEM_OBTAINED = id("item_obtained"), ITEM_USED = id("item_used"),
        BLOCK_INTERACTED = id("block_interacted"), OBSERVATION_COMPLETED = id("observation_completed"), KNOWLEDGE_LEARNED = id("knowledge_learned");
    private static ResourceLocation id(String name) { return new ResourceLocation("frostedresearch", name); }
    public record Signal(ResourceLocation event, Optional<KnowledgeElement> subject) {
        public Signal(ResourceLocation event, KnowledgeElement subject) { this(event, Optional.of(subject)); }
    }
    private record Work(KnowledgeService service, List<Signal> signals, DoubleSupplier random) {}
    private static final ThreadLocal<ArrayDeque<Work>> QUEUE = new ThreadLocal<>();
    public static boolean requiredMet(KnowledgeService service, ResourceLocation idea) {
        var definition = KnowledgeDefinitions.current().ideas().get(idea);
        return definition != null && definition.enabled() && definition.acquisition().required().stream().allMatch(c -> service.matchKnowledge(c).isPresent());
    }
    public static int optionalCount(KnowledgeService service, ResourceLocation idea) {
        var definition = KnowledgeDefinitions.current().ideas().get(idea);
        return definition == null ? 0 : (int) definition.acquisition().optional().stream().filter(c -> service.matchKnowledge(c).isPresent()).count();
    }
    public static double chance(KnowledgeService service, ResourceLocation idea) {
        var definition = KnowledgeDefinitions.current().ideas().get(idea);
        return Math.min(1, definition.acquisition().chance().orElse(FRConfig.SERVER.knowledgeBehaviorChance.get())
            + optionalCount(service, idea) * FRConfig.SERVER.knowledgeBehaviorOptionalBonus.get());
    }
    /** Public server API for mods/KubeJS: one custom event ID and an optional immutable typed subject. */
    public static void emit(ServerPlayer player, ResourceLocation event, Optional<KnowledgeElement> subject) {
        fire(KnowledgeService.forPlayer(player), List.of(new Signal(event, subject)), player.getRandom()::nextDouble);
    }
    /** Batch signals from ONE operation so matching multiple entrances never rolls an idea twice. */
    public static void fire(KnowledgeService service, List<Signal> signals, DoubleSupplier random) {
        var queue = QUEUE.get();
        if (queue != null) { queue.add(new Work(service, List.copyOf(signals), random)); return; }
        queue = new ArrayDeque<>(); QUEUE.set(queue); queue.add(new Work(service, List.copyOf(signals), random));
        Set<String> attempted = new HashSet<>();
        try {
            while (!queue.isEmpty()) {
                Work work = queue.remove();
                work.signals.forEach(signal -> MinecraftForge.EVENT_BUS.post(new KnowledgeBehaviorEvent(work.service.teamId(), work.service.actor(), signal.event, signal.subject)));
                Map<ResourceLocation, IdeaDefinition> eligible = new TreeMap<>(Comparator.comparing(Object::toString));
                work.signals.forEach(signal -> eligible.putAll(KnowledgeDefinitions.current().behaviorIdeas(signal.event)));
                for (var entry : eligible.entrySet()) {
                    ResourceLocation idea = entry.getKey(); KnowledgeKey key = KnowledgeKey.idea(idea);
                    if (work.service.query(key).inArchive() || work.service.query(key).inInbox()) continue;
                    boolean matches = entry.getValue().acquisition().triggers().stream().anyMatch(trigger -> work.signals.stream().anyMatch(signal ->
                        trigger.event().equals(signal.event) && trigger.subject().map(c -> signal.subject.filter(c::matches).isPresent()).orElse(true)));
                    if (!matches || !requiredMet(work.service, idea) || !attempted.add(work.service.teamId() + ":" + idea)) continue;
                    if (work.random.getAsDouble() < chance(work.service, idea)) {
                        work.service.produce(KnowledgeElement.idea(idea), new AcquisitionSource("behavior", work.service.actor(), work.service.time()));
                        var acquired = work.service.query(key);
                        if (acquired.inArchive() || acquired.inInbox()) work.service.actor().ifPresent(actor -> {
                            var server = com.teammoeg.chorda.util.CDistHelper.getServer();
                            var player = server == null ? null : server.getPlayerList().getPlayer(actor);
                            if (player != null) player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("knowledge.frostedresearch.behavior.acquired",
                                net.minecraft.network.chat.Component.translatableWithFallback(entry.getValue().title(), entry.getValue().title())));
                        });
                    }
                }
            }
        } finally { QUEUE.remove(); }
    }
}
