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

import com.teammoeg.frostedheart.content.town.resource.ItemResourceAttribute;
import com.teammoeg.frostedheart.content.town.resource.ItemStackResourceKey;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.DoubleAdder;

public class TownResourceActionResults {
    /**
     *
     * @param action 对应的ItemResourceAttributeAction
     * @param allModified 应修改量是否等于实际修改量
     * @param totalModifiedAmount 已修改资源总量
     * @param residualAmount 应修改但未修改的资源量
     * @param details 具体消耗的物品明细。Mao中Double均为正数，为消耗或添加量。action中可查看操作是添加还是消耗。
     */
    public static record ItemResourceAttributeCostActionResult(
            TownResourceActions.ItemResourceAttributeCostAction action, boolean allModified, double totalModifiedAmount, double residualAmount, Map<ItemStackResourceKey, Double> details) implements ITownResourceAttributeActionResult {
        @Override
        public TownResourceActions.ItemResourceAttributeCostAction getAction() {
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

    public record ItemResourceActionResult (TownResourceActions.ItemResourceAction action, boolean allModified, double modifiedAmount, double residualAmount)
            implements ITownResourceActionResult{

        @Override
        public TownResourceActions.ItemResourceAction getAction() {
            return action;
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
    public record ItemStackActionResult(TownResourceActions.ItemStackAction action, Boolean allModified, ItemStack itemStackModified, ItemStack itemStackLeft) implements ITownResourceActionResult {
        @Override
        public TownResourceActions.ItemStackAction getAction(){
            return action;
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
    public record TownResourceTypeCostActionResult(TownResourceActions.TownResourceTypeCostAction action, boolean allCosted, double totalModifiedAmount, double residualAmount, List<ITownResourceAttributeActionResult> details) implements ITownResourceActionResult {

        @Override
        public TownResourceActions.TownResourceTypeCostAction getAction() {
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

    public record GetActionResult(TownResourceActions.GetAction action, double amount) implements ITownResourceActionResult {
        @Override
        public ITownResourceAction getAction() {
            return action;
        }
    }
}
