package com.teammoeg.frostedresearch.knowledge.state;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedresearch.knowledge.model.*;
import com.teammoeg.frostedresearch.knowledge.definition.ProjectRequirement;
import java.util.*;
import java.util.stream.Collectors;

/** Unmapped flat-run progress is retained for diagnosis, never credited to future stages. */
public record LegacyStageProgress(Map<String,List<Integer>> calculations,Map<String,Map<String,Observation>> evidence,
        Map<String,Integer> experiments,Map<String,ProjectRequirement.Sample> samples) {
    public static final Codec<LegacyStageProgress> CODEC=RecordCodecBuilder.create(i -> i.group(
        field(Codec.INT.listOf(),"calculations").forGetter(LegacyStageProgress::calculations),
        field(Codec.unboundedMap(Codec.STRING,Observation.CODEC),"evidence").forGetter(LegacyStageProgress::evidence),
        field(Codec.INT,"experiments").forGetter(LegacyStageProgress::experiments),
        field(ProjectRequirement.Sample.CODEC,"samples").forGetter(LegacyStageProgress::samples)
    ).apply(i,LegacyStageProgress::new));
    private static <T> MapCodec<Map<String,T>> field(Codec<T> value,String name) {
        return KnowledgeKey.strictOptionalField(Codec.unboundedMap(Codec.STRING,value),name).xmap(v -> v.orElse(Map.of()),v -> v.isEmpty() ? Optional.empty() : Optional.of(v));
    }
    public LegacyStageProgress {
        calculations=calculations.entrySet().stream().collect(Collectors.toUnmodifiableMap(Map.Entry::getKey,e -> List.copyOf(e.getValue())));
        evidence=evidence.entrySet().stream().collect(Collectors.toUnmodifiableMap(Map.Entry::getKey,e -> Map.copyOf(e.getValue())));
        experiments=Map.copyOf(experiments);samples=Map.copyOf(samples);
    }
    public static LegacyStageProgress empty() { return new LegacyStageProgress(Map.of(),Map.of(),Map.of(),Map.of()); }
    public boolean isEmpty() { return calculations.isEmpty() && evidence.isEmpty() && experiments.isEmpty() && samples.isEmpty(); }
    public static <T> Map<String,T> select(Map<String,T> source,Set<String> ids,boolean included) {
        return source.entrySet().stream().filter(e -> ids.contains(e.getKey())==included).collect(Collectors.toUnmodifiableMap(Map.Entry::getKey,Map.Entry::getValue));
    }
    public static LegacyStageProgress excluded(ResearchHistory run,Set<String> ids) {
        return new LegacyStageProgress(select(run.calculations(),ids,false),select(run.evidence(),ids,false),select(run.experiments(),ids,false),select(run.samples(),ids,false));
    }
}
