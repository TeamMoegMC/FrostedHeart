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

import com.teammoeg.chorda.Chorda;
import com.teammoeg.chorda.client.CInputHelper;
import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.cui.base.UILayer;
import com.teammoeg.chorda.text.Components;

import net.minecraft.ChatFormatting;

/**
 * 编辑对话框基类，提供可堆叠的模态对话框功能。
 * 对话框支持链式打开（新对话框记住前一个），关闭时自动恢复前一个对话框。
 * 按Esc键关闭当前对话框。
 * <p>
 * Base class for edit dialogs providing stackable modal dialog functionality.
 * Dialogs support chained opening (new dialog remembers the previous one) and
 * automatically restore the previous dialog on close. Pressing Esc closes the
 * current dialog.
 */
public abstract class EditDialog extends UILayer {
    UIElement previous;
    EditorManager sc;

    /**
     * 创建一个编辑对话框。
     * <p>
     * Creates an edit dialog.
     *
     * @param panel 父UI元素 / the parent UI element
     */
    public EditDialog(UIElement panel) {
        super(panel.getManager().getPrimaryLayer());
        if (panel.getLayerHolder() instanceof EditorManager)
            sc = (EditorManager)panel.getLayerHolder();
    }

    /**
     * 关闭对话框并刷新。
     * <p>
     * Closes the dialog and refreshes.
     */
    public void close() {
        close(true);
    }
    /**
     * 关闭对话框，可选是否刷新。
     * <p>
     * Closes the dialog with optional refresh.
     *
     * @param refresh 是否刷新父界面 / whether to refresh the parent UI
     */
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

    /**
     * 对话框关闭前的回调。
     * <p>
     * Callback invoked before the dialog is closed.
     */
    public abstract void onClose();

    /**
     * 对话框关闭后的回调。
     * <p>
     * Callback invoked after the dialog is closed.
     */
    public void onClosed() {

    }

    /**
     * 打开此对话框，将当前对话框压栈。
     * <p>
     * Opens this dialog, pushing the current dialog onto the stack.
     */
    public void open() {
        if (sc.getDialog() != this) {
            previous = sc.getDialog();
            sc.openDialog(this, true);
        }
    }
}
