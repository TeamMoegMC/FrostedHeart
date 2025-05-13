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

package com.teammoeg.frostedheart.content.town.resource;

import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.Map;

/**
 * 提供了对城镇资源进行操作的一些方法。
 */
public class TownResourceManager {
    public final TownResourceHolder resourceHolder;

    public TownResourceManager(TownResourceHolder holder){
        this.resourceHolder = holder;
    }

    public TownResourceManager(){
        this.resourceHolder = new TownResourceHolder();
    }

    /**
     * 获取城镇中某个ITownResourceAttribute的储量。
     * 若为ItemResourceAttribute，获取的是城镇中，所有该ItemResourceAttribute对应物品的(物品数量 * 单个物品能转化为ItemResourceAttribute数量)之和。
     * 这也是TownResourceManager中，两种cost(ItemResourceAttribute)能消耗的最大数量。
     * 举个例子，比如城镇中，有20个铁锭、10个铁块，1个铁锭可转化为1 metal_1，1个铁块可转化为9 metal_1，使用这个get方法获取metal_1的数量，会得到20*1+10*9=110.
     */
    public double get(ITownResourceAttribute attribute){
        return resourceHolder.get(attribute);
    }

    /**
     * 获取此种ITownResourceType对应的所有资源数量。
     * @return 该ITownResourceType对应的所有ITownResourceAttribute的资源数量之和。
     */
    public double get(ITownResourceType type){
        return getAllAboveLevel(type,0);
    }

    /**
     * 获取城镇中某个物品的储量。
     */
    public double get(ItemStack itemStack){
        return resourceHolder.get(itemStack);
    }

    /**
     * 获取城镇剩余(未被占用)的容量。
     */
    public double getCapacityLeft(){
        return get(VirtualResourceType.MAX_CAPACITY.generateAttribute(0)) - resourceHolder.getOccupiedCapacity();
    }

    /**
     * 获取给定的level以上的，某ITownResourceType的资源数量之和。
     */
    public double getAllAboveLevel(ITownResourceType type, int minLevel){
        if(!type.isLevelValid(minLevel)) return 0;
        double sum = 0;
        for(int i=minLevel;i<=type.getMaxLevel();i++){
            sum += get(type.generateAttribute(i));
        }
        return sum;
    }

    /**
     * 获取给定的level之间的，某ITownResourceType的资源数量之和。
     */
    public double getAllBetweenLevel(ITownResourceType type, int minLevel, int maxLevel){
        if(!type.isLevelValid(minLevel) || !type.isLevelValid(maxLevel)) return 0;
        double sum = 0;
        for(int i=minLevel;i<=maxLevel;i++){
            sum += get(type.generateAttribute(i));
        }
        return sum;
    }

    /**
     * Add resource to the town if there is enough capacity.
     * @param key The resource key
     * @param amount The amount to add
     */
    public SimpleResourceActionResult addIfHaveCapacity(VirtualResourceAttribute key, double amount){
        if(!key.type.needCapacity){
            resourceHolder.addUnsafe(key,amount);
            return new SimpleResourceActionResult(true, amount, key);
        }
        double spaceLeft = getCapacityLeft();
        if(spaceLeft>=amount){
            resourceHolder.addUnsafe(key,amount);
            return new SimpleResourceActionResult(true, amount, key);
        }
        return SimpleResourceActionResult.NOT_SUCCESS;
    }

    /**
     * Add resource to the town if there is enough capacity.
     * What actually added is the VirtualResourceKey with level 0.
     * @param type The virtual resource type, which will be converted to a VirtualResourceKey with level 0.
     * @param amount The amount to add
     */
    public SimpleResourceActionResult addIfHaveCapacity(VirtualResourceType type, double amount){
        return addIfHaveCapacity(type.generateAttribute(0), amount);
    }

    /**
     * Add item to the town if there is enough capacity.
     * @param itemStack The item to add. The count of item will be ignored.
     * @param amount The amount to add
     */
    public SimpleResourceActionResult addIfHaveCapacity(ItemStack itemStack, double amount){
        double spaceLeft = getCapacityLeft();
        if(spaceLeft>=amount){
            resourceHolder.addUnsafe(itemStack, amount);
            return new SimpleResourceActionResult(true, amount, 0, 0);
        }
        return SimpleResourceActionResult.NOT_SUCCESS;
    }

    //You can't add ItemResourceAttribute to town, because item resource are saved as ItemStackKey.

