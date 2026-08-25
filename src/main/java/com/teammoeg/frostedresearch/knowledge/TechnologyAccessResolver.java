/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge;

import blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler.IMultiblock;
import com.teammoeg.chorda.dataholders.team.TeamDataHolder;
import com.teammoeg.frostedresearch.FHResearch;
import com.teammoeg.frostedresearch.FRSpecialDataTypes;
import com.teammoeg.frostedresearch.ResearchHooks;
import com.teammoeg.frostedresearch.api.KnowledgeDataAPI;
import com.teammoeg.frostedresearch.data.ResearchData;
import com.teammoeg.frostedresearch.data.TeamKnowledgeData;
import com.teammoeg.frostedresearch.data.TeamResearchData;
import com.teammoeg.frostedresearch.research.Research;
import com.teammoeg.frostedresearch.research.effects.Effect;
import com.teammoeg.frostedresearch.research.effects.EffectBuilding;
import com.teammoeg.frostedresearch.research.effects.EffectCrafting;
import com.teammoeg.frostedresearch.research.effects.EffectUse;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Builds the sole access read model consumed by gameplay, machines and JEI. */
public final class TechnologyAccessResolver {
    private TechnologyAccessResolver() {
    }

    public static KnowledgeProjection projectKnowledge(TeamKnowledgeData data) {
        List<KnowledgeProjection.FindingEntry> findings = new ArrayList<>();
        ResearchResultCatalog.Snapshot catalog = ResearchResultCatalog.current();
        for (ResourceLocation resultId : data.findingIds()) {
            ResearchResultCatalog.ResultEntry entry = catalog.result(resultId);
            if (entry != null && entry.result() instanceof ResearchResult.Finding finding) {
                AccessSource.ResultSource source = resultSource(entry);
                findings.add(new KnowledgeProjection.FindingEntry(finding.id(), finding.views(), source));
            }
        }
        return new KnowledgeProjection(findings);
    }

    public static TechnologyAccessProjection project(TeamDataHolder team) {
        TeamKnowledgeData knowledge = team.getData(FRSpecialDataTypes.KNOWLEDGE_DATA);
        TeamResearchData legacy = team.getData(FRSpecialDataTypes.RESEARCH_DATA);
        return project(knowledge, legacy);
    }

    public static TechnologyAccessProjection project(TeamKnowledgeData knowledge, TeamResearchData legacy) {
        ResearchResultCatalog.Snapshot catalog = ResearchResultCatalog.current();
        Set<ResourceLocation> managedRecipes = new LinkedHashSet<>(catalog.managedRecipes());
        Set<ResourceLocation> managedMultiblocks = new LinkedHashSet<>(catalog.managedMultiblocks());
        Set<ResourceLocation> managedBlocks = new LinkedHashSet<>(catalog.managedBlocks());
        ResearchHooks.getLockList(ResearchHooks.RECIPE_UNLOCK_LIST).forEach(recipe -> managedRecipes.add(recipe.getId()));
        ResearchHooks.getLockList(ResearchHooks.MULTIBLOCK_UNLOCK_LIST).forEach(multiblock ->
                managedMultiblocks.add(multiblock.getUniqueName()));
        ResearchHooks.getLockList(ResearchHooks.BLOCK_UNLOCK_LIST).forEach(block -> {
            ResourceLocation id = ForgeRegistries.BLOCKS.getKey(block);
            if (id != null) managedBlocks.add(id);
        });

        Map<ResourceLocation, List<AccessSource>> recipeSources = new LinkedHashMap<>();
        Map<ResourceLocation, List<AccessSource>> multiblockSources = new LinkedHashMap<>();
        Map<ResourceLocation, List<AccessSource>> blockSources = new LinkedHashMap<>();
        addResultSources(knowledge, catalog, recipeSources, multiblockSources, blockSources);
        addLegacySources(legacy, recipeSources, multiblockSources, blockSources);
        return TechnologyAccessProjection.create(managedRecipes, managedMultiblocks, managedBlocks,
                recipeSources, multiblockSources, blockSources);
    }

