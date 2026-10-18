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

package com.teammoeg.chorda.dataholders.team;

import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SinglePlayerTeamTest {
    @Test
    void presentMemberReturnsTheOnlineMember() {
        Object onlineMember = new Object();

        Collection<Object> members = SinglePlayerTeam.presentMember(onlineMember);

        assertEquals(1, members.size());
        assertSame(onlineMember, members.iterator().next());
    }

    @Test
    void presentMemberReturnsEmptyForAnOfflineMember() {
        assertTrue(SinglePlayerTeam.presentMember(null).isEmpty());
    }
}
