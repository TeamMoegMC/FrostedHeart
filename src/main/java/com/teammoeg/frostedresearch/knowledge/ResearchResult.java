/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** Stable, data-authored outcomes produced by the generative research system. */
public sealed interface ResearchResult permits ResearchResult.Finding, ResearchResult.Design,
        ResearchResult.Construction, ResearchResult.Procedure, ResearchResult.Prototype {
    Codec<ResultType> RESULT_TYPE_CODEC = Codec.STRING.comapFlatMap(token -> {
        for (ResultType value : ResultType.values()) {
            if (value.token().equals(token)) return DataResult.success(value);
        }
        return DataResult.error(() -> "Unknown research result type: " + token);
    }, ResultType::token);
    Codec<ResearchResult> DISPATCH_CODEC = RESULT_TYPE_CODEC.dispatch(
            "type", ResearchResult::type, ResearchResult::codecFor);
    Codec<ResearchResult> CODEC = new Codec<>() {
        @Override
        public <T> DataResult<Pair<ResearchResult, T>> decode(DynamicOps<T> ops, T input) {
            DataResult<MapLike<T>> mapResult = ops.getMap(input);
            return mapResult.flatMap(map -> {
                T typeValue = map.get("type");
                if (typeValue == null) return DataResult.error(() -> "Missing research result type");
                return ops.getStringValue(typeValue).flatMap(type -> {
                    if ("construction".equals(type) && map.get("usable_blocks") != null) {
                        return DataResult.error(() -> "Construction cannot declare usable_blocks");
                    }
                    if ("procedure".equals(type) && map.get("multiblocks") != null) {
                        return DataResult.error(() -> "Procedure cannot declare multiblocks");
                    }
                    return DISPATCH_CODEC.decode(ops, input);
                });
            });
        }

        @Override
        public <T> DataResult<T> encode(ResearchResult input, DynamicOps<T> ops, T prefix) {
            return DISPATCH_CODEC.encode(input, ops, prefix);
        }
    };

    ResourceLocation id();

    ResultType type();

    private static Codec<? extends ResearchResult> codecFor(ResultType type) {
        return switch (type) {
            case FINDING -> Finding.CODEC.codec();
            case DESIGN -> Design.CODEC.codec();
            case CONSTRUCTION -> Construction.CODEC.codec();
            case PROCEDURE -> Procedure.CODEC.codec();
            case PROTOTYPE -> Prototype.CODEC.codec();
        };
    }

    enum ResultType {
        FINDING("finding"),
        DESIGN("design"),
        CONSTRUCTION("construction"),
        PROCEDURE("procedure"),
        PROTOTYPE("prototype");

        private final String token;

        ResultType(String token) {
            this.token = token;
        }

        public String token() {
            return token;
        }

        public static ResultType fromToken(String token) {
            for (ResultType value : values()) {
                if (value.token.equals(token)) return value;
            }
            throw new IllegalArgumentException("Unknown research result type: " + token);
        }
    }

    record Finding(ResourceLocation id, List<ResourceLocation> views) implements ResearchResult {
        static final MapCodec<Finding> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("id").forGetter(Finding::id),
                ResourceLocation.CODEC.listOf().optionalFieldOf("views", List.of()).forGetter(Finding::views)
        ).apply(instance, Finding::new));

        public Finding {
            views = List.copyOf(views);
        }

        @Override
        public ResultType type() {
            return ResultType.FINDING;
        }
    }

    record Design(ResourceLocation id, List<ResourceLocation> recipes) implements ResearchResult {
        static final MapCodec<Design> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("id").forGetter(Design::id),
                ResourceLocation.CODEC.listOf().fieldOf("recipes").forGetter(Design::recipes)
        ).apply(instance, Design::new));

        public Design {
            recipes = List.copyOf(recipes);
        }

        @Override
        public ResultType type() {
            return ResultType.DESIGN;
        }
    }

    record Construction(ResourceLocation id, List<ResourceLocation> multiblocks) implements ResearchResult {
        static final MapCodec<Construction> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("id").forGetter(Construction::id),
                ResourceLocation.CODEC.listOf().fieldOf("multiblocks").forGetter(Construction::multiblocks)
        ).apply(instance, Construction::new));

        public Construction {
            multiblocks = List.copyOf(multiblocks);
        }

        @Override
        public ResultType type() {
            return ResultType.CONSTRUCTION;
        }
    }

    record Procedure(ResourceLocation id, List<ResourceLocation> usableBlocks) implements ResearchResult {
        static final MapCodec<Procedure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("id").forGetter(Procedure::id),
                ResourceLocation.CODEC.listOf().fieldOf("usable_blocks").forGetter(Procedure::usableBlocks)
        ).apply(instance, Procedure::new));

        public Procedure {
            usableBlocks = List.copyOf(usableBlocks);
        }

        @Override
        public ResultType type() {
            return ResultType.PROCEDURE;
        }
    }

    record Prototype(ResourceLocation id, ResourceLocation profile) implements ResearchResult {
        static final MapCodec<Prototype> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("id").forGetter(Prototype::id),
                ResourceLocation.CODEC.fieldOf("profile").forGetter(Prototype::profile)
        ).apply(instance, Prototype::new));

        @Override
        public ResultType type() {
            return ResultType.PROTOTYPE;
        }
    }
}
