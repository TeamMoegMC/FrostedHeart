package com.teammoeg.frostedheart.clusterserver;

import java.util.LinkedList;

import com.teammoeg.chorda.client.ClientUtils;
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
		if(ClientUtils.mc().level!=null)
			ClientUtils.mc().level.disconnect();
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
		if(temporary) {
			callStack.addLast(last.toString());
		}
		ServerList servers = new ServerList(ClientUtils.mc());
		servers.load();
		ServerData serverdata = servers.get(ip);
		if (serverdata == null) {
			serverdata = new ServerData(I18n.get("selectServer.defaultName"), "", false);
			servers.add(serverdata, true);
			servers.save();
		}
		handleDisconnect();
		if(ClientUtils.mc().screen instanceof ConnectScreenAccess)
			ClientUtils.mc().screen=null;
		ConnectScreen.startConnecting(new JoinMultiplayerScreen(new TitleScreen()), ClientUtils.mc(), ServerAddress.parseString(serverdata.ip), serverdata, false);
		
	}
}
