/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.town.model;

import com.teammoeg.frostedheart.content.town.TownCareLaw;
import com.teammoeg.frostedheart.content.town.resident.ResidentNutrition;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TownResidentCareModelTest {
    private static final UUID DEPENDENT = new UUID(0, 1);
    private static final UUID WORKER = new UUID(0, 2);

    @Test
    void assessmentCombinesHealthMentalAndWorstNutritionChannel() {
        TownResidentCareModel.Need need = TownResidentCareModel.assess(
                DEPENDENT, 0, 90, 90,
                new ResidentNutrition(10, 70, 70, 70),
                1, 20, 20, 0, 0);

        assertFalse(need.labourCapable());
        assertTrue(need.critical());
        assertEquals(1, need.severeNutritionChannels());
        assertEquals(1.0 - 10.0 / 70.0, need.primaryRisk(), 1.0e-12);
    }

    @Test
    void careLawChangesTheFirstResidentialSortingGroup() {
        TownResidentCareModel.Need dependent = new TownResidentCareModel.Need(
                DEPENDENT, false, false, 0.2, 0.2, 0);
        TownResidentCareModel.Need worker = new TownResidentCareModel.Need(
                WORKER, true, false, 0.8, 0.8, 0);

        assertEquals(List.of(worker, dependent), List.of(dependent, worker).stream()
                .sorted(TownResidentCareModel.comparator(TownCareLaw.CLINICAL_TRIAGE)).toList());
        assertEquals(List.of(dependent, worker), List.of(dependent, worker).stream()
                .sorted(TownResidentCareModel.comparator(TownCareLaw.DEPENDENT_FIRST)).toList());
        assertEquals(List.of(worker, dependent), List.of(dependent, worker).stream()
                .sorted(TownResidentCareModel.comparator(TownCareLaw.WORKFORCE_FIRST)).toList());
    }

    @Test
    void currentHouseWinsOnlyInsideTheSameRiskBand() {
        TownResidentCareModel.Need incumbent = new TownResidentCareModel.Need(
                DEPENDENT, false, false, 0.51, 0.51, 0);
        TownResidentCareModel.Need newcomer = new TownResidentCareModel.Need(
                WORKER, false, false, 0.53, 0.53, 0);
        var comparator = TownResidentCareModel.comparatorForHouse(
                TownCareLaw.CLINICAL_TRIAGE, DEPENDENT::equals);

        assertEquals(List.of(incumbent, newcomer), List.of(newcomer, incumbent).stream()
                .sorted(comparator).toList());
    }
}
