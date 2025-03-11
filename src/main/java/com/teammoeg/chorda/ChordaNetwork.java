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

package com.teammoeg.chorda;

import com.teammoeg.chorda.network.ContainerDataSyncMessageS2C;
import com.teammoeg.chorda.network.ContainerOperationMessageC2S;
import com.teammoeg.chorda.network.CBaseNetwork;

public class ChordaNetwork extends CBaseNetwork{
	public static final ChordaNetwork INSTANCE=new ChordaNetwork();
    private ChordaNetwork() {
		super(Chorda.MODID);
	}
    @Override
	public void registerMessages() {
        //Fundamental Message
        registerMessage("container_operation", ContainerOperationMessageC2S.class);
        registerMessage("container_sync", ContainerDataSyncMessageS2C.class);

    }
}
