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

package com.teammoeg.chorda.client.cui.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import com.teammoeg.chorda.client.CInputHelper;
import com.teammoeg.chorda.client.CInputHelper.Cursor;
import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.Focusable;
import com.teammoeg.chorda.client.cui.base.MouseButton;
import com.teammoeg.chorda.client.cui.base.TooltipBuilder;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.cui.base.UILayer;
import com.teammoeg.chorda.client.cui.base.Verifier;
import com.teammoeg.chorda.client.cui.base.Verifier.VerifyResult;
import com.teammoeg.chorda.client.cui.theme.Coloring;
import com.teammoeg.chorda.client.cui.theme.UIColors;

import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

/**
 * 文本输入框控件。支持文本输入、编辑、选择、复制粘贴、撤销等完整文本编辑功能。
 * 支持输入验证、占位文本、光标闪烁和选区高亮。
 * <p>
 * Text input box widget. Supports full text editing features including input, editing,
 * selection, copy-paste, and undo. Supports input validation, ghost/placeholder text,
 * cursor blinking, and selection highlighting.
 */
public class TextBox extends UIElement implements Focusable {
	/** 是否处于焦点状态 / Whether focused */
	private boolean isFocused = false;
	/** 字符数量限制 / Character count limit */
	public int charLimit = 2000;
	/** 文本颜色 / Text color */
	public Coloring textColor=UIColors.BUTTON_TEXT;
	
	public Coloring errorColor=UIColors.ERROR_TEXT;
	/** 占位文本（未输入内容时显示） / Ghost/placeholder text shown when empty */
	public String ghostText = "";
	/** 当前输入文本 / Current input text */
	private String text = "";
	/** 文本显示起始位置（用于滚动） / Text display start position (for scrolling) */
	private int displayPos;
	/** 光标位置 / Cursor position */
	private int cursorPos;
	/** 选区高亮位置 / Selection highlight position */
	private int highlightPos;
	/** 文本验证结果 / Text validation result */
	private VerifyResult validText;
	/** 最大输入长度 / Maximum input length */
	private int maxLength = 1024;
	/** 文本输入验证器 / Text input verifier */
	private Verifier<String> filter;
	/** 文本绘制起始X偏移 / Text drawing start X offset */
	protected int textStartPos=4;
	/** 是否右对齐 / Whether right-aligned */
	protected boolean rightAlign;
	/** 文本偏移位置（用于右对齐计算） / Text offset position (for right-alignment calculation) */
	protected int textOffsetPos;
	/** 最后一个通过验证的文本 / Last text that passed validation */
	@Getter
	protected String lastValidText="";
	/**
	 * 创建文本输入框。
	 * <p>
	 * Creates a text input box.
	 *
	 * @param panel 父级UI图层 / Parent UI layer
	 */
	public TextBox(UILayer panel) {
		super(panel);
		filter=Verifier.nonNull();
		validText=filter.test("");
	}

	/**
	 * 设置最大输入长度。
	 * <p>
	 * Sets the maximum input length.
	 *
	 * @param maxLength 最大长度 / Maximum length
	 */
	public void setMaxLength(int maxLength) {
		this.maxLength = maxLength;
	}

	/** {@inheritDoc} */
	@Override
	public void setWidth(int v) {
		super.setWidth(v);
		scrollTo(getCursorPos());
	}

	/**
	 * 设置文本右对齐。
	 * <p>
	 * Sets the text to right-aligned.
	 */
	public void setRightAlign() {
		rightAlign=true;
	}
	@Override
	public final boolean isFocused() {
		return isFocused;
	}

	@Override
	public final void setFocused(boolean focused) {
		if (isFocused != focused) {
			isFocused = focused;
			if (focused) {
				getLayerHolder().focusOn(this);
			}
		}
	}

	/**
	 * 设置文本输入验证器。
	 * <p>
	 * Sets the text input verifier.
	 *
	 * @param filter 验证器 / Verifier
	 */
	public void setFilter(Verifier<String> filter) {
		this.filter = filter;
	}

	/**
	 * 获取当前输入文本。
	 * <p>
	 * Gets the current input text.
	 *
	 * @return 当前文本 / Current text
	 */
	public final String getText() {
		return text;
	}

	/**
	 * 获取当前选区内的文本。
	 * <p>
	 * Gets the text within the current selection.
	 *
	 * @return 选中的文本 / Selected text
	 */
	public String getSelectedText() {
		return text.substring(Math.min(cursorPos, highlightPos), Math.max(cursorPos, highlightPos));
	}

