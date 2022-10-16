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

package com.teammoeg.frostedheart.research.events;

import com.teammoeg.frostedheart.research.Research;
import net.minecraftforge.eventbus.api.Event;

public class ClientResearchStatusEvent extends Event {
    Research research;
    boolean completion;
    boolean statusChanged;
    public Research getResearch() {
        return research;
    }

    public boolean isCompletion() {
        return completion;
    }

	public ClientResearchStatusEvent(Research research, boolean completion, boolean statusChanged) {
		super();
		this.research = research;
		this.completion = completion;
		this.statusChanged = statusChanged;
	}

	public boolean isStatusChanged() {
		return statusChanged;
	}



}
