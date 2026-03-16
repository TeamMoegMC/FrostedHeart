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
import java.util.function.Predicate;

import com.mojang.datafixers.util.Pair;
import com.teammoeg.chorda.client.cui.base.MouseButton;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.cui.base.Verifier;
import com.teammoeg.chorda.client.cui.widgets.Button;
import com.teammoeg.chorda.client.cui.widgets.TextButton;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.chorda.text.Components;

import net.minecraft.network.chat.Component;

/**
 * 文本输入提示对话框，提供单行文本输入框配合确认/取消按钮。
 * 支持可选的文本验证器。
 * <p>
 * Text input prompt dialog providing a single-line text input field with
 * confirm/cancel buttons. Supports an optional text verifier.
 */
public class EditPrompt extends BaseEditDialog {
    LabeledTextBox box;
    Button ok;
    Button cancel;
    /**
     * 创建一个文本输入提示对话框。
     * <p>
     * Creates a text input prompt dialog.
     *
     * @param panel      父UI元素 / the parent UI element
     * @param label      输入框标签 / the input label
     * @param val        初始文本值 / the initial text value
     * @param onFinished 确认回调 / the confirm callback
     * @param verifier   文本验证器，可为null / the text verifier, may be null
     */
    public EditPrompt(UIElement panel, Component label, String val, Consumer<String> onFinished,Verifier<String> verifier) {
        super(panel);
        box = new LabeledTextBox(this, label, val,verifier);
        ok = new TextButton(this, Components.translatable("gui.accept"), CIcons.nop()) {

            @Override
            public void onClicked(MouseButton arg0) {
                try {
                    onFinished.accept(box.getText());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                close();
            }

        };
        cancel = new TextButton(this, Components.translatable("gui.cancel"), CIcons.nop()) {

            @Override
            public void onClicked(MouseButton arg0) {
                close();
            }

        };
        cancel.setSize(300, 20);
        ok.setSize(300, 20);
    }

    /**
     * 打开一个无验证器的文本输入提示对话框。
     * <p>
     * Opens a text input prompt dialog without a verifier.
     *
     * @param p 父UI元素 / the parent UI element
     * @param l 标签 / the label
     * @param v 初始值 / the initial value
     * @param f 确认回调 / the confirm callback
     */
    public static void open(UIElement p, Component l, String v, Consumer<String> f) {
        new EditPrompt(p, l, v, f,null).open();
    }
    /**
     * 打开一个带验证器的文本输入提示对话框。
     * <p>
     * Opens a text input prompt dialog with a verifier.
     *
     * @param p     父UI元素 / the parent UI element
     * @param l     标签 / the label
     * @param v     初始值 / the initial value
     * @param f     确认回调 / the confirm callback
     * @param verif 文本验证器 / the text verifier
     */
    public static void open(UIElement p, Component l, String v, Consumer<String> f,Verifier<String> verif) {
        new EditPrompt(p, l, v, f,verif).open();
    }
    @Override
    public void addUIElements() {

        add(box);
        add(ok);
        add(cancel);
    }

    @Override
    public void onClose() {
    }


}
