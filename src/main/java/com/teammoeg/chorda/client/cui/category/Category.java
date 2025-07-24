package com.teammoeg.chorda.client.cui.category;

import com.teammoeg.chorda.client.MouseHelper;
import com.teammoeg.chorda.client.cui.Layer;
import com.teammoeg.chorda.client.cui.MouseButton;
import com.teammoeg.chorda.client.cui.UIWidget;
import com.teammoeg.chorda.client.icon.FlatIcon;
import com.teammoeg.chorda.client.ui.Colors;
import com.teammoeg.chorda.client.cui.LimitedTextField;
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public class Category extends Layer {
    public static final int MAX_DEPTH = 16;
    public static final int CHILDREN_OFFSET = 8;
    private final int depth;
    private final LimitedTextField title;
    @Getter
    protected final Category rootCategory;
    @Getter
    protected UIWidget selected;
    @Getter
    protected boolean opened = false;
    protected List<Entry> entries = new ArrayList<>();
    public int backgroundColor = Colors.L_BG_GRAY;

    public Category(Layer panel, Component title) {
        super(panel);
        setSize(panel.getWidth(), Entry.DEF_HEIGHT);
        this.title = new LimitedTextField(this, title, getWidth());

        if (panel instanceof Category p) {
            rootCategory = p.rootCategory;
            if (p.depth >= MAX_DEPTH) {
                depth = 1;
                setParent(p.rootCategory);
                rootCategory.add(this);
                setTitle(Component.literal("Category depth cannot greater than %s!".formatted(MAX_DEPTH)).withStyle(ChatFormatting.RED));
                return;
            }
            depth = p.depth + 1;
        } else {
            depth = 0;
            rootCategory = this;
        }
        panel.add(this);
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, int w, int h) {
        super.render(graphics, x, y, w, h);
        if (opened) {
            FlatIcon.DOWN.render(graphics.pose(), x+1, y+3, Colors.L_TEXT_GRAY);
        } else {
            FlatIcon.RIGHT.render(graphics.pose(), x, y+4, Colors.L_TEXT_GRAY);
        }
    }

    @Override
    public void drawBackground(GuiGraphics graphics, int x, int y, int w, int h) {
        graphics.fill(x, y, x+w, y+Entry.DEF_HEIGHT, backgroundColor);
        if (isMouseOver()) {
            graphics.fill(x-4, y, x-2, y+Entry.DEF_HEIGHT, Colors.L_TEXT_GRAY);
        }
    }

    @Override
    public void refresh() {
        setWidth(parent.getWidth() - CHILDREN_OFFSET);
        setOffsetY(Entry.DEF_HEIGHT);
        setOffsetX(CHILDREN_OFFSET);

        elements.remove(title);
        recalcContentSize();
        if (opened) {
            for (UIWidget ele : elements) {
                ele.refresh();
            }
            alignWidgets();
            setHeight(getContentHeight() + Entry.DEF_HEIGHT);
        } else {
            setHeight(Entry.DEF_HEIGHT);
        }
        elements.add(title);

        int titleOffsetX = 4;
        title.setX(getX() + titleOffsetX);
        title.setY(-Entry.DEF_HEIGHT + 4);
        title.setWidth(getWidth() - titleOffsetX - CHILDREN_OFFSET);
    }

    public void addEntries(Collection<? extends Entry> entries) {
        entries.forEach(this::add);
    }

    @Override
    public void addUIElements() {
        add(title);
    }

    @Override
    public void alignWidgets() {
        if (opened) {
            align(2, 2, false);
        } else {
            recalcContentSize();
        }
    }

    public void setOpened(boolean opened) {
        if (this.opened != opened) {
            this.opened = opened;
            rootCategory.getParent().refresh();
        }
    }

    public void select(UIWidget widget) {
        rootCategory.setSelected(widget);
    }

    private void setSelected(UIWidget widget) {
        selected = widget;
        for (UIWidget element : getElements()) {
            if (element instanceof Category sc) {
                sc.setSelected(widget);
            }
        }
    }

    @Override
    public boolean onMousePressed(MouseButton button) {
        if (!isMouseOver()) return false;

        if (MouseHelper.isMouseIn(getMouseX(), getMouseY(), 0, 0, getWidth(), Entry.DEF_HEIGHT)) {
            switch (button) {
                case LEFT -> {
                    setOpened(!opened);
                    return true;
                }
                case RIGHT -> {
                    setOpened(false);
                    return true;
                }
            }
        }

        for (int i = elements.size() - 1; i >= 0; i--) {
            UIWidget element = elements.get(i);
            if (element.isEnabled() && element.isVisible() && element.onMousePressed(button)) {
                return true;
            }
        }
        return false;
    }

    public Category setBgColor(int color) {
        this.backgroundColor = color;
        return this;
    }

    @Override
    public void getTooltip(Consumer<Component> list) {
        if (isEnabled() && MouseHelper.isMouseIn(getMouseX(), getMouseY(), 0, 0, getWidth(), Entry.DEF_HEIGHT)) {
            list.accept(getTitle());
        }
        super.getTooltip(list);
    }

    public void setTitle(Component title) {
        this.title.setTitle(title);
    }

    @Override
    public Component getTitle() {
        return title.getTitle();
    }
}
