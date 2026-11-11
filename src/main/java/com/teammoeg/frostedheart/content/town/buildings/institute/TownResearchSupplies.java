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

package com.teammoeg.frostedheart.content.town.buildings.institute;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.resource.ItemStackResourceKey;
import com.teammoeg.frostedheart.content.town.resource.TeamTownResourceHolder;
import com.teammoeg.frostedheart.content.town.resource.action.ResourceActionMode;
import com.teammoeg.frostedheart.content.town.resource.action.ResourceActionType;
import com.teammoeg.frostedheart.content.town.resource.action.TownResourceActionResults;
import com.teammoeg.frostedheart.content.town.resource.action.TownResourceActions;
import com.teammoeg.frostedresearch.knowledge.definition.ProjectRequirement;
import com.teammoeg.frostedresearch.knowledge.labor.ResearchSupplies;

import net.minecraft.core.registries.BuiltInRegistries;

/**
 * 研究人力从城镇仓库取用每天的物品，与住宅从仓库取食物相同；数量可以是小数。
 * <p>
 * Research labor draws its daily items from the town warehouse, as houses draw food; amounts may be fractional. A
 * joint take reserves stock item by item, so two entries that match the same item cannot both claim it.
 */
public final class TownResearchSupplies implements ResearchSupplies {
    private final TeamTown town;

    public TownResearchSupplies(TeamTown town) {
        this.town = town;
    }

    @Override
    public double share(List<ProjectRequirement.Material> items, List<Double> amounts) {
        Map<ItemStackResourceKey, Double> stock = town.getResourceHolder().getAllItems();
        double share = 1;
        for (int i = 0; i < items.size(); i++) {
            double wanted = amounts.get(i);
            if (!(wanted > TeamTownResourceHolder.DELTA)) continue;
            double held = 0;
            for (Map.Entry<ItemStackResourceKey, Double> entry : stock.entrySet())
                if (items.get(i).matches(entry.getKey().toItemStack())) held += entry.getValue();
            share = Math.min(share, held / wanted);
        }
        return Math.max(0, share);
    }

    @Override
    public boolean take(List<ProjectRequirement.Material> items, List<Double> amounts) {
        Map<ItemStackResourceKey, Double> stock = new HashMap<>(town.getResourceHolder().getAllItems());
        List<ItemStackResourceKey> order = new ArrayList<>(stock.keySet());
        // A stable order keeps repeated days drawing from the same stacks.
        order.sort(Comparator.comparing((ItemStackResourceKey key) -> BuiltInRegistries.ITEM.getKey(key.getItem()).toString())
                .thenComparing(key -> String.valueOf(key.getCompoundTag())));
        List<Map.Entry<ItemStackResourceKey, Double>> plan = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            double wanted = amounts.get(i);
            if (!(wanted > TeamTownResourceHolder.DELTA)) continue;
            for (ItemStackResourceKey key : order) {
                if (wanted <= TeamTownResourceHolder.DELTA) break;
                double held = stock.getOrDefault(key, 0.0);
                if (held <= TeamTownResourceHolder.DELTA || !items.get(i).matches(key.toItemStack())) continue;
                double used = Math.min(held, wanted);
                stock.put(key, held - used);
                plan.add(Map.entry(key, used));
                wanted -= used;
            }
            if (wanted > TeamTownResourceHolder.DELTA) return false;
        }
        for (Map.Entry<ItemStackResourceKey, Double> use : plan) {
            TownResourceActionResults.ItemResourceActionResult result = town.getActionExecutorHandler().execute(
                    new TownResourceActions.ItemResourceAction(use.getKey().toItemStack(), ResourceActionType.COST,
                            use.getValue(), ResourceActionMode.MAXIMIZE));
            if (result.modifiedAmount() + TeamTownResourceHolder.DELTA < use.getValue())
                FHMain.LOGGER.warn("Research labor took {} of {} {} that the warehouse listed", result.modifiedAmount(),
                        use.getValue(), BuiltInRegistries.ITEM.getKey(use.getKey().getItem()));
        }
        return true;
    }
}
