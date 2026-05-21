/*
 * Copyright (c) 2026 TeamMoeg
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

package com.teammoeg.frostedscenario;

import com.teammoeg.chorda.network.CBaseNetwork;
import com.teammoeg.frostedscenario.network.C2SClientReadyPacket;
import com.teammoeg.frostedscenario.network.C2SLinkClickedPacket;
import com.teammoeg.frostedscenario.network.C2SRenderingStatusMessage;
import com.teammoeg.frostedscenario.network.C2SScenarioCookies;
import com.teammoeg.frostedscenario.network.C2SScenarioResponsePacket;
import com.teammoeg.frostedscenario.network.C2SSettingsPacket;
import com.teammoeg.frostedscenario.network.S2CRequestCookieMessage;
import com.teammoeg.frostedscenario.network.S2CScenarioCommandPacket;
import com.teammoeg.frostedscenario.network.S2CSenarioActPacket;
import com.teammoeg.frostedscenario.network.S2CSenarioScenePacket;
import com.teammoeg.frostedscenario.network.S2CSetCookiesMessage;
import com.teammoeg.frostedscenario.network.S2CWaitTransMessage;

public class FSNetwork extends CBaseNetwork {
	public static final FSNetwork INSTANCE=new FSNetwork();
	private FSNetwork() {
		super(FSMain.MODID);
	}

	@Override
	public void registerMessages() {

        //Scenario Messages
        registerMessage("scenario_client_op", C2SScenarioResponsePacket.class);
        registerMessage("scenario_server_command", S2CScenarioCommandPacket.class);
        registerMessage("scenario_scene", S2CSenarioScenePacket.class);
        registerMessage("scenario_ready", C2SClientReadyPacket.class);
        registerMessage("scenario_link", C2SLinkClickedPacket.class);
        registerMessage("scenario_act", S2CSenarioActPacket.class);
        registerMessage("scenario_settings", C2SSettingsPacket.class);
        registerMessage("scenario_set_cookie", S2CSetCookiesMessage.class);
        registerMessage("scenario_get_cookie", S2CRequestCookieMessage.class);
        registerMessage("scenario_send_cookie", C2SScenarioCookies.class);
        registerMessage("scenario_render_status",C2SRenderingStatusMessage.class);
        registerMessage("scenario_wait_render",S2CWaitTransMessage.class);
        
	}

}
