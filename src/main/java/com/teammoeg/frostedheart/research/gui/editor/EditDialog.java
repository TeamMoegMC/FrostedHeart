package com.teammoeg.frostedheart.research.gui.editor;

import com.teammoeg.frostedheart.client.util.ClientUtils;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import com.teammoeg.frostedheart.research.gui.drawdesk.DrawDeskScreen;

import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.ui.input.Key;
import net.minecraft.util.text.TextFormatting;

public abstract class EditDialog extends Panel {
	EditDialog previous;
	DrawDeskScreen sc;
	public EditDialog(Widget panel) {
		super(panel.getGui());
		if(panel.getGui() instanceof DrawDeskScreen)
			sc=(DrawDeskScreen) panel.getGui();
	}
	public void open() {
		previous=sc.getDialog();
		sc.openDialog(this,true);
	}
	public void close() {
		close(true);
	}
	public void close(boolean refresh) {
		try {
		onClose();
		}catch(Exception ex) {};
		try {
			if(previous!=null) {
				sc.closeDialog(false);
				sc.openDialog(previous,refresh);
			}else
				sc.closeDialog(refresh);
		}catch(Exception ex) {
			ex.printStackTrace();
			ClientUtils.getPlayer().sendMessage(GuiUtils.str("Fatal error on switching dialog! see log for details").mergeStyle(TextFormatting.RED), null);
			sc.closeGui();
		};
		try {
		onClosed();
		}catch(Exception ex) {};
	}
	public abstract void onClose();
	public void onClosed() {
		
	}
	@Override
	public boolean keyPressed(Key key) {
		if(key.esc()) {
			close();
			//this.closeGui(true);
			return true;
		}
		return super.keyPressed(key);
	}
}
