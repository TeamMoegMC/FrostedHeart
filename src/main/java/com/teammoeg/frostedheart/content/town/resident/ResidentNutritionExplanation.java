/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.resident;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.List;

/** Builds resident nutrition explanations from the persisted settlement snapshot. */
public final class ResidentNutritionExplanation {
    private ResidentNutritionExplanation() {
    }

    public static List<Component> lines(Resident resident) {
        List<Component> lines = new ArrayList<>();
        ResidentNutritionSnapshot snapshot = resident.getNutritionSnapshot();
        if (!snapshot.hasData()) {
            lines.add(Component.translatable("gui.frostedheart.resident.no_nutrition_snapshot")
                    .withStyle(ChatFormatting.GRAY));
            return lines;
        }
        lines.add(delta("gui.frostedheart.resident.health_recovery",
                snapshot.recovery().healthRecovery()));
        lines.add(delta("gui.frostedheart.resident.mental_recovery",
                snapshot.recovery().mentalRecovery()));
        lines.add(percent("gui.frostedheart.resident.physical_activity",
                snapshot.activity().physical()));
        lines.add(percent("gui.frostedheart.resident.learning_activity",
                snapshot.activity().learning()));
        lines.add(percent("gui.frostedheart.resident.strength_effective_activity",
                snapshot.strength().effectiveActivity()));
        lines.add(delta("gui.frostedheart.resident.strength_growth",
                snapshot.strength().growth()));
        lines.add(delta("gui.frostedheart.resident.strength_nutrition_decay",
                -snapshot.strength().nutritionDecay()));
        lines.add(delta("gui.frostedheart.resident.strength_age_decay",
                -snapshot.strength().ageDecay()));
        lines.add(delta("gui.frostedheart.resident.strength_net_change",
                snapshot.strength().netChange()));
        lines.add(percent("gui.frostedheart.resident.intelligence_effective_activity",
                snapshot.intelligence().effectiveActivity()));
        lines.add(delta("gui.frostedheart.resident.intelligence_growth",
                snapshot.intelligence().growth()));
        lines.add(delta("gui.frostedheart.resident.intelligence_nutrition_decay",
                -snapshot.intelligence().nutritionDecay()));
        lines.add(delta("gui.frostedheart.resident.intelligence_age_decay",
                -snapshot.intelligence().ageDecay()));
        lines.add(delta("gui.frostedheart.resident.intelligence_net_change",
                snapshot.intelligence().netChange()));

        ResidentNutritionSupportModel.Weights weights = Resident.nutritionSupportWeights().normalized();
        ResidentNutritionSupportModel.WeightRow combinedWeights =
                new ResidentNutritionSupportModel.WeightRow(
                        weights.health().protein() + weights.mental().protein()
                                + weights.strength().protein() + weights.intelligence().protein(),
                        weights.health().fat() + weights.mental().fat()
                                + weights.strength().fat() + weights.intelligence().fat(),
                        weights.health().vegetable() + weights.mental().vegetable()
                                + weights.strength().vegetable() + weights.intelligence().vegetable(),
                        weights.health().carbohydrate() + weights.mental().carbohydrate()
                                + weights.strength().carbohydrate() + weights.intelligence().carbohydrate());
        List<ResidentNutritionSupportModel.Nutrient> limits =
                ResidentNutritionSupportModel.limitingNutrients(
                        snapshot.satisfaction(), combinedWeights, 2);
        if (limits.isEmpty()) {
            lines.add(Component.translatable("gui.frostedheart.resident.nutrition_supported")
                    .withStyle(ChatFormatting.GREEN));
        } else {
            MutableComponent line = Component.translatable(
                    "gui.frostedheart.resident.main_limits").append(Component.literal(": "));
            for (int i = 0; i < limits.size(); i++) {
                if (i > 0) line.append(Component.literal(", "));
                line.append(Component.translatable(limits.get(i).translationKey()));
            }
            lines.add(line.withStyle(ChatFormatting.YELLOW));
        }
        return List.copyOf(lines);
    }

    private static Component percent(String key, double value) {
        return Component.translatable(key).append(Component.literal(
                ": " + Math.round(Math.max(0.0, Math.min(1.0, finite(value))) * 100.0) + "%"));
    }

    private static Component delta(String key, double value) {
        double safe = finite(value);
        return Component.translatable(key).append(Component.literal(
                ": " + String.format(java.util.Locale.ROOT, "%+.2f", safe)));
    }

    private static double finite(double value) {
        return Double.isFinite(value) ? value : 0.0;
    }
}
