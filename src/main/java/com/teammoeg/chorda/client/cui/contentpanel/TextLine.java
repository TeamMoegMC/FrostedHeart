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
import com.teammoeg.chorda.client.cui.base.TooltipBuilder;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.cui.widgets.Button;
import com.teammoeg.chorda.client.icon.FlatIcon;
import com.teammoeg.chorda.client.ui.CGuiHelper;
import com.teammoeg.chorda.client.ui.Colors;
import com.teammoeg.frostedheart.content.archive.Alignment;
import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

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
    protected Button button = null;

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
                hasShadow, backgroundColor, alignment);

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

    public TextLine button(Component title, Consumer<MouseButton> clickAction) {
        return button(title, FlatIcon.JUMP, clickAction);
    }

    public TextLine button(Component title, FlatIcon icon, Consumer<MouseButton> clickAction) {
        if (clickAction == null) return this;
        button = new TLButton(title, icon, clickAction);
        add(button);
        refresh();
        return this;
    }

    class TLButton extends Button {
        Consumer<MouseButton> clickAction;
        FlatIcon fIcon;

        public TLButton(Component t, FlatIcon icon, Consumer<MouseButton> clickAction) {
            super(TextLine.this, t, icon.toCIcon());
            this.clickAction = clickAction;
            this.fIcon = icon;
            setSize(icon.size.width + 2, icon.size.height + 2);
        }

        @Override
        public void onClicked(MouseButton button) {
            clickAction.accept(button);
        }

        @Override
        public void render(GuiGraphics graphics, int x, int y, int w, int h) {
            graphics.fill(x, y, x+w, y+h, Colors.L_BG_GRAY);
            icon.draw(graphics, x+1, y+1, fIcon.size.width, fIcon.size.height);
            if (isMouseOver()) {
                CGuiHelper.drawBox(graphics, x, y, w, h, trimmingColor == 0 ? Colors.themeColor() : trimmingColor, true);
            }
        }

        @Override
        public void getTooltip(TooltipBuilder tooltip) {
            tooltip.accept(getTitle());
        }
    }

    @Override
    public void refresh() {
        super.refresh();
        int width = (getWidth() - ((isTitle||isQuote) ? 8 : 0));
        width -= button == null ? 0 : button.getWidth();
        width = (int)(width * (1.0F/scale));

        var pre = text.getString().split("\\n");
        splitText.clear();
        for (String s : pre) {
            splitText.addAll(getFont().split(StringTextComponentParser.parse(s), width));
        }
        setHeight(splitText.size() * (DEF_LINE_HEIGHT + (isQuote ? 2 : 0)) * scale);

        if (button != null) {
            button.setPos(getWidth() - button.getWidth(), (getHeight()-button.getHeight())/2);
        }
    }

    @Override
    public Component getTitle() {
        return getText();
    }
}