    public static boolean hasFinding(UUID teamId, ResourceLocation findingId) {
        return KnowledgeDataAPI.getData(teamId)
                .map(data -> projectKnowledge(data.get()).hasFinding(findingId)).orElse(false);
    }

    public static boolean isRecipeUnlocked(UUID teamId, ResourceLocation recipeId) {
        return projection(teamId).recipe(recipeId).allowed();
    }

    public static boolean canFormMultiblock(UUID teamId, ResourceLocation multiblockId) {
        return projection(teamId).multiblock(multiblockId).allowed();
    }

    public static boolean canUseBlock(UUID teamId, ResourceLocation blockId) {
        return projection(teamId).block(blockId).allowed();
    }

    public static TechnologyAccessProjection projection(UUID teamId) {
        if (teamId == null) return project(new TeamKnowledgeData(), new TeamResearchData());
        TeamDataHolder holder = com.teammoeg.chorda.dataholders.team.CTeamDataManager.INSTANCE.get(teamId);
        return holder == null ? project(new TeamKnowledgeData(), new TeamResearchData()) : project(holder);
    }

    private static void addResultSources(TeamKnowledgeData knowledge, ResearchResultCatalog.Snapshot catalog,
            Map<ResourceLocation, List<AccessSource>> recipeSources,
            Map<ResourceLocation, List<AccessSource>> multiblockSources,
            Map<ResourceLocation, List<AccessSource>> blockSources) {
        for (ResourceLocation id : knowledge.designIds()) {
            ResearchResultCatalog.ResultEntry entry = catalog.result(id);
            if (entry != null && entry.result() instanceof ResearchResult.Design design) {
                design.recipes().forEach(target -> add(recipeSources, target, resultSource(entry)));
            }
        }
        for (ResourceLocation id : knowledge.constructionIds()) {
            ResearchResultCatalog.ResultEntry entry = catalog.result(id);
            if (entry != null && entry.result() instanceof ResearchResult.Construction construction) {
                construction.multiblocks().forEach(target -> add(multiblockSources, target, resultSource(entry)));
            }
        }
        for (ResourceLocation id : knowledge.procedureIds()) {
            ResearchResultCatalog.ResultEntry entry = catalog.result(id);
            if (entry != null && entry.result() instanceof ResearchResult.Procedure procedure) {
                procedure.usableBlocks().forEach(target -> add(blockSources, target, resultSource(entry)));
            }
        }
    }

    private static void addLegacySources(TeamResearchData legacy,
            Map<ResourceLocation, List<AccessSource>> recipeSources,
            Map<ResourceLocation, List<AccessSource>> multiblockSources,
            Map<ResourceLocation, List<AccessSource>> blockSources) {
        for (Research research : FHResearch.getAllResearch()) {
            ResearchData state = legacy.getExistingData(research).orElse(null);
            if (state == null) continue;
            if (!state.isCompleted()) continue;
            for (Effect effect : research.getEffects()) {
                if (!state.isEffectGranted(effect)) continue;
                AccessSource source = new AccessSource.LegacySource(research.getId(), effect.getNonce());
                if (effect instanceof EffectCrafting crafting) {
                    for (Recipe<?> recipe : crafting.getUnlocks()) add(recipeSources, recipe.getId(), source);
                } else if (effect instanceof EffectBuilding building) {
                    IMultiblock multiblock = building.getMultiblock();
                    if (multiblock != null) add(multiblockSources, multiblock.getUniqueName(), source);
                } else if (effect instanceof EffectUse use) {
                    for (Block block : use.getBlocks()) {
                        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(block);
                        if (id != null) add(blockSources, id, source);
                    }
                }
            }
        }
    }

    private static AccessSource.ResultSource resultSource(ResearchResultCatalog.ResultEntry entry) {
        return new AccessSource.ResultSource(entry.topicId(), entry.result().type(), entry.result().id());
    }

    private static void add(Map<ResourceLocation, List<AccessSource>> sources,
            ResourceLocation target, AccessSource source) {
        List<AccessSource> values = sources.computeIfAbsent(target, ignored -> new ArrayList<>());
        if (!values.contains(source)) values.add(source);
    }
}
