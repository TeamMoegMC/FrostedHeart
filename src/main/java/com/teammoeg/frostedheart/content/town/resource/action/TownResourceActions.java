package com.teammoeg.frostedheart.content.town.resource.action;

import com.teammoeg.frostedheart.content.town.resource.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.function.Predicate;

/**
 * 这个类本身没有作用，仅用于将一堆Action及其 Result 整合在一起
 */
public class TownResourceActions {

    public static ITownResourceAction createAttributeCostAction(ITownResourceAttribute resourceToModify, double amount, ResourceActionMode actionMode){
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
    public record ItemResourceAttributeCostAction(ItemResourceAttribute resourceToModify, double amount, ResourceActionMode actionMode) implements ITownResourceAction {
    }

    /**
     *
     * @param action 对应的ItemResourceAttributeAction
     * @param allModified 应修改量是否等于实际修改量
     * @param totalModifiedAmount 已修改资源总量
     * @param residualAmount 应修改但未修改的资源量
     * @param details 具体消耗的物品明细。Mao中Double均为正数，为消耗或添加量。action中可查看操作是添加还是消耗。
     */
    public static record ItemResourceAttributeCostActionResult(ItemResourceAttributeCostAction action, boolean allModified, double totalModifiedAmount, double residualAmount, Map<ItemStackResourceKey, Double> details) implements ITownResourceAttributeActionResult {
        @Override
        public ItemResourceAttributeCostAction getAction() {
            return action;
        }


        @Override
        public double getAmount() {
            return action.amount();
        }

        @Override
        public int getLevel() {
            return action.resourceToModify().getLevel();
        }

        @Override
        public ItemResourceAttribute getTownResourceAttribute() {
            return action.resourceToModify();
        }
    }

    /**
     * 添加/消耗物品资源。数量为给定的amount。
     */
    public record ItemResourceAction(ItemStack itemToModify, ResourceActionType actionType, double amount, ResourceActionMode actionMode) implements ITownResourceAction{
        public boolean isAdd() {
            return actionType == ResourceActionType.ADD;
        }
    }

    public record ItemResourceActionResult (ItemResourceAction action, boolean allModified, double modifiedAmount, double residualAmount)
            implements ITownResourceActionResult{

        @Override
        public ItemResourceAction getAction() {
            return action;
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

    /**
     * @param action 对应的ItemStackAction
     * @param allModified 应修改量是否等于实际修改量
     * @param itemStackModified 实际修改的ItemStack
     * @param itemStackLeft 未能成功修改时，剩余的ItemStack。
     *                      添加时，若应添加数量大于剩余容量，则为应添加量-实际添加量，消耗时，若应消耗量大于剩余物品数量，则为应消耗量-实际消耗量。
     *                      若全部成功添加/消耗，应为ItemStack.EMPTY
     */
    public record ItemStackActionResult(ItemStackAction action, Boolean allModified, ItemStack itemStackModified, ItemStack itemStackLeft) implements ITownResourceActionResult {
        @Override
        public ItemStackAction getAction(){
            return action;
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

    /**
     *
     * @param action 对应的TownResourceTypeCostAction
     * @param allCosted 应消耗量是否等于消耗量
     * @param totalModifiedAmount 实际消耗总量
     * @param residualAmount 应消耗但未消耗量
     * @param details 具体每个等级的TownResourceAttribute的消耗情况。若消耗的是物品，在这些result里还可以找到具体消耗的物品数量
     */
    public record TownResourceTypeCostActionResult(TownResourceTypeCostAction action, boolean allCosted, double totalModifiedAmount, double residualAmount, List<ITownResourceAttributeActionResult> details) implements ITownResourceActionResult {

        @Override
        public TownResourceTypeCostAction getAction() {
            return action;
        }


        public double getMinLevel(){
            return details.stream()
                    .map(ITownResourceAttributeActionResult::getLevel)
                    .min(Double::compare)
                    .orElse(0);
        }

        /**
         * @return 消耗资源的平均等级，按消耗量加权平均。
         */
        public double getAverageLevel(){
            DoubleAdder levelAdder = new DoubleAdder();
            details.forEach(result -> levelAdder.add(result.getLevel() * result.totalModifiedAmount()));
            return levelAdder.doubleValue() / totalModifiedAmount;
        }
    }

    /**
     * 这个Result只记录数量，对应的资源类型可在action本身获取
     * @param action 对应的ResourceAttributeAction
     * @param allModified 是否全部修改成功
     * @param modifiedAmount 实际添加/消耗的资源数量
     * @param residualAmount 应修改但未修改的资源数量
     */
    public record VirtualResourceAttributeActionResult(VirtualResourceAttributeAction action, boolean allModified, double modifiedAmount, double residualAmount ) implements ITownResourceAttributeActionResult {

        @Override
        public VirtualResourceAttributeAction getAction() {
            return action;
        }


        @Override
        public double getAmount() {
            return action.amount();
        }

        @Override
        public int getLevel() {
            return action.resourceToModify().getLevel();
        }

        @Override
        public VirtualResourceAttribute getTownResourceAttribute() {
            return action.resourceToModify();
        }

        public double totalModifiedAmount(){
            return modifiedAmount;
        }
    }

    public record VirtualResourceAttributeAction(VirtualResourceAttribute resourceToModify, double amount, ResourceActionType actionType, ResourceActionMode actionMode) implements ITownResourceAction {
        public boolean isAdd(){
            return actionType == ResourceActionType.ADD;
        }
    }

    public record GetAction(IGettable toGet) implements ITownResourceAction{
    }

    public record GetActionResult(GetAction action, double amount) implements ITownResourceActionResult {
        @Override
        public ITownResourceAction getAction() {
            return action;
        }
    }

    public static double get(IActionExecutorHandler handler, IGettable toGet){
        return ((GetActionResult)handler.execute(new GetAction(toGet))).amount();
    }
    public static double get(IActionExecutorHandler handler, ItemStack toGet){
        return get(handler, new ItemStackResourceKey(toGet));
    }
    public static double get(IActionExecutorHandler handler, Item toGet){
        return get(handler, new ItemStackResourceKey(new ItemStack(toGet)));
    }
}
