package com.teammoeg.frostedheart.trade.policy.actions;

import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.trade.FHVillagerData;
import net.minecraft.network.PacketBuffer;

public class AddFlagValueAction extends SetFlagValueAction {

	public AddFlagValueAction(JsonObject jo) {
		super(jo);
	}

	public AddFlagValueAction(PacketBuffer buffer) {
		super(buffer);
	}

	public AddFlagValueAction(String name, int value) {
		super(name, value);
	}

	@Override
	public void deal(FHVillagerData data, int num) {
		data.flags.compute(name,(k,v)->{
			int vn=0;
			if(v!=null)vn+=v;
			vn+=num*value;
			return vn==0?null:vn;
			
		});
	}


}
