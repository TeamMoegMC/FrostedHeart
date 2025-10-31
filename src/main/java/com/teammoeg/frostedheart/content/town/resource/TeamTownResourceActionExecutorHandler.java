package com.teammoeg.frostedheart.content.town.resource;

import com.teammoeg.frostedheart.content.town.resource.action.*;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class TeamTownResourceActionExecutorHandler extends AbstractTownResourceActionExecutorHandler {
    //进行操作的数据本身
    public TeamTownResourceHolder resourceHolder;

    TeamTownResourceActionExecutorHandler(TeamTownResourceHolder resourceHolder) {
        this.resourceHolder = resourceHolder;
        registerExecutor(TownResourceActions.ItemResourceAttributeCostAction.class, new ItemResourceAttributeCostActionExecutor());
        registerExecutor(TownResourceActions.ItemStackAction.class, new ItemStackActionExecutor());
        registerExecutor(TownResourceActions.TownResourceTypeCostAction.class, new TownResourceTypeCostActionExecutor());
        registerExecutor(TownResourceActions.VirtualResourceAttributeAction.class, new VirtualResourceAttributeActionExecutor());
        registerExecutor(TownResourceActions.ItemResourceAction.class, new ItemResourceActionExecutor());
        registerExecutor(TownResourceActions.GetAction.class, new GetActionExecutor());
        registerExecutor(TownResourceActions.GetCapacityLeftAction.class, new GetCapacityLeftActionExecutor());
    }

    //以下为各个Action分别的Executor
    public class ItemResourceAttributeCostActionExecutor implements ITownResourceActionExecutor<TownResourceActions.ItemResourceAttributeCostAction> {

        @Override
        public TownResourceActionResults.ItemResourceAttributeCostActionResult execute(TownResourceActions.ItemResourceAttributeCostAction action) {
            double availableAmount;
            availableAmount = resourceHolder.get(action.resourceToModify());
            double toCost = action.amount();
            if(availableAmount<action.amount()){
                if(action.actionMode()==ResourceActionMode.ATTEMPT || availableAmount<= TeamTownResourceHolder.DELTA){
                    return new TownResourceActionResults.ItemResourceAttributeCostActionResult(action, false, 0, action.amount(), Collections.emptyMap());
                } else if(action.actionMode()==ResourceActionMode.MAXIMIZE){
                    toCost = availableAmount;
                }
            }
            double toCostCopy = toCost;//toCost接下来会修改，复制一份用于记录数量
            Map<ItemStackResourceKey, Double> costDetail = new HashMap<>();
            Map<ItemStackResourceKey, Double> items = resourceHolder.getAllItemsByResourceAttribute(action.resourceToModify());
            for(ItemStackResourceKey itemStackResourceKey : items.keySet()){
                double itemResourceAmount = TeamTownResourceHolder.getResourceAmount(itemStackResourceKey, action.resourceToModify());
                double itemLeft = resourceHolder.get(itemStackResourceKey);
                double itemAmountToCost = Math.min(toCost/itemResourceAmount, itemLeft);
                costDetail.put(itemStackResourceKey, itemAmountToCost);
                resourceHolder.costUnsafe(itemStackResourceKey, itemAmountToCost);
                toCost -= itemAmountToCost * itemResourceAmount;
                if(toCost<= TeamTownResourceHolder.DELTA) break;
            }
            return new TownResourceActionResults.ItemResourceAttributeCostActionResult(action, true, toCostCopy, action.amount() - toCost, costDetail);
        }
    }

    public class ItemStackActionExecutor implements ITownResourceActionExecutor<TownResourceActions.ItemStackAction> {
        @Override
        public TownResourceActionResults.ItemStackActionResult execute(TownResourceActions.ItemStackAction action) {
            int amount = action.itemToModify().getCount();
            double availableAmount;
            if (action.isAdd()) availableAmount = resourceHolder.getCapacityLeft();
            else availableAmount = resourceHolder.get(action.itemToModify());
            if (availableAmount < amount) {
                if (action.actionMode() == ResourceActionMode.ATTEMPT || availableAmount <= TeamTownResourceHolder.DELTA) {
                    return new TownResourceActionResults.ItemStackActionResult(action, false, ItemStack.EMPTY, action.itemToModify().copy());
                }
                int toModify = (int) Math.floor(availableAmount);
                if (action.isAdd()) {
                    resourceHolder.addUnsafe(action.itemToModify(), toModify);
                } else {
                    resourceHolder.costUnsafe(action.itemToModify(), toModify);
                }
                return new TownResourceActionResults.ItemStackActionResult(action, false, action.itemToModify().copyWithCount(toModify), action.itemToModify().copyWithCount(amount - toModify));
            } else {
                if (action.isAdd()) {
                    resourceHolder.addUnsafe(action.itemToModify(), amount);
                } else {
                    resourceHolder.costUnsafe(action.itemToModify(), amount);
                }
                return new TownResourceActionResults.ItemStackActionResult(action, true, action.itemToModify().copy(), ItemStack.EMPTY);
            }
        }
    }

    public class TownResourceTypeCostActionExecutor implements ITownResourceActionExecutor<TownResourceActions.TownResourceTypeCostAction> {
        @Override
        public TownResourceActionResults.TownResourceTypeCostActionResult execute(TownResourceActions.TownResourceTypeCostAction action) {
            double availableAmount = resourceHolder.get(action.resourceToCost());
            double toCost = action.amount();
            if(action.amount() > availableAmount){
                if(action.actionMode()==ResourceActionMode.ATTEMPT || availableAmount<= TeamTownResourceHolder.DELTA){
                    return new TownResourceActionResults.TownResourceTypeCostActionResult(action, false, 0, action.amount(), Collections.emptyList());
                } else if(action.actionMode()==ResourceActionMode.MAXIMIZE){
                    toCost = availableAmount;
                }
            }
            int startLevel;
            java.util.function.Predicate<Integer> levelLimit;
            int step;
            if (action.order() == ResourceActionOrder.ASCENDING) {
                startLevel = action.minLevel();
                levelLimit = level -> level <= action.maxLevel();
                step = 1;
            } else {
                startLevel = action.maxLevel();
                levelLimit = level -> level >= action.minLevel();
                step = -1;
            }
            double toCostCopy = toCost;
            java.util.List<ITownResourceAttributeActionResult<? extends ITownResourceAttributeAction>> details = new java.util.ArrayList<>();
            for(int level = startLevel ; levelLimit.test(level) ; level += step){
                ITownResourceAttributeAction attributeAction = TownResourceActions.createAttributeCostAction(action.resourceToCost().generateAttribute(level), toCost, ResourceActionMode.MAXIMIZE);
                ITownResourceAttributeActionResult<? extends ITownResourceAttributeAction> result = (ITownResourceAttributeActionResult<? extends ITownResourceAttributeAction>)TeamTownResourceActionExecutorHandler.this.execute(attributeAction);
                details.add(result);
                toCost = result.residualAmount();
                if(toCost<= TeamTownResourceHolder.DELTA) break;
            }
            return new TownResourceActionResults.TownResourceTypeCostActionResult(action, action.amount() <= availableAmount, toCostCopy, action.amount() - toCostCopy, details);
        }
    }

    public class VirtualResourceAttributeActionExecutor implements ITownResourceActionExecutor<TownResourceActions.VirtualResourceAttributeAction> {
        @Override
        public TownResourceActionResults.VirtualResourceAttributeActionResult execute(TownResourceActions.VirtualResourceAttributeAction action) {
            double availableAmount;
            if(action.isAdd()){
                if(action.resourceToModify().getType().needCapacity){
                    availableAmount = resourceHolder.getCapacityLeft();
                } else{
                    availableAmount = Double.POSITIVE_INFINITY;
                }
            }
            else availableAmount = resourceHolder.get(action.resourceToModify());
            double toModify = action.amount();
            if(availableAmount < action.amount()){
                if(action.actionMode() == ResourceActionMode.ATTEMPT || availableAmount <= TeamTownResourceHolder.DELTA){
                    return new TownResourceActionResults.VirtualResourceAttributeActionResult(action, false, 0, action.amount());
                } else if(action.actionMode() == ResourceActionMode.MAXIMIZE){
                    toModify = availableAmount;
                }
            }
            if(action.isAdd()){
                resourceHolder.addUnsafe(action.resourceToModify(), toModify);
            } else{
                resourceHolder.costUnsafe(action.resourceToModify(), toModify);
            }
            if(availableAmount < action.amount()){
                return new TownResourceActionResults.VirtualResourceAttributeActionResult(action, false, toModify, action.amount() - toModify);
            } else{
                return new TownResourceActionResults.VirtualResourceAttributeActionResult(action, true, toModify, 0);
            }
        }
    }

    public class ItemResourceActionExecutor implements ITownResourceActionExecutor<TownResourceActions.ItemResourceAction> {
        @Override
        public TownResourceActionResults.ItemResourceActionResult execute(TownResourceActions.ItemResourceAction action) {
            double amount = action.amount();
            double availableAmount;
            if (action.isAdd()) availableAmount = resourceHolder.getCapacityLeft();
            else availableAmount = resourceHolder.get(action.itemToModify());
            if (availableAmount < amount) {
                if (action.actionMode() == ResourceActionMode.ATTEMPT || availableAmount <= TeamTownResourceHolder.DELTA) {
                    return new TownResourceActionResults.ItemResourceActionResult(action, false, 0, amount);
                }
                if (action.isAdd()) {
                    resourceHolder.addUnsafe(action.itemToModify(), availableAmount);
                } else {
                    resourceHolder.costUnsafe(action.itemToModify(), availableAmount);
                }
                return new TownResourceActionResults.ItemResourceActionResult(action, false, availableAmount, amount - availableAmount);
            } else {
                if (action.isAdd()) {
                    resourceHolder.addUnsafe(action.itemToModify(), amount);
                } else {
                    resourceHolder.costUnsafe(action.itemToModify(), amount);
                }
                return new TownResourceActionResults.ItemResourceActionResult(action, true, amount, 0);
            }
        }
    }

    public class GetActionExecutor implements ITownResourceActionExecutor<TownResourceActions.GetAction> {
        @Override
        public TownResourceActionResults.GetActionResult execute(TownResourceActions.GetAction action) {
            double amount = resourceHolder.get(action.toGet());
            return new TownResourceActionResults.GetActionResult(action, amount);
        }
    }

    public class GetCapacityLeftActionExecutor implements ITownResourceActionExecutor<TownResourceActions.GetCapacityLeftAction> {
        @Override
        public TownResourceActionResults.GetCapacityLeftActionResult execute(TownResourceActions.GetCapacityLeftAction action) {
            double amount = resourceHolder.getCapacityLeft();
            return new TownResourceActionResults.GetCapacityLeftActionResult(action, amount);
        }
    }
}