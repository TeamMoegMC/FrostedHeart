package com.teammoeg.frostedheart.content.tools;

import com.teammoeg.frostedheart.base.item.FHBaseItem;

public class FHLeveledTool extends FHBaseItem {
	protected int level;
	public FHLeveledTool(String name,int lvl, Properties properties) {
		super(name, properties);
		this.level=lvl;
	}
	public int getLevel() {
		return level;
	}
	

}
