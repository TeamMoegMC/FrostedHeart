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

package com.teammoeg.frostedheart.content.trade.policy.snapshot;

import com.teammoeg.frostedheart.content.trade.FHVillagerData;
import com.teammoeg.frostedheart.content.trade.policy.DemandData;

import net.minecraft.world.item.crafting.Ingredient;

public class BuyData {

    String id;
    int store;
    DemandData bd;
    int canRestock;
    public BuyData(FHVillagerData vd,String id, int store, DemandData bd) {
        super();
        this.id = id;
        this.store = store;
        this.bd = bd;
        canRestock=bd.canRestock(vd);
    }

    public int canRestock(FHVillagerData data) {
        return canRestock;
    }

    public Ingredient getItem() {
        return bd.item;
    }

    public int getPrice() {
        return bd.price;
    }

    public int getStore() {
        return store;
    }

    public boolean isFullStock() {
        return store >= bd.maxstore;
    }

    public void reduceStock(FHVillagerData data, int count) {
        bd.soldactions.forEach(c -> c.deal(data, count));
        data.storage.computeIfPresent(id, (k, v) -> v - count);
        store-=count;
    }
}
