package com.teammoeg.chorda.client.cui.category;

import com.teammoeg.chorda.client.MouseHelper;
import com.teammoeg.chorda.client.cui.UILayer;
import com.teammoeg.chorda.client.cui.LimitedTextField;
import com.teammoeg.chorda.client.cui.MouseButton;
import com.teammoeg.chorda.client.cui.UIElement;
import com.teammoeg.chorda.client.icon.FlatIcon;
import com.teammoeg.chorda.client.ui.Colors;
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public class Category extends UILayer {
    public static final int MAX_DEPTH = 16;
    public static final int CHILDREN_OFFSET = 8;
    private final int depth;
    private final LimitedTextField title;
    @Getter
    protected final Category root;
    @Getter
    protected Entry selected;
    @Getter
    protected boolean opened = false;
    public int backgroundColor = Colors.L_BG_GRAY;
    protected FlatIcon.FlatIconWidget icon;

    public Category(UILayer panel, Component title) {
        super(panel);
        setSize(panel.getWidth(), Entry.DEF_HEIGHT);

        this.title = new LimitedTextField(this, title, getWidth());
        this.icon = FlatIcon.RIGHT.toWidget(this, Colors.L_TEXT_GRAY);
        this.icon.setSize(10, 10);
        addUIElements();

        if (panel instanceof Category p) {
            root = p.root;
            if (p.depth >= MAX_DEPTH) {
                depth = 1;
                setParent(p.root);
                root.add(this);
                setTitle(Component.literal("Category depth cannot greater than %s!".formatted(MAX_DEPTH)).withStyle(ChatFormatting.RED));
                return;
            }
            depth = p.depth + 1;
        } else {
            depth = 0;
            root = this;
        }
        if (!panel.getElements().contains(this)) {
            panel.add(this);
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
        setWidth(parent.getWidth() - (parent instanceof Category ? CHILDREN_OFFSET : 0));
        setOffsetY(Entry.DEF_HEIGHT);
        setOffsetX(CHILDREN_OFFSET);

        elements.remove(title);
        elements.remove(icon);
        recalcContentSize();
        if (opened) {
            for (UIElement ele : elements) {
                ele.refresh();
            }
            alignWidgets();
            setHeight(getContentHeight() + Entry.DEF_HEIGHT);
        } else {
            setHeight(Entry.DEF_HEIGHT);
        }
        elements.add(icon);
        elements.add(title);

        icon.setPos(-CHILDREN_OFFSET, -Entry.DEF_HEIGHT + 3);
        int titleOffsetX = 4;
        title.setPos(titleOffsetX, -Entry.DEF_HEIGHT + 4);
        title.setWidth(getWidth() - titleOffsetX - CHILDREN_OFFSET);
    }

    public void addAll(Collection<? extends UIElement> widgets) {
        widgets.forEach(this::add);
    }

    @Override
    public void addUIElements() {
        add(this.title);
        add(this.icon);
    }

    @Override
    public void alignWidgets() {
        if (opened) {
            align(2, 2, false);
        } else {
            recalcContentSize();
        }
    }

    /**
     * @param path 获取路径的最后一个元素
     * @return 路径的最后一个元素
     */
    public UIElement find(String path) {
        if (path == null || path.isBlank()) return null;

        String[] segments = path.split("/");
        Category currentCategory = root;

        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i].trim();
            if (segment.isEmpty()) continue;

            List<UIElement> elements = currentCategory.getElements();
            UIElement found = null;

            for (UIElement element : elements) {
                if (segment.equals(CategoryHelper.getRawTitle(element))) {
                    found = element;
                    break;
                }
            }

            if (found == null) return null;

            if (i == segments.length - 1) {
                return found;
            } else if (found instanceof Category) {
                currentCategory = (Category) found;
            } else {
                return null;
            }
        }

        return null;
    }

    /**
     * @param path 打开路径中的所有Category
     * @return 路径的最后一个元素
     */
    public UIElement open(String path) {
        UIElement widget = find(path);
        if (widget == null) {
            return null;
        } else if (widget instanceof Category category) {
            category.setOpened(true);
        }

        UIElement parent = widget.getParent();
        while (parent instanceof Category category) {
            category.setOpened(true);
            parent = parent.getParent();
        }

        if (widget instanceof Entry entry) {
            select(entry);
        }
        return widget;
    }

    public void setOpened(boolean opened) {
        if (this.opened != opened) {
            this.opened = opened;
            icon.setIcon(opened ? FlatIcon.DOWN : FlatIcon.RIGHT);
            root.parent.refresh();
        }
    }

    public void select(Entry widget) {
        root.setSelected(widget);
    }

    private void setSelected(Entry widget) {
        selected = widget;
        for (UIElement element : getElements()) {
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
            UIElement element = elements.get(i);
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
        if (isEnabled() && isVisible() && MouseHelper.isMouseIn(getMouseX(), getMouseY(), 0, 0, getWidth(), Entry.DEF_HEIGHT)) {
            list.accept(getTitle());
        }
        super.getTooltip(list);
    }

    @Override
    public boolean isVisible() {
        return getParent() instanceof Category p ? p.isOpened() : super.isVisible();
    }

    public void setTitle(Component title) {
        this.title.setTitle(title);
    }

    @Override
    public Component getTitle() {
        return title.getTitle();
    }
}
