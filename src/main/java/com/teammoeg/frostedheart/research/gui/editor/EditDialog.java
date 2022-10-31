/*
 * Copyright (c) 2022 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

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
        if (panel.getGui() instanceof DrawDeskScreen)
            sc = (DrawDeskScreen) panel.getGui();
    }

    public void open() {
        if (sc.getDialog() != this) {
            previous = sc.getDialog();
            sc.openDialog(this, true);
        }
    }

    public void close() {
        close(true);
    }

    public void close(boolean refresh) {
        try {
            onClose();
        } catch (Exception ex) {
        	ex.printStackTrace();
        }
        ;
        try {
            if (previous != null) {
                sc.closeDialog(false);
                sc.openDialog(previous, refresh);
            } else
                sc.closeDialog(refresh);
        } catch (Exception ex) {
            ex.printStackTrace();
            ClientUtils.getPlayer().sendMessage(GuiUtils.str("Fatal error on switching dialog! see log for details").mergeStyle(TextFormatting.RED), null);
            sc.closeGui();
        }
        ;
        try {
            onClosed();
        } catch (Exception ex) {
        }
        ;
    }

    public abstract void onClose();

    public void onClosed() {

    }

    @Override
    public boolean keyPressed(Key key) {
        if (key.esc()) {
            close();
            //this.closeGui(true);
            return true;
        }
        return super.keyPressed(key);
    }
}
