package com.teammoeg.frostedheart.team;

import java.util.UUID;

import com.teammoeg.frostedheart.util.OptionalLazy;

/**
 * Synced data holder for client
 */
public class ClientDataHolder {
	
	/** The instance. */
	public static ClientDataHolder INSTANCE=new ClientDataHolder();
	
	private TeamDataHolder holder=new TeamDataHolder(UUID.randomUUID(),OptionalLazy.empty());
	private ClientDataHolder() {
	}
	
	/**
	 * Reset all data for client.
	 */
	public void reset() {
		holder=new TeamDataHolder(UUID.randomUUID(),OptionalLazy.empty());
	}
	
	/**
	 * Gets the single instance of ClientDataHolder.
	 *
	 * @return single instance of ClientDataHolder
	 */
	public TeamDataHolder getInstance() {
		return holder;
	}
}
