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

package com.teammoeg.frostedheart.content.research.research.clues;

import com.teammoeg.chorda.team.TeamDataHolder;
import com.teammoeg.frostedheart.content.research.ResearchListeners;
import com.teammoeg.frostedheart.content.research.data.TeamResearchData;
import com.teammoeg.frostedheart.content.research.research.Research;
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

    public TickListenerClue(String name, String desc, String hint, float contribution) {
        super(name, desc, hint, contribution);
    }

    @Override
    public void initListener(TeamDataHolder t, Research parent) {
        ResearchListeners.getTickClues().add(super.getClueClosure(parent), t.getId());
    }

    public abstract boolean isCompleted(TeamResearchData t, ServerPlayer player);

    @Override
    public void removeListener(TeamDataHolder t, Research parent) {
        ResearchListeners.getTickClues().remove(super.getClueClosure(parent), t.getId());
    }

}
