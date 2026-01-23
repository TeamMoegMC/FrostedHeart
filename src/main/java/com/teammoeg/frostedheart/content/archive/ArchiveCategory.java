package com.teammoeg.frostedheart.content.archive;

import com.teammoeg.chorda.client.AnimationUtil;
import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.cui.LayerScrollBar;
import com.teammoeg.chorda.client.cui.MouseButton;
import com.teammoeg.chorda.client.cui.UIElement;
import com.teammoeg.chorda.client.cui.UILayer;
import com.teammoeg.chorda.client.cui.category.Category;
import com.teammoeg.chorda.client.cui.category.CategoryHelper;
import com.teammoeg.chorda.client.cui.category.Entry;
import com.teammoeg.chorda.client.cui.contentpanel.ContentPanel;
import com.teammoeg.chorda.client.cui.contentpanel.LineHelper;
import com.teammoeg.chorda.client.icon.FlatIcon;
import com.teammoeg.chorda.client.ui.CGuiHelper;
import com.teammoeg.chorda.client.ui.Colors;
import com.teammoeg.frostedheart.content.tips.Tip;
import com.teammoeg.frostedheart.content.tips.TipManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.*;

public class ArchiveCategory extends UILayer {
    public final LayerScrollBar scrollBar;
    protected final ContentPanel panel;
    private final Category root = new Category(this, Component.literal("root"));

    public static String currentPath = "";

    protected ArchiveCategory(UIElement panel, ContentPanel contentPanel) {
        super(panel);
        this.panel = contentPanel;
        this.scrollBar = new LayerScrollBar(parent, true, this) {
            @Override
            public void render(GuiGraphics graphics, int x, int y, int width, int height) {}
        };
        this.scrollBar.setScrollStep((Entry.DEF_HEIGHT+2)*2);
        this.root.clearElement();
        addUIElements();
        scrollTo(open(currentPath));
    }

    @Override
    public void drawBackground(GuiGraphics graphics, int x, int y, int w, int h) {
        final int border = 8;
        graphics.fill(x-border, y-border, x+w+border, y+h+border, 0xFF444651);
        CGuiHelper.drawBox(graphics, x-border, y-border, w+border*2, h+border*2, Colors.L_BG_GRAY, true);
    }

    public UIElement open(String path) {
        var entry = root.open(path);
        if (entry != null) {
            currentPath = path;
            if (entry instanceof ArchiveEntry ae) {
                // 在内容面板显示内容
                List<UIElement> contents = new ArrayList<>(ae.getContents());
                contents.addAll(ae.getExtraElements());
                panel.fillContent(contents);
                // 选中条目
                ae.getParent().select(ae);
                // 已读
                ae.read = ae.read();
            }
        }
        return entry;
    }

    public void scrollTo(UIElement widget) {
        if (widget != null) {
            scrollBar.setValue(widget.getScreenY());
        }
    }

    public UIElement find(String path) {
        return root.find(path);
    }

    @Override
    public void refresh() {
        setPosAndSize(0, 0, 100, (int) (ClientUtils.screenHeight()*0.8F));

        recalcContentSize();
        for (UIElement element : elements) {
            element.refresh();
        }
        alignWidgets();

        if (getY()+getContentHeight() < getY()+getHeight() || -getOffsetY()>scrollBar.getMax()) {
            scrollBar.setValue(scrollBar.getMax());
        }
    }

    public void addCategory() {
        Category tipCategory = new Category(this, Component.translatable("gui.frostedheart.archive.category.tips"));
        Set<String> childTipIds = new HashSet<>();
        Map<String, Category> subTipCategory = new HashMap<>();
        for (Tip tip : TipManager.INSTANCE.state().getAllUnlockedTips()) {
            // 获取所有子提示的 ID
            childTipIds.addAll(tip.getChildren());
            // 获取所有分类
            String categoryName = tip.getCategory();
            if (!categoryName.isBlank() && !subTipCategory.containsKey(categoryName)) {
                subTipCategory.put(categoryName, new Category(tipCategory, Component.translatable(categoryName)));
            }
        }
        List<TipEntry> tipEntries = new ArrayList<>();
        for (Tip tip : TipManager.INSTANCE.state().getAllUnlockedTips()) {
            if (tip.isHide()) continue;
            if (!childTipIds.contains(tip.getId())) {
                if (!tip.getCategory().isBlank()) {
                    // 在子分类添加提示
                    Category category = subTipCategory.get(tip.getCategory());
                    category.add(new TipEntry(category, panel, tip));
                } else {
                    // 在主提示分类添加所有非子提示且无分类的提示
                    TipEntry tipEntry = new TipEntry(tipCategory, panel, tip);
                    tipEntries.add(tipEntry);
                }
            }
        }
        // 在主提示分类里添加所有子分类和无分类提示
        tipCategory.addAll(tipEntries);
        root.getElements().add(tipCategory);
    }

