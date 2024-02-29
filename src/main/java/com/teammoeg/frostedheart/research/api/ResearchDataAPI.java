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

package com.teammoeg.frostedheart.research.api;

import java.util.UUID;

import com.teammoeg.frostedheart.research.data.ResearchVariant;
import com.teammoeg.frostedheart.research.data.TeamResearchData;
import com.teammoeg.frostedheart.team.SpecialDataManager;
import com.teammoeg.frostedheart.team.SpecialDataTypes;

import dev.ftb.mods.ftbteams.FTBTeamsAPI;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;

public class ResearchDataAPI {

    public static TeamResearchData getData(PlayerEntity id) {
        if (id instanceof ServerPlayerEntity)
            return SpecialDataManager.INSTANCE.getData(FTBTeamsAPI.getPlayerTeam((ServerPlayerEntity) id)).getData(SpecialDataTypes.RESEARCH_DATA);
       // return TeamResearchData.getClientInstance();
        return ClientResearchDataAPI.getData();
    }

    public static TeamResearchData getData(UUID id) {
        return SpecialDataManager.INSTANCE.getData(id).getData(SpecialDataTypes.RESEARCH_DATA);

    }

    public static double getVariantDouble(PlayerEntity id, ResearchVariant name) {
        if (id instanceof ServerPlayerEntity)
            return getData(id).getVariantDouble(name);
        return ClientResearchDataAPI.getData().getVariantDouble(name);

    }

    public static double getVariantDouble(UUID id, ResearchVariant name) {
        return getVariants(id).getDouble(name.getToken());
    }

    public static long getVariantLong(PlayerEntity id, ResearchVariant name) {
        if (id instanceof ServerPlayerEntity)
            return getData(id).getVariantLong(name);
        return ClientResearchDataAPI.getData().getVariantLong(name);

    }

    public static long getVariantLong(UUID id, ResearchVariant name) {
        return getVariants(id).getLong(name.getToken());
    }

    public static CompoundNBT getVariants(PlayerEntity id) {
        if (id instanceof ServerPlayerEntity)
            return getData(id).getVariants();
        return ClientResearchDataAPI.getData().getVariants();

    }
    
    public static CompoundNBT getVariants(UUID id) {
        TeamResearchData trd= getData(id);
        if(trd!=null)
        	return trd.getVariants();
        return new CompoundNBT();

    }
    public static void sendVariants(PlayerEntity id) {
        if (id instanceof ServerPlayerEntity)
        	getData(id).sendVariantPacket();

    }
    
    public static void sendVariants(UUID id) {
        TeamResearchData trd=getData(id);
        if(trd!=null)
        	trd.sendVariantPacket();

    }
    
    
    public static boolean isResearchComplete(PlayerEntity id, String research) {
        if (id instanceof ServerPlayerEntity)
            return SpecialDataManager.INSTANCE.getData(FTBTeamsAPI.getPlayerTeam((ServerPlayerEntity) id)).getData(SpecialDataTypes.RESEARCH_DATA).getData(research).isCompleted();
        return ClientResearchDataAPI.getData().getData(research).isCompleted();
    }

    public static void putVariantDouble(ServerPlayerEntity id, ResearchVariant name, double val) {
    	getData(id).putVariantDouble(name, val);
    	sendVariants(id);
    }
    public static void putVariantDouble(ServerPlayerEntity id, String name, double val) {
    	getVariants(id).putDouble(name, val);
    	sendVariants(id);
    }
    public static void putVariantDouble(UUID id, ResearchVariant name, double val) {
    	getData(id).putVariantDouble(name, val);
    	sendVariants(id);
    }
    public static void putVariantDouble(UUID id, String name, double val) {
        getVariants(id).putDouble(name, val);
        sendVariants(id);
    }


    public static void putVariantLong(ServerPlayerEntity id, ResearchVariant name, long val) {
    	getData(id).putVariantLong(name, val);
    	sendVariants(id);
    }
    public static void putVariantLong(ServerPlayerEntity id, String name, long val) {
    	getVariants(id).putDouble(name, val);
    	sendVariants(id);
    }
    public static void putVariantLong(UUID id, ResearchVariant name, long val) {
    	getData(id).putVariantLong(name, val);
    	sendVariants(id);
    }
    public static void putVariantLong(UUID id, String name, long val) {
        getVariants(id).putLong(name, val);
        sendVariants(id);
    }
    private ResearchDataAPI() {
    }
}
