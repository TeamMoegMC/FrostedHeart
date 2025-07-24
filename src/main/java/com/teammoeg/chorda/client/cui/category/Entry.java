package com.teammoeg.chorda.client.cui.category;

import com.teammoeg.chorda.client.cui.Layer;
import com.teammoeg.chorda.client.cui.MouseButton;
import com.teammoeg.chorda.client.cui.UIWidget;
import com.teammoeg.chorda.client.ui.Colors;
import com.teammoeg.chorda.client.cui.LimitedTextField;
import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public abstract class Entry extends Layer {
    public static final int DEF_HEIGHT = 16;
    @Getter
    protected final Layer panel;
    protected LimitedTextField title;

    public Entry(Category parent, Layer affectedPanel, Component title) {
        super(parent);
        this.panel = affectedPanel;
        this.title = new LimitedTextField(this, title, getWidth());
        addUIElements();
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, int w, int h) {
        super.render(graphics, x, y, w, h);
    }

    @Override
    public void drawBackground(GuiGraphics graphics, int x, int y, int w, int h) {
        graphics.fill(x, y, x+w, y+h, Colors.L_BG_GRAY);
        if (isMouseOver() && isEnabled()) {
            graphics.fill(x-4, y, x-2, y+h, Colors.L_TEXT_GRAY);
        }
        if (getParent().selected == this) {
            graphics.fill(x-4, y, x-2, y+h, Colors.L_BG_GREEN);
        }
    }

    @Override
    public void refresh() {
        setWidth(parent.getWidth()-8);
        setHeight(DEF_HEIGHT);
        int offsetX = 4;
        setOffsetX(offsetX);
        int y = Math.round((getHeight() - title.getHeight())*0.5F);
        title.setPos(getX(), y);
        title.setWidth(getWidth()-offsetX);

        recalcContentSize();
        for (UIWidget element : elements) {
            element.refresh();
        }
        alignWidgets();
    }

    @Override
    public boolean onMousePressed(MouseButton button) {
        boolean pressed = super.onMousePressed(button);
        if (pressed) {
            getParent().select(this);
        }
        return pressed;
    }

    @Override
    public void addUIElements() {
        add(title);
    }

    @Override
    public Category getParent() {
        return (Category) super.getParent();
    }

    @Override
    public void getTooltip(Consumer<Component> list) {
        if (!hasTooltip() || !isMouseOver()) {
            return;
        }
        list.accept(getTitle());
        super.getTooltip(list);
    }

    public void setTitle(Component title) {
        this.title.setTitle(title);
    }

    @Override
    public Component getTitle() {
        return this.title.getTitle();
    }

    @Override
    public void alignWidgets() {}
}
