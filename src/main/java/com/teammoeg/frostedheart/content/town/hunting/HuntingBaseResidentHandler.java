package com.teammoeg.frostedheart.content.town.hunting;

import static java.lang.Double.*;

import com.teammoeg.frostedheart.content.town.TownWorkerStatus;
import com.teammoeg.frostedheart.content.town.TownWorkerType;
import com.teammoeg.frostedheart.content.town.WorkerResidentHandler;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.content.town.worker.TownWorkerData;

public class HuntingBaseResidentHandler extends WorkerResidentHandler {
    private HuntingBaseResidentHandler(TownWorkerType type) {
        super(type);
    }
    public static final HuntingBaseResidentHandler INSTANCE = new HuntingBaseResidentHandler(TownWorkerType.HUNTING_BASE);

    @Override
    public double getResidentPriority(TownWorkerData workerData) {
        HuntingBaseState state = (HuntingBaseState)workerData.getState();
        if(state.status != TownWorkerStatus.VALID) return NEGATIVE_INFINITY;
        int maxResident = state.maxResidents;
        double rating = state.getRating();
        int currentResidentNum = state.getResidents().size();
        if(currentResidentNum < maxResident) {
            return -currentResidentNum + (double) currentResidentNum / maxResident + 0.5/*the base priority of workerType*/ + rating;
        }
        return NEGATIVE_INFINITY;
    }

    @Override
    public double getResidentPriority(TownWorkerData workerData, int currentResidentNum) {
    	HuntingBaseState state = (HuntingBaseState)workerData.getState();
        if(state.status != TownWorkerStatus.VALID) return NEGATIVE_INFINITY;
        int maxResident = state.maxResidents;
        if(currentResidentNum > maxResident) return NEGATIVE_INFINITY;
        double rating = state.getRating();
        return -currentResidentNum + (double) currentResidentNum / maxResident + 0.5/*the base priority of workerType*/ + rating;
    }

    @Override
    public double getResidentScore(Resident resident) {
        double healthPart =  1 / (1 + Math.exp(-resident.getHealth() * 0.09 + 5.5 )) + 0.028;
        double mentalPart = 0.2 + 0.8 * Math.sqrt(resident.getMental() / 100);
        double strengthPart = 0.3 + 0.7 * WorkerResidentHandler.CalculatingFunction1(resident.getStrength());
        double intelligencePart = 0.8 + 0.2 * WorkerResidentHandler.CalculatingFunction1(resident.getIntelligence());
        double workProficiencyPart = 0.1 + 0.9 * WorkerResidentHandler.CalculatingFunction1(resident.getWorkProficiency(this.type));
        return healthPart * mentalPart * strengthPart * intelligencePart * workProficiencyPart;
    }
}
