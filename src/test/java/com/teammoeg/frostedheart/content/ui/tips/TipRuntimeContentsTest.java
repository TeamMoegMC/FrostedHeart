/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.ui.tips;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.SharedConstants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TipRuntimeContentsTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void runtimeComponentsDoNotChangePersistedTipCodec() {
        Tip source = Tip.builder("runtime-test")
                .contents("persisted.translation.key")
                .components(Component.literal("private runtime text"))
                .temporary()
                .build();

        JsonObject encoded = Tip.CODEC.encodeStart(JsonOps.INSTANCE, source)
                .getOrThrow(false, message -> fail(message)).getAsJsonObject();
        assertFalse(encoded.has("runtimeContents"));
        assertFalse(encoded.toString().contains("private runtime text"));

        Tip decoded = Tip.CODEC.parse(JsonOps.INSTANCE, encoded)
                .getOrThrow(false, message -> fail(message));
        assertEquals(source.contents(), decoded.contents());
        assertTrue(decoded.runtimeContents().isEmpty());
    }
}
