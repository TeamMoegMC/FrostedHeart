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

package com.teammoeg.frostedheart.content.trade.policy.snapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
        public void fetchTrades(FHVillagerData vd,Map<String, Float> data) {
        }

        @Override
        public void register(BaseData bd) {
        }
    };
    Map<String, BaseData> data = new HashMap<>();
    List<BuyData> buys = new ArrayList<>();
    // LinkedHashMap 保证注册序：儿童截断取"前一半"时双端确定，不会因 HashMap 迭代序产生键集分歧
    Map<String, SellData> sells = new LinkedHashMap<>();
    public int maxExp;

    public void calculateRecovery(int deltaDays, FHVillagerData data) {
        this.data.values().forEach(t -> t.tick(deltaDays, data));
    }

    public void fetchTrades(FHVillagerData vd,Map<String, Float> data) {
        this.data.values().forEach(t -> t.fetch(this,vd, data));
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

    /**
     * 儿童商贩：只保留前一半商品条目（低档为主），至少保留 1 条。价格与库存不动。
     */
    public void trimTradesToHalf() {
        if (sells.size() > 1) {
            List<SellData> list = new ArrayList<>(sells.values());
            Map<String, SellData> kept = new LinkedHashMap<>();
            for (SellData sd : list.subList(0, Math.max(1, list.size() / 2)))
                kept.put(sd.getId(), sd);
            sells = kept;
        }
        if (buys.size() > 1) {
            buys = new ArrayList<>(buys.subList(0, Math.max(1, buys.size() / 2)));
        }
    }

    @Override
    public String toString() {
        return "PolicySnapshot [data=" + data + ", buys=" + getBuys() + ", sells=" + getSells() + "]";
    }
}