	/**
	 * 设置文本内容。
	 * <p>
	 * Sets the text content.
	 *
	 * @param string 新文本 / New text
	 * @param triggerChange 是否触发文本变化回调 / Whether to trigger text change callback
	 */
	public void setText(String string, boolean triggerChange) {
		//if (!filter.test(string).isError()) {
			if (string.length() > maxLength) {
				text = string.substring(0, maxLength);
			} else {
				text = string;
			}

			validText = isValid(text);
			if(!validText.isError())
				lastValidText=text;
			moveCursorToEnd(false);
			setSelectionPos(cursorPos);
			if (triggerChange) {
				onTextChanged();
			}
		//}
	}

	/**
	 * 设置文本内容并触发文本变化回调。
	 * <p>
	 * Sets the text content and triggers the text change callback.
	 *
	 * @param s 新文本 / New text
	 */
	public final void setText(String s) {
		setText(s, true);
	}

	/**
	 * 移动光标指定偏移量。
	 * <p>
	 * Moves the cursor by the specified offset.
	 *
	 * @param pos 偏移量（正数向右，负数向左） / Offset (positive for right, negative for left)
	 * @param extendSelection 是否扩展选区 / Whether to extend the selection
	 */
	public void moveCursor(int pos, boolean extendSelection) {
		moveCursorTo(getCursorPos(pos), extendSelection);
	}

	private int getCursorPos(int pos) {
		return Util.offsetByCodepoints(text, cursorPos, pos);
	}

	/**
	 * 设置光标位置并滚动到该位置。
	 * <p>
	 * Sets the cursor position and scrolls to it.
	 *
	 * @param pos 光标位置 / Cursor position
	 */
	public void setCursorPosition(int pos) {
		cursorPos = Mth.clamp(pos, 0, text.length());
		scrollTo(cursorPos);
	}

	/**
	 * 将光标移动到指定位置。
	 * <p>
	 * Moves the cursor to the specified position.
	 *
	 * @param pos 目标位置 / Target position
	 * @param extendSelection 是否扩展选区 / Whether to extend the selection
	 */
	public void moveCursorTo(int pos, boolean extendSelection) {
		setCursorPosition(pos);
		if (!extendSelection) {
			setSelectionPos(cursorPos);
		}

		onTextChanged();
	}

	/**
	 * 将光标移动到文本开头。
	 * <p>
	 * Moves the cursor to the start of the text.
	 *
	 * @param extendSelection 是否扩展选区 / Whether to extend the selection
	 */
	public void moveCursorToStart(boolean extendSelection) {
		moveCursorTo(0, extendSelection);
	}

	/**
	 * 将光标移动到文本末尾。
	 * <p>
	 * Moves the cursor to the end of the text.
	 *
	 * @param extendSelection 是否扩展选区 / Whether to extend the selection
	 */
	public void moveCursorToEnd(boolean extendSelection) {
		moveCursorTo(text.length(), extendSelection);
	}

	/**
	 * 设置光标位置。
	 * <p>
	 * Sets the cursor position.
	 *
	 * @param pos 光标位置 / Cursor position
	 */
	public void setCursorPos(int pos) {
		cursorPos = Mth.clamp(pos, 0, text.length());
		scrollTo(cursorPos);
	}

	/**
	 * 设置选区位置。
	 * <p>
	 * Sets the selection position.
	 *
	 * @param i 选区位置 / Selection position
	 */
	public void setSelectionPos(int i) {
		highlightPos = Mth.clamp(i, 0, text.length());
		scrollTo(highlightPos);
	}

	/**
	 * 获取当前光标位置。
	 * <p>
	 * Gets the current cursor position.
	 *
	 * @return 光标位置 / Cursor position
	 */
	public int getCursorPos() {
		return cursorPos;
	}

	/**
	 * 在光标位置插入文本，替换选区内的文本（如果有）。
	 * <p>
	 * Inserts text at the cursor position, replacing selected text if any.
	 *
	 * @param string 要插入的文本 / Text to insert
	 */
	public void insertText(String string) {
		int selStart = Math.min(cursorPos, highlightPos);
		int selEnd = Math.max(cursorPos, highlightPos);
		int space = maxLength - text.length() - (selStart - selEnd);
		if (space > 0) {
			String filtered = SharedConstants.filterText(string);
			int nToInsert = filtered.length();
			if (space < nToInsert) {
				if (Character.isHighSurrogate(filtered.charAt(space - 1))) {
					--space;
				}

				filtered = filtered.substring(0, space);
				nToInsert = space;
			}

			String newText = (new StringBuilder(text)).replace(selStart, selEnd, filtered).toString();
			validText = isValid(newText);
			if(!validText.isError())
				lastValidText=newText;
			//if (!validText.isError()) {
				text = newText;
				setCursorPosition(selStart + nToInsert);
				setSelectionPos(cursorPos);
				onTextChanged();
			//}
		}
	}

