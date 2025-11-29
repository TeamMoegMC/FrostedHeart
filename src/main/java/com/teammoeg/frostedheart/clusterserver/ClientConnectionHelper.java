package com.teammoeg.frostedheart.clusterserver;

import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;

import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.mixin.client.ConnectScreenAccess;

import dev.ftb.mods.ftbchunks.client.map.MapManager;
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
		if (!ClientUtils.getMc().isSameThread()) {
			FHMain.LOGGER.warn("not same thread");
			ClientUtils.getMc().submitAsync(()->handleDisconnect());
			return;
		}
		
		if(ClientUtils.getMc().level!=null) {
			ClientUtils.getMc().level.disconnect();
			if(ClientUtils.getMc().isLocalServer()) {
				ClientUtils.getMc().clearLevel(new GenericDirtMessageScreen(Component.translatable("menu.savingLevel")));
				ClientUtils.getMc().setScreen(new TitleScreen());
			}else {
				ClientUtils.getMc().clearLevel();
				back2ServerScreen();
			}
			
			
		}
	}

	public static void back2ServerScreen() {
		ClientUtils.getMc().setScreen(new JoinMultiplayerScreen(new TitleScreen()));
	}
	public static void back() {
		String name=callStack.pollLast();
		if(name!=null)
			joinNewServer(name,false);
	}
	
	@SuppressWarnings("resource")
	public static void joinNewServer(String ip,boolean temporary) {
		if (!ClientUtils.getMc().isSameThread()) {
			FHMain.LOGGER.warn("not same thread");
			ClientUtils.getMc().submitAsync(()->joinNewServer(ip,temporary));
			return;
		}
		handleDisconnect();
		rawJoinNewServer(ClientUtils.getMc().screen,ip,temporary);
	}
	@SuppressWarnings("resource")
	public static void rawJoinNewServer(Screen previous,String ip,boolean temporary) {
		try {
		if(temporary) {
			callStack.addLast(last.toString());
		}
		MapManager.shutdown();
		ServerList servers = new ServerList(ClientUtils.getMc());
		servers.load();
		ServerData serverdata = servers.get(ip);
		if (serverdata == null) {
			serverdata = new ServerData(ip, ip, false);
			servers.add(serverdata, true);
			servers.save();
		}

		ConnectScreen.startConnecting(previous, ClientUtils.getMc(), ServerAddress.parseString(serverdata.ip), serverdata, false);
		}catch(Throwable t) {
			t.printStackTrace();
		}
	}
}
