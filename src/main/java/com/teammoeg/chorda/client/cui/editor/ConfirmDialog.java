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

import com.teammoeg.chorda.client.cui.Button;
import com.teammoeg.chorda.client.cui.MouseButton;
import com.teammoeg.chorda.client.cui.TextButton;
import com.teammoeg.chorda.client.cui.TextField;
import com.teammoeg.chorda.client.cui.UIWidget;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.chorda.lang.Components;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.util.client.Lang;

import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class ConfirmDialog extends BaseEditDialog {
    TextField tf;
    Button cancel;
    Button ok;
    Consumer<Boolean> fin;
    boolean selected = false;

    public ConfirmDialog(UIWidget panel, Component label, boolean exp, Consumer<Boolean> onFinished) {
        super(panel);
        tf = new TextField(this).setColor(0xFFFF0000).setMaxWidth(200).setText(label);
        fin = onFinished;
        selected = !exp;
        cancel = new TextButton(this, Lang.translateKey("gui.cancel"), CIcons.nop()) {

            @Override
            public void onClicked(MouseButton arg0) {

                close();
            }

        };
        ok = new TextButton(this, Lang.translateKey("gui.accept"), CIcons.nop()) {

            @Override
            public void onClicked(MouseButton arg0) {
                selected = exp;
                close();
            }

        };
        ok.setHeight(20);
        ok.setWidth(200);
        cancel.setHeight(20);
        cancel.setWidth(200);
    }

    @Override
    public void onClose() {
    }

    @Override
    public void onClosed() {
        if (!selected)
            try {
                fin.accept(false);
            } catch (Exception ex) {
                FHMain.LOGGER.error("Error in ConfirmDialog", ex);
            }
    }

	@Override
	public void addUIElements() {
		add(tf);
        add(ok);
        add(cancel);
	}

}
