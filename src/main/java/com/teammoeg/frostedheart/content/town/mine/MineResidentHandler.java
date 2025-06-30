package com.teammoeg.frostedheart.content.town.mine;

import com.teammoeg.frostedheart.content.town.TownWorkerData;
import com.teammoeg.frostedheart.content.town.TownWorkerState;
import com.teammoeg.frostedheart.content.town.TownWorkerType;
import com.teammoeg.frostedheart.content.town.WorkerResidentHandler;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import net.minecraft.nbt.CompoundTag;

import static java.lang.Double.NEGATIVE_INFINITY;

public class MineResidentHandler extends WorkerResidentHandler {
    private MineResidentHandler(TownWorkerType type){
        super(type);
    }

    public static final MineResidentHandler INSTANCE = new MineResidentHandler(TownWorkerType.MINE);

    @Override
    public double getResidentPriority(TownWorkerData workerData) {
        CompoundTag tileEntityNBT = workerData.getWorkData().getCompound("tileEntity");
        if(tileEntityNBT.getByte("workerState") != TownWorkerState.VALID.getStateNum()) return NEGATIVE_INFINITY;
        int maxResident = tileEntityNBT.getInt("maxResident");
        int currentResidentNum = tileEntityNBT.getInt("currentResidentNum");
        if(currentResidentNum > maxResident) return NEGATIVE_INFINITY;
        double rating = tileEntityNBT.getDouble("rating");
        return -currentResidentNum + 1.0 * currentResidentNum / maxResident + 0.4/*the base priority of workerType*/ + rating;
    }

    @Override
    public double getResidentPriority(TownWorkerData workerData, int currentResidentNum) {
        CompoundTag tileEntityNBT = workerData.getWorkData().getCompound("tileEntity");
        if(tileEntityNBT.getByte("workerState") != TownWorkerState.VALID.getStateNum()) return NEGATIVE_INFINITY;
        int maxResident = tileEntityNBT.getInt("maxResident");
        if(currentResidentNum > maxResident) return NEGATIVE_INFINITY;
        double rating = tileEntityNBT.getDouble("rating");
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
