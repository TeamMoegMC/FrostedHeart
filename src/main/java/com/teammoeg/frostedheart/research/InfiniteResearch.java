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

package com.teammoeg.frostedheart.research;

import java.util.function.Supplier;

import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.research.data.ResearchData;
import com.teammoeg.frostedheart.research.data.TeamResearchData;
import com.teammoeg.frostedheart.research.effects.Effect;
import dev.ftb.mods.ftbteams.data.Team;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.IItemProvider;

public class InfiniteResearch extends Research {

	public InfiniteResearch(String path, ResearchCategory category, Supplier<Research>... parents) {
		super(path, category, parents);
	}

	public InfiniteResearch() {
	}

	public InfiniteResearch(String id, JsonObject jo) {
		super(id, jo);
	}

	public InfiniteResearch(PacketBuffer data) {
		super(data);
	}

	public InfiniteResearch(String id, ResearchCategory category, IItemProvider icon, Supplier<Research>... parents) {
		super(id, category, icon, parents);
	}

	public InfiniteResearch(String id, ResearchCategory category, ItemStack icon, Supplier<Research>... parents) {
		super(id, category, icon, parents);
	}
	public void grantEffects(TeamResearchData team,ServerPlayerEntity spe) {
		super.grantEffects(team,spe);
    	boolean allgranted=true;
		for (Effect e : getEffects()) {
            allgranted&=team.isEffectGranted(e);
    	}
		if(allgranted) {
			ResearchData rd=team.getData(this);
			team.clearData(this);
			ResearchData rd2=team.getData(this);
			rd2.setLevel(rd.getLevel()+1);
			Team t=team.getTeam().orElse(null);
			if(t!=null)
				this.sendProgressPacket(t);
			
		}
    }
}