    /**
     * Add resource to the town.
     * If space is not enough, some(same amount with capacity left) of the resource will be added, and others will lose.
     * @param amount The amount to add.
     * @return The result of the action. You can know if all the resource is added, and how many resources are added.
     */
    public SimpleResourceActionResult addToMax(VirtualResourceAttribute key, double amount){
        if(!key.type.needCapacity){
            resourceHolder.addUnsafe(key,amount);
            return new SimpleResourceActionResult(true, amount, key);
        }
        double capacityLeft = getCapacityLeft();
        if(capacityLeft <= 0) return SimpleResourceActionResult.NOT_SUCCESS;
        if(capacityLeft>=amount){
            resourceHolder.addUnsafe(key,amount);
            return new SimpleResourceActionResult(true, amount, key);
        } else {
            resourceHolder.addUnsafe(key,capacityLeft);
            return new SimpleResourceActionResult(false, capacityLeft, key);
        }
    }

    /**
     * Add item to the town.
     * If space is not enough, some(same amount with capacity left) of the resource will be added, and others will lose.
     * @param amount The amount to add.
     * @return The result of the action. You can know if all the resource is added, and how many resources are added.
     */
    public SimpleResourceActionResult addToMax(ItemStack itemStack, double amount){
        double capacityLeft = getCapacityLeft();
        if(capacityLeft <= 0) return SimpleResourceActionResult.NOT_SUCCESS;
        if(capacityLeft>=amount){
            resourceHolder.addUnsafe( itemStack,amount);
            return new SimpleResourceActionResult(true, amount, 0, 0);
        } else {
            resourceHolder.addUnsafe(itemStack,capacityLeft);
            return new SimpleResourceActionResult(false, capacityLeft, 0, 0);
        }
    }

    /**
     * Cost resource from the town.
     * If there is not enough resource, nothing will be cost.
     * @return The result of the action. You can know if all the resource is costed, and how many resources are costed, etc.
     */
    public SimpleResourceActionResult costIfHaveEnough(VirtualResourceAttribute key, double amount){
        if(amount <=0) return SimpleResourceActionResult.NOT_SUCCESS;
        if(get(key) >= amount){
            resourceHolder.costUnsafe(key,amount);
            return new SimpleResourceActionResult(true, amount, key);
        } else {
            return SimpleResourceActionResult.NOT_SUCCESS;
        }
    }

    /**
     * Cost item from the town.
     * If there is not enough item, nothing will be cost.
     * @return The result of the action. You can know if all the resource is costed, and how many resources are costed, etc.
     */
    public SimpleResourceActionResult costIfHaveEnough(ItemStack itemStack, double amount){
        if(amount <=0) return SimpleResourceActionResult.NOT_SUCCESS;
        if(get(itemStack) >= amount){
            resourceHolder.costUnsafe(itemStack,amount);
            return new SimpleResourceActionResult(true, amount, 0, 0);
        } else {
            return SimpleResourceActionResult.NOT_SUCCESS;
        }
    }

    /**
     * Cost resource from the town.
     * If there is not enough resource, nothing will be cost.
     * ItemResourceAttribute对应的所有物品都有可能被消耗。
     * 消耗的模式是：按照某种顺序，先消耗某种物品直到消耗完，再消耗下一种物品。
     * 我也不知道这个“某种顺序”究竟是什么，这取决于从缓存中读取的顺序。
     * @return The result of the action. You can know if all the resource is costed, and how many resources are costed, etc.
     */
    public SimpleResourceActionResult costIfHaveEnough(ItemResourceAttribute attribute, double amount){
        double resourceLeft = get(attribute);
        if(resourceLeft<=amount) return SimpleResourceActionResult.NOT_SUCCESS;
        double toCost;
        Map<ItemStack, Double> items = resourceHolder.getAllItems(attribute);
        toCost = amount;
        for(ItemStack itemStack : items.keySet()){
            double itemResourceAmount = TownResourceHolder.getResourceAmount(itemStack, attribute);
            SimpleResourceActionResult result = costToEmpty(itemStack, toCost / itemResourceAmount);
            toCost -= result.actualAmount() * itemResourceAmount;
            if(toCost<=TownResourceHolder.DELTA) break;
        }
        return new SimpleResourceActionResult(true, amount, attribute);
    }

