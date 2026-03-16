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

package com.teammoeg.chorda.client.cui.contentpanel;

import com.teammoeg.chorda.client.StringTextComponentParser;
import com.teammoeg.chorda.client.cui.base.MouseButton;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.cui.widgets.Button;
import com.teammoeg.chorda.client.icon.FlatIcon;
import com.teammoeg.chorda.client.ui.CGuiHelper;
import com.teammoeg.frostedheart.content.archive.Alignment;
import com.teammoeg.frostedheart.content.tips.ClickActions;
import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 文本行，用于在内容面板中显示富文本内容。
 * 支持自动换行、文字缩放、标题样式、引用样式、阴影效果和行内按钮。
 * 文本通过StringTextComponentParser解析，支持格式代码和翻译键。
 * <p>
 * Text line for displaying rich text content in a content panel.
 * Supports automatic line wrapping, text scaling, title styling, quote styling,
 * shadow effects, and inline buttons. Text is parsed via StringTextComponentParser,
 * supporting formatting codes and translation keys.
 */
public class TextLine extends Line<TextLine> {
    @Getter
    protected Component text;
    protected final List<FormattedCharSequence> splitText = new ArrayList<>();
    protected int backgroundColor = 0;
    protected int trimmingColor = 0;
    protected boolean isTitle = false;
    protected boolean isQuote = false;
    protected boolean hasShadow = false;
    protected int scale = 1;
    protected List<Button> buttons = new ArrayList<>();

    public TextLine(UIElement parent, Component text, Alignment alignment) {
        super(parent, alignment);
        this.text = text;
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, int w, int h) {
        super.render(graphics, x, y, w, h);
        var pose = graphics.pose();
        pose.pushPose();

        if (scale > 1) {
            pose.translate(-x, -y, 0);
            pose.scale(scale, scale, 1);
            w = Math.round((float) w / scale);
            h = Math.round((float) h / scale);
        }

        int textW = w;
        if (isTitle) {
            graphics.fill(x, y, x+w, y+h, trimmingColor);
            int offset = switch (alignment) {
                case LEFT -> 2;
                case CENTER -> 0;
                case RIGHT -> -2;
            };
            textW -= offset;
            pose.translate(offset, 0, 0);

        } else if (isQuote) {
            graphics.fill(x, y, x+4, y+h, trimmingColor);
            int offset = switch (alignment) {
                case LEFT -> 8;
                case CENTER -> 0;
                case RIGHT -> -2;
            };
            textW -= offset;
            pose.translate(offset, 1F, 0);
        }

        pose.translate(0, 2F, 0);

        CGuiHelper.drawStringLinesInBound(graphics, getFont(), splitText, x, y, textW, color, 3,
                theme().isUITextShadow() || hasShadow, backgroundColor, alignment);

        pose.popPose();
    }

    public TextLine text(Component text) {
        this.text = text;
        refresh();
        return this;
    }

    public TextLine scale(int scale) {
        this.scale = Math.max(1, scale);
        refresh();
        return this;
    }

    public TextLine bgColor(int color) {
        this.backgroundColor = color;
        return this;
    }

    public TextLine title(int trimmingColor, int scale) {
        this.isTitle = true;
        this.trimmingColor = trimmingColor;
        scale(scale);
        return this;
    }

    public TextLine quote(int trimmingColor) {
        this.isQuote = true;
        this.color = trimmingColor;
        this.trimmingColor = trimmingColor;
        return this;
    }

    public TextLine shadow() {
        this.hasShadow = true;
        return this;
    }

    public TextLine button(Button button) {
        var old = button;
        var icon = (old instanceof FlatIconButton f) ? f.fIcon : FlatIcon.JUMP;
        button = new FlatIconButton(this, old.getTitle(), icon, old::onClicked);

        buttons.add(button);
        add(button);
        refresh();
        return this;
    }

    public TextLine button(ClickActions.ClickAction clickAction) {
        return clickAction == ClickActions.NO_ACTION ? this : button(clickAction.getDesc(), b -> clickAction.run());
    }

    public TextLine button(Component title, Consumer<MouseButton> clickAction) {
        return button(title, FlatIcon.JUMP, clickAction);
    }

    public TextLine button(Component title, FlatIcon icon, Consumer<MouseButton> clickAction) {
        var button = new FlatIconButton(this, title, icon, clickAction);
        return button(button);
    }

    @Override
    public void refresh() {
        super.refresh();

        int width = (getWidth() - ((isTitle||isQuote) ? 8 : 0));
        if (!buttons.isEmpty()) {
            int btnX = alignment == Alignment.RIGHT ? 0 : getWidth();
            for (Button button : buttons) {
                button.setPos(btnX-12, (getHeight() - button.getHeight())/2);
                btnX += alignment == Alignment.RIGHT ? button.getWidth()+2 : -button.getWidth()-2;
            }
            if (alignment == Alignment.RIGHT) {
                width -= btnX;
            } else {
                width -= width-btnX;
            }
        }
        width = (int)(width * (1.0F/scale));

        var pre = text.getString().split("\\n");
        splitText.clear();
        for (String s : pre) {
            splitText.addAll(getFont().split(StringTextComponentParser.parse(s), width));
        }
        setHeight(splitText.size() * (DEF_LINE_HEIGHT + (isQuote ? 2 : 0)) * scale);
    }

    @Override
    public Component getTitle() {
        return getText();
    }
}
