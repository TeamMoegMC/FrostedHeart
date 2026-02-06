/*
 * Copyright (c) 2026 TeamMoeg
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

package com.teammoeg.frostedresearch.events;

import java.util.Map;

import javax.annotation.Nullable;

import com.teammoeg.frostedresearch.UnlockList;
import com.teammoeg.frostedresearch.data.TeamResearchData;
import com.teammoeg.frostedresearch.data.UnlockListType;

import lombok.Getter;
import net.minecraftforge.eventbus.api.Event;

public class PopulateUnlockListEvent  extends Event{
	@Nullable
	@Getter
	private final TeamResearchData data;
	@Getter
	private final Map<UnlockListType<?>,UnlockList<?>> unlockList;

	public <T> void registerUnlockList(UnlockListType<T> name,UnlockList<T> unlockList) {
		this.unlockList.put(name, unlockList);
	}

	public PopulateUnlockListEvent(TeamResearchData data) {
		super();
		this.data = data;
		this.unlockList = data.unlocklists;
	}

	public PopulateUnlockListEvent(Map<UnlockListType<?>, UnlockList<?>> unlockList) {
		super();
		this.data=null;
		this.unlockList = unlockList;
	}

}
