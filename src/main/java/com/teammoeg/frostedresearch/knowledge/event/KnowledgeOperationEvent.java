/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge.event;

import com.teammoeg.frostedresearch.knowledge.model.KnowledgeElement;
import com.teammoeg.frostedresearch.knowledge.state.AcquisitionSource;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

import java.util.Optional;
import java.util.UUID;

/**
 * 统一操作事件；Check 可取消，Committed 只在实际变更后发布。 / Unified knowledge operation events.
 */
public abstract class KnowledgeOperationEvent extends Event {
    private final UUID team;
    private final Optional<UUID> actor;
    private final String operation;
    private final KnowledgeElement element;
    private final AcquisitionSource source;

    protected KnowledgeOperationEvent(UUID team, Optional<UUID> actor, String operation, KnowledgeElement element, AcquisitionSource source) {
        this.team = team;
        this.actor = actor;
        this.operation = operation;
        this.element = element;
        this.source = source;
    }

    public UUID team() {
        return team;
    }

    public Optional<UUID> actor() {
        return actor;
    }

    public String operation() {
        return operation;
    }

    public KnowledgeElement element() {
        return element;
    }

    public AcquisitionSource source() {
        return source;
    }

    @Cancelable
    public static final class Check extends KnowledgeOperationEvent {
        public Check(UUID team, Optional<UUID> actor, String operation, KnowledgeElement element, AcquisitionSource source) {
            super(team, actor, operation, element, source);
        }
    }

    public static final class Committed extends KnowledgeOperationEvent {
        public Committed(UUID team, Optional<UUID> actor, String operation, KnowledgeElement element, AcquisitionSource source) {
            super(team, actor, operation, element, source);
        }
    }
}
