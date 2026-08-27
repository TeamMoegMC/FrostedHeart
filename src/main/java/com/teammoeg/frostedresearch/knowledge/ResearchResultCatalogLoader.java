/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge;

import blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.teammoeg.frostedresearch.ResearchCatalog;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/** Reads and validates the result-bearing slice of datapack research topics. */
public final class ResearchResultCatalogLoader {
    static final String TOPIC_DIRECTORY = "frostedresearch/topics";
    static final String PROFILE_DIRECTORY = "frostedresearch/prototypes";

    private ResearchResultCatalogLoader() {
    }

    public static ResearchResultCatalog.Candidate load(ResourceManager resources, RecipeManager recipes) {
        List<String> diagnostics = new ArrayList<>();
        Map<ResourceLocation, PrototypeProfileDefinition> profiles = decodeProfiles(resources, diagnostics);
        Map<ResourceLocation, ResearchTopicDefinition> topics = decodeTopics(resources, diagnostics);
        Set<ResourceLocation> blockTags = listDefinitionIds(resources, "tags/blocks");
        validate(topics, profiles, recipes, diagnostics, blockTags::contains);
        diagnostics.sort(String::compareTo);
        if (!diagnostics.isEmpty()) throw new ResearchResultCatalog.ValidationException(diagnostics);
        return new ResearchResultCatalog.Candidate(topics, profiles);
    }

    private static Map<ResourceLocation, PrototypeProfileDefinition> decodeProfiles(
            ResourceManager resources, List<String> diagnostics) {
        Map<ResourceLocation, PrototypeProfileDefinition> definitions = new LinkedHashMap<>();
        listJson(resources, PROFILE_DIRECTORY).forEach((resourceId, resource) -> {
            ResourceLocation id = definitionId(resourceId, PROFILE_DIRECTORY);
            try (Reader reader = resource.openAsReader()) {
                JsonElement json = JsonParser.parseReader(reader);
                PrototypeProfileDefinition definition = PrototypeProfileDefinition.CODEC.parse(JsonOps.INSTANCE, json)
                        .resultOrPartial(message -> diagnostics.add(id + ": " + message)).orElse(null);
                if (definition != null) definitions.put(id, definition);
            } catch (Exception exception) {
                diagnostics.add(id + ": " + exception.getMessage());
            }
        });
        return definitions;
    }

    private static Map<ResourceLocation, ResearchTopicDefinition> decodeTopics(
            ResourceManager resources, List<String> diagnostics) {
        Map<ResourceLocation, ResearchTopicDefinition> definitions = new LinkedHashMap<>();
        listJson(resources, TOPIC_DIRECTORY).forEach((resourceId, resource) -> {
            ResourceLocation id = definitionId(resourceId, TOPIC_DIRECTORY);
            try (Reader reader = resource.openAsReader()) {
                JsonElement json = JsonParser.parseReader(reader);
                validateExclusiveFields(id, json, diagnostics);
                ResearchTopicDefinition definition = ResearchTopicDefinition.CODEC.parse(JsonOps.INSTANCE, json)
                        .resultOrPartial(message -> diagnostics.add(id + ": " + message)).orElse(null);
                if (definition != null) definitions.put(id, definition);
            } catch (Exception exception) {
                diagnostics.add(id + ": " + exception.getMessage());
            }
        });
        return definitions;
    }