    public SimpleResourceActionResult costIfHaveEnough(ITownResourceAttribute attribute, double amount){
        if(attribute instanceof ItemResourceAttribute) return costIfHaveEnough((ItemResourceAttribute)attribute, amount);
        if(attribute instanceof VirtualResourceAttribute) return costIfHaveEnough((VirtualResourceAttribute)attribute, amount);
        return SimpleResourceActionResult.NOT_SUCCESS;
    }

    /**
     * Cost item from the town.
     * If there is not enough resource, all resource left will be cost.
     * @param itemStack The item to cost. The count of item will be ignored.
     * @return The result of the action. You can know if all the resource is costed, and how many resources are costed, etc.
     */
    public SimpleResourceActionResult costToEmpty(ItemStack itemStack, double amount){
        double resourceLeft = get(itemStack);
        if(resourceLeft<=0) return SimpleResourceActionResult.NOT_SUCCESS;
        if(resourceLeft>=amount){
            resourceHolder.costUnsafe(itemStack,amount);
            return new SimpleResourceActionResult(true, amount);
        } else {
            resourceHolder.costUnsafe(itemStack,resourceLeft);
            return new SimpleResourceActionResult(false, resourceLeft);
        }
    }

    /**
     * Cost resource from the town.
     * If there is not enough resource, all resource left will be cost.
     * ItemResourceAttribute对应的所有物品都有可能被消耗。
     * 消耗的模式是：按照某种顺序，先消耗某种物品直到消耗完，再消耗下一种物品。
     * 我也不知道这个“某种顺序”究竟是什么，这取决于从缓存中读取的顺序。
     * @return The result of the action. You can know if all the resource is costed, and how many resources are costed, etc.
     */
    public SimpleResourceActionResult costToEmpty(ItemResourceAttribute attribute, double amount){
        double resourceLeft = get(attribute);
        double toCost;
        Map<ItemStack, Double> items = resourceHolder.getAllItems(attribute);
        toCost = Math.min(resourceLeft, amount);
        for(ItemStack itemStack : items.keySet()){
            double itemResourceAmount = TownResourceHolder.getResourceAmount(itemStack, attribute);
            SimpleResourceActionResult result = costToEmpty(itemStack, toCost / itemResourceAmount);
            toCost -= result.actualAmount() * itemResourceAmount;
            if(toCost<=TownResourceHolder.DELTA) break;
        }
        return new SimpleResourceActionResult(amount <= resourceLeft, Math.min(resourceLeft, amount), attribute);
    }

    /**
     * Cost resource from the town.
     * If there is not enough resource, nothing will be cost.
     * @return The result of the action. You can know if all the resource is costed, and how many resources are costed, etc.
     */
    public SimpleResourceActionResult costToEmpty(VirtualResourceAttribute key, double amount){
        if(amount <=0) return SimpleResourceActionResult.NOT_SUCCESS;
        double resourceLeft = get(key);
        if(resourceLeft<=0) return SimpleResourceActionResult.NOT_SUCCESS;
        resourceHolder.costUnsafe(key,Math.min(resourceLeft, amount));
        return new SimpleResourceActionResult(true, Math.min(resourceLeft, amount), key);
    }

    /**
     * Cost resource from the town.
     * If there is not enough resource, nothing will be cost.
     * @return The result of the action. You can know if all the resource is costed, and how many resources are costed, etc.
     */
    public SimpleResourceActionResult costToEmpty(ITownResourceAttribute attribute, double amount){
        if(attribute instanceof ItemResourceAttribute) return costToEmpty((ItemResourceAttribute)attribute, amount);
        else if (attribute instanceof VirtualResourceAttribute) return costToEmpty((VirtualResourceAttribute)attribute, amount);
        return SimpleResourceActionResult.NOT_SUCCESS;
    }

    /**
     * Cost resource from the town.
     * 所有此TownResourceType、且等级在给定level之间的的资源都可以被消耗。
     * 消耗顺序为：先消耗level低的TownResourceAttribute，后消耗level高的
     * If there is not enough resource, nothing will be cost.
     */
    public SimpleResourceActionResult costBetweenLevelIfHaveEnough(ITownResourceType type, double amount, int minLevel, int maxLevel){
        if(amount <=0) return SimpleResourceActionResult.NOT_SUCCESS;
        if(minLevel>maxLevel) return SimpleResourceActionResult.NOT_SUCCESS;
        if(!type.isLevelValid(minLevel) || !type.isLevelValid(maxLevel)) return SimpleResourceActionResult.NOT_SUCCESS;
        double resourceLeft = getAllBetweenLevel(type,minLevel, maxLevel);
        if(resourceLeft < amount) {
            return SimpleResourceActionResult.NOT_SUCCESS;
        }
        double resourcesToCost = amount;
        int minLevelCount = maxLevel;
        double averageLevelCount = 0;
        for(int level = minLevel; level <= maxLevel; level++){
            if(resourcesToCost<=0) break;
            SimpleResourceActionResult result = costToEmpty(type.generateAttribute(level), resourcesToCost);
            resourcesToCost -= result.actualAmount();
            if(result.allSuccess()){
                minLevelCount = Math.min(minLevelCount, level);
            }
            averageLevelCount += result.actualAmount() * level;
        }
        averageLevelCount /= amount;
        return new SimpleResourceActionResult(true, amount, minLevelCount, averageLevelCount);
    }

