package com.teammoeg.frostedheart.content.town.resource;

import com.teammoeg.frostedheart.content.town.resource.actionattributes.ResourceActionMode;
import com.teammoeg.frostedheart.content.town.resource.actionattributes.ResourceActionOrder;
import com.teammoeg.frostedheart.content.town.resource.actionattributes.ResourceActionType;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.function.Predicate;

/**
 * 这个类本身没有作用，仅用于将一堆Action及其 Result 整合在一起
 */
public class TownResourceActions {

    /**
     * 不可直接添加ItemResourceAttribute，只能添加对应的物品
     */
    public static record ItemResourceAttributeCostAction(ItemResourceAttribute resourceToModify, double amount, ResourceActionMode actionMode) implements ITownResourceAttributeAction {

        @Override
        public ItemResourceAttributeCostActionResult apply(TownResourceHolder resourceHolder) {
            double availableAmount;
            availableAmount = resourceHolder.get(resourceToModify);
            double toCost = amount;
            if(availableAmount<amount){
                if(actionMode==ResourceActionMode.ATTEMPT || availableAmount<=TownResourceHolder.DELTA){
                    return new ItemResourceAttributeCostActionResult(this, false, 0, amount, Collections.emptyMap());
                } else if(actionMode==ResourceActionMode.MAXIMIZE){
                    toCost = availableAmount;
                }
            }
            double toCostCopy = toCost;//toCost接下来会修改，复制一份用于记录数量
            Map<ItemStackResourceKey, Double> costDetail = new HashMap<>();
            Map<ItemStackResourceKey, Double> items = resourceHolder.getAllItemsByResourceAttribute(resourceToModify);
            for(ItemStackResourceKey itemStackResourceKey : items.keySet()){
                double itemResourceAmount = TownResourceHolder.getResourceAmount(itemStackResourceKey, resourceToModify);
                double itemLeft = resourceHolder.get(itemStackResourceKey);
                double itemAmountToCost = Math.min(toCost/itemResourceAmount, itemLeft);
                costDetail.put(itemStackResourceKey, itemAmountToCost);
                resourceHolder.costUnsafe(itemStackResourceKey, itemAmountToCost);
                toCost -= itemAmountToCost * itemResourceAmount;
                if(toCost<=TownResourceHolder.DELTA) break;
            }
            return new ItemResourceAttributeCostActionResult(this, true, toCostCopy, amount - toCost, costDetail);
        }
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
        public ITownResourceAction getAction() {
            return action;
        }

