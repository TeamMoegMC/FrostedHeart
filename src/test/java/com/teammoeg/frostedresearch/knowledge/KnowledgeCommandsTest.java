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

/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class KnowledgeCommandsTest {
    private static CommandDispatcher<CommandSourceStack> dispatcher;
    private static CommandSourceStack operator;

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        dispatcher = new CommandDispatcher<>();
        dispatcher.register(KnowledgeCommands.tree());
        operator = new CommandSourceStack(CommandSource.NULL, Vec3.ZERO, Vec2.ZERO, null, 2, "test", Component.literal("test"), null, null);
    }

    private static CommandContext<CommandSourceStack> parse(String command) {
        var parse = dispatcher.parse(command, operator);
        assertFalse(parse.getReader().canRead(), () -> command + " stops before " + parse.getReader().getRemaining() + " " + parse.getExceptions());
        assertNotNull(parse.getContext().getCommand(), command + " is incomplete");
        return parse.getContext().build(command);
    }

    @Test
    void definitionIdsNeedNoQuotesAndObservationsTakeRecordUuids() {
        assertEquals(new ResourceLocation("knowledge_example", "pressure_plate_design"),
                ResourceLocationArgument.getId(parse("knowledge forget result knowledge_example:pressure_plate_design"), "id"));
        assertEquals(new ResourceLocation("knowledge_example", "pressure"),
                ResourceLocationArgument.getId(parse("knowledge grant idea knowledge_example:pressure"), "id"));
        UUID record = UUID.randomUUID();
        assertEquals(record, UuidArgument.getUuid(parse("knowledge status observation " + record), "id"));
        assertTrue(dispatcher.parse("knowledge forget project knowledge_example:pressure_control", operator).getReader().canRead(),
                "projects are not knowledge identities");
    }
}