    /**
     * Cost resource from the town.
     * 所有此TownResourceType、且等级大于minLevel的资源都可以被消耗。
     * 消耗顺序为：先消耗level低的TownResourceAttribute，后消耗level高的
     * If there is not enough resource, nothing will be cost.
     */
    public SimpleResourceActionResult costAboveLevelIfHaveEnough(ITownResourceType type, double amount, int minLevel){
        return costBetweenLevelIfHaveEnough(type, amount, minLevel, type.getMaxLevel());
    }

    /**
     * Cost resource from the town.
     * 所有此TownResourceType的资源都可以被消耗。
     * 消耗顺序为：先消耗level低的TownResourceAttribute，后消耗level高的
     * If there is not enough resource, nothing will be cost.
     */
    public SimpleResourceActionResult costLowestLevelIfHaveEnough(ITownResourceType type, double amount){
        return costAboveLevelIfHaveEnough(type, amount, 0);
    }

    /**
     * Cost resource from the town.
     * 所有此TownResourceType的资源都可以被消耗。
     * 消耗顺序为：先消耗level高的TownResourceAttribute，后消耗level低的
     * If there is not enough resource, nothing will be cost.
     */
    public SimpleResourceActionResult costHighestLevelIfHaveEnough(ITownResourceType type, double amount){
        if(amount <=0) return SimpleResourceActionResult.NOT_SUCCESS;
        int maxLevel = type.getMaxLevel();
        double resourceLeft = getAllBetweenLevel(type,0, maxLevel);
        if(resourceLeft < amount) {
            return SimpleResourceActionResult.NOT_SUCCESS;
        }
        double resourcesToCost = amount;
        int minLevelCount = maxLevel;
        double averageLevelCount = 0;
        for(int level = maxLevel; level >= 0; level--){
            if(resourcesToCost<=0) break;
            SimpleResourceActionResult result = costToEmpty(type.generateAttribute(level), resourcesToCost);
            resourcesToCost -= result.actualAmount();
            if(result.allSuccess()){
                minLevelCount = Math.min(minLevelCount, level);
            }
            averageLevelCount += result.actualAmount() * level;
        }
        averageLevelCount /= amount;
        return new SimpleResourceActionResult(true, amount, minLevelCount, averageLevelCount);
    }

    /**
     * Cost resource from the town.
     * 所有此TownResourceType，且等级在给定level之间的资源都可以被消耗。
     * 消耗顺序为：先消耗level低的TownResourceAttribute，后消耗level高的
     * If there is not enough resource, all resource left will be cost.
     */
    public SimpleResourceActionResult costBetweenLevelToEmpty(ITownResourceType type, double amount, int minLevel, int maxLevel){
        if(amount <=0) return SimpleResourceActionResult.NOT_SUCCESS;
        if(minLevel>maxLevel) return SimpleResourceActionResult.NOT_SUCCESS;
        if(!type.isLevelValid(minLevel) || !type.isLevelValid(maxLevel)) return SimpleResourceActionResult.NOT_SUCCESS;
        double resourceLeft = getAllBetweenLevel(type,minLevel, maxLevel);
        double resourcesToCost = Math.min(amount, resourceLeft);
        int minLevelCount = maxLevel;
        double averageLevelCount = 0;
        for(int level = minLevel; level <= maxLevel; level++){
            if(resourcesToCost<=0) break;
            SimpleResourceActionResult result = costToEmpty(type.generateAttribute(level), resourcesToCost);
            resourcesToCost -= result.actualAmount();
            if(result.allSuccess()){
                minLevelCount = Math.min(minLevelCount, level);
            }
            averageLevelCount += result.actualAmount() * level;
        }
        averageLevelCount /= amount;
        return new SimpleResourceActionResult(true, Math.min(amount, resourceLeft), minLevelCount, averageLevelCount);
    }

