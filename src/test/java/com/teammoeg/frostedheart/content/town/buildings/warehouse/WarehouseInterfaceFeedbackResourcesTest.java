/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.buildings.warehouse;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WarehouseInterfaceFeedbackResourcesTest {
    private static final List<String> FACINGS = List.of("north", "east", "south", "west");
    private static final List<String> VISUAL_STATES =
            List.of("active", "disabled", "shortage", "unavailable");
    private static final List<String> TRANSPORT_DETAIL_KEYS = List.of(
            "transport_realtime",
            "transport_daily_report",
            "transport_daily_total",
            "transport_daily_reserved",
            "transport_daily_effective_rate",
            "transport_details_expand",
            "transport_details_collapse",
            "transport_endpoint",
            "transport_effective_warehouses",
            "transport_endpoint_rate",
            "transport_endpoint_distance",
            "transport_endpoint_metrics",
            "transport_endpoint_kind.warehouse_interface",
            "transport_admission.active",
            "transport_admission.disabled",
            "transport_admission.throttled");
    private static final List<String> INTERFACE_TRANSPORT_KEYS = List.of(
            "active",
            "throttled",
            "disabled",
            "rate_increase_rejected",
            "invalid_request",
            "current_rate",
            "reserved",
            "town_capacity");

    @Test
    void blockstateCoversEveryFacingAndFiniteVisualState() {
        JsonObject variants = readJson(
                "/assets/frostedheart/blockstates/warehouse_interface.json")
                .getAsJsonObject("variants");

        assertEquals(FACINGS.size() * VISUAL_STATES.size(), variants.size());
        for (String facing : FACINGS) {
            for (String visualState : VISUAL_STATES) {
                String variantKey = "facing=" + facing + ",transport_state=" + visualState;
                assertTrue(variants.has(variantKey), variantKey);
                String model = variants.getAsJsonObject(variantKey).get("model").getAsString();
                String[] modelId = model.split(":", 2);
                String modelPath = "/assets/" + modelId[0] + "/models/" + modelId[1] + ".json";
                assertNotNull(getClass().getResource(modelPath), modelPath);
            }
        }
    }

    @Test
    void mayorSealTransportDetailsAreLocalizedInBothLanguages() {
        for (String language : List.of("en_us", "zh_cn")) {
            JsonObject translations = readJson(
                    "/assets/frostedheart/lang/" + language + ".json");
            for (String suffix : TRANSPORT_DETAIL_KEYS) {
                String key = "gui.frostedheart.town_manager.virtual_resource." + suffix;
                assertTrue(translations.has(key), language + ": " + key);
            }
        }
    }

    @Test
    void warehouseInterfaceSingleRateFeedbackIsLocalizedInBothLanguages() {
        for (String language : List.of("en_us", "zh_cn")) {
            JsonObject translations = readJson(
                    "/assets/frostedheart/lang/" + language + ".json");
            for (String suffix : INTERFACE_TRANSPORT_KEYS) {
                String key = "gui.frostedheart.warehouse_interface.transport." + suffix;
                assertTrue(translations.has(key), language + ": " + key);
            }
            String messageKey = "message.frostedheart.warehouse_interface.transport.new_endpoint_rejected";
            assertTrue(translations.has(messageKey), language + ": " + messageKey);
        }
    }

    @Test
    void newEndpointFailureTargetsOnlyTheKnownOperator() {
        UUID operatorId = UUID.randomUUID();

        assertEquals(Optional.of(operatorId),
                WarehouseInterfaceBlockEntity.admissionFailureRecipient(true, operatorId));
        assertEquals(Optional.empty(),
                WarehouseInterfaceBlockEntity.admissionFailureRecipient(true, null));
        assertEquals(Optional.empty(),
                WarehouseInterfaceBlockEntity.admissionFailureRecipient(false, operatorId));
    }

    private JsonObject readJson(String path) {
        InputStream stream = getClass().getResourceAsStream(path);
        assertNotNull(stream, path);
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (java.io.IOException exception) {
            throw new AssertionError("Failed to read " + path, exception);
        }
    }
}
