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
