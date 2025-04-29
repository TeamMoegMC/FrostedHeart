package com.teammoeg.frostedheart.clusterserver;

import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;

import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.mixin.client.ConnectScreenAccess;

import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

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
			ClientUtils.mc().submitAsync(()->handleDisconnect());
			return;
		}
		if(ClientUtils.mc().level!=null) {
			ClientUtils.mc().level.disconnect();
			if(ClientUtils.mc().isLocalServer()) {
				ClientUtils.mc().clearLevel(new GenericDirtMessageScreen(Component.translatable("menu.savingLevel")));
				ClientUtils.mc().setScreen(new TitleScreen());
			}else {
				ClientUtils.mc().clearLevel();
				back2ServerScreen();
			}
			
			
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
		handleDisconnect();
		rawJoinNewServer(ClientUtils.mc().screen,ip,temporary);
	}
	@SuppressWarnings("resource")
	public static void rawJoinNewServer(Screen previous,String ip,boolean temporary) {
		try {
		if(temporary) {
			callStack.addLast(last.toString());
		}
			
		ServerList servers = new ServerList(ClientUtils.mc());
		servers.load();
		ServerData serverdata = servers.get(ip);
		if (serverdata == null) {
			serverdata = new ServerData(ip, ip, false);
			servers.add(serverdata, true);
			servers.save();
		}

		ConnectScreen.startConnecting(previous, ClientUtils.mc(), ServerAddress.parseString(serverdata.ip), serverdata, false);
		}catch(Throwable t) {
			t.printStackTrace();
		}
	}
}