    private static Map<ResourceLocation, Resource> listJson(ResourceManager resources, String directory) {
        Map<ResourceLocation, Resource> listed = resources.listResources(directory,
                id -> id.getPath().endsWith(".json"));
        Map<ResourceLocation, Resource> sorted = new LinkedHashMap<>();
        listed.entrySet().stream().sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> sorted.put(entry.getKey(), entry.getValue()));
        return sorted;
    }

    private static ResourceLocation definitionId(ResourceLocation resourceId, String directory) {
        String path = resourceId.getPath();
        String relative = path.substring(directory.length() + 1, path.length() - ".json".length());
        return new ResourceLocation(resourceId.getNamespace(), relative);
    }

    private static Set<ResourceLocation> listDefinitionIds(ResourceManager resources, String directory) {
        Set<ResourceLocation> ids = new LinkedHashSet<>();
        resources.listResources(directory, id -> id.getPath().endsWith(".json")).keySet().stream()
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .map(id -> definitionId(id, directory))
                .forEach(ids::add);
        return Set.copyOf(ids);
    }

    private static void validateExclusiveFields(ResourceLocation topicId, JsonElement json, List<String> diagnostics) {
        if (!json.isJsonObject()) return;
        JsonElement results = json.getAsJsonObject().get("results");
        if (results == null || !results.isJsonArray()) return;
        for (int index = 0; index < results.getAsJsonArray().size(); index++) {
            JsonElement element = results.getAsJsonArray().get(index);
            if (!element.isJsonObject()) continue;
            JsonObject result = element.getAsJsonObject();
            String type = result.has("type") ? result.get("type").getAsString() : "";
            if ("construction".equals(type) && result.has("usable_blocks")) {
                diagnostics.add(topicId + "/result[" + index + "]: Construction cannot declare usable_blocks");
            }
            if ("procedure".equals(type) && result.has("multiblocks")) {
                diagnostics.add(topicId + "/result[" + index + "]: Procedure cannot declare multiblocks");
            }
        }
    }

    static void validate(Map<ResourceLocation, ResearchTopicDefinition> topics,
            Map<ResourceLocation, PrototypeProfileDefinition> profiles,
            RecipeManager recipes, List<String> diagnostics) {
        validate(topics, profiles, recipes, diagnostics, tag -> {
            net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> key =
                    net.minecraft.tags.BlockTags.create(tag);
            return ForgeRegistries.BLOCKS.tags().isKnownTagName(key);
        });
    }

    static void validate(Map<ResourceLocation, ResearchTopicDefinition> topics,
            Map<ResourceLocation, PrototypeProfileDefinition> profiles,
            RecipeManager recipes, List<String> diagnostics, Set<ResourceLocation> knownBlockTags) {
        validate(topics, profiles, recipes, diagnostics, knownBlockTags::contains);
    }

    private static void validate(Map<ResourceLocation, ResearchTopicDefinition> topics,
            Map<ResourceLocation, PrototypeProfileDefinition> profiles,
            RecipeManager recipes, List<String> diagnostics, Predicate<ResourceLocation> knownBlockTag) {
        Map<ResourceLocation, ResourceLocation> resultOwners = new HashMap<>();
        profiles.forEach((id, profile) -> {
            if (profile.format() != PrototypeProfileDefinition.CURRENT_FORMAT) {
                diagnostics.add(id + ": prototype format must be " + PrototypeProfileDefinition.CURRENT_FORMAT);
            }
            if (profile.revision() <= 0) diagnostics.add(id + ": prototype revision must be positive");
        });
        topics.forEach((topicId, topic) -> {
            if (topic.format() != ResearchTopicDefinition.CURRENT_FORMAT) {
                diagnostics.add(topicId + ": topic format must be " + ResearchTopicDefinition.CURRENT_FORMAT);
            }
            for (ResearchTopicDefinition.ItemReward reward : topic.rewards()) {
                if (!ForgeRegistries.ITEMS.containsKey(reward.item())) {
                    diagnostics.add(topicId + ": unknown reward item " + reward.item());
                }
                if (reward.count() <= 0) diagnostics.add(topicId + ": reward count must be positive");
            }
            validateWorkflow(topicId, topic, diagnostics, knownBlockTag);
            for (ResearchResult result : topic.results()) {
                ResourceLocation previous = resultOwners.putIfAbsent(result.id(), topicId);
                if (previous != null) {
                    diagnostics.add(result.id() + ": duplicate result id in " + previous + " and " + topicId);
                }
                if (result.id().toString().length() > ResearchCatalog.MAX_STABLE_ID_LENGTH) {
                    diagnostics.add(result.id() + ": result id exceeds " + ResearchCatalog.MAX_STABLE_ID_LENGTH + " characters");
                }
                validateResult(topicId, result, profiles, recipes, diagnostics);
            }
        });
    }

    private static void validateWorkflow(ResourceLocation topicId, ResearchTopicDefinition topic,
            List<String> diagnostics, Predicate<ResourceLocation> knownBlockTag) {
        Set<ResourceLocation> resultIds = topic.results().stream().map(ResearchResult::id)
                .collect(java.util.stream.Collectors.toSet());
        for (ResearchTopicDefinition.IdeaSource source : topic.ideaSources()) {
            if (!ResearchWorkflowRegistry.hasIdeaSource(source.provider())) {
                diagnostics.add(topicId + ": unknown idea provider " + source.provider());
            }
            for (ResourceLocation tag : source.requiredTags()) {
                if (!knownBlockTag.test(tag)) {
                    diagnostics.add(topicId + ": unknown block tag " + tag);
                }
            }
        }
        topic.inspiration().ifPresent(inspiration -> {
            if (!ResearchWorkflowRegistry.hasInspirationProvider(inspiration.provider())) {
                diagnostics.add(topicId + ": unknown inspiration provider " + inspiration.provider());
            }
            if (inspiration.paperLevel() < 0) diagnostics.add(topicId + ": paper_level must not be negative");
        });
        for (ResearchTopicDefinition.Protocol protocol : topic.protocols()) {
            if (!ResearchWorkflowRegistry.hasProtocol(protocol.resolver())) {
                diagnostics.add(topicId + ": unknown protocol resolver " + protocol.resolver());
            }
            if (protocol.outcomes().isEmpty()) diagnostics.add(topicId + ": protocol outcomes must not be empty");
            for (String outcome : protocol.outcomes()) {
                if (!ResearchWorkflowRegistry.COMPARISON_OUTCOMES.contains(outcome)) {
                    diagnostics.add(topicId + ": unknown protocol outcome " + outcome);
                }
            }
        }
        topic.resolution().ifPresent(resolution -> {
            if (!ResearchWorkflowRegistry.hasResolution(resolution.resolver())) {
                diagnostics.add(topicId + ": unknown resolution resolver " + resolution.resolver());
            }
            if (resolution.results().isEmpty()) diagnostics.add(topicId + ": resolution results must not be empty");
            for (ResourceLocation result : resolution.results()) {
                if (!resultIds.contains(result)) diagnostics.add(topicId + ": resolution references unknown result " + result);
            }
        });
        for (ResearchResult result : topic.results()) {
            if (result instanceof ResearchResult.Finding finding) {
                for (ResourceLocation view : finding.views()) {
                    if (!ResearchWorkflowRegistry.hasFindingView(view)) {
                        diagnostics.add(topicId + "/" + result.id() + ": unknown finding view " + view);
                    }
                }
            }
        }
    }

    private static void validateResult(ResourceLocation topicId, ResearchResult result,
            Map<ResourceLocation, PrototypeProfileDefinition> profiles,
            RecipeManager recipes, List<String> diagnostics) {
        String owner = topicId + "/" + result.id();
        if (result instanceof ResearchResult.Design design) {
            validateNonEmpty(owner, "recipes", design.recipes(), diagnostics);
            validateDuplicates(owner, "recipe", design.recipes(), diagnostics);
            for (ResourceLocation recipe : design.recipes()) {
                if (recipes.byKey(recipe).isEmpty()) diagnostics.add(owner + ": unknown recipe " + recipe);
            }
        } else if (result instanceof ResearchResult.Construction construction) {
            validateNonEmpty(owner, "multiblocks", construction.multiblocks(), diagnostics);
            validateDuplicates(owner, "multiblock", construction.multiblocks(), diagnostics);
            for (ResourceLocation multiblock : construction.multiblocks()) {
                if (MultiblockHandler.getByUniqueName(multiblock) == null) {
                    diagnostics.add(owner + ": unknown multiblock " + multiblock);
                }
            }
        } else if (result instanceof ResearchResult.Procedure procedure) {
            validateNonEmpty(owner, "usable_blocks", procedure.usableBlocks(), diagnostics);
            validateDuplicates(owner, "usable block", procedure.usableBlocks(), diagnostics);
            for (ResourceLocation block : procedure.usableBlocks()) {
                if (!ForgeRegistries.BLOCKS.containsKey(block)) diagnostics.add(owner + ": unknown block " + block);
            }
        } else if (result instanceof ResearchResult.Prototype prototype && !profiles.containsKey(prototype.profile())) {
            diagnostics.add(owner + ": unknown prototype profile " + prototype.profile());
        }
    }

    private static void validateNonEmpty(String owner, String field, List<?> values, List<String> diagnostics) {
        if (values.isEmpty()) diagnostics.add(owner + ": " + field + " must not be empty");
    }

    private static void validateDuplicates(String owner, String kind,
            List<ResourceLocation> values, List<String> diagnostics) {
        Set<ResourceLocation> seen = new LinkedHashSet<>();
        for (ResourceLocation value : values) {
            if (!seen.add(value)) diagnostics.add(owner + ": duplicate " + kind + " " + value);
        }
    }
}