    /**
     * Cost resource from the town.
     * 所有此TownResourceType，且等级大于等于minLevel的资源都可以被消耗。
     * 消耗顺序为：先消耗level低的TownResourceAttribute，后消耗level高的
     * If there is not enough resource, all resource left will be cost.
     */
    public SimpleResourceActionResult costAboveLevelToEmpty(ITownResourceType type, double amount, int minLevel){
        return costBetweenLevelToEmpty(type, amount, minLevel, type.getMaxLevel());
    }

    /**
     * Cost resource from the town.
     * 所有此TownResourceType资源都可以被消耗。
     * 消耗顺序为：先消耗level低的TownResourceAttribute，后消耗level高的
     * If there is not enough resource, all resource left will be cost.
     */
    public SimpleResourceActionResult costLowestLevelToEmpty(ITownResourceType type, double amount){
        return costBetweenLevelToEmpty(type, amount, 0, 0);
    }

    /**
     * Cost resource from the town.
     * 所有此TownResourceType，且等级在给定level之间的资源都可以被消耗。
     * 消耗顺序为：先消耗level高的TownResourceAttribute，后消耗level低的
     * If there is not enough resource, all resource left will be cost.
     */
    public SimpleResourceActionResult costHighestLevelToEmpty(ITownResourceType type, double amount){
        if(amount <=0) return SimpleResourceActionResult.NOT_SUCCESS;
        int maxLevel = type.getMaxLevel();
        double resourceLeft = getAllBetweenLevel(type,0, maxLevel);
        double resourcesToCost = Math.min(amount, resourceLeft);
        int minLevelCount = maxLevel;
        double averageLevelCount = 0;
        for(int level = maxLevel; level >= 0; level--){
            if(resourcesToCost<=0) break;
            SimpleResourceActionResult result = costToEmpty(type.generateAttribute(level), resourcesToCost);
            resourcesToCost -= result.actualAmount();
            if(result.allSuccess()){
                minLevelCount = Math.min(minLevelCount, level);
            }
            averageLevelCount += result.actualAmount() * level;
        }
        averageLevelCount /= amount;
        return new SimpleResourceActionResult(true, Math.min(amount, resourceLeft), minLevelCount, averageLevelCount);
    }

    /**
     * Set the amount of resource.
     * 对于需要容量的资源，不建议使用这个方法，因为这可能使资源储量超过上限。
     */
    @Deprecated
    public void set(VirtualResourceAttribute key, double amount){
        resourceHolder.set(key, amount);
    }

    @Deprecated
    public void set(VirtualResourceType type, double amount){
        resourceHolder.set(type.generateAttribute(0), amount);
    }

    /**
     * 将所有服务资源设置为0
     */
    public void resetAllServices(){
        Arrays.stream(VirtualResourceType.values())
                .filter(type -> type.isService)
                .forEach(type -> set(type,0));
    }

    /**
     *
     * @param allSuccess actually added/costed == amount to add/cost
     * @param actualAmount actually added/costed
     * @param lowestLevel lowest level of added/costed resources
     * @param averageLevel average level of added/costed resources. 根据数量加权平均
     */
    public static record SimpleResourceActionResult(boolean allSuccess, double actualAmount, double lowestLevel, double averageLevel){
        public static final SimpleResourceActionResult NOT_SUCCESS = new SimpleResourceActionResult(false, 0, 0, 0);

        public SimpleResourceActionResult(boolean allSuccess, double actualAmount, double lowestLevel, double averageLevel){
            if(actualAmount<0){
                this.allSuccess = false;
                this.actualAmount = 0;
                this.lowestLevel = 0;
                this.averageLevel = 0;
            }
            else {
                this.allSuccess = allSuccess;
                this.actualAmount = actualAmount;
                this.lowestLevel = lowestLevel;
                this.averageLevel = averageLevel;
            }
        }

        public SimpleResourceActionResult(boolean allSuccess, double actualAmount, ITownResourceAttribute attribute){
            this(allSuccess, actualAmount, attribute.getLevel(), attribute.getLevel());
        }

        /**
         * If you just costed a ItemStack, not a ITownResourceAttribute, use this constructor.
         * ItemStack don't have a certain level so the lowestLevel and averageLevel are both 0.
         */
        public SimpleResourceActionResult(boolean allSuccess, double actualAmount){
            this(allSuccess, actualAmount, 0, 0);
        }
    }
}
