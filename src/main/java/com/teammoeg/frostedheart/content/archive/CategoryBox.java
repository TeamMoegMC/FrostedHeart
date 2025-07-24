package com.teammoeg.frostedheart.content.archive;

import com.teammoeg.chorda.client.AnimationUtil;
import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.cui.Layer;
import com.teammoeg.chorda.client.cui.LayerScrollBar;
import com.teammoeg.chorda.client.cui.MouseButton;
import com.teammoeg.chorda.client.cui.UIWidget;
import com.teammoeg.chorda.client.cui.contentpanel.ContentPanel;
import com.teammoeg.chorda.client.ui.CGuiHelper;
import com.teammoeg.chorda.client.ui.Colors;
import com.teammoeg.chorda.client.cui.category.Category;
import com.teammoeg.chorda.client.cui.category.Entry;
import com.teammoeg.chorda.client.cui.contentpanel.Line;
import com.teammoeg.chorda.client.cui.contentpanel.LineHelper;
import com.teammoeg.frostedheart.content.tips.Tip;
import com.teammoeg.frostedheart.content.tips.TipManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CategoryBox extends Layer {
    public final LayerScrollBar scrollBar;
    protected final ContentPanel panel;

    protected CategoryBox(UIWidget panel, ContentPanel contentPanel) {
        super(panel);
        this.panel = contentPanel;
        this.scrollBar = new LayerScrollBar(parent, true, this) {
            @Override
            public boolean isVisible() {
                return false;
            }
        };
        scrollBar.setScrollStep((Entry.DEF_HEIGHT+2)*2);
        addUIElements();
    }

    @Override
    public void refresh() {
        setPosAndSize(0, 0, 100, (int) (ClientUtils.screenHeight()*0.8F));

        recalcContentSize();
        for (UIWidget element : elements) {
            element.refresh();
        }
        alignWidgets();

        if (getY()+getContentHeight() < getY()+getHeight() || -getOffsetY()>scrollBar.getMax()) {
            scrollBar.setValue(scrollBar.getMax());
        }
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, int w, int h) {
        int border = 8;
        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(0, 0, -1);
        graphics.fill(x-border, y-border, x+w+border, y+h+border, -2, 0xFF444651);
        CGuiHelper.drawBox(graphics, x-border, y-border, w+border*2, h+border*2, Colors.L_BG_GRAY, true);
        pose.popPose();

        super.render(graphics, x, y, w, h);
    }

    public void addCategory() {
        Category tipCategory = new Category(this, Component.translatable("gui.frostedheart.archive.category.tips"));
        Set<String> childTipIds = new HashSet<>();
        Map<String, Category> subTipCategory = new HashMap<>();
        for (Tip tip : TipManager.INSTANCE.state().getAllUnlockedTips()) {
            // 获取所有子提示的ID
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
        tipCategory.addEntries(tipEntries);
    }

    @Override
    public void addUIElements() {
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
        public List<Line<?>> getContents() {
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

        public abstract Collection<Line<?>> getContents();

        @Override
        public boolean onMousePressed(MouseButton button) {
            if (!isMouseOver()) return false;

            if (isEnabled() && isVisible()) {
                if (button == MouseButton.LEFT) {
                    getPanel().fillContent(getContents());
                    getParent().select(this);
                    read = read();
                    return true;
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

        @Override
        public ContentPanel getPanel() {
            return (ContentPanel) panel;
        }
    }
}
