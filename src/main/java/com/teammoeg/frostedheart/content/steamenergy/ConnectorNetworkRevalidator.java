package com.teammoeg.frostedheart.content.steamenergy;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ConnectorNetworkRevalidator<T extends BlockEntity&NetworkConnector> {
	@Getter
	@Setter
	private HeatNetwork network;
	private int revalidateTick;
	private final T current;

	public ConnectorNetworkRevalidator(T current) {
		super();
		this.current = current;
	}

	public void tick() {
		if(network!=null&&!network.isValid())
			network=null;
		if(network!=null)
			if(++revalidateTick>=10) {
				network.startConnectionFromBlock(current);
			}
	}
	public void onConnectionChange(Direction face,boolean isConnect) {
        if (network == null) return;
        if (isConnect)
        	network.startConnectionFromBlock(current, face);
        else
            network.requestUpdate();
	}
	public boolean hasNetwork() {
		return network!=null;
	}

}
