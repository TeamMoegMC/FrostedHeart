package com.teammoeg.frostedheart.research.gui;

import java.util.function.Consumer;

import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.util.TooltipList;

public class ToolTipWidget extends Widget {
	Consumer<TooltipList> csm;
	public ToolTipWidget(Panel p,Consumer<TooltipList> csm) {
		super(p);
		this.csm=csm;
	}

	@Override
	public void addMouseOverText(TooltipList list) {
		super.addMouseOverText(list);
		csm.accept(list);
	}



}
