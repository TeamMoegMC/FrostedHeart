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
