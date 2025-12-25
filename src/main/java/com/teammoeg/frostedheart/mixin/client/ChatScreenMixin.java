/*
 * Copyright (c) 2024 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart.mixin.client;

import org.spongepowered.asm.mixin.Mixin;

import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.content.scenario.client.FHScenarioClient;
import com.teammoeg.frostedheart.content.scenario.network.C2SLinkClickedPacket;

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
	    		C2SLinkClickedPacket packet=new C2SLinkClickedPacket(ev.getValue().substring(FHScenarioClient.LINK_SYMBOL.length()));
	    		FHNetwork.INSTANCE.sendToServer(packet);
	    		return true;
	    	}
		}
		return super.handleComponentClicked(style);
	}

}
