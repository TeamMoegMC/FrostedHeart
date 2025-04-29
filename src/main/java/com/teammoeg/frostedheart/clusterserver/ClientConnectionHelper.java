package com.teammoeg.frostedheart.clusterserver;

import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;

import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.mixin.client.ConnectScreenAccess;

import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.resources.language.I18n;

public class ClientConnectionHelper {
	public static ServerAddress last;
	public static LinkedList<String> callStack=new LinkedList<>();
	public static String token;
	
	public ClientConnectionHelper() {

	}

	@SuppressWarnings("resource")
	public static void handleDisconnect() {
		if (!ClientUtils.mc().isSameThread()) {
			FHMain.LOGGER.warn("not same thread");
			ClientUtils.mc().submitAsync(()->joinNewServer(ip,temporary));
			return;
		}
		if(ClientUtils.mc().level!=null) {
			ClientUtils.mc().level.disconnect();
			ClientUtils.mc().clearLevel();
			ClientUtils.mc().setScreen(new JoinMultiplayerScreen(new TitleScreen()));
		}
	}

	public static void back2ServerScreen() {
		ClientUtils.mc().setScreen(new JoinMultiplayerScreen(new TitleScreen()));
	}
	public static void back() {
		String name=callStack.pollLast();
		if(name!=null)
			joinNewServer(name,false);
	}
	@SuppressWarnings("resource")
	public static void joinNewServer(String ip,boolean temporary) {
		if (!ClientUtils.mc().isSameThread()) {
			FHMain.LOGGER.warn("not same thread");
			ClientUtils.mc().submitAsync(()->joinNewServer(ip,temporary));
			return;
		}
		try {
		if(temporary) {
			callStack.addLast(last.toString());
		}
		handleDisconnect();
			
		ServerList servers = new ServerList(ClientUtils.mc());
		servers.load();
		ServerData serverdata = servers.get(ip);
		if (serverdata == null) {
			serverdata = new ServerData(ip, ip, false);
			servers.add(serverdata, true);
			servers.save();
		}

		ConnectScreen.startConnecting(ClientUtils.mc().screen, ClientUtils.mc(), ServerAddress.parseString(serverdata.ip), serverdata, false);
		}catch(Throwable t) {
			t.printStackTrace();
		}
	}
}