        @Override
        public void applyForce(TownResourceHolder resourceHolder) {
            details.forEach(resourceHolder::costUnsafe);
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

    public static record ItemStackAction(ItemStack itemToModify, ResourceActionType actionType,
                                         ResourceActionMode actionMode) implements ITownResourceAction {

        public boolean isAdd() {
            return actionType == ResourceActionType.ADD;
        }

        @Override
        public ItemStackActionResult apply(TownResourceHolder townResourceHolder) {
            int amount = itemToModify.getCount();
            double availableAmount;
            if (isAdd()) availableAmount = townResourceHolder.getCapacityLeft();
            else availableAmount = townResourceHolder.get(itemToModify);
            if (availableAmount < amount) {
                if (actionMode == ResourceActionMode.ATTEMPT || availableAmount <= TownResourceHolder.DELTA) {
                    return new ItemStackActionResult(this, false, ItemStack.EMPTY, itemToModify.copy());
                }
                int toModify = (int) Math.floor(availableAmount);
                if (isAdd()) {
                    townResourceHolder.addUnsafe(itemToModify, toModify);
                } else {
                    townResourceHolder.costUnsafe(itemToModify, toModify);
                }
                return new ItemStackActionResult(this, false, itemToModify.copyWithCount(toModify), itemToModify.copyWithCount(amount - toModify));
            } else {
                if (isAdd()) {
                    townResourceHolder.addUnsafe(itemToModify, amount);
                } else {
                    townResourceHolder.costUnsafe(itemToModify, amount);
                }
                return new ItemStackActionResult(this, true, itemToModify.copy(), ItemStack.EMPTY);
            }
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
    public static record ItemStackActionResult(ItemStackAction action, Boolean allModified, ItemStack itemStackModified, ItemStack itemStackLeft) implements IResourceActionResult{
        @Override
        public ItemStackAction getAction(){
            return action;
        }

        @Override
        public void applyForce(TownResourceHolder resourceHolder){
            if(action.isAdd()){
                resourceHolder.addUnsafe(itemStackModified);
            }else {
                resourceHolder.costUnsafe(itemStackModified);
            }
        }
    }

    public static record TownResourceTypeCostAction(ITownResourceType resourceToCost, double amount, int minLevel, int maxLevel, ResourceActionMode actionMode, ResourceActionOrder order) implements ITownResourceAction {
        public TownResourceTypeCostAction{
            if(minLevel>maxLevel)
                throw new IllegalArgumentException("minLevel must be less than maxLevel");
            if(minLevel<0)
                throw new IllegalArgumentException("minLevel must be greater than 0");
            if(maxLevel>resourceToCost.getMaxLevel())
                throw new IllegalArgumentException("minLevel must be less than maxLevel");
        }

        @Override
        public IResourceActionResult apply(TownResourceHolder resourceHolder) {
            double availableAmount = resourceHolder.get(resourceToCost);
            double toCost = amount;
            if(amount > availableAmount){
                if(actionMode==ResourceActionMode.ATTEMPT || availableAmount<=TownResourceHolder.DELTA){
                    return new TownResourceTypeCostActionResult(this, false, 0, amount, Collections.emptyList());
                } else if(actionMode==ResourceActionMode.MAXIMIZE){
                    toCost = availableAmount;
                }
            }
            int startLevel;
            Predicate<Integer> levelLimit;
            int step;
            if (order == ResourceActionOrder.ASCENDING) {
                startLevel = minLevel;
                levelLimit = level -> level <= maxLevel;
                step = 1;
            } else {
                startLevel = maxLevel;
                levelLimit = level -> level >= minLevel;
                step = -1;
            }
            double toCostCopy = toCost;
            List<ITownResourceAttributeActionResult> details = new ArrayList<>();
            for(int level = startLevel ; levelLimit.test(level) ; level += step){
                ITownResourceAttributeAction action = ITownResourceAttributeAction.create(resourceToCost.generateAttribute(level), toCost, ResourceActionMode.MAXIMIZE);
                ITownResourceAttributeActionResult result = action.apply(resourceHolder);
                toCost = result.residualAmount();
                if(toCost<=TownResourceHolder.DELTA) break;
            }
            return new TownResourceTypeCostActionResult(this, amount <= availableAmount, toCostCopy, amount - toCostCopy, details);
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
    public static record TownResourceTypeCostActionResult(TownResourceTypeCostAction action, boolean allCosted, double totalModifiedAmount, double residualAmount, List<ITownResourceAttributeActionResult> details) implements IResourceActionResult {

        @Override
        public ITownResourceAction getAction() {
            return action;
        }

        @Override
        public void applyForce(TownResourceHolder resourceHolder) {
            details.forEach(detailResult -> detailResult.applyForce(resourceHolder));
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
    public static record VirtualResourceAttributeActionResult(VirtualResourceAttributeAction action, boolean allModified, double modifiedAmount, double residualAmount ) implements ITownResourceAttributeActionResult {

        @Override
        public ITownResourceAction getAction() {
            return action;
        }

        @Override
        public void applyForce(TownResourceHolder resourceHolder) {
            VirtualResourceAttribute resourceKey = action.resourceToModify();
            if(action.isAdd()){
                resourceHolder.addUnsafe(resourceKey,modifiedAmount);
            } else{
                resourceHolder.costUnsafe(resourceKey,modifiedAmount);
            }
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

    public static record VirtualResourceAttributeAction(VirtualResourceAttribute resourceToModify, double amount, ResourceActionType actionType, ResourceActionMode actionMode) implements ITownResourceAttributeAction {
        public boolean isAdd(){
            return actionType == ResourceActionType.ADD;
        }
        @Override
        public VirtualResourceAttributeActionResult apply(TownResourceHolder resourceHolder) {
            double availableAmount;
            if(isAdd()) availableAmount = resourceHolder.getCapacityLeft();
            else availableAmount = resourceHolder.get(resourceToModify);
            double toModify = amount;
            if(availableAmount < amount){
                if(actionMode == ResourceActionMode.ATTEMPT || availableAmount <= TownResourceHolder.DELTA){
                    return new VirtualResourceAttributeActionResult(this, false, 0, amount);
                } else if(actionMode == ResourceActionMode.MAXIMIZE){
                    toModify = availableAmount;
                }
            }
            if(isAdd()){
                resourceHolder.addUnsafe(resourceToModify, toModify);
            } else{
                resourceHolder.costUnsafe(resourceToModify, toModify);
            }
            if(availableAmount < amount){
                return new VirtualResourceAttributeActionResult(this, false, toModify, amount - toModify);
            } else{
                return new VirtualResourceAttributeActionResult(this, true, toModify, 0);
            }
        }
    }
}
