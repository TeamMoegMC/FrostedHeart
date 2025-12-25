package com.teammoeg.chorda.dataholders.client;

import com.teammoeg.chorda.dataholders.DataHolderMap;

public class ClientDataHolder extends DataHolderMap<ClientDataHolder> {
	public ClientDataHolder() {
		super("ClientData");
	}
	boolean isDirty;
	Object lock=new Object();
	public void markDirty() {
		synchronized(lock) {
			isDirty=true;
		}
	}
	boolean getAndClearDirty(){
		boolean ret=isDirty;
		isDirty=false;
		return ret;
	}
}
