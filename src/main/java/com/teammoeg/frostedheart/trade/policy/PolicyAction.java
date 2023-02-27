package com.teammoeg.frostedheart.trade.policy;

import com.teammoeg.frostedheart.trade.FHVillagerData;
import com.teammoeg.frostedheart.util.Writeable;

public interface PolicyAction extends Writeable {
	public void deal(FHVillagerData data,int num);
}
