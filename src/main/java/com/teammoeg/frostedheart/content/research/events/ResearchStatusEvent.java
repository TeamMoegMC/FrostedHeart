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

package com.teammoeg.frostedheart.content.research.events;

import com.teammoeg.frostedheart.content.research.research.Research;


import dev.ftb.mods.ftbteams.api.Team;
import net.minecraftforge.eventbus.api.Event;

public class ResearchStatusEvent extends Event {
    Research research;
    Team team;
    boolean completion;

    public ResearchStatusEvent(Research research, Team team, boolean completion) {
        this.research = research;
        this.team = team;
        this.completion = completion;
    }

    public Research getResearch() {
        return research;
    }

    public Team getTeam() {
        return team;
    }

    public boolean isCompletion() {
        return completion;
    }

}
