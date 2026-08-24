/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.transport.device;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.teammoeg.frostedheart.content.town.transport.P2PRouteCardState;
import com.teammoeg.frostedheart.content.town.transport.P2PTerminalEndpoint;
import com.teammoeg.frostedheart.content.town.transport.P2PTerminalRole;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class P2PTerminalResourcesTest {
    private static final List<String> TERMINALS = List.of(
            "shipping_terminal",
            "receiving_terminal",
            "bidirectional_logistics_terminal");

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void terminalBlockstatesReferenceCompleteRoleSpecificModels() {
        for (String terminal : TERMINALS) {
            JsonObject variants = readJson("/assets/frostedheart/blockstates/"
                    + terminal + ".json").getAsJsonObject("variants");
            assertEquals(6, variants.size());
            for (String facing : List.of("north", "east", "south", "west", "up", "down")) {
                String key = "facing=" + facing;
                assertTrue(variants.has(key), terminal + ": " + key);
                String model = variants.getAsJsonObject(key).get("model").getAsString();
                assertEquals("frostedheart:block/" + terminal, model);
            }
            assertEquals(270, variants.getAsJsonObject("facing=up").get("x").getAsInt());
            assertEquals(90, variants.getAsJsonObject("facing=down").get("x").getAsInt());
            assertNotNull(getClass().getResource(
                    "/assets/frostedheart/models/block/" + terminal + ".json"));
            assertNotNull(getClass().getResource(
                    "/assets/frostedheart/models/item/" + terminal + ".json"));
        }
        assertNotNull(getClass().getResource(
                "/assets/frostedheart/models/item/freight_route_card.json"));
    }

    @Test
    void allSixDirectionsKeepTheInventoryFaceOppositeTheFront() {
        assertEquals(6, P2PTerminalBlock.FACING.getPossibleValues().size());
        for (Direction direction : Direction.values()) {
            assertTrue(P2PTerminalBlock.FACING.getPossibleValues().contains(direction));
            assertEquals(direction.getOpposite(),
                    P2PTerminalBlock.inventoryConnectionFace(direction));
            assertTrue(P2PTerminalBlockEntity.exposesExternalInventoryOn(
                    P2PTerminalRole.BIDIRECTIONAL, direction));
            assertFalse(P2PTerminalBlockEntity.exposesExternalInventoryOn(
                    P2PTerminalRole.SHIPPING, direction));
            assertFalse(P2PTerminalBlockEntity.exposesExternalInventoryOn(
                    P2PTerminalRole.RECEIVING, direction));
        }
        assertTrue(P2PTerminalBlockEntity.exposesExternalInventoryOn(
                P2PTerminalRole.BIDIRECTIONAL, null));
    }

    @Test
    void modelsDistinguishTheContainerFaceFromFreightStationSides() {
        Map<String, String> endpointPorts = Map.of(
                "shipping_terminal", "shipping_terminal_port",
                "receiving_terminal", "receiving_terminal_port");
        for (Map.Entry<String, String> entry : endpointPorts.entrySet()) {
            String terminal = entry.getKey();
            JsonObject textures = readJson("/assets/frostedheart/models/block/"
                    + terminal + ".json").getAsJsonObject("textures");
            assertEquals("frostedheart:block/" + entry.getValue(),
                    textures.get("south").getAsString(), terminal);
            for (String side : List.of("particle", "down", "up", "west", "east")) {
                assertEquals("frostedheart:block/transport_station",
                        textures.get(side).getAsString(), terminal + ": " + side);
            }
        }

        JsonObject bidirectionalTextures = readJson(
                "/assets/frostedheart/models/block/bidirectional_logistics_terminal.json")
                .getAsJsonObject("textures");
        for (String side : List.of("particle", "down", "up", "south", "west", "east")) {
            assertEquals("frostedheart:block/transport_station",
                    bidirectionalTextures.get(side).getAsString(), side);
        }
    }

    @Test
    void everyReferencedTerminalTextureIsA16PixelAsset() throws IOException {
        List<String> blockTextures = List.of(
                "transport_station",
                "shipping_terminal_port",
                "receiving_terminal_port",
                "shipping_terminal_front",
                "receiving_terminal_front",
                "bidirectional_logistics_terminal_front");
        for (String texture : blockTextures) {
            assertTexture16("/assets/frostedheart/textures/block/" + texture + ".png");
        }
        assertTexture16("/assets/frostedheart/textures/item/freight_route_card.png");
        assertTexture16("/assets/frostedheart/textures/item/freight_route_card_selected.png");
    }

    @Test
    void endpointPortRoleColorsAreConfinedToASixPixelSquareBorder() throws IOException {
        BufferedImage shipping = readImage(
                "/assets/frostedheart/textures/block/shipping_terminal_port.png");
        BufferedImage receiving = readImage(
                "/assets/frostedheart/textures/block/receiving_terminal_port.png");
        int differences = 0;
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                if (shipping.getRGB(x, y) == receiving.getRGB(x, y)) {
                    continue;
                }
                differences++;
                assertTrue(x >= 5 && x <= 10 && y >= 5 && y <= 10,
                        "role color outside 6x6 port at " + x + "," + y);
                assertTrue(x == 5 || x == 10 || y == 5 || y == 10,
                        "role color inside port opening at " + x + "," + y);
            }
        }
        assertEquals(20, differences);
        int shippingCorner = shipping.getRGB(5, 5);
        int receivingCorner = receiving.getRGB(5, 5);
        assertTrue((shippingCorner & 0xff) > (shippingCorner >> 16 & 0xff));
        assertTrue((receivingCorner >> 16 & 0xff) > (receivingCorner & 0xff));
    }

    @Test
    void selectedRouteCardUsesItsOwnModelOverride() {
        JsonObject model = readJson(
                "/assets/frostedheart/models/item/freight_route_card.json");
        JsonObject override = model.getAsJsonArray("overrides").get(0).getAsJsonObject();
        assertEquals(1, override.getAsJsonObject("predicate")
                .get("custom_model_data").getAsInt());
        assertEquals("frostedheart:item/freight_route_card_selected",
                override.get("model").getAsString());
        assertNotNull(getClass().getResource(
                "/assets/frostedheart/models/item/freight_route_card_selected.json"));
    }

    @Test
    void terminalLayoutsHaveCompleteRoleAndControlLocalization() {
        for (String locale : List.of("zh_cn", "en_us")) {
            JsonObject lang = readJson("/assets/frostedheart/lang/" + locale + ".json");
            for (String key : List.of(
                    "container.frostedheart.shipping_terminal",
                    "container.frostedheart.receiving_terminal",
                    "container.frostedheart.bidirectional_logistics_terminal",
                    "gui.frostedheart.p2p_terminal.filter",
                    "gui.frostedheart.p2p_terminal.input_filter",
                    "gui.frostedheart.p2p_terminal.output_filter",
                    "gui.frostedheart.p2p_terminal.rate_adjust_hint",
                    "gui.frostedheart.p2p_terminal.position",
                    "gui.frostedheart.p2p_terminal.connection_flow",
                    "gui.frostedheart.p2p_terminal.connection_flow_bidirectional",
                    "gui.frostedheart.p2p_terminal.connection_flow_source",
                    "gui.frostedheart.p2p_terminal.connection_flow_target_rate",
                    "gui.frostedheart.p2p_terminal.role.local",
                    "gui.frostedheart.p2p_terminal.pending_items",
                    "gui.frostedheart.p2p_terminal.received_items",
                    "gui.frostedheart.p2p_terminal.status.receiver_container_unavailable",
                    "gui.frostedheart.p2p_terminal.status.peer_unloaded",
                    "tooltip.frostedheart.freight_route_card.selected",
                    "tooltip.frostedheart.freight_route_card.selected_hint")) {
                assertTrue(lang.has(key), locale + ": " + key);
            }
            String capacity = lang.get("gui.frostedheart.p2p_terminal.capacity").getAsString();
            assertEquals(2, capacity.split("%s", -1).length - 1, locale);
        }
    }

    @Test
    void freightRouteCardStateRoundTripsAndClearsWithoutResidualAuthority() {
        ItemStack stack = new ItemStack(Items.PAPER);
        P2PTerminalEndpoint endpoint = new P2PTerminalEndpoint(
                GlobalPos.of(Level.OVERWORLD, new BlockPos(3, 64, 5)),
                P2PTerminalRole.SHIPPING);

        FreightRouteCardItem.setState(stack, P2PRouteCardState.selected(endpoint));
        assertEquals(endpoint, FreightRouteCardItem.getState(stack)
                .selectedEndpoint().orElseThrow());
        assertEquals(1, stack.getTag().getInt("CustomModelData"));

        UUID connectionId = new UUID(4L, 7L);
        FreightRouteCardItem.setState(stack, P2PRouteCardState.connected(connectionId));
        assertEquals(connectionId, FreightRouteCardItem.getState(stack)
                .connectionId().orElseThrow());
        assertTrue(FreightRouteCardItem.getState(stack).selectedEndpoint().isEmpty());
        assertFalse(stack.getTag().contains("CustomModelData"));

        FreightRouteCardItem.setState(stack, P2PRouteCardState.EMPTY);
        assertEquals(P2PRouteCardState.EMPTY, FreightRouteCardItem.getState(stack));
        assertFalse(stack.hasTag());
    }

    private void assertTexture16(String path) throws IOException {
        BufferedImage image = readImage(path);
        assertEquals(16, image.getWidth(), path);
        assertEquals(16, image.getHeight(), path);
    }

    private BufferedImage readImage(String path) throws IOException {
        try (InputStream stream = getClass().getResourceAsStream(path)) {
            assertNotNull(stream, path);
            BufferedImage image = ImageIO.read(stream);
            assertNotNull(image, path);
            return image;
        }
    }

    private JsonObject readJson(String path) {
        InputStream stream = getClass().getResourceAsStream(path);
        assertNotNull(stream, path);
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException exception) {
            throw new AssertionError("Failed to read " + path, exception);
        }
    }
}
