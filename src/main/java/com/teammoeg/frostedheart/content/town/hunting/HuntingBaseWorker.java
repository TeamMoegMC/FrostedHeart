package com.teammoeg.frostedheart.content.town.hunting;

import com.teammoeg.frostedheart.content.town.*;
import com.teammoeg.frostedheart.content.town.resource.action.ResourceActionMode;
import com.teammoeg.frostedheart.content.town.resource.action.ResourceActionType;
import com.teammoeg.frostedheart.content.town.resource.action.TownResourceActionResults;
import com.teammoeg.frostedheart.content.town.resource.action.TownResourceActions;
import com.teammoeg.frostedheart.content.town.worker.TownWorkerData;
import com.teammoeg.frostedheart.content.town.worker.WorkerState;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.*;

public class HuntingBaseWorker implements TownWorker<WorkerState> {
    private HuntingBaseWorker(){
        super();
    }
    public static final HuntingBaseWorker INSTANCE = new HuntingBaseWorker();
    @Override
    public boolean work(Town town, CompoundTag workData) {
        if (town instanceof TeamTown teamTown) {//the town must be team town because it needs to get all camps in the town.
            TreeSet<AbstractMap.SimpleEntry<TownWorkerData, Double>> camps = new TreeSet<>(Comparator.comparingDouble(AbstractMap.SimpleEntry::getValue));//Double: rating
            ArrayList<TownWorkerData> campsUnchecked = new ArrayList<>();
            teamTown.getTownBlocks().values().forEach(
                    (TownWorkerData data) -> {
                        if (data.getType() == TownWorkerType.HUNTING_CAMP) {
                            campsUnchecked.add(data);
                        }
                    });
            if (!campsUnchecked.isEmpty()) {
                double baseRating = workData.getDouble("rating");
                for (TownWorkerData data : campsUnchecked) {
                    AbstractMap.SimpleEntry<TownWorkerData, Double> dataPair = new AbstractMap.SimpleEntry<>(data, data.getWorkData().getDouble("rating") * baseRating * 2);//double: 考虑到与之距离过近的camp之后新计算的rating    *2:下面遍历的时候会遍历到它自己
                    for (TownWorkerData data2 : campsUnchecked) {
                        if (data2.getPos().distSqr(data.getPos()) < 128)
                            dataPair.setValue(dataPair.getValue() * (0.5 + 0.5 * (data2.getPos().distSqr(data.getPos())) / 128));
                    }
                    camps.add(dataPair);
                }
            }
            int residentsLeft = workData.getInt("maxResident");
            if (!camps.isEmpty() && residentsLeft > 0) {
                Iterator<AbstractMap.SimpleEntry<TownWorkerData, Double>> iterator = camps.iterator();
                while (iterator.hasNext() && residentsLeft > 0) {
                    double add = iterator.next().getValue();
                    TownResourceActionResults.ItemResourceActionResult result = (TownResourceActionResults.ItemResourceActionResult) town
                            .getActionExecutorHandler()
                            .execute(new TownResourceActions.ItemResourceAction(new ItemStack(Items.BEEF), ResourceActionType.ADD, add, ResourceActionMode.MAXIMIZE));
                    if (!result.allModified()) return false;
                    residentsLeft--;
                }
            }
            return true;
        }
        return false;
    }
}
