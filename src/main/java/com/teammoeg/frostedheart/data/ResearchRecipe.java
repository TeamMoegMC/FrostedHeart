package com.teammoeg.frostedheart.data;

import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.research.FHResearch;
import com.teammoeg.frostedheart.research.Research;
import com.teammoeg.frostedheart.research.TeamResearchData;
import com.teammoeg.frostedheart.research.api.ResearchDataAPI;

import dev.ftb.mods.ftbteams.FTBTeamsAPI;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;

public class ResearchRecipe extends JsonDataHolder {

	public ResearchRecipe(JsonObject data) {
		super(data);
	}
	public String getVariant() {
		return super.getString("variant");
	}
	public String getResearch() {
		return super.getString("research");
	}
	public boolean test(PlayerEntity pe) {
		if(pe instanceof ServerPlayerEntity){
			String research=getResearch();
			if(research!=null) {
				Research rs=FHResearch.getResearch(research).get();
				if(rs!=null&&rs.isCompleted(FTBTeamsAPI.getPlayerTeam((ServerPlayerEntity)pe)))return true;
				return false;
			}
			String variant=getVariant();
			if(variant!=null) {
				return ResearchDataAPI.getVariants((ServerPlayerEntity) pe).getBoolean(variant);
			}
		}
		return false;
	}
}
