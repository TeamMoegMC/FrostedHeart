package com.teammoeg.frostedheart.content.town.mine;

import com.teammoeg.frostedheart.content.town.TownWorkerStatus;
import com.teammoeg.frostedheart.content.town.TownWorkerType;
import com.teammoeg.frostedheart.content.town.WorkerResidentHandler;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.content.town.worker.TownWorkerData;
import com.teammoeg.frostedheart.content.town.worker.WorkerState;

import net.minecraft.nbt.CompoundTag;

import static java.lang.Double.NEGATIVE_INFINITY;

public class MineResidentHandler extends WorkerResidentHandler {
    private MineResidentHandler(TownWorkerType type){
        super(type);
    }

    public static final MineResidentHandler INSTANCE = new MineResidentHandler(TownWorkerType.MINE);

    @Override
    public double getResidentPriority(TownWorkerData workerData) {
        MineBaseState state = (MineBaseState) workerData.getState();
        if(state.status != TownWorkerStatus.VALID) return NEGATIVE_INFINITY;
        int maxResident = state.maxResidents;
        int currentResidentNum = state.getResidents().size();
        if(currentResidentNum > maxResident) return NEGATIVE_INFINITY;
        double rating = state.getRating();
        return -currentResidentNum + 1.0 * currentResidentNum / maxResident + 0.4/*the base priority of workerType*/ + rating;
    }

    @Override
    public double getResidentPriority(TownWorkerData workerData, int currentResidentNum) {
    	MineBaseState state = (MineBaseState) workerData.getState();
    	if(state.status != TownWorkerStatus.VALID) return NEGATIVE_INFINITY;
        int maxResident = state.maxResidents;
        if(currentResidentNum > maxResident) return NEGATIVE_INFINITY;
        double rating = state.getRating();
        return -currentResidentNum + 1.0 * currentResidentNum / maxResident + 0.4/*the base priority of workerType*/ + rating;
    }


    @Override
    public double getResidentScore(Resident resident) {
        double healthPart = WorkerResidentHandler.CalculatingFunction2(resident.getHealth(), 0.12);
        double mentalPart = 0.6 + 0.4 * (0.524+0.5*(1-Math.exp(-0.03*resident.getMental())));
        double strengthPart = 0.3 + 0.7 * WorkerResidentHandler.CalculatingFunction1(resident.getStrength());
        double proficiencyPart = 0.6 + 0.4 * WorkerResidentHandler.CalculatingFunction1(resident.getWorkProficiency(TownWorkerType.MINE));
        return healthPart * mentalPart * strengthPart * proficiencyPart;
    }
}
