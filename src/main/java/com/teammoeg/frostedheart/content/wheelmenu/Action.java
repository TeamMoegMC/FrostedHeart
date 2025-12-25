package com.teammoeg.frostedheart.content.wheelmenu;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public interface Action {
	public static  enum NoAction implements Action{
		INSTANCE;
		@OnlyIn(Dist.CLIENT)
		@Override
		public void execute(Selection selection) {
		}
		
	}
	@OnlyIn(Dist.CLIENT)
	void execute(Selection selection);
}