	private void scrollTo(int pos) {
		Font font = getFont();
		if (font != null) {
			displayPos = Math.min(displayPos, text.length());
			String string = font.plainSubstrByWidth(text.substring(displayPos), getWidth());
			int k = string.length() + displayPos;
			if (pos == displayPos) {
				displayPos -= font.plainSubstrByWidth(text, getWidth(), true).length();
			}

			if (pos > k) {
				displayPos += pos - k;
			} else if (pos <= displayPos) {
				displayPos -= displayPos - pos;
			}

			displayPos = Mth.clamp(displayPos, 0, text.length());
		}
	}

	/**
	 * 获取从当前光标位置按单词移动后的位置。
	 * <p>
	 * Gets the position after moving by words from the current cursor position.
	 *
	 * @param count 移动的单词数（正数向右，负数向左） / Number of words to move (positive for right, negative for left)
	 * @return 目标位置 / Target position
	 */
	public int getWordPosition(int count) {
		return getWordPosition(count, getCursorPos());
	}

	private int getWordPosition(int count, int fromPos) {
		int res = fromPos;
		boolean backwards = count < 0;
		int absCount = Math.abs(count);

		for(int m = 0; m < absCount; ++m) {
			if (!backwards) {
				int n = text.length();
				res = text.indexOf(' ', res);
				if (res == -1) {
					res = n;
				} else {
					while(res < n && text.charAt(res) == ' ') {
						++res;
					}
				}
			} else {
				while(res > 0 && text.charAt(res - 1) == ' ') {
					--res;
				}

				while(res > 0 && text.charAt(res - 1) != ' ') {
					--res;
				}
			}
		}

		return res;
	}

	/**
	 * 是否允许输入。子类可重写以控制输入权限。
	 * <p>
	 * Whether input is allowed. Subclasses can override to control input permissions.
	 *
	 * @return 是否允许输入 / Whether input is allowed
	 */
	public boolean allowInput() {
		return true;
	}

	private void deleteText(int count) {
		if (Screen.hasControlDown()) {
			deleteWords(count);
		} else {
			deleteChars(count);
		}
	}

	/**
	 * 删除指定数量的单词。
	 * <p>
	 * Deletes the specified number of words.
	 *
	 * @param count 要删除的单词数 / Number of words to delete
	 */
	public void deleteWords(int count) {
		if (!text.isEmpty()) {
			if (highlightPos != cursorPos) {
				insertText("");
			} else {
				deleteCharsToPos(getWordPosition(count));
			}
		}
	}

	/**
	 * 删除指定数量的字符。
	 * <p>
	 * Deletes the specified number of characters.
	 *
	 * @param count 要删除的字符数 / Number of characters to delete
	 */
	public void deleteChars(int count) {
		deleteCharsToPos(getCursorPos(count));
	}

	/**
	 * 删除从光标到指定位置之间的字符。
	 * <p>
	 * Deletes characters between the cursor and the specified position.
	 *
	 * @param pos 目标位置 / Target position
	 */
	public void deleteCharsToPos(int pos) {
		if (!text.isEmpty()) {
			if (highlightPos != cursorPos) {
				insertText("");
			} else {
				int from = Math.min(pos, cursorPos);
				int to = Math.max(pos, cursorPos);
				if (from != to) {
					String newText = new StringBuilder(text).delete(from, to).toString();
					validText=isValid(newText);
					if(!validText.isError())
						lastValidText=newText;
					//if (!isValid(newText).isError()) {
						text = newText;
						moveCursorTo(from, false);
					//}
				}
			}
		}
	}
	/** 鼠标是否按下（用于拖拽选择） / Whether mouse is pressed (for drag selection) */
	boolean isPressed;

