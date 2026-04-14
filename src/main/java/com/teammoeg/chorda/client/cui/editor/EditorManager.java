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

import com.teammoeg.chorda.client.cui.base.UIElement;

/**
 * 编辑器管理器接口，负责编辑对话框的打开、关闭和GUI生命周期管理。
 * 由PrimaryLayer实现，为编辑器框架提供对话框堆栈管理能力。
 * <p>
 * Editor manager interface responsible for opening/closing edit dialogs and GUI
 * lifecycle management. Implemented by PrimaryLayer to provide dialog stack
 * management for the editor framework.
 */
public interface EditorManager {
	/**
	 * 打开一个编辑对话框。
	 * <p>
	 * Opens an edit dialog.
	 *
	 * @param dialog 要打开的对话框 / the dialog to open
	 * @param refresh  是否刷新 / whether to refresh
	 */
	public void openDialog(UIElement dialog,boolean refresh);
	/**
	 * 关闭当前对话框。
	 * <p>
	 * Closes the current dialog.
	 *
	 * @param refresh 是否刷新 / whether to refresh
	 */
	public void closeDialog(boolean refresh);
	/**
	 * 关闭整个GUI。
	 * <p>
	 * Closes the entire GUI.
	 */
	public void closeGui();
	/**
	 * 获取当前打开的对话框。
	 * <p>
	 * Gets the currently open dialog.
	 *
	 * @return 当前对话框，无则返回null / the current dialog, or null if none
	 */
	public UIElement getDialog();
}
