/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.town;

import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.content.town.worker.TownWorkerData;

import net.minecraft.nbt.*;

import java.util.UUID;

/**
 * 为需要居民工作的工作方块数据准备，统一处理居民相关内容
 */
public abstract class WorkerResidentHandler {

    public static final WorkerResidentHandler DUMMY = new WorkerResidentHandler(TownWorkerType.DUMMY) {
        @Override
        public double getResidentPriority(TownWorkerData workerData) {
            return 0;
        }

        @Override
        public double getResidentPriority(TownWorkerData workerData, int currentResidentNum) {
            return 0;
        }

        @Override
        public double getResidentScore(Resident resident) {
            return 0;
        }
    };

    public final TownWorkerType type;

    protected WorkerResidentHandler(TownWorkerType type) {
        this.type = type;
    }

    /**
     * 检查输入的TownWorkerData的类型是否是需要居民的工作方块
     */
    public static boolean isResidentWorker(TownWorkerData workerData) {
        if(workerData == null) return false;
        if(workerData.getType() == TownWorkerType.HOUSE) return true;
        return workerData.getType().needsResident();
    }

    /**
     * Get priority when assigning work
     * Should ONLY be used if the worker holds residents, like house, mine, etc.
     * 每有一个居民在此工作，优先级应减少1左右，这样可以使居民尽可能均匀地分配在所有工作方块中。
     * <br>
     * 当居民数量大于最大居民数时，应该返回Double. NEGATIVE_INFINITY
     * <br>
     * 这个方法不应直接调用下面的那个同名方法，以避免重复读取nbt中的数据。
     */
    public abstract double getResidentPriority(TownWorkerData workerData);

    /**
     * 当因为某些原因，工作方块中的居民数量没有保存在nbt(TownWorkerData.workData)中，使用此方法来计算分配居民的优先级。
     * 这个方法存在是因为我在分配居民工作时，为了避免对nbt进行大量次数的修改，将居民的变化暂存在其它地方而未改变TownWorkerData。workData。
     * 常规情况下应使用上边的方法。
     * @param workerData 工作方块的数据
     * @param residentNum 未保存在nbt中的，实际的居民数量
     */
    public abstract double getResidentPriority(TownWorkerData workerData, int residentNum);

    /**
     * 获取居民在此种类工作方块工作的适合程度。
     * 决定居民的工作效率。
     * 此方块寻找合适的工作者时，分数高的居民会优先进入。
     */
    public abstract double getResidentScore(Resident resident);

    /**
     * 判断居民能否在此工作方块工作。
     * <br>
     * 可在子类覆写此方法，以对不同的工作设置工作条件。
     */
    public boolean canResidentWork(Resident resident){
        if(resident.getHealth() <= 10) return false;
        if(resident.getMental() <= 5) return false;
        if(resident.getHousePos() == null) return false;
        return true;
    }

    /**
     * 判断居民能否被分配到此工作方块。用于分配工作。
     * 相比较 canResidentWork，此方法的区别在于会判断居民是否已有工作。
     * <br>
     * 此方法无需在子类覆写。
     */
    public boolean canResidentBeAssigned(Resident resident){
        if(resident.getWorkPos() == null) return false;
        return canResidentWork(resident);
    }

    /**
     * 用于调整数据。
     * <br>
     * 1-exp型，在x大概为20时，数值达到一半
     */
    public static double CalculatingFunction1(double num){
        if(num <= 0){
            return 0;
        }
        return 1-Math.exp(-num*0.04);
    }

    /**
     * 用于调整数据。
     * <br>
     * S型曲线，关于点(50,0.5)对称
     * <br>
     * @param num x
     * @param parameter1 这个数值越大，曲线越陡峭。一般取0.1时可得到一个陡峭度适中的曲线。
     */
    public static double CalculatingFunction2(double num, double parameter1){
        return 1/(1+Math.exp(-num * parameter1 + 50 * parameter1));
    }

    
 
}
