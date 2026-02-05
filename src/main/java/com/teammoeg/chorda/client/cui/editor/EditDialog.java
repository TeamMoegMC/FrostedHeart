/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.chorda.client.cui.editor;

import com.teammoeg.chorda.Chorda;
import com.teammoeg.chorda.client.CInputHelper;
import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.cui.UILayer;
import com.teammoeg.chorda.text.Components;
import com.teammoeg.chorda.client.cui.UIElement;

import net.minecraft.ChatFormatting;

public abstract class EditDialog extends UILayer {
    EditDialog previous;
    EditorManager sc;

    public EditDialog(UIElement panel) {
        super(panel.getManager().getPrimaryLayer());
        if (panel.getLayerHolder() instanceof EditorManager)
            sc = (EditorManager)panel.getLayerHolder();
    }

    public void close() {
        close(true);
    }
    public void close(boolean refresh) {
        try {
            onClose();
        } catch (Throwable ex) {
            ex.printStackTrace();
            Chorda.LOGGER.error("Error closing dialog", ex);
        }
        try {
            if (previous != null) {
                sc.closeDialog(false);
                sc.openDialog(previous, refresh);
            } else
                sc.closeDialog(refresh);
        } catch (Throwable ex) {
            ex.printStackTrace();
            ClientUtils.getPlayer().sendSystemMessage(Components.str("Fatal error on switching dialog! see log for details").withStyle(ChatFormatting.RED));
            sc.closeGui();
        }
        try {
            onClosed();
        } catch (Throwable ex) {
            ex.printStackTrace();
            throw new RuntimeException("Error on dialog close", ex);
        }
    }

    @Override
    public boolean onKeyPressed(int key,int scan,int mod) {
        try {
            if (CInputHelper.isEsc(key)) {
                close();
                //this.closeGui(true);
                return true;
            }
            return super.onKeyPressed(key,scan,mod);
        } catch (Throwable ex) {
            ex.printStackTrace();
            throw new RuntimeException("Error on dialog close", ex);
        }
    }

    public abstract void onClose();

    public void onClosed() {

    }

    public void open() {
        if (sc.getDialog() != this) {
            previous = sc.getDialog();
            sc.openDialog(this, true);
        }
    }
}
