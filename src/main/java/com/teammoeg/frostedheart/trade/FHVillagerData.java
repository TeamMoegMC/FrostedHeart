package com.teammoeg.frostedheart.trade;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.teammoeg.frostedheart.climate.ClimateData;
import com.teammoeg.frostedheart.research.api.ResearchDataAPI;
import com.teammoeg.frostedheart.research.data.ResearchVariant;
import com.teammoeg.frostedheart.util.SerializeUtil;

import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.potion.Effects;
import net.minecraft.stats.StatisticsManager;
import net.minecraft.stats.Stats;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.village.GossipType;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.util.Constants.NBT;

public class FHVillagerData {
	ResourceLocation policytype;
	Map<String,Float> storage=new HashMap<>();
	long totaltraded;
	int sawmurder;
	int bargain;
	int totalbenefit;
	int tradelevel;
	long lastUpdated;
	
	public ActionResultType trade(PlayerEntity pe) {
		return ActionResultType.func_233537_a_(pe.world.isRemote);
	}
	public void initLegacy() {
		if(policytype==null) {
			
		}
	}
	public void update(ServerWorld w) {
		long day=ClimateData.getWorldDay(w);
		if(day>lastUpdated) {
			long delta=day-lastUpdated;
			bargain=0;
			totalbenefit=(int) (totalbenefit*Math.pow(0.75f, delta));
			lastUpdated=day;
			getPolicy().calculateRecovery((int) delta, storage);
		}
	}
	public PolicySnapshot getPolicy() {
		return TradePolicy.policies.get(policytype).get(this);
	}
	public CompoundNBT serialize(CompoundNBT data) {
		CompoundNBT list=new CompoundNBT();
		
		for(Entry<String, Float> k:storage.entrySet()) {
			list.putFloat(k.getKey(),k.getValue());
		}
		data.put("storage", list);
		if(policytype!=null)
			data.putString("type",policytype.toString());
		data.putLong("total",totaltraded);
		data.putInt("murder",sawmurder);
		data.putInt("bargain", bargain);
		data.putInt("benefit", totalbenefit);
		data.putInt("level", tradelevel);
		data.putLong("last", lastUpdated);
		return data;
	}
	public void deserialize(CompoundNBT data) {
		CompoundNBT nbt=data.getCompound("storage");
		storage.clear();
		for(String k:nbt.keySet()) {
			storage.put(k,nbt.getFloat(k));
		}
		if(data.contains("type"))
			policytype=new ResourceLocation(data.getString("type"));
		totaltraded=data.getLong("total");
		data.putInt("murder",sawmurder);
		data.putInt("bargain", bargain);
		data.putInt("benefit", totalbenefit);
		data.putInt("level", tradelevel);
		data.putLong("last", lastUpdated);
	}
	public RelationList getRelationShip(PlayerEntity pe,VillagerEntity ve) {
		RelationList list=new RelationList();
		list.put(RelationModifier.FOREIGNER,-10);
		if(!ResearchDataAPI.isResearchComplete(pe,"villager_language"))
			list.put(RelationModifier.UNKNOWN_LANGUAGE, -30);
		list.put(RelationModifier.CHARM,(int) ResearchDataAPI.getVariantDouble(pe,ResearchVariant.VILLAGER_RELATION));
		int killed=getStats(pe).getValue(Stats.ENTITY_KILLED,EntityType.VILLAGER);
		int kdc=(int) Math.min(killed,ResearchDataAPI.getVariantDouble(pe,ResearchVariant.VILLAGER_FORGIVENESS));
		list.put(RelationModifier.KILLED_HISTORY, (killed-kdc)*-5);
		if(ve.getGossip().getReputation(pe.getUniqueID(),e->e==GossipType.MINOR_NEGATIVE)>0)
			list.put(RelationModifier.HURT,-10);
		list.put(RelationModifier.KILLED_SAW,-25*sawmurder);
		if(pe.getActivePotionEffect(Effects.HERO_OF_THE_VILLAGE)!=null) 
			list.put(RelationModifier.SAVED_VILLAGE,10);
		list.put(RelationModifier.RECENT_BARGAIN,-bargain*10);
		list.put(RelationModifier.TRADE_LEVEL,getTradeRelation());
		list.put(RelationModifier.RECENT_BENEFIT,getRecentBenefit());
		return list;
	}
	public int getRecentBenefit() {
		return 0;
	}
	public int getTradeRelation() {
		return 0;
	}
	public int getTradeLevel() {
		return tradelevel;
	}
	private static StatisticsManager getStats(PlayerEntity pe) {
		if(pe instanceof ServerPlayerEntity)
			return ((ServerPlayerEntity) pe).getStats();
		return ((ClientPlayerEntity)pe).getStats();
	}
}
