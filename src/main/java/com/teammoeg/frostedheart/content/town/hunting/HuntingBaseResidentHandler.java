package com.teammoeg.frostedheart.content.town.hunting;

import com.teammoeg.frostedheart.content.town.TownWorkerStatus;
import com.teammoeg.frostedheart.content.town.TownWorkerType;
import com.teammoeg.frostedheart.content.town.WorkerResidentHandler;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.content.town.worker.TownWorkerData;

import net.minecraft.nbt.CompoundTag;

import static java.lang.Double.NEGATIVE_INFINITY;

public class HuntingBaseResidentHandler extends WorkerResidentHandler {
    private HuntingBaseResidentHandler(TownWorkerType type) {
        super(type);
    }
    public static final HuntingBaseResidentHandler INSTANCE = new HuntingBaseResidentHandler(TownWorkerType.HUNTING_BASE);

    @Override
    public double getResidentPriority(TownWorkerData workerData) {
        CompoundTag tileEntityNBT = workerData.getWorkData().getCompound("tileEntity");
        if(tileEntityNBT.getByte("workerState") != TownWorkerStatus.VALID.getStateNum()) return NEGATIVE_INFINITY;
        int maxResident = tileEntityNBT.getInt("maxResident");
        double rating = tileEntityNBT.getDouble("rating");
        int currentResidentNum = tileEntityNBT.getInt("currentResidentNum");
        if(currentResidentNum < maxResident) {
            return -currentResidentNum + (double) currentResidentNum / maxResident + 0.5/*the base priority of workerType*/ + rating;
        }
        return NEGATIVE_INFINITY;
    }

    @Override
    public double getResidentPriority(TownWorkerData workerData, int currentResidentNum) {
        CompoundTag tileEntityNBT = workerData.getWorkData().getCompound("tileEntity");
        if(tileEntityNBT.getByte("workerState") != TownWorkerStatus.VALID.getStateNum()) return NEGATIVE_INFINITY;
        int maxResident = tileEntityNBT.getInt("maxResident");
        if(currentResidentNum > maxResident) return NEGATIVE_INFINITY;
        double rating = tileEntityNBT.getDouble("rating");
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
