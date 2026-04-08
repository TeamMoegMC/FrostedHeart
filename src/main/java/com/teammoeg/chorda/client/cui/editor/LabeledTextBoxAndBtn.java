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


import com.teammoeg.chorda.client.cui.base.MouseButton;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.cui.widgets.Button;
import com.teammoeg.chorda.client.cui.widgets.TextButton;
import com.teammoeg.chorda.client.icon.CIcons;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 带标签的文本输入框和按钮组合控件，在LabeledTextBox基础上增加一个操作按钮。
 * 按钮点击时通过回调函数接收一个文本设置器，用于外部逻辑设置文本框内容。
 * <p>
 * Labeled text box with an additional action button, extending LabeledTextBox.
 * When the button is clicked, the callback receives a text setter that allows
 * external logic to set the text box content.
 */
public class LabeledTextBoxAndBtn extends LabeledTextBox {
    protected List<Button> buttons =  new ArrayList<>();
    protected Button btn;

    public LabeledTextBoxAndBtn(UIElement panel, Component lab, String txt, Component btn, Consumer<Consumer<String>> onbtn) {
        super(panel, lab, txt);
        this.btn = new TextButton(this, btn, CIcons.nop()) {
            @Override
            public void onClicked(MouseButton arg0) {
                onbtn.accept(s -> obj.setText(s));
            }
        };
    }

    @Override
    public void addUIElements() {
        super.addUIElements();
        buttons.forEach(this::add);
    }

    public void addButton(Button btn) {
        buttons.add(btn);
    }

    @Override
    public void alignWidgets() {
        super.alignWidgets();
        int w1 = (int)(this.getWidth()*0.45F);
        obj.setX(getWidth()-w1);
        obj.setWidth(w1);

        int w2 = obj.getX();
        for (Button button : buttons) {
            button.setX(w2 -= button.getWidth()+2);
        }
        setHeight(getContentHeight());
    }
}
