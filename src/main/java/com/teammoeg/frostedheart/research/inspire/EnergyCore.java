package com.teammoeg.frostedheart.research.inspire;

import java.util.Map.Entry;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.climate.IWarmKeepingEquipment;
import com.teammoeg.frostedheart.climate.TemperatureCore;
import com.teammoeg.frostedheart.climate.chunkdata.ChunkData;
import com.teammoeg.frostedheart.compat.CuriosCompat;
import com.teammoeg.frostedheart.content.recipes.DietGroupCodec;
import com.teammoeg.frostedheart.data.FHDataManager;
import com.teammoeg.frostedheart.network.PacketHandler;
import com.teammoeg.frostedheart.network.research.FHEnergyDataSyncPacket;
import com.teammoeg.frostedheart.research.TeamResearchData;
import com.teammoeg.frostedheart.research.api.ResearchDataAPI;
import com.teammoeg.frostedheart.util.FHEffects;

import blusunrize.immersiveengineering.common.util.ItemNBTHelper;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.SleepingTimeCheckEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.network.PacketDistributor;
import top.theillusivec4.diet.api.DietCapability;
import top.theillusivec4.diet.api.IDietTracker;

@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EnergyCore {
	public EnergyCore() {
	}
	public static void dT(ServerPlayerEntity player) {
		//System.out.println("dt");
		CompoundNBT data=TemperatureCore.getFHData(player);
		long tenergy=data.getLong("energy");
		if(tenergy<10000)tenergy=10000;
		double utbody=data.getDouble("Tbody");
		long tsls=data.getLong("lastsleep");
		tsls++;
		data.putLong("lastsleep", tsls);
		boolean isBodyNotWell=player.getActivePotionEffect(FHEffects.HYPERTHERMIA)!=null||player.getActivePotionEffect(FHEffects.HYPOTHERMIA)!=null;
		if(!isBodyNotWell) {
			double m;
			TeamResearchData trd=ResearchDataAPI.getData(player);
			long M=(long) trd.getVariants().getDouble("maxEnergy")+25000;
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
				double t=MathHelper.clamp(tsls, 1, Integer.MAX_VALUE)/1200d;
				m=(utbody/(t*t*t*t*t*t+utbody*2)+0.5)*M;
			}else {
				m=0.5*M;
			}
			double n=trd.getTeam().get().getOnlineMembers().size();
			n=1+0.75*(n-1);
			//System.out.println(n);
			//System.out.println(m);
			double dtenergy=((0.3934f*(1-tenergy/m)*tenergy+1.3493f*(dietValue-0.4)*tenergy)/(1200*(n)));
			//System.out.println(dtenergy);
			if(dtenergy>0||tenergy>15000) {
				tenergy+=dtenergy;
				double frac=MathHelper.frac(dtenergy);
				if(frac>0&&Math.random()<frac)
					tenergy++;
				
			}
		}
		data.putLong("energy", tenergy);
		TemperatureCore.setFHData(player, data);
	}
	public static void applySleep(float tenv,ServerPlayerEntity player) {
		float nkeep=0;
		CompoundNBT data=TemperatureCore.getFHData(player);
		long lsd=data.getLong("lastsleepdate");
		long csd=(player.world.getDayTime()+12000L)/24000L;
		if(csd==lsd)return;
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
		
		data.putDouble("utbody",Math.pow(10,4-Math.abs(tbody-40)/10));
		data.putLong("lastsleep", 0);
		data.putLong("lastsleepdate",csd);
		TemperatureCore.setFHData(player, data);
		/*if(tbody>=60) {
			int str=(int) ((tbody-50)/10);
			player.addPotionEffect(new EffectInstance)
		}else if(tbody<10) {
			
		}*/
	}
	public static boolean consumeEnergy(ServerPlayerEntity player,int val) {
		if(player.abilities.isCreativeMode)return true;
		CompoundNBT data=TemperatureCore.getFHData(player);
		long energy=data.getLong("energy");
		if(energy-10000>val) {
			energy-=val;
			data.putLong("energy",energy);
			TemperatureCore.setFHData(player, data);
			PacketHandler.send(PacketDistributor.PLAYER.with(() -> player),new FHEnergyDataSyncPacket(data));
			return true;
		}
		long penergy=data.getLong("penergy");
		if(penergy>=val) {
			penergy-=val;
			data.putLong("penergy",penergy);
			TemperatureCore.setFHData(player, data);
			PacketHandler.send(PacketDistributor.PLAYER.with(() -> player),new FHEnergyDataSyncPacket(data));
			return true;
		}
		return false;
	}
	public static void addPersistentEnergy(ServerPlayerEntity player,int val) {
		CompoundNBT data=TemperatureCore.getFHData(player);
		long energy=data.getLong("penergy")+val;
		data.putLong("penergy",energy);
		TemperatureCore.setFHData(player, data);
		PacketHandler.send(PacketDistributor.PLAYER.with(() -> player),new FHEnergyDataSyncPacket(data));
	}
	public static void addEnergy(ServerPlayerEntity player,int val) {
		CompoundNBT data=TemperatureCore.getFHData(player);
		long energy=data.getLong("energy")+val;
		data.putLong("energy",energy);
		TemperatureCore.setFHData(player, data);
		PacketHandler.send(PacketDistributor.PLAYER.with(() -> player),new FHEnergyDataSyncPacket(data));
	}
	public static boolean hasEnoughEnergy(PlayerEntity player,int val) {
		if(player.abilities.isCreativeMode)return true;
		CompoundNBT data=TemperatureCore.getFHData(player);
		return data.getLong("energy")>val+10000||data.getLong("penergy")>val;
	}
	public static void reportEnergy(PlayerEntity player) {
		CompoundNBT data=TemperatureCore.getFHData(player);
		player.sendMessage(new StringTextComponent("Energy:"+data.getLong("energy")+",Persist Energy: "+data.getLong("penergy")),player.getUniqueID());
	}
	@SubscribeEvent
	public static void tickEnergy(PlayerTickEvent event) {
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
