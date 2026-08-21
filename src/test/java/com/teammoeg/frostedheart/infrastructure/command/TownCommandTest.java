/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.infrastructure.command;

import com.mojang.brigadier.tree.CommandNode;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import net.minecraft.commands.CommandSourceStack;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TownCommandTest {
    @Test
    void parsesAllOptionalResidentAgeNames() {
        assertEquals(Resident.AGE_INFANT, TownCommand.parseResidentAge("infant"));
        assertEquals(Resident.AGE_CHILD, TownCommand.parseResidentAge("child"));
        assertEquals(Resident.AGE_ADULT, TownCommand.parseResidentAge("ADULT"));
        assertEquals(Resident.AGE_ELDER, TownCommand.parseResidentAge("elder"));
        assertEquals(-1, TownCommand.parseResidentAge("unknown"));
    }

    @Test
    void residentAddArgumentsAreOptionalInDocumentedOrder() {
        CommandNode<CommandSourceStack> add = TownCommand.residentAddCommand().build();
        assertNotNull(add.getCommand());
        CommandNode<CommandSourceStack> count = requiredChild(add, "count");
        CommandNode<CommandSourceStack> age = requiredChild(count, "age");
        CommandNode<CommandSourceStack> firstName = requiredChild(age, "first_name");
        CommandNode<CommandSourceStack> lastName = requiredChild(firstName, "last_name");
        assertNotNull(count.getCommand());
        assertNotNull(age.getCommand());
        assertNotNull(firstName.getCommand());
        assertNotNull(lastName.getCommand());
    }

    @Test
    void fixedFullNamesReceiveOneBasedBatchOrdinals() {
        TownCommand.ResidentName name = TownCommand.resolveResidentName(
                "Ada", "Lovelace", 3,
                () -> { throw new AssertionError("first-name pool should not be used"); },
                () -> { throw new AssertionError("last-name pool should not be used"); });
        assertEquals("Ada 3", name.firstName());
        assertEquals("Lovelace", name.lastName());
    }

    @Test
    void omittedNamePartsUseTheirRandomPools() {
        TownCommand.ResidentName firstOnly = TownCommand.resolveResidentName(
                "Ada", null, 1, () -> "RandomFirst", () -> "RandomLast");
        assertEquals("Ada", firstOnly.firstName());
        assertEquals("RandomLast", firstOnly.lastName());

        TownCommand.ResidentName lastOnly = TownCommand.resolveResidentName(
                null, "Lovelace", 1, () -> "RandomFirst", () -> "RandomLast");
        assertEquals("RandomFirst", lastOnly.firstName());
        assertEquals("Lovelace", lastOnly.lastName());

        TownCommand.ResidentName neither = TownCommand.resolveResidentName(
                null, null, 1, () -> "RandomFirst", () -> "RandomLast");
        assertEquals("RandomFirst", neither.firstName());
        assertEquals("RandomLast", neither.lastName());
    }

    @Test
    void batchStopsAtFirstHousingRejectionAndReturnsAddedCount() {
        List<Integer> attempted = new ArrayList<>();
        int added = TownCommand.addUntilRejected(5, ordinal -> ordinal, ordinal -> {
            attempted.add(ordinal);
            return ordinal < 3;
        });
        assertEquals(2, added);
        assertEquals(List.of(1, 2, 3), attempted);
        assertThrows(IllegalArgumentException.class,
                () -> TownCommand.addUntilRejected(0, ordinal -> ordinal, ordinal -> true));
    }

    private static CommandNode<CommandSourceStack> requiredChild(
            CommandNode<CommandSourceStack> parent,
            String name
    ) {
        CommandNode<CommandSourceStack> child = parent.getChild(name);
        assertNotNull(child);
        return child;
    }
}
