/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge.link;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedresearch.knowledge.model.*;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

/**
 * 数据包联络规则。局域槽位绑定互异知识身份，顺序不参与规则语义。
 */
public record LinkRule(List<Slot> inputs, List<CrossCondition> conditions, ResourceLocation output,
                       List<String> hints) {
    public static final Codec<LinkRule> CODEC = RecordCodecBuilder.create(i -> i.group(
            Slot.CODEC.listOf().fieldOf("inputs").forGetter(LinkRule::inputs),
            CrossCondition.CODEC.listOf().optionalFieldOf("conditions", List.of()).forGetter(LinkRule::conditions),
            ResourceLocation.CODEC.fieldOf("output").forGetter(LinkRule::output),
            Codec.STRING.listOf().optionalFieldOf("hints", List.of()).forGetter(LinkRule::hints)
    ).apply(i, LinkRule::new));

    public LinkRule {
        inputs = List.copyOf(inputs);
        conditions = List.copyOf(conditions);
        hints = List.copyOf(hints);
    }

    public LinkRule(List<Slot> inputs, List<CrossCondition> conditions, ResourceLocation output) {
        this(inputs, conditions, output, List.of());
    }

    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        if (inputs.size() < 2 || inputs.size() > 5) errors.add("Link rules require 2 to 5 input slots");
        if (conditions.size() > 5) errors.add("Link rules allow at most 5 cross conditions");
        Map<String, Slot> names = new LinkedHashMap<>();
        for (Slot slot : inputs) {
            if (slot.name.isBlank() || names.putIfAbsent(slot.name, slot) != null)
                errors.add("Empty or duplicate slot name: " + slot.name);
            slot.validate(errors);
        }
        for (CrossCondition condition : conditions) condition.validate(names, errors);
        return errors;
    }

    public record Slot(String name, KnowledgeKey.Kind kind, Optional<ResourceLocation> definition,
                       Optional<Observation.Type> observationType, List<FieldCondition> conditions) {
        public static final Codec<Slot> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("name").forGetter(Slot::name),
                KnowledgeKey.KIND_CODEC.fieldOf("kind").forGetter(Slot::kind),
                ResourceLocation.CODEC.optionalFieldOf("definition").forGetter(Slot::definition),
                KnowledgeKey.enumCodec(Observation.Type.class).optionalFieldOf("observation_type").forGetter(Slot::observationType),
                FieldCondition.CODEC.listOf().optionalFieldOf("conditions", List.of()).forGetter(Slot::conditions)
        ).apply(i, Slot::new));

        public Slot {
            conditions = List.copyOf(conditions);
        }

        public boolean matches(KnowledgeElement element, TagLookup tags) {
            if (element.key().kind() != kind) return false;
            if (kind != KnowledgeKey.Kind.OBSERVATION)
                return definition.map(id -> id.toString().equals(element.key().id())).orElse(false);
            return element.observation().filter(o -> observationType.map(t -> t == o.type()).orElse(true))
                    .filter(o -> conditions.stream().allMatch(c -> c.matches(o, tags))).isPresent();
        }

        public void validate(List<String> errors) {
            if (kind == KnowledgeKey.Kind.OBSERVATION) {
                if (definition.isPresent()) errors.add("Observation slots cannot reference a knowledge definition");
                for (FieldCondition c : conditions) c.validate(observationType, errors);
            } else if (definition.isEmpty() || observationType.isPresent() || !conditions.isEmpty())
                errors.add("Idea/result slots require definition and cannot read observation fields");
        }
    }

    public record CrossCondition(Kind kind, String left, String field, String right, String rightField,
                                 FieldCondition.Operator operator, Optional<Double> value) {
        public enum Kind {COMPARE, DISTANCE}

        public static final Codec<CrossCondition> CODEC = RecordCodecBuilder.create(i -> i.group(
                KnowledgeKey.enumCodec(Kind.class).fieldOf("kind").forGetter(CrossCondition::kind),
                Codec.STRING.fieldOf("left").forGetter(CrossCondition::left),
                Codec.STRING.optionalFieldOf("field", "").forGetter(CrossCondition::field),
                Codec.STRING.fieldOf("right").forGetter(CrossCondition::right),
                Codec.STRING.optionalFieldOf("right_field", "").forGetter(CrossCondition::rightField),
                KnowledgeKey.enumCodec(FieldCondition.Operator.class).fieldOf("operator").forGetter(CrossCondition::operator),
                Codec.DOUBLE.optionalFieldOf("value").forGetter(CrossCondition::value)
        ).apply(i, CrossCondition::new));

        public boolean matches(Map<String, KnowledgeElement> bindings) {
            KnowledgeElement a = bindings.get(left), b = bindings.get(right);
            if (a == null || b == null) return true; // Defer until both referenced slots are bound.
            if (a.observation().isEmpty() || b.observation().isEmpty()) return false;
            Observation oa = a.observation().get(), ob = b.observation().get();
            if (kind == Kind.COMPARE) return FieldCondition.compare(oa.value(field), ob.value(otherField()), operator);
            if (!FieldCondition.compare(oa.value("dimension"), ob.value("dimension"), FieldCondition.Operator.EQ))
                return false;
            double squared = 0;
            for (String axis : List.of("x", "y", "z")) {
                var x = oa.value(axis);
                var y = ob.value(axis);
                if (!x.known() || !y.known() || x.number().isEmpty() || y.number().isEmpty()) return false;
                double d = x.number().get() - y.number().get();
                squared += d * d;
            }
            return value.isPresent() && FieldCondition.compare(ObservationValue.known(Math.sqrt(squared)), ObservationValue.known(value.get()), operator);
        }

        public String otherField() {
            return rightField.isEmpty() ? field : rightField;
        }

        public Set<String> fieldsRead() {
            return kind == Kind.DISTANCE ? Set.of("dimension", "x", "y", "z") : Set.copyOf(List.of(field, otherField()));
        }

        void validate(Map<String, Slot> slots, List<String> errors) {
            Slot a = slots.get(left), b = slots.get(right);
            if (a == null || b == null) {
                errors.add("Cross condition references an unknown slot");
                return;
            }
            if (a.kind != KnowledgeKey.Kind.OBSERVATION || b.kind != KnowledgeKey.Kind.OBSERVATION) {
                errors.add("Cross field conditions require observation slots");
                return;
            }
            if (operator == FieldCondition.Operator.EXISTS || operator == FieldCondition.Operator.UNKNOWN || operator == FieldCondition.Operator.NOT_APPLICABLE || operator == FieldCondition.Operator.TAG)
                errors.add("Cross conditions require a comparison operator");
            if (kind == Kind.DISTANCE) {
                if (value.filter(v -> Double.isFinite(v) && v >= 0).isEmpty())
                    errors.add("Distance requires a finite nonnegative value");
                if (!field.isEmpty() || !rightField.isEmpty())
                    errors.add("Distance reads world coordinates, not a declared field");
                if (a.observationType.filter(t -> t == Observation.Type.ITEM).isPresent() || b.observationType.filter(t -> t == Observation.Type.ITEM).isPresent())
                    errors.add("Item observations have no world distance");
                return;
            }
            if (value.isPresent()) errors.add("Field comparison reads the other input, not a constant value");
            var at = ObservationFields.type(field);
            var bt = ObservationFields.type(otherField());
            if (at.isEmpty() || bt.isEmpty() || at.get() != bt.get())
                errors.add("Cross condition fields have unknown or incompatible types");
            else if (operator != FieldCondition.Operator.EQ && operator != FieldCondition.Operator.NE && at.get() != ObservationValue.ValueType.NUMBER)
                errors.add("Ordered cross comparison requires numeric fields");
            if (a.observationType.isPresent() && !ObservationFields.applies(field, a.observationType.get())
                    || b.observationType.isPresent() && !ObservationFields.applies(otherField(), b.observationType.get()))
                errors.add("Cross condition reads an inapplicable field");
        }
    }
}
