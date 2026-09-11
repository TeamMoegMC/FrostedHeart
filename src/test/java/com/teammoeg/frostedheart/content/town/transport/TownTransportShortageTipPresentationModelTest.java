/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.transport;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TownTransportShortageTipPresentationModelTest {
    @Test
    void presentationLimitsRowsAndUsesStableLocalizationKeys() {
        List<TownTransportShortageNotice> notices = List.of(
                notice(1.0, 2.0), notice(2.0, 4.0),
                notice(3.0, 6.0), notice(4.0, 8.0));

        TownTransportShortageTipPresentationModel.Presentation presentation =
                TownTransportShortageTipPresentationModel.create(notices);

        assertEquals(3, presentation.visibleNotices().size());
        assertEquals(1, presentation.overflowCount());
        assertEquals(TownTransportShortageTipPresentationModel.TITLE_KEY,
                presentation.titleKey());
        assertEquals("12.35",
                TownTransportShortageTipPresentationModel.formatCapacity(12.345));
        assertEquals("80.0%",
                TownTransportShortageTipPresentationModel.formatScale(0.8));
    }

    @Test
    void englishAndChineseResourcesContainEveryPresentationAndActionKey() {
        for (String resource : List.of(
                "/assets/frostedheart/lang/en_us.json",
                "/assets/frostedheart/lang/zh_cn.json")) {
            JsonObject language = readLanguage(resource);
            assertTrue(language.has(TownTransportShortageTipPresentationModel.TITLE_KEY));
            assertTrue(language.has(TownTransportShortageTipPresentationModel.DETAIL_KEY));
            assertTrue(language.has(TownTransportShortageTipPresentationModel.OVERFLOW_KEY));
            assertTrue(language.has("tips.frostedheart.click_action.open_town_transport"));
        }
    }

    private static TownTransportShortageNotice notice(double total, double reserved) {
        return TownTransportShortageNotice.from(total, reserved).orElseThrow();
    }

    private static JsonObject readLanguage(String resource) {
        var stream = TownTransportShortageTipPresentationModelTest.class
                .getResourceAsStream(resource);
        assertNotNull(stream, resource);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (java.io.IOException exception) {
            throw new AssertionError(exception);
        }
    }
}