	/**
	 * 获取鼠标在文本中的位置（像素偏移）。
	 * <p>
	 * Gets the mouse position within the text (pixel offset).
	 *
	 * @return 鼠标文本位置 / Mouse text position
	 */
	public int getMouseTextPosition() {
		return (int)getMouseX()-textStartPos+3-textOffsetPos;
	}
	/** {@inheritDoc} */
	@Override
	public boolean onMousePressed(MouseButton button) {
		if (isMouseOver()) {
			setFocused(true);
			if (button==MouseButton.LEFT) {
				if (isFocused) {
					int i =getMouseTextPosition();
					String s = getFont().plainSubstrByWidth(text.substring(displayPos), getWidth());
					if (CInputHelper.isShiftKeyDown()) {
						setSelectionPos(getFont().plainSubstrByWidth(s,i).length() + displayPos);
					} else {
						isPressed=true;
						setCursorPos(getFont().plainSubstrByWidth(s,i).length() + displayPos);
						setSelectionPos(getCursorPos());
					}
				}
			} else if (button==MouseButton.RIGHT && !getText().isEmpty() && allowInput()) {
				setText("");
			}

			return true;
		} else {
			setFocused(false);
		}

		return false;
	}

	/** {@inheritDoc} */
	@Override
	public boolean onKeyPressed(int keyCode,int scanCode,int modifier) {
		if (!isFocused()) {
			return false;
		} else if (CInputHelper.isSelectAll(keyCode)) {
			setCursorPos(text.length());
			setSelectionPos(0);
			return true;
		} else if (CInputHelper.isCopy(keyCode)) {
			CInputHelper.setClipboardText(getSelectedText());
			return true;
		} else if (CInputHelper.isPaste(keyCode)) {
			insertText(CInputHelper.getClipboardText());
			return true;
		} else if (CInputHelper.isCut(keyCode)) {
			CInputHelper.setClipboardText(getSelectedText());
			insertText("");
			return true;
		} else {
			switch (keyCode) {
				case GLFW.GLFW_KEY_ESCAPE -> {
					setFocused(false);
					return true;
				}
				case GLFW.GLFW_KEY_BACKSPACE -> {
					deleteText(-1);
					return true;
				}
				case GLFW.GLFW_KEY_HOME -> {
					moveCursorToStart(Screen.hasShiftDown());
					return true;
				}
				case GLFW.GLFW_KEY_LEFT -> {
					if (Screen.hasControlDown()) {
						moveCursorTo(getWordPosition(-1), Screen.hasShiftDown());
					} else {
						moveCursor(-1, Screen.hasShiftDown());
					}
					return true;
				}
				case GLFW.GLFW_KEY_RIGHT -> {
					if (Screen.hasControlDown()) {
						moveCursorTo(getWordPosition(1), Screen.hasShiftDown());
					} else {
						moveCursor(1, Screen.hasShiftDown());
					}
					return true;
				}
				case GLFW.GLFW_KEY_END -> {
					moveCursorToEnd(Screen.hasShiftDown());
					return true;
				}
				case GLFW.GLFW_KEY_DELETE -> {
					deleteText(1);
					return true;
				}
				case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
					if (!validText.isError()) {
						setFocused(false);
						onEnterPressed();
					}
					return true;
				}
				case GLFW.GLFW_KEY_TAB -> {
					if (!validText.isError()) {
						setFocused(false);
						onTabPressed();
					}
					return true;
				}
			}
		}

