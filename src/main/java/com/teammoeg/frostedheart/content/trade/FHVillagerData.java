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

package com.teammoeg.frostedheart.content.trade;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import com.teammoeg.frostedheart.content.climate.WorldClimate;
import com.teammoeg.frostedheart.content.research.api.ResearchDataAPI;
import com.teammoeg.frostedheart.content.research.data.ResearchVariant;
import com.teammoeg.frostedheart.content.trade.gui.TradeContainer;
import com.teammoeg.frostedheart.content.trade.policy.TradePolicy;
import com.teammoeg.frostedheart.content.trade.policy.snapshot.PolicySnapshot;
import com.teammoeg.frostedheart.util.FHUtils;
import com.teammoeg.frostedheart.util.TranslateUtils;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.MenuProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.gossip.GossipType;
import net.minecraft.server.level.ServerLevel;

public class FHVillagerData implements MenuProvider {
    public ResourceLocation policytype;
    public Map<String, Float> storage = new HashMap<>();
    public Map<String, Integer> flags = new HashMap<>();
    Map<UUID, PlayerRelationData> relations = new HashMap<>();
    long lastUpdated = -1;
    public int bargain;
    public long totaltraded;
    private int tradelevel;
    public Villager parent;

    private static ServerStatsCounter getStats(Player pe) {
        if (pe instanceof ServerPlayer)
            return ((ServerPlayer) pe).getStats();
        return null;
        //return pe.getStats();
    }

    public FHVillagerData(Villager parent) {
        super();
        this.parent = parent;
    }

    @Override
    public AbstractContainerMenu createMenu(int p1, Inventory p2, Player p3) {
        TradeContainer tc = new TradeContainer(p1, p2, parent);
        tc.setData(this, p3);
        return tc;
    }

    public void deserialize(CompoundTag data) {
        CompoundTag nbt = data.getCompound("storage");
        storage.clear();
        for (String k : nbt.getAllKeys())
            storage.put(k, nbt.getFloat(k));
        flags.clear();
        nbt = data.getCompound("flags");
        for (String ks : nbt.getAllKeys())
            flags.put(ks, nbt.getInt(ks));
        setTradelevel(data.getInt("level"));
        totaltraded = data.getLong("total");
        ListTag rel = data.getList("relations", Tag.TAG_COMPOUND);
        relations.clear();
        for (Tag u : rel) {
            CompoundTag item = (CompoundTag) u;
            PlayerRelationData prd = new PlayerRelationData();
            prd.deserialize(item);
            relations.put(item.getUUID("id"), prd);
        }
        if (data.contains("type"))
            policytype = new ResourceLocation(data.getString("type"));
        lastUpdated = data.getLong("last");
    }

    public void deserializeFromRecv(CompoundTag data) {
        CompoundTag nbt = data.getCompound("storage");
        storage.clear();
        for (String k : nbt.getAllKeys())
            storage.put(k, nbt.getFloat(k));
        flags.clear();
        nbt = data.getCompound("flags");
        for (String ks : nbt.getAllKeys())
            flags.put(ks, nbt.getInt(ks));
        setTradelevel(data.getInt("level"));
        totaltraded = data.getLong("total");
        if (data.contains("type"))
            policytype = new ResourceLocation(data.getString("type"));
    }

    @Override
    public Component getDisplayName() {
        return TranslateUtils.translateGui("trade.title");
    }

    public PolicySnapshot getPolicy() {
        if (policytype == null) return PolicySnapshot.empty;
        return TradePolicy.policies.get(policytype).get(this);
    }

    public TradePolicy getPolicyType() {
        if (policytype == null) return null;
        return TradePolicy.policies.get(policytype);
    }

    public PlayerRelationData getRelationDataForRead(Player pe) {
        return relations.getOrDefault(pe.getUUID(), PlayerRelationData.EMPTY);
    }

    public PlayerRelationData getRelationDataForWrite(Player pe) {
        return relations.computeIfAbsent(pe.getUUID(), d -> new PlayerRelationData());
    }

