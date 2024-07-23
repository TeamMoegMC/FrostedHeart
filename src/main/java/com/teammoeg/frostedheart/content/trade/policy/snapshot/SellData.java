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
import com.teammoeg.frostedheart.content.trade.policy.ProductionData;

import net.minecraft.world.item.ItemStack;

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

    public boolean canRestock(FHVillagerData data) {

        return this.data.canRestock(data);
    }

    public String getId() {
        return id;
    }

    public ItemStack getItem() {
        return data.item;
    }

    public int getPrice() {
        return data.price;
    }

    public int getStore() {
        return store;
    }

    public boolean isFullStock() {
        return store >= data.maxstore;
    }

    public void reduceStock(FHVillagerData data, int count) {
        data.storage.computeIfPresent(getId(), (k, v) -> v - count);
        this.data.soldactions.forEach(c -> c.deal(data, count));
    }

}