		return true;
	}

	/** {@inheritDoc} */
	@Override
	public boolean onIMEInput(char c, int modifiers) {
		if (isFocused()) {
			if (SharedConstants.isAllowedChatCharacter(c)) {
				insertText(Character.toString(c));
			}

			return true;
		}

		return false;
	}

	/**
	 * 文本内容变化时的回调方法。子类可重写以响应文本变化。
	 * <p>
	 * Callback method when text content changes. Subclasses can override to respond to text changes.
	 */
	public void onTextChanged() {
	}

	/**
	 * Tab键按下时的回调方法。
	 * <p>
	 * Callback method when the Tab key is pressed.
	 */
	public void onTabPressed() {
	}

	/**
	 * 回车键按下时的回调方法。
	 * <p>
	 * Callback method when the Enter key is pressed.
	 */
	public void onEnterPressed() {
	}

	/**
	 * 获取格式化后的显示文本。未聚焦且文本为空时返回占位文本。
	 * <p>
	 * Gets the formatted display text. Returns ghost text when unfocused and text is empty.
	 *
	 * @return 格式化后的显示文本 / Formatted display text
	 */
	public String getFormattedText() {
		return (!isFocused() && text.isEmpty() && !ghostText.isEmpty()) ? (ChatFormatting.ITALIC + ghostText) : text;
	}

	/** {@inheritDoc} */
	@Override
	public void getTooltip(TooltipBuilder tooltip) {
		super.getTooltip(tooltip);
		if(validText.hint()!=null)
			tooltip.accept(validText.hint());
	}

	/** {@inheritDoc} */
	@Override
	public void render(GuiGraphics graphics, int x, int y, int w, int h, RenderingHint hint) {
		drawTextBox(graphics, x, y, w, h, hint);
		var drawGhostText = !isFocused() && text.isEmpty() && !ghostText.isEmpty();
		var textToDraw = getFormattedText();
		graphics.enableScissor( x, y, x+w, y+h);
		if(this.isPressed) {
			if(CInputHelper.isMouseLeftDown()) {
				int i = getMouseTextPosition();
				String s = getFont().plainSubstrByWidth(text.substring(displayPos), getWidth());
				setSelectionPos(getFont().plainSubstrByWidth(s,i).length() + displayPos);
			}else
				isPressed=false;
			
		}
		Coloring color =validText.isError() ? errorColor : textColor;
		int cursorColor=color.getColorARGB(this, x, y, hint)|((drawGhostText ? 0x78000000 : 0xFF000000));
		int j = cursorPos - displayPos;
		String s = getFont().plainSubstrByWidth(textToDraw.substring(displayPos), w);
		if(rightAlign)
			textOffsetPos=w-getFont().width(s);
		int textX = x + textStartPos+textOffsetPos;
		int textY = y + (h - 8) / 2;
		int textX1 = textX;

		// render text up to cursor pos
		if (!s.isEmpty()) {
			var s1 = j > 0 && j <= s.length() ? s.substring(0, j) : s;
			textX1 = graphics.drawString(getFont(), Component.literal(s1), textX, textY, cursorColor, false);
		}

		// calculate cursor draw pos
		var drawCursor = cursorPos < textToDraw.length() || textToDraw.length() >= charLimit;
		var cursorX = textX1;
		if (j <= 0 || j > s.length()) {
			cursorX = j > 0 ? textX + w : textX;
		} else if (drawCursor) {
			cursorX = textX1 - 1;
		}

		// render text after cursor pos
		if (j > 0 && j < s.length()) {
			graphics.drawString(getFont(), Component.literal(s.substring(j)), textX1, textY, cursorColor, false);
		}

		// render the cursor
		if (j >= 0 && j <= s.length() && isFocused() && System.currentTimeMillis() % 1000L > 500L) {
			if (drawCursor) {
				graphics.fill(cursorX, textY - 1, cursorX+1, textY +getFont().lineHeight + 1, cursorColor);

			} else {
				int lh=textY + getFont().lineHeight - 2;
				graphics.fill(cursorX, lh, cursorX+5, lh+1, cursorColor);

			}
		}

		// highlight the selection if needed
		int k = Mth.clamp(highlightPos - displayPos, 0, s.length());
		if (k != j) {
			int xMax = textX + getFont().width(Component.literal(s.substring(0, k)));

			int startX = Math.min(cursorX, xMax - 1);
			int endX = Math.max(cursorX, xMax - 1);
			int startY = textY - 1;
			int endY = textY + getFont().lineHeight;

			endX = Math.min(endX, x + w);
			startX = Math.min(startX, x + w);

			graphics.fill(RenderType.guiTextHighlight(), startX, startY, endX, endY, 0x80000080);
		}

		graphics.disableScissor();
		RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
	}

	/**
	 * 绘制文本框背景。子类可重写以自定义背景绘制。
	 * <p>
	 * Draws the text box background. Subclasses can override for custom background rendering.
	 *
	 * @param graphics 图形上下文 / Graphics context
	 * @param x X坐标 / X coordinate
	 * @param y Y坐标 / Y coordinate
	 * @param w 宽度 / Width
	 * @param h 高度 / Height
	 */
	public void drawTextBox(GuiGraphics graphics, int x, int y, int w, int h, RenderingHint hint) {
		hint.theme(this).drawTextboxBackground(graphics, x, y, w, h, isFocused());
	}

	/**
	 * 验证文本是否合法。
	 * <p>
	 * Validates whether the text is valid.
	 *
	 * @param txt 要验证的文本 / Text to validate
	 * @return 验证结果 / Validation result
	 */
	public VerifyResult isValid(String txt) {
		return filter.test(txt);
	}

	/**
	 * 获取当前文本是否通过验证。
	 * <p>
	 * Gets whether the current text passes validation.
	 *
	 * @return 文本是否合法 / Whether the text is valid
	 */
	public final boolean isTextValid() {
		return !validText.isError();
	}

	/** {@inheritDoc} */
	@Override
	public Cursor getCursor() {
		return Cursor.IBEAM;
	}

}