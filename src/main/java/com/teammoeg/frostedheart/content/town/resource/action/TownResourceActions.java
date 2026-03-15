/*
 * Copyright (c) 2026 TeamMoeg
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

package com.teammoeg.frostedheart.content.town.resource.action;

import com.teammoeg.frostedheart.content.town.resource.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 这个类本身没有作用，仅用于将一堆Action及其整合在一起
 */
public class TownResourceActions {

    public static <A extends ITownResourceAction<R>, R extends ITownResourceAttributeActionResult<A>> ITownResourceAction<? extends ITownResourceAttributeActionResult<?>> createAttributeCostAction(ITownResourceAttribute resourceToModify, double amount, ResourceActionMode actionMode){
        if(resourceToModify instanceof ItemResourceAttribute itemAttribute){
            return new ItemResourceAttributeCostAction(itemAttribute, amount, actionMode);
        } else if(resourceToModify instanceof VirtualResourceAttribute virtualResourceAttribute){
            return new VirtualResourceAttributeAction(virtualResourceAttribute, amount, ResourceActionType.COST, actionMode);
        }
        throw new IllegalArgumentException("resourceToModify must be ItemResourceAttribute or VirtualResourceAttribute now. If there is a new type of ITownResourceAttribute, please add it in this method.");
    }

    /**
     * 根据资源属性消耗资源，可消耗多种同属性的不同物品。
     * 不可直接添加ItemResourceAttribute，只能添加对应的物品
     */
    public record ItemResourceAttributeCostAction(ItemResourceAttribute resourceToModify, double amount, ResourceActionMode actionMode)
            implements ITownResourceAction<TownResourceActionResults.ItemResourceAttributeCostActionResult> {
    }

    /**
     * 添加/消耗物品资源。数量为给定的amount。
     */
    public record ItemResourceAction(ItemStack itemToModify, ResourceActionType actionType, double amount, ResourceActionMode actionMode)
            implements ITownResourceAction<TownResourceActionResults.ItemResourceActionResult>{
        public boolean isAdd() {
            return actionType == ResourceActionType.ADD;
        }
    }

    /**
     * 添加/消耗物品。添加/消耗量即为ItemStack的物品数。
     */
    public record ItemStackAction(ItemStack itemToModify, ResourceActionType actionType,
                                  ResourceActionMode actionMode)
            implements ITownResourceAction<TownResourceActionResults.ItemStackActionResult> {
        public boolean isAdd() {
            return actionType == ResourceActionType.ADD;
        }

    }

    public record TownResourceTypeCostAction(ITownResourceType resourceToCost, double amount, int minLevel, int maxLevel, ResourceActionMode actionMode, ResourceActionOrder order)
            implements ITownResourceAction<TownResourceActionResults.TownResourceTypeCostActionResult> {
        public TownResourceTypeCostAction{
            if(minLevel>maxLevel)
                throw new IllegalArgumentException("minLevel must be less than maxLevel");
            if(minLevel<0)
                throw new IllegalArgumentException("minLevel must be greater than 0");
            if(maxLevel>resourceToCost.getMaxLevel())
                throw new IllegalArgumentException("minLevel must be less than maxLevel");
        }

    }

    public record VirtualResourceAttributeAction(VirtualResourceAttribute resourceToModify, double amount, ResourceActionType actionType, ResourceActionMode actionMode)
            implements ITownResourceAction<TownResourceActionResults.VirtualResourceAttributeActionResult> {
        public boolean isAdd(){
            return actionType == ResourceActionType.ADD;
        }
    }

    public record GetAction(IGettable toGet) implements ITownResourceAction<TownResourceActionResults.GetActionResult>{
    }

    public static double get(IActionExecutorHandler handler, IGettable toGet){
        return handler.execute(new GetAction(toGet)).amount();
    }
    public static double get(IActionExecutorHandler handler, ItemStack toGet){
        return get(handler, new ItemStackResourceKey(toGet));
    }
    public static double get(IActionExecutorHandler handler, Item toGet){
        return get(handler, new ItemStackResourceKey(new ItemStack(toGet)));
    }
}
