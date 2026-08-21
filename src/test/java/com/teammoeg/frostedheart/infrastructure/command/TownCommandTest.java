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

import com.teammoeg.frostedheart.content.town.resident.Resident;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TownCommandTest {
    @Test
    void parsesAllOptionalResidentAgeNames() {
        assertEquals(Resident.AGE_INFANT, TownCommand.parseResidentAge("infant"));
        assertEquals(Resident.AGE_CHILD, TownCommand.parseResidentAge("child"));
        assertEquals(Resident.AGE_ADULT, TownCommand.parseResidentAge("ADULT"));
        assertEquals(Resident.AGE_ELDER, TownCommand.parseResidentAge("elder"));
        assertEquals(-1, TownCommand.parseResidentAge("unknown"));
    }
}
