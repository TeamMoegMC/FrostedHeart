package com.teammoeg.frostedheart.content.town.resource.action;

import com.teammoeg.frostedheart.content.town.resource.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 这个类本身没有作用，仅用于将一堆Action及其 Result 整合在一起
 * 要使用Action修改城镇资源，需要使用{@link ITownResourceActionExecutor}, {@link ITownResourceActionExecutorHandler}
 */
public class TownResourceActions {

    public static ITownResourceAttributeAction createAttributeCostAction(ITownResourceAttribute resourceToModify, double amount, ResourceActionMode actionMode){
        if(resourceToModify instanceof ItemResourceAttribute itemAttribute){
            return new ItemResourceAttributeCostAction(itemAttribute, amount, actionMode);
        } else if(resourceToModify instanceof VirtualResourceAttribute virtualResourceAttribute){
            return new VirtualResourceAttributeAction(virtualResourceAttribute, amount, ResourceActionType.COST, actionMode);
        }
        throw new IllegalArgumentException("resourceToModify must be ItemResourceAttribute or VirtualResourceAttribute now. If there is a new type of ITownResourceAttribute, please add it in this method.");
    }

    /**
     * 不可直接添加ItemResourceAttribute，只能添加对应的物品
     */
    public record ItemResourceAttributeCostAction(ItemResourceAttribute resourceToModify, double amount, ResourceActionMode actionMode) implements ITownResourceAttributeAction {
    }

    /**
     * 添加/消耗物品资源。数量为给定的amount。
     */
    public record ItemResourceAction(ItemStack itemToModify, ResourceActionType actionType, double amount, ResourceActionMode actionMode) implements ITownResourceAction{
        public boolean isAdd() {
            return actionType == ResourceActionType.ADD;
        }
    }

    /**
     * 添加/消耗物品。添加/消耗量即为ItemStack的物品数。
     */
    public record ItemStackAction(ItemStack itemToModify, ResourceActionType actionType,
                                  ResourceActionMode actionMode) implements ITownResourceAction {
        public boolean isAdd() {
            return actionType == ResourceActionType.ADD;
        }

    }

    public record TownResourceTypeCostAction(ITownResourceType resourceToCost, double amount, int minLevel, int maxLevel, ResourceActionMode actionMode, ResourceActionOrder order) implements ITownResourceAction {
        public TownResourceTypeCostAction{
            if(minLevel>maxLevel)
                throw new IllegalArgumentException("minLevel must be less than maxLevel");
            if(minLevel<0)
                throw new IllegalArgumentException("minLevel must be greater than 0");
            if(maxLevel>resourceToCost.getMaxLevel())
                throw new IllegalArgumentException("minLevel must be less than maxLevel");
        }

    }

    public record VirtualResourceAttributeAction(VirtualResourceAttribute resourceToModify, double amount, ResourceActionType actionType, ResourceActionMode actionMode) implements ITownResourceAttributeAction {
        public boolean isAdd(){
            return actionType == ResourceActionType.ADD;
        }
    }

    public record GetAction(IGettable toGet) implements ITownResourceAction{
    }

    public record GetCapacityLeftAction() implements ITownResourceAction{}

    public static double get(ITownResourceActionExecutorHandler handler, IGettable toGet){
        return ((TownResourceActionResults.GetActionResult)handler.execute(new GetAction(toGet))).amount();
    }
    public static double get(ITownResourceActionExecutorHandler handler, ItemStack toGet){
        return get(handler, new ItemStackResourceKey(toGet));
    }
    public static double get(ITownResourceActionExecutorHandler handler, Item toGet){
        return get(handler, new ItemStackResourceKey(new ItemStack(toGet)));
    }

    public static double getCapacityLeft(ITownResourceActionExecutorHandler handler){
        return ((TownResourceActionResults.GetCapacityLeftActionResult)handler.execute(new GetCapacityLeftAction())).amount();
    }

}
