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

package com.teammoeg.frostedheart.trade;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import com.teammoeg.frostedheart.client.util.GuiUtils;
import com.teammoeg.frostedheart.climate.WorldClimate;
import com.teammoeg.frostedheart.research.api.ResearchDataAPI;
import com.teammoeg.frostedheart.research.data.ResearchVariant;
import com.teammoeg.frostedheart.trade.gui.TradeContainer;
import com.teammoeg.frostedheart.trade.policy.TradePolicy;
import com.teammoeg.frostedheart.trade.policy.snapshot.PolicySnapshot;
import com.teammoeg.frostedheart.util.FHUtils;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.potion.Effects;
import net.minecraft.stats.StatisticsManager;
import net.minecraft.stats.Stats;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.village.GossipType;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.util.Constants.NBT;

public class FHVillagerData implements INamedContainerProvider {
    public ResourceLocation policytype;
    public Map<String, Float> storage = new HashMap<>();
    public Map<String, Integer> flags = new HashMap<>();
    Map<UUID, PlayerRelationData> relations = new HashMap<>();
    long lastUpdated = -1;
    public int bargain;
    public long totaltraded;
    private int tradelevel;
    public VillagerEntity parent;

    private static StatisticsManager getStats(PlayerEntity pe) {
        if (pe instanceof ServerPlayerEntity)
            return ((ServerPlayerEntity) pe).getStats();
        return ((ClientPlayerEntity) pe).getStats();
    }

    public FHVillagerData(VillagerEntity parent) {
        super();
        this.parent = parent;
    }

    @Override
    public Container createMenu(int p1, PlayerInventory p2, PlayerEntity p3) {
        TradeContainer tc = new TradeContainer(p1, p2, parent);
        tc.setData(this, p3);
        return tc;
    }

    public void deserialize(CompoundNBT data) {
        CompoundNBT nbt = data.getCompound("storage");
        storage.clear();
        for (String k : nbt.keySet())
            storage.put(k, nbt.getFloat(k));
        flags.clear();
        nbt = data.getCompound("flags");
        for (String ks : nbt.keySet())
            flags.put(ks, nbt.getInt(ks));
        setTradelevel(data.getInt("level"));
        totaltraded = data.getLong("total");
        ListNBT rel = data.getList("relations", NBT.TAG_COMPOUND);
        relations.clear();
        for (INBT u : rel) {
            CompoundNBT item = (CompoundNBT) u;
            PlayerRelationData prd = new PlayerRelationData();
            prd.deserialize(item);
            relations.put(item.getUniqueId("id"), prd);
        }
        if (data.contains("type"))
            policytype = new ResourceLocation(data.getString("type"));
        lastUpdated = data.getLong("last");
    }

    public void deserializeFromRecv(CompoundNBT data) {
        CompoundNBT nbt = data.getCompound("storage");
        storage.clear();
        for (String k : nbt.keySet())
            storage.put(k, nbt.getFloat(k));
        flags.clear();
        nbt = data.getCompound("flags");
        for (String ks : nbt.keySet())
            flags.put(ks, nbt.getInt(ks));
        setTradelevel(data.getInt("level"));
        totaltraded = data.getLong("total");
        if (data.contains("type"))
            policytype = new ResourceLocation(data.getString("type"));
    }

    @Override
    public ITextComponent getDisplayName() {
        return GuiUtils.translateGui("trade.title");
    }

    public PolicySnapshot getPolicy() {
        if (policytype == null) return PolicySnapshot.empty;
        return TradePolicy.policies.get(policytype).get(this);
    }

    public TradePolicy getPolicyType() {
        if (policytype == null) return null;
        return TradePolicy.policies.get(policytype);
    }

    public PlayerRelationData getRelationDataForRead(PlayerEntity pe) {
        return relations.getOrDefault(pe.getUniqueID(), PlayerRelationData.EMPTY);
    }

    public PlayerRelationData getRelationDataForWrite(PlayerEntity pe) {
        return relations.computeIfAbsent(pe.getUniqueID(), d -> new PlayerRelationData());
    }

    public RelationList getRelationShip(PlayerEntity pe) {
        RelationList list = new RelationList();
        PlayerRelationData player = relations.getOrDefault(pe.getUniqueID(), PlayerRelationData.EMPTY);
        list.put(RelationModifier.FOREIGNER, -10);
        if (!ResearchDataAPI.isResearchComplete(pe, "villager_language"))
            list.put(RelationModifier.UNKNOWN_LANGUAGE, -30);
        list.put(RelationModifier.CHARM, (int) ResearchDataAPI.getVariantDouble(pe, ResearchVariant.VILLAGER_RELATION));
        int killed = getStats(pe).getValue(Stats.ENTITY_KILLED, EntityType.VILLAGER);
        int kdc = (int) Math.min(killed, ResearchDataAPI.getVariantDouble(pe, ResearchVariant.VILLAGER_FORGIVENESS));
        list.put(RelationModifier.KILLED_HISTORY, (killed - kdc) * -5);
        if (parent.getGossip().getReputation(pe.getUniqueID(), e -> e == GossipType.MINOR_NEGATIVE) > 0)
            list.put(RelationModifier.HURT, -10);
        list.put(RelationModifier.KILLED_SAW, -25 * player.sawmurder);
        if (pe.getActivePotionEffect(Effects.HERO_OF_THE_VILLAGE) != null)
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

    public void initLegacy(VillagerEntity ve) {
        if (policytype == null) {
            policytype = TradePolicy.random(ve.getRNG()).getName();
            parent.setVillagerData(parent.getVillagerData().withProfession(getPolicyType().getProfession()));
        }
    }

    public CompoundNBT serialize(CompoundNBT data) {
        ListNBT list = new ListNBT();
        CompoundNBT stor = new CompoundNBT();
        for (Entry<String, Float> k : storage.entrySet()) {
            stor.putFloat(k.getKey(), k.getValue());
        }
        CompoundNBT flag = new CompoundNBT();
        for (Entry<String, Integer> k : flags.entrySet())
            flag.putInt(k.getKey(), k.getValue());
        data.put("flags", flag);
        for (Entry<UUID, PlayerRelationData> i : relations.entrySet()) {
            CompoundNBT items = new CompoundNBT();
            items.putUniqueId("id", i.getKey());
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

    public CompoundNBT serializeForSend(CompoundNBT data) {
        ListNBT list = new ListNBT();
        CompoundNBT stor = new CompoundNBT();
        for (Entry<String, Float> k : storage.entrySet()) {
            stor.putFloat(k.getKey(), k.getValue());
        }
        CompoundNBT flag = new CompoundNBT();
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

    public ActionResultType trade(PlayerEntity pe) {
        return ActionResultType.func_233537_a_(pe.world.isRemote);
    }

    public void update(ServerWorld w, PlayerEntity trigger) {
        initLegacy(parent);
        long day = WorldClimate.getWorldDay(w);
        FHUtils.ofMap(relations, trigger.getUniqueID()).ifPresent(t -> t.update(day));
        ;
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
