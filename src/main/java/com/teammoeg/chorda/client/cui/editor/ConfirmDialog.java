/*
 * Copyright (c) 2026 TeamMoeg
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

import java.util.function.Consumer;

import com.teammoeg.chorda.Chorda;
import com.teammoeg.chorda.client.cui.base.MouseButton;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.cui.widgets.Button;
import com.teammoeg.chorda.client.cui.widgets.TextButton;
import com.teammoeg.chorda.client.cui.widgets.TextField;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.chorda.text.Components;

import net.minecraft.network.chat.Component;

/**
 * 确认对话框，显示一条消息并提供确认和取消按钮。
 * 用户点击确认或取消后通过回调返回布尔结果。
 * <p>
 * Confirmation dialog displaying a message with confirm and cancel buttons.
 * Returns a boolean result via callback when the user clicks confirm or cancel.
 */
public class ConfirmDialog extends BaseEditDialog {
    TextField tf;
    Button cancel;
    Button ok;
    Consumer<Boolean> fin;
    boolean selected = false;

    /**
     * 创建一个确认对话框。
     * <p>
     * Creates a confirmation dialog.
     *
     * @param panel      父UI元素 / the parent UI element
     * @param label      对话框消息文本 / the dialog message text
     * @param exp        确认时返回的布尔值 / the boolean value returned on confirm
     * @param onFinished 完成回调 / the completion callback
     */
    public ConfirmDialog(UIElement panel, Component label, boolean exp, Consumer<Boolean> onFinished) {
        super(panel);
        tf = new TextField(this).setColor(0xFFFF0000).setMaxWidth(200).setText(label);
        fin = onFinished;
        selected = !exp;
        cancel = new TextButton(this, Components.translatable("gui.cancel"), CIcons.nop()) {

            @Override
            public void onClicked(MouseButton arg0) {

                close();
            }

        };
        ok = new TextButton(this, Components.translatable("gui.accept"), CIcons.nop()) {

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
            	Chorda.LOGGER.error("Error in ConfirmDialog", ex);
            }
    }

	@Override
	public void addUIElements() {
		add(tf);
        add(ok);
        add(cancel);
	}

}
