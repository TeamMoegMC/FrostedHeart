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

import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.cui.base.PrimaryLayer;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.cui.screenadapter.CUIScreenWrapper;
import com.teammoeg.chorda.client.cui.theme.SimpleTechTheme;
import com.teammoeg.chorda.client.cui.theme.Theme;
import com.teammoeg.chorda.client.cui.theme.UIColors;
import com.teammoeg.chorda.client.cui.widgets.TextField;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * 编辑器工具类，提供打开独立编辑器屏幕和创建标题控件的便捷方法。
 * 用于在没有现有CUI屏幕的情况下启动编辑器对话框。
 * <p>
 * Editor utility class providing convenience methods for opening standalone editor
 * screens and creating title widgets. Used to launch editor dialogs when no existing
 * CUI screen is available.
 */
public class EditUtils {

    private EditUtils() {
    }

    /**
     * 使用默认主题打开独立编辑器屏幕进行编辑。
     * <p>
     * Opens a standalone editor screen for editing with the default theme.
     *
     * @param editor   编辑器实例 / the editor instance
     * @param title    编辑器标题 / the editor title
     * @param oldVlaue 初始值 / the initial value
     * @param onChange 值变更回调 / the value change callback
     * @param <T>      被编辑的值类型 / the type of value being edited
     */
    public static <T> void edit(Editor<T> editor, Component title, T oldVlaue,Consumer<T> onChange) {
    	editor.open(openEditorScreen(), title, oldVlaue, onChange);
        //new ResearchEditorDialog(EditUtils.openEditorScreen(), r, r.getCategory()).open();
    }
    /**
     * 使用指定主题打开独立编辑器屏幕进行编辑。
     * <p>
     * Opens a standalone editor screen for editing with the specified theme.
     *
     * @param editor   编辑器实例 / the editor instance
     * @param theme    主题 / the theme
     * @param title    编辑器标题 / the editor title
     * @param oldVlaue 初始值 / the initial value
     * @param onChange 值变更回调 / the value change callback
     * @param <T>      被编辑的值类型 / the type of value being edited
     */
    public static <T> void edit(Editor<T> editor, Theme theme, Component title, T oldVlaue,Consumer<T> onChange) {
    	editor.open(openEditorScreen(theme), title, oldVlaue, onChange);
        //new ResearchEditorDialog(EditUtils.openEditorScreen(), r, r.getCategory()).open();
    }
    /**
     * 使用默认主题打开编辑器屏幕。
     * <p>
     * Opens an editor screen with the default theme.
     *
     * @return 编辑器屏幕的主层 / the primary layer of the editor screen
     */
    public static UIElement openEditorScreen() {
    	return openEditorScreen(SimpleTechTheme.INSTANCE);
    }
    
    /**
     * 使用指定主题打开编辑器屏幕。
     * <p>
     * Opens an editor screen with the specified theme.
     *
     * @param theme 主题，为null时使用默认主题 / the theme, uses default if null
     * @return 编辑器屏幕的主层 / the primary layer of the editor screen
     */
    public static UIElement openEditorScreen(Theme theme) {
    	CUIScreenWrapper wrapper=new CUIScreenWrapper(new PrimaryLayer());
    	wrapper.getPrimaryLayer().setTheme(theme == null ? SimpleTechTheme.INSTANCE : theme);
    	ClientUtils.getMc().setScreen(wrapper);
    	return wrapper.getPrimaryLayer();
    }
    /**
     * 创建标题文本控件。
     * <p>
     * Creates a title text field widget.
     *
     * @param p     父UI元素 / the parent UI element
     * @param title 标题文本 / the title text
     * @return 标题文本控件 / the title text field
     */
    public static TextField getTitle(UIElement p, String title) {

        return new TextField(p).setMaxWidth(200).setText(title).setColor(UIColors.UI_TEXT);
    }
    /**
     * 创建标题文本控件（使用Component）。
     * <p>
     * Creates a title text field widget (using Component).
     *
     * @param p     父UI元素 / the parent UI element
     * @param title 标题文本组件 / the title text component
     * @return 标题文本控件 / the title text field
     */
    public static TextField getTitle(UIElement p, Component title) {

        return new TextField(p).setMaxWidth(200).setText(title).setColor(UIColors.UI_TEXT);
    }
  
}
