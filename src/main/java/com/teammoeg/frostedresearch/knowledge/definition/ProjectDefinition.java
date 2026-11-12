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
package com.teammoeg.frostedresearch.knowledge.definition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

/**
 * 知识图谱使用的项目、成果关联与研究需求；执行进度由启动记录保存。
 * A project with its results and research requirements; progress lives in each research run.
 */
public record ProjectDefinition(String title, String body, List<ResourceLocation> results,
                                List<ProjectRequirement> requirements, Optional<ProjectAccess> access, Optional<LegacyReplacement> legacyReplacement, List<StageDefinition> stages) {
    public static final Codec<ProjectDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.optionalFieldOf("title", "").forGetter(ProjectDefinition::title),
            Codec.STRING.optionalFieldOf("body", "").forGetter(ProjectDefinition::body),
            ResourceLocation.CODEC.listOf().fieldOf("results").forGetter(ProjectDefinition::results),
            ProjectRequirement.field(ProjectRequirement.CODEC.listOf(), "requirements", List.of()).forGetter(ProjectDefinition::requirements),
            com.teammoeg.frostedresearch.knowledge.model.KnowledgeKey.strictOptionalField(ProjectAccess.CODEC, "access").forGetter(ProjectDefinition::access),
            com.teammoeg.frostedresearch.knowledge.model.KnowledgeKey.strictOptionalField(LegacyReplacement.CODEC, "legacy_replacement").forGetter(ProjectDefinition::legacyReplacement),
            ProjectRequirement.field(StageDefinition.CODEC.listOf(), "stages", List.of()).forGetter(ProjectDefinition::stages)
    ).apply(i, ProjectDefinition::new));

    public ProjectDefinition {
        stages = List.copyOf(stages);
        results = List.copyOf(results);
        requirements = List.copyOf(requirements);
    }

    public ProjectDefinition(String title, String body, List<ResourceLocation> results, List<ProjectRequirement> requirements) {
        this(title, body, results, requirements, Optional.empty(), Optional.empty(), List.of());
    }

    public ProjectDefinition(String title, String body, List<ResourceLocation> results, List<ProjectRequirement> requirements, Optional<ProjectAccess> access) {
        this(title, body, results, requirements, access, Optional.empty(), List.of());
    }

    public ProjectDefinition(String title, String body, List<ResourceLocation> results, List<ProjectRequirement> requirements,
            Optional<ProjectAccess> access, Optional<LegacyReplacement> legacyReplacement) {
        this(title,body,results,requirements,access,legacyReplacement,List.of());
    }
    public List<StageDefinition> orderedStages() {
        return stages.isEmpty() ? List.of(new StageDefinition("main",title,"","","",requirements.stream()
            .map(r -> new StageDefinition.Task(r,false,"")).toList(),List.of(),List.of())) : stages;
    }
    public List<ProjectRequirement> allRequirements() { return orderedStages().stream().flatMap(s -> s.tasks().stream()).map(StageDefinition.Task::requirement).toList(); }
    public Optional<StageDefinition> stage(String id) { return orderedStages().stream().filter(s -> s.id().equals(id)).findFirst(); }

    public ProjectDefinition(String title, List<ResourceLocation> results) {
        this(title, "", results, List.of());
    }

    public Optional<ProjectRequirement> requirement(String id) {
        return allRequirements().stream().filter(r -> r.id().equals(id)).findFirst();
    }
}
