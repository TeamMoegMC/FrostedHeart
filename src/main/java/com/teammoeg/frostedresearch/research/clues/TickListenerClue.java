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

package com.teammoeg.frostedresearch.research.clues;

import com.teammoeg.chorda.dataholders.team.TeamDataHolder;
import com.teammoeg.frostedresearch.ResearchHooks;
import com.teammoeg.frostedresearch.data.TeamResearchData;
import com.teammoeg.frostedresearch.research.Research;

import net.minecraft.server.level.ServerPlayer;

public abstract class TickListenerClue extends ListenerClue {

    public TickListenerClue() {
        super();
    }


    public TickListenerClue(BaseData data) {
        super(data);
    }


    public TickListenerClue(String name, float contribution) {
        super(name, contribution);
    }


   

	public TickListenerClue(String nonce, String name, String desc, String hint, float contribution, boolean required, boolean alwaysOn) {
		super(nonce, name, desc, hint, contribution, required, alwaysOn);
	}


	@Override
    public void initListener(TeamDataHolder t, Research parent) {
        ResearchHooks.getTickClues().add(super.getClueClosure(parent), t.getId());
    }

    public abstract boolean isCompleted(TeamResearchData t, ServerPlayer player);

    @Override
    public void removeListener(TeamDataHolder t, Research parent) {
        ResearchHooks.getTickClues().remove(super.getClueClosure(parent), t.getId());
    }

}
