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

package com.teammoeg.frostedheart.content.steamenergy;

import net.minecraft.world.inventory.AbstractContainerMenu;

import java.util.Collection;

import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.cui.screenadapter.CUIMenuScreenWrapper;

public class ClientHeatHandler {
	public static void loadEndPoint(Collection<HeatEndpoint> data) {
		AbstractContainerMenu c=ClientUtils.getMc().player.containerMenu;
		if(c instanceof HeatStatContainer) {
			((HeatStatContainer)c).data=data;
			if(ClientUtils.getMc().screen instanceof CUIMenuScreenWrapper) {
				CUIMenuScreenWrapper<?> msw=(CUIMenuScreenWrapper<?>) ClientUtils.getMc().screen;
				msw.getPrimaryLayer().refreshElements();
			}
		}
	}
}
