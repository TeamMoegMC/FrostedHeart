/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.town.resident;

import com.teammoeg.frostedheart.content.utility.oredetect.GeologyResearchIntegration;
import com.teammoeg.frostedresearch.knowledge.PersonKnowledgePackageCatalog;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/** Frosted Heart content packages carried by refugees and residents. */
public final class PersonKnowledgeIntegration {
    public static final ResourceLocation PROSPECTING_EXPERIENCE =
            id("prospecting_experience");
    public static final ResourceLocation COLD_WEATHER_EXPERIENCE =
            id("cold_weather_experience");

    private PersonKnowledgeIntegration() {
    }

    public static void register() {
        PersonKnowledgePackageCatalog.register(new PersonKnowledgePackageCatalog.Definition(
                PROSPECTING_EXPERIENCE, 0, 10,
                "message.frostedheart.person_knowledge.shared_prospecting_experience",
                Optional.of(new PersonKnowledgePackageCatalog.OfferTemplate(
                        GeologyResearchIntegration.TOPIC, GeologyResearchIntegration.IDEA))));
        PersonKnowledgePackageCatalog.register(new PersonKnowledgePackageCatalog.Definition(
                COLD_WEATHER_EXPERIENCE, 10, 20,
                "message.frostedheart.person_knowledge.shared_cold_weather_experience",
                Optional.empty()));
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation("frostedheart", path);
    }
}
