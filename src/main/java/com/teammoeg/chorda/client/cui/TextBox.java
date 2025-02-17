package com.teammoeg.chorda.client.cui;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.systems.RenderSystem;
import com.teammoeg.chorda.client.CInputHelper;
import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.lang.Components;
import com.teammoeg.chorda.client.CInputHelper.Cursor;

import java.util.Objects;
import java.util.function.Predicate;

public class TextBox extends UIElement implements Focusable {
	private boolean isFocused = false;
	public int charLimit = 2000;
	public int textColor;

	public String ghostText = "";
	private String text = "";
	private int displayPos;
	private int cursorPos;
	private int highlightPos;
	private boolean validText = true;
	private int maxLength = 1024;
	private Predicate<String> filter;

	public TextBox(Layer panel) {
		super(panel);

		this.filter = Objects::nonNull;
	}
	public void setMaxLength(int maxLength) {
		this.maxLength = maxLength;
	}

	@Override
	public void setWidth(int v) {
		super.setWidth(v);
		scrollTo(getCursorPos());
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

	public void setFilter(Predicate<String> filter) {
		this.filter = filter;
	}

	public final String getText() {
		return text;
	}

	public String getSelectedText() {
		return text.substring(Math.min(cursorPos, highlightPos), Math.max(cursorPos, highlightPos));
	}

	public void setText(String string, boolean triggerChange) {
		if (filter.test(string)) {
			if (string.length() > maxLength) {
				text = string.substring(0, maxLength);
			} else {
				text = string;
			}

			validText = isValid(text);

			moveCursorToEnd(false);
			setSelectionPos(cursorPos);
			if (triggerChange) {
				onTextChanged();
			}
		}
	}

	public final void setText(String s) {
		setText(s, true);
	}

	public void moveCursor(int pos, boolean extendSelection) {
		moveCursorTo(getCursorPos(pos), extendSelection);
	}

	private int getCursorPos(int pos) {
		return Util.offsetByCodepoints(text, cursorPos, pos);
	}

	public void setCursorPosition(int pos) {
		cursorPos = Mth.clamp(pos, 0, text.length());
		scrollTo(cursorPos);
	}

	public void moveCursorTo(int pos, boolean extendSelection) {
		setCursorPosition(pos);
		if (!extendSelection) {
			setSelectionPos(cursorPos);
		}

		onTextChanged();
	}

	public void moveCursorToStart(boolean extendSelection) {
		moveCursorTo(0, extendSelection);
	}

	public void moveCursorToEnd(boolean extendSelection) {
		moveCursorTo(text.length(), extendSelection);
	}

	public void setCursorPos(int pos) {
		cursorPos = Mth.clamp(pos, 0, text.length());
		scrollTo(cursorPos);
	}

	public void setSelectionPos(int i) {
		highlightPos = Mth.clamp(i, 0, text.length());
		scrollTo(highlightPos);
	}

	public int getCursorPos() {
		return cursorPos;
	}

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
			if (validText) {
				text = newText;
				setCursorPosition(selStart + nToInsert);
				setSelectionPos(cursorPos);
				onTextChanged();
			}
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

	public void deleteWords(int count) {
		if (!text.isEmpty()) {
			if (highlightPos != cursorPos) {
				insertText("");
			} else {
				deleteCharsToPos(getWordPosition(count));
			}
		}
	}

	public void deleteChars(int count) {
		deleteCharsToPos(getCursorPos(count));
	}

	public void deleteCharsToPos(int pos) {
		if (!text.isEmpty()) {
			if (highlightPos != cursorPos) {
				insertText("");
			} else {
				int from = Math.min(pos, cursorPos);
				int to = Math.max(pos, cursorPos);
				if (from != to) {
					String newText = new StringBuilder(text).delete(from, to).toString();
					if (filter.test(newText)) {
						text = newText;
						moveCursorTo(from, false);
					}
				}
			}
		}
	}

	@Override
	public boolean onMousePressed(MouseButton button) {
		if (isMouseOver()) {
			setFocused(true);
			if (button==MouseButton.LEFT) {
				if (isFocused) {
					int i = getMouseX() - getX();
					String s = getFont().plainSubstrByWidth(text.substring(displayPos), getWidth());
					if (CInputHelper.isShiftKeyDown()) {
						setSelectionPos(getFont().plainSubstrByWidth(s,i).length() + displayPos);
					} else {
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
					if (validText) {
						setFocused(false);
						onEnterPressed();
					}
					return true;
				}
				case GLFW.GLFW_KEY_TAB -> {
					if (validText) {
						setFocused(false);
						onTabPressed();
					}
					return true;
				}
			}
		}

		return true;
	}

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

	public void onTextChanged() {
	}

	public void onTabPressed() {
	}

	public void onEnterPressed() {
	}

	public String getFormattedText() {
		return (!isFocused() && text.isEmpty() && !ghostText.isEmpty()) ? (ChatFormatting.ITALIC + ghostText) : text;
	}

	@Override
	public void render(GuiGraphics graphics, int x, int y, int w, int h) {
		drawTextBox(graphics, x, y, w, h);
		var drawGhostText = !isFocused() && text.isEmpty() && !ghostText.isEmpty();
		var textToDraw = getFormattedText();
		graphics.enableScissor( x, y, w, h);
	
		int cursorColor = (validText ? this.getLayerHolder().getFontColor():this.getLayerHolder().getErrorColor())&0xffffff|((drawGhostText ? 120 : 255)<<24);
		var j = cursorPos - displayPos;
		var s = getFont().plainSubstrByWidth(textToDraw.substring(displayPos), w);
		var textX = x + 4;
		var textY = y + (h - 8) / 2;
		var textX1 = textX;

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
			var xMax = textX + getFont().width(Component.literal(s.substring(0, k)));

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

	public void drawTextBox(GuiGraphics graphics, int x, int y, int w, int h) {
		int i = this.isFocused() ? -1 : -6250336;
		graphics.fill(x - 1, y - 1, x + w + 1, y + h + 1, i);
		graphics.fill(x, y, x + w, y + h, -16777216);
	}

	public boolean isValid(String txt) {
		return filter.test(txt);
	}

	public final boolean isTextValid() {
		return validText;
	}

	@Override
	public Cursor getCursor() {
		return Cursor.IBEAM;
	}

}