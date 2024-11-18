package com.teammoeg.frostedheart.mixin.client;

import org.spongepowered.asm.mixin.Mixin;

import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.content.scenario.client.FHScenarioClient;
import com.teammoeg.frostedheart.content.scenario.network.ClientLinkClickedPacket;

import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.ClickEvent.Action;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

@Mixin(ChatScreen.class)
public class ChatScreenMixin extends Screen {

	public ChatScreenMixin(Component pTitle) {
		super(pTitle);
	}

	@Override
	public boolean handleComponentClicked(Style style) {
		ClickEvent ev=style.getClickEvent();
		if(ev!=null&&ev.getAction()==Action.RUN_COMMAND) {
	    	if(ev.getValue().startsWith(FHScenarioClient.LINK_SYMBOL)) {
	    		ClientLinkClickedPacket packet=new ClientLinkClickedPacket(ev.getValue().substring(FHScenarioClient.LINK_SYMBOL.length()));
	    		FHNetwork.sendToServer(packet);
	    		return true;
	    	}
		}
		return super.handleComponentClicked(style);
	}

}
