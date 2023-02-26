package com.teammoeg.frostedheart.trade;

import com.teammoeg.frostedheart.util.Writeable;

public interface PolicyAction extends Writeable {
	public void deal(FHVillagerData data,int num);
}
