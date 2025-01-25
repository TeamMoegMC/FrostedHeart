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

package com.teammoeg.frostedheart.content.steamenergy;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * The Class ConnectorNetworkRevalidator.
 * Built-In Manager for NetworkConnector, would handle revalidate and connection automatically
 * @param <T> the block entity type
 */
public class ConnectorNetworkRevalidator<T extends BlockEntity&NetworkConnector> {
	/**
	 * Current connected network
	 * */
	@Getter
	@Setter
	private HeatNetwork network;
	
	private int revalidateTick;
	
	private final T current;

	/**
	 * Instantiates a new connector network revalidator
	 *
	 * @param current the block entity
	 */
	public ConnectorNetworkRevalidator(T current) {
		super();
		this.current = current;
	}

	/**
	 * Tick.
	 */
	public void tick() {
		if(network!=null&&!network.isValid())
			network=null;
		if(network!=null)
			if(++revalidateTick>=10) {
				network.refreshConnectedEndpoints(current.getBlockPos());
				network.startConnectionFromBlock(current);
			}
	}
	
	/**
	 * Call when surrounding connection status change.
	 *
	 * @param face the outbound face of current block
	 * @param isConnect true if is new connection established, false if disconnected
	 */
	public void onConnectionChange(Direction face,boolean isConnect) {
        if (network == null) return;
        if (isConnect)
        	network.startConnectionFromBlock(current, face);
        else
            network.requestUpdate();
	}
	
	/**
	 * Checks for network.
	 *
	 * @return true, if network cotained
	 */
	public boolean hasNetwork() {
		return network!=null;
	}

}
