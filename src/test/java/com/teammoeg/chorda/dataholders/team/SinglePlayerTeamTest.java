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
