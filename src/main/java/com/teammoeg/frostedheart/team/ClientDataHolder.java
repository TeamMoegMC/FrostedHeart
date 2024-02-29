package com.teammoeg.frostedheart.team;

import java.util.UUID;

import com.teammoeg.frostedheart.util.OptionalLazy;

public class ClientDataHolder {
	public static ClientDataHolder INSTANCE=new ClientDataHolder();
	private TeamDataHolder holder=new TeamDataHolder(UUID.randomUUID(),OptionalLazy.empty());
	private ClientDataHolder() {
	}
	public void reset() {
		holder=new TeamDataHolder(UUID.randomUUID(),OptionalLazy.empty());
	}
	public TeamDataHolder getInstance() {
		return holder;
	}
}
