/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart.content.ui.tips;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import com.teammoeg.chorda.util.CUtils;

import net.minecraft.network.chat.Component;

class TipRuntimeContentsTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        CUtils.startTestEnvironment();
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
