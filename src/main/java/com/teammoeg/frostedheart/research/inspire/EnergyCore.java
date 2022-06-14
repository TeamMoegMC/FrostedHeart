package com.teammoeg.frostedheart.research.inspire;

import java.util.Map.Entry;

import com.teammoeg.frostedheart.FHConfig;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.climate.IWarmKeepingEquipment;
import com.teammoeg.frostedheart.climate.TemperatureCore;
import com.teammoeg.frostedheart.climate.chunkdata.ChunkData;
import com.teammoeg.frostedheart.compat.CuriosCompat;
import com.teammoeg.frostedheart.content.recipes.DietGroupCodec;
import com.teammoeg.frostedheart.data.FHDataManager;
import com.teammoeg.frostedheart.research.TeamResearchData;
import com.teammoeg.frostedheart.research.api.ResearchDataAPI;

import blusunrize.immersiveengineering.common.util.ItemNBTHelper;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.stats.Stats;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.SleepingTimeCheckEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.diet.api.DietCapability;
import top.theillusivec4.diet.api.IDietTracker;

@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EnergyCore {
	public EnergyCore() {
	}
	public static void dT(ServerPlayerEntity player) {
		CompoundNBT data=TemperatureCore.getFHData(player);
		long tenergy=data.getLong("energy");
		if(tenergy<1)tenergy=1;
		double utbody=data.getDouble("Tbody");
		double m;
		TeamResearchData trd=ResearchDataAPI.getData(player);
		long M=(long) trd.getVariants().getDouble("maxEnergy");
		M*=(1+trd.getVariants().getDouble("pmaxEnergy"));
		double dietValue=0;
		IDietTracker idt=DietCapability.get(player).orElse(null);
		int tdv=0;
		for(Entry<String, Float> vs:idt.getValues().entrySet())
			if(DietGroupCodec.getGroup(vs.getKey()).isBeneficial()) {
				dietValue+=vs.getValue();
				tdv++;
			}
		
		if(tdv!=0)
			dietValue/=tdv;
		if(utbody!=0) {
			int tslastsleep=MathHelper.clamp(player.getStats().getValue(Stats.CUSTOM.get(Stats.TIME_SINCE_REST)), 1, Integer.MAX_VALUE);
			m=(utbody/(Math.pow(tslastsleep,6)+utbody*2)+0.5)*M;
		}else {
			m=0.5*M;
		}
		tenergy+=((0.3934f*(1-tenergy/m)*tenergy+1.3493f*(dietValue-0.4)*tenergy)/(1200*trd.getTeam().get().getOnlineMembers().size()));
		data.putLong("energy", tenergy);
		TemperatureCore.setFHData(player, data);
	}
	public static float applySleep(float tenv,ServerPlayerEntity player) {
		float nkeep=0;
		for (ItemStack is : CuriosCompat.getAllCuriosIfVisible(player)) {
			if (is == null)
				continue;
			Item it = is.getItem();
			if (it instanceof IWarmKeepingEquipment) {//only for direct warm keeping
				nkeep += ((IWarmKeepingEquipment) it).getFactor(player, is);
			} else {
				IWarmKeepingEquipment iw = FHDataManager.getArmor(is);
				if (iw != null)
					nkeep += iw.getFactor(player, is);
			}
		}
		for (ItemStack is : player.getArmorInventoryList()) {
			if (is.isEmpty())
				continue;
			Item it = is.getItem();
			if (it instanceof IWarmKeepingEquipment) {
				nkeep += ((IWarmKeepingEquipment) it).getFactor(player, is);
			} else {//include inner
				String s = ItemNBTHelper.getString(is, "inner_cover");
				IWarmKeepingEquipment iw = null;
				EquipmentSlotType aes = MobEntity.getSlotForItemStack(is);
				if (s.length() > 0 && aes != null) {
					iw = FHDataManager.getArmor(s + "_" + aes.getName());
				} else
					iw = FHDataManager.getArmor(is);
				if (iw != null)
					nkeep += iw.getFactor(player, is);
			}
		}
		if(nkeep>1)
			nkeep=1;
		float nta=(1-nkeep)+0.5f;
		float tbody=30/nta+tenv;
		if(tbody>35)
			tbody=(tbody-37)/2+37;
		CompoundNBT data=TemperatureCore.getFHData(player);
		data.putDouble("utbody",Math.pow(10,tbody/15));
		TemperatureCore.setFHData(player, data);
		return tbody;
	}
	public static boolean consumeEnergy(ServerPlayerEntity player,int val) {
		CompoundNBT data=TemperatureCore.getFHData(player);
		long energy=data.getLong("energy");
		if(energy-1>val) {
			energy-=val;
			data.putLong("energy",energy);
			TemperatureCore.setFHData(player, data);
			return true;
		}
		long penergy=data.getLong("penergy");
		if(penergy>=val) {
			penergy-=val;
			data.putLong("penergy",penergy);
			TemperatureCore.setFHData(player, data);
			return true;
		}
		return false;
	}
	public static void addPersistentEnergy(ServerPlayerEntity player,int val) {
		CompoundNBT data=TemperatureCore.getFHData(player);
		long energy=data.getLong("penergy")+val;
		data.putLong("penergy",energy);
		TemperatureCore.setFHData(player, data);
	}
	public static boolean hasEnoughEnergy(PlayerEntity player,int val) {
		CompoundNBT data=TemperatureCore.getFHData(player);
		return data.getLong("penergy")>val+1||data.getLong("penergy")>val;
	}
	@SubscribeEvent
	public static void updateTemperature(PlayerTickEvent event) {
		if (event.side == LogicalSide.SERVER && event.phase == Phase.START
				&& event.player instanceof ServerPlayerEntity) {
			
			ServerPlayerEntity player = (ServerPlayerEntity) event.player;
			if (player.ticksExisted % 20 == 0)
				EnergyCore.dT(player);
		}
	}
	@SubscribeEvent
	public static void death(PlayerEvent.Clone ev) {
		if(ev.isWasDeath()) {
			CompoundNBT cnbt=TemperatureCore.getFHData(ev.getPlayer());
			cnbt.putLong("penergy",TemperatureCore.getFHData(ev.getOriginal()).getLong("penergy"));
			TemperatureCore.setFHData(ev.getPlayer(), cnbt);
		}else {
			CompoundNBT cnbt=TemperatureCore.getFHData(ev.getOriginal());
			TemperatureCore.setFHData(ev.getPlayer(),cnbt);
		}
	}
	@SubscribeEvent
	public static void checkSleep(SleepingTimeCheckEvent event) {
		if(event.getPlayer().getSleepTimer()>=100&&!event.getPlayer().getEntityWorld().isRemote) {
			EnergyCore.applySleep(ChunkData.getTemperature(event.getPlayer().getEntityWorld(),event.getSleepingLocation().orElseGet(event.getPlayer()::getPosition)),(ServerPlayerEntity) event.getPlayer());
		}
	}
}
