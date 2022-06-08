package com.teammoeg.frostedheart.research.gui.editor;

import com.teammoeg.frostedheart.research.gui.FHIcons;
import com.teammoeg.frostedheart.research.gui.FHIcons.FHIcon;
import com.teammoeg.frostedheart.research.gui.FHIcons.FHItemIcon;

import dev.ftb.mods.ftblibrary.ui.Widget;
import net.minecraft.item.ItemStack;

public class IconEditor extends EditDialog {
	public static Editor<FHIcon> EDITOR=(p,l,v,c)->{
		new SelectItemStackDialog(p,l,v instanceof FHItemIcon?((FHItemIcon) v).getStack():ItemStack.EMPTY,i->{if(v instanceof FHItemIcon||!i.isEmpty())c.accept(FHIcons.getIcon(i));}).open();
	};
	public IconEditor(Widget panel) {
		super(panel);
	}

	@Override
	public void onClose() {
	}

	@Override
	public void addWidgets() {
	}

	@Override
	public void alignWidgets() {
	}

}
