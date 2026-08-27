/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge;

import com.teammoeg.frostedresearch.data.TeamKnowledgeData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Executable method referenced by a topic protocol. */
public interface ProtocolHandler {
    List<ActionCard> actions(ResourceLocation topicId, ResearchTopicDefinition topic,
            ResearchTopicDefinition.Protocol protocol, TeamKnowledgeData data, IdeaRecord idea);

    Optional<Execution> execute(ServerPlayer player, ResourceLocation topicId,
            ResearchTopicDefinition topic, ResearchTopicDefinition.Protocol protocol,
            TeamKnowledgeData data, IdeaRecord idea);

    record Execution(Optional<FieldComparisonArtifact> artifact, boolean ideaReady, Set<UUID> attachedEvidence) {
        public Execution {
            artifact = artifact == null ? Optional.empty() : artifact;
            attachedEvidence = Set.copyOf(attachedEvidence);
        }

        public Execution(FieldComparisonArtifact artifact, boolean ideaReady) {
            this(Optional.of(artifact), ideaReady, Set.of());
        }

        public Execution(FieldComparisonArtifact artifact, boolean ideaReady, Set<UUID> attachedEvidence) {
            this(Optional.of(artifact), ideaReady, attachedEvidence);
        }

        public static Execution readyWithoutArtifact(Set<UUID> attachedEvidence) {
            return new Execution(Optional.empty(), true, attachedEvidence);
        }
    }
}
