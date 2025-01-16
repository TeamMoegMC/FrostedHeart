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

package com.teammoeg.frostedheart.content.research.api;

import java.util.Optional;
import java.util.UUID;

import com.teammoeg.chorda.team.CTeamDataManager;
import com.teammoeg.frostedheart.bootstrap.common.FHSpecialDataTypes;
import com.teammoeg.chorda.team.TeamDataClosure;
import com.teammoeg.chorda.team.TeamDataHolder;
import com.teammoeg.frostedheart.content.research.data.ResearchVariant;
import com.teammoeg.frostedheart.content.research.data.TeamResearchData;

import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;

public class ResearchDataAPI {

    public static TeamDataClosure<TeamResearchData> getData(Player id) {
        if (id instanceof ServerPlayer)
            return CTeamDataManager.INSTANCE.get(FTBTeamsAPI.api().getManager().getTeamForPlayer((ServerPlayer) id).get()).getDataHolder(FHSpecialDataTypes.RESEARCH_DATA);
       // return TeamResearchData.getClientInstance();
        return ClientResearchDataAPI.getData();
    }
    
    public static Optional<TeamDataClosure<TeamResearchData>> getData(UUID id) {
        TeamDataHolder holder= CTeamDataManager.INSTANCE.get(id);
        if(holder==null)
        	return Optional.empty();
        
    	return Optional.of(holder.getDataHolder(FHSpecialDataTypes.RESEARCH_DATA));

    }

    public static double getVariantDouble(Player id, ResearchVariant name) {
        if (id instanceof ServerPlayer)
            return getData(id).get().getVariantDouble(name);
        return ClientResearchDataAPI.getData().get().getVariantDouble(name);

    }

    public static double getVariantDouble(UUID id, ResearchVariant name) {
        return getVariants(id).getDouble(name.getToken());
    }

    public static long getVariantLong(Player id, ResearchVariant name) {
        if (id instanceof ServerPlayer)
            return getData(id).get().getVariantLong(name);
        return ClientResearchDataAPI.getData().get().getVariantLong(name);

    }

    public static long getVariantLong(UUID id, ResearchVariant name) {
        return getVariants(id).getLong(name.getToken());
    }

    public static CompoundTag getVariants(Player id) {
        if (id instanceof ServerPlayer)
            return getData(id).get().getVariants();
        return ClientResearchDataAPI.getData().get().getVariants();

    }
    
    public static CompoundTag getVariants(UUID id) {
        return getData(id).map(t->t.get().getVariants()).orElseGet(CompoundTag::new);

    }
    public static void sendVariants(Player id) {
        if (id instanceof ServerPlayer) {
        	TeamDataClosure<TeamResearchData> closure=getData(id);
        	closure.get().sendVariantPacket(closure.team());
        }

    }
    
    public static void sendVariants(UUID id) {
        Optional<TeamDataClosure<TeamResearchData>> trd=getData(id);
        trd.ifPresent(t->t.get().sendVariantPacket(t.team()));

    }
    
    
    public static boolean isResearchComplete(Player id, String research) {
        if (id instanceof ServerPlayer)
            return CTeamDataManager.INSTANCE.get(FTBTeamsAPI.api().getManager().getTeamForPlayer((ServerPlayer) id).get()).getData(FHSpecialDataTypes.RESEARCH_DATA).getData(research).isCompleted();
        return ClientResearchDataAPI.getData().get().getData(research).isCompleted();
    }

    public static void putVariantDouble(Player playerEntity, String key, double val) {
    	getData(playerEntity).get().putVariantDouble(key, val);
    	sendVariants(playerEntity);
    }
    public static void putVariantDouble(ServerPlayer id, String name, double val) {
    	getVariants(id).putDouble(name, val);
    	sendVariants(id);
    }
    public static void putVariantDouble(UUID id, ResearchVariant name, double val) {
    	getData(id).ifPresent(t->{
    		t.get().putVariantDouble(name, val);
    		sendVariants(id);
    	}); 
    	
    }
    public static void putVariantDouble(UUID id, String name, double val) {
        getVariants(id).putDouble(name, val);
        sendVariants(id);
    }


    public static void putVariantLong(ServerPlayer id, ResearchVariant name, long val) {
    	getData(id).get().putVariantLong(name, val);
    	sendVariants(id);
    }
    public static void putVariantLong(ServerPlayer id, String name, long val) {
    	getVariants(id).putDouble(name, val);
    	sendVariants(id);
    }
    public static void putVariantLong(UUID id, ResearchVariant name, long val) {
    	getData(id).ifPresent(t->{
    		t.get().putVariantLong(name, val);
    		sendVariants(id);
    	});
    	
    }
    public static void putVariantLong(UUID id, String name, long val) {
        getVariants(id).putLong(name, val);
        sendVariants(id);
    }
    private ResearchDataAPI() {
    }
}
