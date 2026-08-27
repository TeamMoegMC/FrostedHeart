/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.chorda.client.icon.CIcons;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

/** Phase-one slice of a future complete research topic definition. */
public record ResearchTopicDefinition(
        int format,
        Presentation presentation,
        List<ResearchResult> results,
        List<ItemReward> rewards,
        Legacy legacy,
        List<IdeaSource> ideaSources,
        Optional<Inspiration> inspiration,
        List<Protocol> protocols,
        Optional<Resolution> resolution) {
    public static final int CURRENT_FORMAT = 3;
    public static final Codec<ResearchTopicDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("format").forGetter(ResearchTopicDefinition::format),
            Presentation.CODEC.optionalFieldOf("presentation", Presentation.EMPTY).forGetter(ResearchTopicDefinition::presentation),
            ResearchResult.CODEC.listOf().optionalFieldOf("results", List.of()).forGetter(ResearchTopicDefinition::results),
            ItemReward.CODEC.listOf().optionalFieldOf("rewards", List.of()).forGetter(ResearchTopicDefinition::rewards),
            Legacy.CODEC.optionalFieldOf("legacy", Legacy.NONE).forGetter(ResearchTopicDefinition::legacy),
            IdeaSource.CODEC.listOf().optionalFieldOf("idea_sources", List.of()).forGetter(ResearchTopicDefinition::ideaSources),
            Inspiration.CODEC.optionalFieldOf("inspiration").forGetter(ResearchTopicDefinition::inspiration),
            Protocol.CODEC.listOf().optionalFieldOf("protocols", List.of()).forGetter(ResearchTopicDefinition::protocols),
            Resolution.CODEC.optionalFieldOf("resolution").forGetter(ResearchTopicDefinition::resolution)
    ).apply(instance, ResearchTopicDefinition::new));

    public ResearchTopicDefinition {
        results = List.copyOf(results);
        rewards = List.copyOf(rewards);
        ideaSources = List.copyOf(ideaSources);
        protocols = List.copyOf(protocols);
    }

    public ResearchTopicDefinition(int format, Presentation presentation,
            List<ResearchResult> results, List<ItemReward> rewards) {
        this(format, presentation, results, rewards, Legacy.NONE, List.of(), Optional.empty(), List.of(), Optional.empty());
    }

    public record Presentation(Optional<CIcons.CIcon> icon) {
        public static final Presentation EMPTY = new Presentation(Optional.empty());
        public static final Codec<Presentation> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                CIcons.CODEC.optionalFieldOf("icon").forGetter(Presentation::icon)
        ).apply(instance, Presentation::new));
    }

    public record ItemReward(ResourceLocation item, int count) {
        public static final Codec<ItemReward> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("item").forGetter(ItemReward::item),
                Codec.INT.optionalFieldOf("count", 1).forGetter(ItemReward::count)
        ).apply(instance, ItemReward::new));
    }

    public record Legacy(Mode mode) {
        public static final Legacy NONE = new Legacy(Mode.NONE);
        public static final Codec<Legacy> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.optionalFieldOf("mode", "none").xmap(Mode::fromToken, Mode::token)
                        .forGetter(value -> value.mode)
        ).apply(instance, Legacy::new));

        public enum Mode {
            NONE("none"),
            COEXIST("coexist");

            private final String token;
            Mode(String token) { this.token = token; }
            public String token() { return token; }
            public static Mode fromToken(String token) {
                return java.util.Arrays.stream(values()).filter(value -> value.token.equals(token)).findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Unknown legacy mode " + token));
            }
        }
    }

    public record IdeaSource(ResourceLocation provider, ResourceLocation idea,
            List<ResourceLocation> requiredTags) {
        public static final Codec<IdeaSource> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("provider").forGetter(IdeaSource::provider),
                ResourceLocation.CODEC.fieldOf("idea").forGetter(IdeaSource::idea),
                ResourceLocation.CODEC.listOf().optionalFieldOf("required_tags", List.of()).forGetter(IdeaSource::requiredTags)
        ).apply(instance, IdeaSource::new));
        public IdeaSource { requiredTags = List.copyOf(requiredTags); }
    }

    public record Inspiration(ResourceLocation provider, ResourceLocation idea, int paperLevel) {
        public static final Codec<Inspiration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("provider").forGetter(Inspiration::provider),
                ResourceLocation.CODEC.fieldOf("idea").forGetter(Inspiration::idea),
                Codec.INT.optionalFieldOf("paper_level", 0).forGetter(Inspiration::paperLevel)
        ).apply(instance, Inspiration::new));
    }

    public record Protocol(ResourceLocation id, ResourceLocation resolver, List<String> outcomes) {
        public static final Codec<Protocol> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("id").forGetter(Protocol::id),
                ResourceLocation.CODEC.fieldOf("resolver").forGetter(Protocol::resolver),
                Codec.STRING.listOf().optionalFieldOf("outcomes", List.of()).forGetter(Protocol::outcomes)
        ).apply(instance, Protocol::new));
        public Protocol { outcomes = List.copyOf(outcomes); }
    }

    public record Resolution(ResourceLocation resolver, ResourceLocation idea,
            List<ResourceLocation> results) {
        public static final Codec<Resolution> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("resolver").forGetter(Resolution::resolver),
                ResourceLocation.CODEC.fieldOf("idea").forGetter(Resolution::idea),
                ResourceLocation.CODEC.listOf().fieldOf("results").forGetter(Resolution::results)
        ).apply(instance, Resolution::new));
        public Resolution { results = List.copyOf(results); }
    }
}
