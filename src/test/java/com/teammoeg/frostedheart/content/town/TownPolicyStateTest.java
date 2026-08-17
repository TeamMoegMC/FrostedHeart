/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.town;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TownPolicyStateTest {
    @Test
    void acceptedChoiceStaysPendingUntilSettlementAndStartsGlobalCooldown() {
        TownPolicyState.EditResult edit = TownPolicyState.DEFAULT.requestCareLaw(
                TownCareLaw.DEPENDENT_FIRST, 12);

        assertTrue(edit.changed());
        assertEquals(TownCareLaw.CLINICAL_TRIAGE, edit.state().careLaw());
        assertEquals(TownCareLaw.DEPENDENT_FIRST, edit.state().displayedCareLaw());
        assertEquals(7, edit.state().remainingCooldown(12));

        TownPolicyState activated = edit.state().activatePending();
        assertEquals(TownCareLaw.DEPENDENT_FIRST, activated.careLaw());
        assertFalse(activated.hasPendingChanges());
        assertFalse(activated.requestCareLaw(TownCareLaw.WORKFORCE_FIRST, 18).changed());
        assertTrue(activated.requestCareLaw(TownCareLaw.WORKFORCE_FIRST, 19).changed());
    }

    @Test
    void unknownSavedOptionFallsBackToClinicalTriage() {
        TownPolicyState state = new TownPolicyState(
                Map.of(TownPolicyState.RESIDENTIAL_CARE, "removed_option"), Map.of(), 0);

        assertEquals(TownCareLaw.CLINICAL_TRIAGE, state.careLaw());
    }
}
