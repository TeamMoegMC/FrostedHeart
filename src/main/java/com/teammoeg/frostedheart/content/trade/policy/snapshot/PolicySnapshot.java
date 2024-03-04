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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.teammoeg.frostedheart.content.trade.FHVillagerData;
import com.teammoeg.frostedheart.content.trade.policy.BaseData;

public class PolicySnapshot {
    public static final PolicySnapshot empty = new PolicySnapshot() {
        @Override
        public void calculateRecovery(int deltaDays, FHVillagerData data) {
        }

        @Override
        public void fetchTrades(Map<String, Float> data) {
        }

        @Override
        public void register(BaseData bd) {
        }
    };
    Map<String, BaseData> data = new HashMap<>();
    List<BuyData> buys = new ArrayList<>();
    Map<String, SellData> sells = new HashMap<>();
    public int maxExp;

    public void calculateRecovery(int deltaDays, FHVillagerData data) {
        this.data.values().forEach(t -> t.tick(deltaDays, data));
    }

    public void fetchTrades(Map<String, Float> data) {
        this.data.values().forEach(t -> t.fetch(this, data));
    }

    public List<BuyData> getBuys() {
        return buys;
    }

    public Map<String, SellData> getSells() {
        return sells;
    }

    public void register(BaseData bd) {
        data.put(bd.getId(), bd);
    }

    public void registerBuy(BuyData bd) {
        getBuys().add(bd);
    }

    public void registerSell(SellData sd) {
        getSells().put(sd.getId(), sd);
    }

    @Override
    public String toString() {
        return "PolicySnapshot [data=" + data + ", buys=" + getBuys() + ", sells=" + getSells() + "]";
    }
}
