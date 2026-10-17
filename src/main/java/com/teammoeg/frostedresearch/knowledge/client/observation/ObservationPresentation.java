/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge.client.observation;

import com.teammoeg.frostedresearch.knowledge.client.KnowledgePresentation;
import com.teammoeg.frostedresearch.knowledge.model.*;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;

import java.util.*;

import static com.teammoeg.frostedresearch.knowledge.client.KnowledgePresentation.t;

/**
 * 观察札记的可读正文；精确测量放入次级详情。 / A readable field note with optional precise details.
 */
public final class ObservationPresentation {
    private ObservationPresentation() {
    }

    public static Component title(Observation observation) {
        return KnowledgePresentation.object(observation);
    }

    public static List<Component> summary(Observation o) {
        List<Component> lines = new ArrayList<>();
        if (o.type() == Observation.Type.ITEM) {
            lines.add(t("observation.item"));
            return List.copyOf(lines);
        }
        String biome = string(o, "biome"), dimension = string(o, "dimension");
        if (!biome.isEmpty() || !dimension.isEmpty())
            lines.add(t("observation.place", KnowledgePresentation.place(biome, false), KnowledgePresentation.place(dimension, true)));
        if (knownNumber(o, "year") && knownNumber(o, "month") && knownNumber(o, "day")) {
            Component date = t("observation.date", integer(o, "year"), integer(o, "month"), integer(o, "day"));
            String period = string(o, "time_period");
            if (!period.isEmpty()) date = date.copy().append(" · ").append(named("period", period));
            lines.add(date);
        } else lines.add(t("observation.date_unknown"));
        String climate = string(o, "climate"), temperature = string(o, "temperature_type");
        if (!climate.isEmpty() && !climate.equals("none")) lines.add(named("climate", climate));
        if (!temperature.isEmpty()) lines.add(named("temperature", temperature));
        String lit = string(o, "block_state.lit"), open = string(o, "block_state.open");
        if (!lit.isEmpty()) lines.add(t(lit.equals("true") ? "observation.lit" : "observation.unlit"));
        if (open.equals("true")) lines.add(t("observation.open"));
        return List.copyOf(lines);
    }

    public static List<Component> details(Observation o) {
        List<Component> lines = new ArrayList<>();
        if (o.type() != Observation.Type.ITEM) {
            if (knownNumber(o, "x") && knownNumber(o, "y") && knownNumber(o, "z"))
                lines.add(t("observation.position", integer(o, "x"), integer(o, "y"), integer(o, "z")));
            if (knownNumber(o, "temperature"))
                lines.add(t("observation.measured_temperature", String.format(Locale.ROOT, "%.1f", o.value("temperature").number().orElseThrow())));
        }
        lines.add(t("observation.author", KnowledgePresentation.observer(o.source().originalObserver())));
        for (var entry : o.values().entrySet()) {
            String key = "knowledge.frostedresearch.field." + entry.getKey();
            if (!(entry.getKey().startsWith("context.") || entry.getKey().startsWith("field.") || entry.getKey().startsWith("item.")) || !Language.getInstance().has(key) || !entry.getValue().known())
                continue;
            ObservationValue value = entry.getValue();
            Component shown = value.number().isPresent() ? Component.literal(String.format(Locale.ROOT, "%.1f", value.number().get())) : KnowledgePresentation.text(value.text().orElse(""));
            lines.add(t("observation.detail", Component.translatable(key), shown));
        }
        return List.copyOf(lines);
    }

    public static List<Component> describe(Observation o) {
        List<Component> result = new ArrayList<>(summary(o));
        result.addAll(details(o));
        return List.copyOf(result);
    }

    private static Component named(String kind, String token) {
        String key = "gui.frostedresearch.journal." + kind + "." + token;
        return Language.getInstance().has(key) ? Component.translatable(key) : t("observation.unrecorded");
    }

    private static String string(Observation o, String field) {
        var v = o.value(field);
        return v.known() ? v.text().orElse("") : "";
    }

    private static boolean knownNumber(Observation o, String field) {
        var v = o.value(field);
        return v.known() && v.number().isPresent();
    }

    private static String integer(Observation o, String field) {
        return String.format(Locale.ROOT, "%.0f", o.value(field).number().orElse(0d));
    }
}
