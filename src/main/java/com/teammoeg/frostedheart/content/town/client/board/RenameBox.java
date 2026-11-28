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

package com.teammoeg.frostedheart.content.town.client.board;

import java.util.function.Consumer;

import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.cui.widgets.TextBox;

/**
 * 改名用的一行输入框：打开时旧名字全选，直接打字就换掉；回车交出文字，按 Esc 或失去焦点就取消。只会交出或取消一次。
 * <p>
 * A one-line box for renaming: it opens with the old name selected, so typing replaces it; Enter hands the text over,
 * Esc or losing focus cancels. It does either exactly once.
 */
public class RenameBox extends TextBox {
    private final Consumer<String> onDone;
    private final Runnable onCancel;
    private boolean finished;

    public RenameBox(UIElement parent, String text, int maxLength, Consumer<String> onDone, Runnable onCancel) {
        super(parent);
        this.onDone = onDone;
        this.onCancel = onCancel;
        setMaxLength(maxLength);
        setText(text, false);
        selectAll();
        // Cleared, the box shows what the name falls back to.
        showGhostWhenFocused = true;
    }

    @Override
    public void onEnterPressed() {
        if (finished) return;
        finished = true;
        onDone.accept(getText().trim());
    }

    @Override
    public void tick() {
        super.tick();
        if (!finished && !isFocused()) {
            finished = true;
            onCancel.run();
        }
    }
}