    @Override
    public boolean onKeyPressed(int keyCode, int scanCode, int modifier) {
        Entry current = root.getSelected();
        if (current == null) {
            List<Entry> list = new ArrayList<>();
            CategoryHelper.collectAllEntries(root, list);
            if (list.isEmpty()) return super.onKeyPressed(keyCode, scanCode, modifier);
            current = list.get(0);
        } else {
            switch (keyCode) {
                case GLFW.GLFW_KEY_TAB -> {
                    if (modifier == GLFW.GLFW_MOD_SHIFT) {
                        current = CategoryHelper.prev(current);
                    } else {
                        current = CategoryHelper.next(current);
                    }
                }
                case GLFW.GLFW_KEY_W, GLFW.GLFW_KEY_UP   -> current = CategoryHelper.prev(current);
                case GLFW.GLFW_KEY_S, GLFW.GLFW_KEY_DOWN -> current = CategoryHelper.next(current);
            }
        }

        if (current == root.getSelected()) {
            return super.onKeyPressed(keyCode, scanCode, modifier);
        }
        scrollTo(open(currentPath = CategoryHelper.path(current)));
        return true;
    }

    @Override
    public void addUIElements() {
        clearElement();
        addCategory();
    }

    @Override
    public void alignWidgets() {
        align(0, 2, false);
    }

    public static class TipEntry extends ArchiveEntry {
        final Tip tip;
        final List<Tip> children;

        public TipEntry(Category parent, ContentPanel affectedPanel, Tip tip) {
            super(parent, affectedPanel, tip.getContents().get(0));
            this.tip = tip;
            this.children = TipManager.INSTANCE.state().getChildren(tip);
            read = isRead();
            setIcon(FlatIcon.LIST);
        }

        @Override
        public boolean isRead() {
            boolean hasUnread = !TipManager.INSTANCE.state().isViewed(tip);
            if (hasUnread) {
                return false;
            }
            for (Tip child : children) {
                hasUnread = !TipManager.INSTANCE.state().isViewed(child);
                if (hasUnread) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean read() {
            TipManager.INSTANCE.state().view(tip, true);
            for (Tip tip : children) {
                TipManager.INSTANCE.state().view(tip, true);
            }
            return true;
        }

        @Override
        public Collection<? extends UIElement> getContents() {
            return LineHelper.fromTip(tip, getPanel());
        }
    }

    public abstract static class ArchiveEntry extends Entry {
        protected boolean read;

        public ArchiveEntry(Category parent, ContentPanel affectedPanel, Component title) {
            super(parent, affectedPanel, title);
            read = isRead();
        }

        @Override
        public void render(GuiGraphics graphics, int x, int y, int w, int h) {
            super.render(graphics, x, y, w, h);
            if (!read) {
                float anim = AnimationUtil.progress(3000, "archive_unread", true);
                anim = ((float)Math.sin(anim*Math.PI*2)*0.5F+0.5F)*0.3F;
                graphics.fill(x, y, x+w, y+h, Colors.setAlpha(Colors.L_BG_GREEN, anim));
            }
        }

        public abstract boolean read();

        public abstract boolean isRead();

        public abstract Collection<? extends UIElement> getContents();

        public Collection<UIElement> getExtraElements() {
            return Collections.emptyList();
        }

        @Override
        public boolean onMousePressed(MouseButton button) {
            if (!isMouseOver()) return false;

            if (isEnabled() && isVisible() && button == MouseButton.LEFT) {
                if (getParent().getRoot().getParent() instanceof ArchiveCategory category) {
                    category.open(CategoryHelper.path(this));
                    return true;
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

        @Override
        public ContentPanel getPanel() {
            return (ContentPanel) panel;
        }
    }
}
