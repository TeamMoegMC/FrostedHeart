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

package com.teammoeg.frostedheart.trade.policy.snapshot;

import com.teammoeg.frostedheart.trade.FHVillagerData;
import com.teammoeg.frostedheart.trade.policy.ProductionData;
import net.minecraft.item.ItemStack;

public class SellData {
    String id;
    int store;

    ProductionData data;

    public SellData(String id, int store, ProductionData data) {
        super();
        this.id = id;
        this.store = store;
        this.data = data;
    }

    public ItemStack getItem() {
        return data.item;
    }

    public int getStore() {
        return store;
    }

    public int getPrice() {
        return data.price;
    }

    public boolean canRestock(FHVillagerData data) {

        return this.data.canRestock(data);
    }

    public boolean isFullStock() {
        return store >= data.maxstore;
    }

    public void reduceStock(FHVillagerData data, int count) {
        data.storage.computeIfPresent(getId(), (k, v) -> v - count);
        this.data.soldactions.forEach(c -> c.deal(data, count));
    }

    public String getId() {
        return id;
    }

}
