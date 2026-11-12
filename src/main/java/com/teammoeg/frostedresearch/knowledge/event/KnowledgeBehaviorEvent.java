package com.teammoeg.frostedresearch.knowledge.event;

import com.teammoeg.frostedresearch.knowledge.model.KnowledgeElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.Event;
import java.util.*;

/** An actual action, independent of whether its subject can be received or learned. */
public final class KnowledgeBehaviorEvent extends Event {
    private final UUID team;
    private final ResourceLocation id;
    private final Optional<UUID> actor;
    private final Optional<KnowledgeElement> subject;
    public KnowledgeBehaviorEvent(UUID team, Optional<UUID> actor, ResourceLocation id, Optional<KnowledgeElement> subject) {
        this.team = team; this.actor = actor; this.id = id; this.subject = subject;
    }
    public Optional<UUID> actor() { return actor; }
    public UUID team() { return team; }
    public ResourceLocation id() { return id; }
    public Optional<KnowledgeElement> subject() { return subject; }
}
