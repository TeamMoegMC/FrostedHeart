/*
 * Copyright (c) 2022-2024 TeamMoeg
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

import javax.annotation.Nullable;

import com.teammoeg.frostedheart.FHCapabilities;
import com.teammoeg.frostedheart.FHEffects;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.climate.player.PlayerTemperatureData;
import com.teammoeg.frostedheart.content.recipes.DietGroupCodec;
import com.teammoeg.frostedheart.research.api.ResearchDataAPI;
import com.teammoeg.frostedheart.research.data.ResearchVariant;
import com.teammoeg.frostedheart.research.data.TeamResearchData;
import com.teammoeg.frostedheart.research.network.FHEnergyDataSyncPacket;
import com.teammoeg.frostedheart.util.client.GuiUtils;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.network.PacketDistributor;
import top.theillusivec4.diet.api.DietCapability;
import top.theillusivec4.diet.api.IDietTracker;

public class EnergyCore implements INBTSerializable<CompoundNBT> {
    long energy;
    long cenergy;
    long penergy;
    long lastsleepdate;
    long lastsleep;
    double utbody;
    
    public static void addEnergy(ServerPlayerEntity player, int val) {
        TeamResearchData trd = ResearchDataAPI.getData(player);
        long M = (long) trd.getVariants().getDouble(ResearchVariant.MAX_ENERGY.getToken()) + 30000;
        M *= (1 + trd.getVariants().getDouble(ResearchVariant.MAX_ENERGY_MULT.getToken()));
        double n = trd.getTeam().get().getOnlineMembers().size();
        n = 1 + 0.8 * (n - 1);
        EnergyCore data=getCapability(player).orElse(null);
        if (val > 0) {
            double rv = val / n;
            double frac = MathHelper.frac(rv);
            val = (int) rv;
            if (frac > 0 && Math.random() < frac)
                val++;

        }
        data.energy = Math.min(data.energy + val, M);

        FHNetwork.send(PacketDistributor.PLAYER.with(() -> player), new FHEnergyDataSyncPacket(player));
    }

    public static void addExtraEnergy(ServerPlayerEntity player, int val) {
    	getCapability(player).ifPresent(t->t.cenergy+=val);
    }

    public static void addPersistentEnergy(ServerPlayerEntity player, int val) {
    	getCapability(player).ifPresent(t->t.penergy+=val);
        FHNetwork.send(PacketDistributor.PLAYER.with(() -> player), new FHEnergyDataSyncPacket(player));
    }

    public static void applySleep(ServerPlayerEntity player) {
        float nkeep = 0;
        EnergyCore data=getCapability(player).orElse(null);
        long lsd = data.lastsleepdate;
        long csd = (player.world.getDayTime() + 12000L) / 24000L;
        //System.out.println("slept");
        if (csd == lsd) return;
        //System.out.println("sleptx");
        float tbody = PlayerTemperatureData.getCapability(player).map(t->t.getFeelTemp()).orElse(0f);
        double out = Math.pow(10, 4 - Math.abs(tbody - 40) / 10);
        //System.out.println(out);
        data.utbody=out;
        data.lastsleep=0;
        data.lastsleepdate=csd;
    }



    public static boolean consumeEnergy(ServerPlayerEntity player, int val) {
        if (player.abilities.isCreativeMode) return true;
        EnergyCore data=getCapability(player).orElse(null);
        if (data.energy >= val) {
        	data.energy -= val;
            FHNetwork.send(PacketDistributor.PLAYER.with(() -> player), new FHEnergyDataSyncPacket(player));
            return true;
        }

        if (data.penergy + data.energy >= val) {
            val -= data.energy;
            data.penergy -= val;
            FHNetwork.send(PacketDistributor.PLAYER.with(() -> player), new FHEnergyDataSyncPacket(player));
            return true;
        }
        return false;
    }

    public static void dT(ServerPlayerEntity player) {
        //System.out.println("dt");
    	EnergyCore data=getCapability(player).orElse(null);
        final long tenergy = data.energy + 10000;
        data.lastsleep++;
        boolean isBodyNotWell = player.getActivePotionEffect(FHEffects.HYPERTHERMIA.get()) != null || player.getActivePotionEffect(FHEffects.HYPOTHERMIA.get()) != null;
        if (!isBodyNotWell) {
            double m;
            TeamResearchData trd = ResearchDataAPI.getData(player);
            long M = (long) trd.getVariants().getDouble(ResearchVariant.MAX_ENERGY.getToken()) + 30000;
            M *= (1 + trd.getVariants().getDouble(ResearchVariant.MAX_ENERGY_MULT.getToken()));
            double dietValue = 0;
            IDietTracker idt = DietCapability.get(player).orElse(null);
            if (idt != null) {
                int tdv = 0;
                for (Entry<String, Float> vs : idt.getValues().entrySet())
                    if (DietGroupCodec.getGroup(vs.getKey()).isBeneficial()) {
                        dietValue += vs.getValue();
                        tdv++;
                    }

                if (tdv != 0)
                    dietValue /= tdv;
            }
            if ( data.utbody != 0) {
                double t = MathHelper.clamp(((int)data.lastsleep), 1, Integer.MAX_VALUE) / 1200d;
                //System.out.println(t);
                m = ( data.utbody / (t * t * t * t * t * t +  data.utbody * 2) + 0.5) * M;
            } else {
                m = 0.5 * M;
            }
            double n = trd.getTeam().get().getOnlineMembers().size();
            n = 1 + 0.8 * (n - 1);
            //System.out.println(m);
            //System.out.println(dietValue);
            double nenergy = (0.3934f * (1 - tenergy / m) + 1.3493f * (dietValue - 0.4)) * tenergy / 1200;
            //System.out.println(nenergy);
            double cenergy = 5 / n;
            if (tenergy * 2 < M && nenergy <= 5) {
                player.addPotionEffect(new EffectInstance(FHEffects.SAD.get(), 200, 0, false, false));
            }
            if (tenergy < 13500)
                nenergy = Math.max(nenergy, 1);
            double dtenergy = nenergy / n;

            if (dtenergy > 0 || tenergy > 15000) {
            	data.energy += dtenergy;
                double frac = MathHelper.frac(dtenergy);
                if (frac > 0 && Math.random() < frac)
                	data.energy++;

            }
            data.cenergy += (int) cenergy;
            double ff = MathHelper.frac(cenergy);
            if (ff > 0 && Math.random() < ff)
            	data.cenergy++;
            data.cenergy=Math.min(data.cenergy, 12000);
        }
        FHNetwork.send(PacketDistributor.PLAYER.with(() -> player), new FHEnergyDataSyncPacket(player));
    }

    public static long getEnergy(PlayerEntity player) {
        return getCapability(player).map(t->t.energy).orElse(0L);
    }

    public static boolean hasEnoughEnergy(PlayerEntity player, int val) {
        if (player.abilities.isCreativeMode) return true;
        long touse = getCapability(player).map(t->t.energy+t.penergy).orElse(0L);
        return touse >= val;
    }

    public static boolean hasExtraEnergy(PlayerEntity player, int val) {
        if (player.abilities.isCreativeMode) return true;
        return getCapability(player).map(t->t.cenergy).orElse(0L) >= val;
    }

    public static void reportEnergy(PlayerEntity player) {
    	
    	EnergyCore data=getCapability(player).orElse(null);
        player.sendMessage(GuiUtils.str("Energy:" + data.energy + ",Persist Energy: " + data.penergy + ",Extra Energy: " + data.cenergy), player.getUniqueID());
    }
 
    public static boolean useExtraEnergy(ServerPlayerEntity player, int val) {
        if (player.abilities.isCreativeMode) return true;
        EnergyCore data=getCapability(player).orElse(null);
        long energy = data.cenergy;
        if (energy >= val) {
            data.cenergy-=val;
            return true;
        }
        return false;
    }

    public EnergyCore() {
    }
    public static LazyOptional<EnergyCore> getCapability(@Nullable PlayerEntity player) {
    	return FHCapabilities.ENERGY.getCapability(player);
    }

	@Override
	public CompoundNBT serializeNBT() {
		CompoundNBT saved=new CompoundNBT();
	    saved.putLong("energy", energy);
	    saved.putLong("cenergy", cenergy);
	    saved.putLong("penergy", penergy);
	    saved.putLong("lastsleepdate", lastsleepdate);
	    saved.putLong("lastsleep", lastsleep);
	    saved.putDouble("utbody", utbody);
		return saved;
	}
	@Override
	public void deserializeNBT(CompoundNBT saved) {
		energy=saved.getLong("energy");
		cenergy=saved.getLong("cenergy");
		penergy=saved.getLong("penergy");
	    lastsleepdate=saved.getLong("lastsleepdate");
	    lastsleep=saved.getLong("lastsleep");
	    utbody=saved.getDouble("utbody");
	}

	public void onrespawn() {
        utbody=0;
        lastsleep=0;
        lastsleepdate=0;
        energy=energy/2;
        
	}
	public void sendUpdate(ServerPlayerEntity player) {
		FHNetwork.send(PacketDistributor.PLAYER.with(() -> player), new FHEnergyDataSyncPacket(player));
	}
}