    public RelationList getRelationShip(Player pe) {
        RelationList list = new RelationList();
        PlayerRelationData player = relations.getOrDefault(pe.getUUID(), PlayerRelationData.EMPTY);
        list.put(RelationModifier.FOREIGNER, -10);
        if (!ResearchDataAPI.isResearchComplete(pe, "villager_language"))
            list.put(RelationModifier.UNKNOWN_LANGUAGE, -30);
        list.put(RelationModifier.CHARM, (int) ResearchDataAPI.getVariantDouble(pe, ResearchVariant.VILLAGER_RELATION));
        int killed=0;
        
        if(getStats(pe)!=null)
        	killed = getStats(pe).getValue(Stats.ENTITY_KILLED.get(EntityType.VILLAGER));
        int kdc = (int) Math.min(killed, ResearchDataAPI.getVariantDouble(pe, ResearchVariant.VILLAGER_FORGIVENESS));
        list.put(RelationModifier.KILLED_HISTORY, (killed - kdc) * -5);
        if (parent.getGossips().getReputation(pe.getUUID(), e -> e == GossipType.MINOR_NEGATIVE) > 0)
            list.put(RelationModifier.HURT, -10);
        list.put(RelationModifier.KILLED_SAW, -25 * player.sawmurder);
        if (pe.getEffect(MobEffects.HERO_OF_THE_VILLAGE) != null)
            list.put(RelationModifier.SAVED_VILLAGE, 10);
        list.put(RelationModifier.RECENT_BARGAIN, -bargain * 10);
        list.put(RelationModifier.TRADE_LEVEL, getTradeLevel() * 5);
        list.put(RelationModifier.RECENT_BENEFIT, player.totalbenefit);
        return list;
    }

    public long getTotaltraded() {
        return totaltraded;
    }

    public int getTradeLevel() {
        return tradelevel;
    }

    public void initLegacy(Villager ve) {
        if (policytype == null) {
            policytype = TradePolicy.random(ve.getRandom()).getName();
            parent.setVillagerData(parent.getVillagerData().setProfession(getPolicyType().getProfession()));
        }
    }

    public CompoundTag serialize(CompoundTag data) {
        ListTag list = new ListTag();
        CompoundTag stor = new CompoundTag();
        for (Entry<String, Float> k : storage.entrySet()) {
            stor.putFloat(k.getKey(), k.getValue());
        }
        CompoundTag flag = new CompoundTag();
        for (Entry<String, Integer> k : flags.entrySet())
            flag.putInt(k.getKey(), k.getValue());
        data.put("flags", flag);
        for (Entry<UUID, PlayerRelationData> i : relations.entrySet()) {
            CompoundTag items = new CompoundTag();
            items.putUUID("id", i.getKey());
            i.getValue().serialize(items);
            list.add(items);
        }
        data.putLong("total", getTotaltraded());
        data.putInt("level", getTradeLevel());
        data.put("storage", stor);
        data.put("relations", list);
        if (policytype != null)
            data.putString("type", policytype.toString());
        data.putLong("last", lastUpdated);
        return data;
    }

    public CompoundTag serializeForSend(CompoundTag data) {
        ListTag list = new ListTag();
        CompoundTag stor = new CompoundTag();
        for (Entry<String, Float> k : storage.entrySet()) {
            stor.putFloat(k.getKey(), k.getValue());
        }
        CompoundTag flag = new CompoundTag();
        for (Entry<String, Integer> k : flags.entrySet())
            flag.putInt(k.getKey(), k.getValue());
        data.put("flags", flag);
        data.putLong("total", getTotaltraded());
        data.putInt("level", getTradeLevel());
        data.put("storage", stor);
        data.put("relations", list);
        if (policytype != null)
            data.putString("type", policytype.toString());
        return data;
    }

    public void setTradelevel(int tradelevel) {
        this.tradelevel = tradelevel;
    }

    public InteractionResult trade(Player pe) {
        return InteractionResult.sidedSuccess(pe.level().isClientSide);
    }

    public void update(ServerLevel w, Player trigger) {
        initLegacy(parent);
        long day = WorldClimate.getWorldDay(w);
        FHUtils.ofMap(relations, trigger.getUUID()).ifPresent(t -> t.update(day));
        if (lastUpdated == -1) {
            lastUpdated = day - 1;
        }
        if (day > lastUpdated) {
            long delta = day - lastUpdated;
            bargain = 0;
            lastUpdated = day;
            getPolicy().calculateRecovery((int) delta, this);
        }
    }

    public void updateLevel() {
        if (totaltraded > 4000) {
            setTradelevel(4);
            return;
        }
        for (int i = 1; i < 5; i++) {
            if (totaltraded < 400 * (i * (i + 1) / 2)) {
                setTradelevel(i - 1);
                break;
            }
        }
    }
}
