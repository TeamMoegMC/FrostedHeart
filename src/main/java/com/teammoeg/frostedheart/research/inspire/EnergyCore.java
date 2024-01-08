/*
 * Copyright (c) 2022 TeamMoeg
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

package com.teammoeg.frostedheart.research.inspire;

import java.util.Map.Entry;

import com.teammoeg.frostedheart.FHEffects;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.FHPacketHandler;
import com.teammoeg.frostedheart.climate.chunkheatdata.ChunkHeatData;
import com.teammoeg.frostedheart.climate.data.FHDataManager;
import com.teammoeg.frostedheart.climate.player.BodyTemperature;
import com.teammoeg.frostedheart.climate.player.IWarmKeepingEquipment;
import com.teammoeg.frostedheart.compat.CuriosCompat;
import com.teammoeg.frostedheart.content.recipes.DietGroupCodec;
import com.teammoeg.frostedheart.research.api.ResearchDataAPI;
import com.teammoeg.frostedheart.research.data.ResearchVariant;
import com.teammoeg.frostedheart.research.data.TeamResearchData;
import com.teammoeg.frostedheart.research.network.FHEnergyDataSyncPacket;

import blusunrize.immersiveengineering.common.util.ItemNBTHelper;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
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
        CompoundNBT data = BodyTemperature.getFHData(player);
        long oenergy=data.getLong("energy");
        final long tenergy = oenergy+10000;
        double utbody = data.getDouble("utbody");
        long tsls = data.getLong("lastsleep");
        tsls++;
        data.putLong("lastsleep", tsls);
        
        int adenergy=0;
        boolean isBodyNotWell = player.getActivePotionEffect(FHEffects.HYPERTHERMIA) != null || player.getActivePotionEffect(FHEffects.HYPOTHERMIA) != null;
        if (!isBodyNotWell) {
            double m;
            TeamResearchData trd = ResearchDataAPI.getData(player);
            long M = (long) trd.getVariants().getDouble(ResearchVariant.MAX_ENERGY.getToken()) + 30000;
            M *= (1 + trd.getVariants().getDouble(ResearchVariant.MAX_ENERGY_MULT.getToken()));
            double dietValue = 0;
            IDietTracker idt = DietCapability.get(player).orElse(null);
            if(idt!=null) {
	            int tdv = 0;
	            for (Entry<String, Float> vs : idt.getValues().entrySet())
	                if (DietGroupCodec.getGroup(vs.getKey()).isBeneficial()) {
	                    dietValue += vs.getValue();
	                    tdv++;
	                }
	
	            if (tdv != 0)
	                dietValue /= tdv;
            }
            if (utbody != 0) {
                double t = MathHelper.clamp(((int)tsls), 1, Integer.MAX_VALUE) / 1200d;
                //System.out.println(t);
                m = (utbody / (t * t * t * t * t * t + utbody * 2) + 0.5) * M;
            } else {
                m = 0.5 * M;
            }
            double n = trd.getTeam().get().getOnlineMembers().size();
            n = 1 + 0.8 * (n - 1);
            //System.out.println(m);
            //System.out.println(dietValue);
            double nenergy=(0.3934f * (1 - tenergy / m) + 1.3493f * (dietValue - 0.4))*tenergy / 1200;
            //System.out.println(nenergy);
            double cenergy=5/n;
            if(tenergy*2<M&&nenergy<=5) {
            	player.addPotionEffect(new EffectInstance(FHEffects.SAD,200));
            }
            if(tenergy<13500)
            	nenergy=Math.max(nenergy,1);
            double dtenergy = nenergy/n;
            
            if (dtenergy > 0 || tenergy > 15000) {
            	adenergy += dtenergy;
                double frac = MathHelper.frac(dtenergy);
                if (frac > 0 && Math.random() < frac)
                	adenergy++;

            }
            int adcenergy=(int) cenergy;
            double ff=MathHelper.frac(cenergy);
            if(ff>0&& Math.random() <ff)
            	adcenergy++;
            data.putLong("cenergy",Math.min(data.getLong("cenergy")+adcenergy,12000));
        }
        data.putLong("energy",oenergy+adenergy);
        BodyTemperature.setFHData(player, data);
    }

    public static void applySleep(float tenv, ServerPlayerEntity player) {
        float nkeep = 0;
        CompoundNBT data = BodyTemperature.getFHData(player);
        long lsd = data.getLong("lastsleepdate");
        long csd = (player.world.getDayTime() + 12000L) / 24000L;
        //System.out.println("slept");
        if (csd == lsd) return;
        //System.out.println("sleptx");
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
        if (nkeep > 1)
            nkeep = 1;
        float nta = (1 - nkeep) + 0.5f;
        float tbody = 30 / nta + tenv;
        double out=Math.pow(10, 4 - Math.abs(tbody - 40) / 10);
        //System.out.println(out);
        data.putDouble("utbody", out);
        data.putLong("lastsleep", 0);
        data.putLong("lastsleepdate", csd);
        BodyTemperature.setFHData(player, data);
		/*if(tbody>=60) {
			int str=(int) ((tbody-50)/10);
			player.addPotionEffect(new EffectInstance)
		}else if(tbody<10) {
			
		}*/
    }

    public static boolean consumeEnergy(ServerPlayerEntity player, int val) {
        if (player.abilities.isCreativeMode) return true;
        CompoundNBT data = BodyTemperature.getFHData(player);
        long energy = data.getLong("energy");
        if (energy  >= val) {
            energy -= val;
            data.putLong("energy", energy);
            BodyTemperature.setFHData(player, data);
            FHPacketHandler.send(PacketDistributor.PLAYER.with(() -> player), new FHEnergyDataSyncPacket(data));
            return true;
        }
        
        long penergy = data.getLong("penergy");
        if (penergy +energy>= val) {
        	val-=energy;
            penergy -= val;
            data.putLong("penergy", penergy);
            data.putLong("energy", 0);
            BodyTemperature.setFHData(player, data);
            FHPacketHandler.send(PacketDistributor.PLAYER.with(() -> player), new FHEnergyDataSyncPacket(data));
            return true;
        }
        return false;
    }

    public static void addPersistentEnergy(ServerPlayerEntity player, int val) {
        CompoundNBT data = BodyTemperature.getFHData(player);
        long energy = data.getLong("penergy") + val;
        data.putLong("penergy", energy);
        BodyTemperature.setFHData(player, data);
        FHPacketHandler.send(PacketDistributor.PLAYER.with(() -> player), new FHEnergyDataSyncPacket(data));
    }
    public static void addExtraEnergy(ServerPlayerEntity player, int val) {
    	
        CompoundNBT data = BodyTemperature.getFHData(player);
        long energy = data.getLong("cenergy") + val;
        data.putLong("cenergy", energy);
        BodyTemperature.setFHData(player, data);
    }
    public static boolean useExtraEnergy(ServerPlayerEntity player, int val) {
    	if (player.abilities.isCreativeMode) return true;
        CompoundNBT data = BodyTemperature.getFHData(player);
        long energy = data.getLong("cenergy");
        if(energy>=val) {
	        data.putLong("cenergy", energy-val);
	        BodyTemperature.setFHData(player, data);
	        return true;
        }
        return false;
    }
    public static boolean hasExtraEnergy(PlayerEntity player, int val) {
    	if (player.abilities.isCreativeMode) return true;
        CompoundNBT data = BodyTemperature.getFHData(player);
        long energy = data.getLong("cenergy");
        return energy>=val;
    }
    public static void addEnergy(ServerPlayerEntity player, int val) {
    	TeamResearchData trd = ResearchDataAPI.getData(player);
    	long M = (long) trd.getVariants().getDouble(ResearchVariant.MAX_ENERGY.getToken()) + 30000;
        M *= (1 + trd.getVariants().getDouble(ResearchVariant.MAX_ENERGY_MULT.getToken()));
        double n = trd.getTeam().get().getOnlineMembers().size();
        n = 1 + 0.8 * (n - 1);
        CompoundNBT data = BodyTemperature.getFHData(player);
        if (val > 0) {
        	double rv=val/n;
            double frac = MathHelper.frac(rv);
            val=(int) rv;
            if (frac > 0 && Math.random() < frac)
            	val++;

        }
        long energy = Math.min(data.getLong("energy") + val,M);
        data.putLong("energy", energy);
        BodyTemperature.setFHData(player, data);
        FHPacketHandler.send(PacketDistributor.PLAYER.with(() -> player), new FHEnergyDataSyncPacket(data));
    }

    public static boolean hasEnoughEnergy(PlayerEntity player, int val) {
        if (player.abilities.isCreativeMode) return true;
        CompoundNBT data = BodyTemperature.getFHData(player);
        long touse=data.getLong("energy")+data.getLong("penergy");
        return touse >= val;
    }
    public static long getEnergy(PlayerEntity player) {
        CompoundNBT data = BodyTemperature.getFHData(player);
        return data.getLong("energy");
    }
    public static void reportEnergy(PlayerEntity player) {
        CompoundNBT data = BodyTemperature.getFHData(player);
        player.sendMessage(new StringTextComponent("Energy:" + data.getLong("energy") + ",Persist Energy: " + data.getLong("penergy")+",Extra Energy: "+data.getLong("cenergy")), player.getUniqueID());
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
/*
    @SubscribeEvent
    public static void death(PlayerEvent.Clone ev) {
        
            CompoundNBT cnbt = new CompoundNBT();
            cnbt.putLong("penergy", TemperatureCore.getFHData(ev.getOriginal()).getLong("penergy"));
            
            TemperatureCore.setFHData(ev.getPlayer(), cnbt);
            //TemperatureCore.setTemperature(ev.getPlayer(), 0, 0);
        
    }*/

    @SubscribeEvent
    public static void checkSleep(SleepingTimeCheckEvent event) {
        if (event.getPlayer().getSleepTimer() >= 100 && !event.getPlayer().getEntityWorld().isRemote) {
            EnergyCore.applySleep(ChunkHeatData.getTemperature(event.getPlayer().getEntityWorld(), event.getSleepingLocation().orElseGet(event.getPlayer()::getPosition)), (ServerPlayerEntity) event.getPlayer());
        }
    }
}
