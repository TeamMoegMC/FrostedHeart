package com.teammoeg.frostedresearch.handler;

import com.teammoeg.frostedresearch.ResearchListeners;

import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

public class ServerReloadListener implements ResourceManagerReloadListener {

	public ServerReloadListener() {
	}

	@Override
	public void onResourceManagerReload(ResourceManager pResourceManager) {
		ResearchListeners.ServerReload();
	}

}
