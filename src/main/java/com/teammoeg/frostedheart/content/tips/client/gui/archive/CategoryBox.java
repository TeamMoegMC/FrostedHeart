package com.teammoeg.frostedheart.content.tips.client.gui.archive;

import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.StringTextComponentParser;
import com.teammoeg.chorda.client.cui.Layer;
import com.teammoeg.chorda.client.cui.LayerScrollBar;
import com.teammoeg.chorda.client.cui.MouseButton;
import com.teammoeg.chorda.client.cui.UIWidget;
import com.teammoeg.chorda.client.ui.ColorHelper;
import com.teammoeg.frostedheart.content.tips.Tip;
import com.teammoeg.frostedheart.content.tips.TipManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CategoryBox extends Layer {
    public static final int DEF_ITEM_HEIGHT = 16;
    public final LayerScrollBar scrollBar;
    protected final DetailBox detailBox;
    public Entry selected;

    public CategoryBox(UIWidget panel, DetailBox detailBox) {
        super(panel);
        this.detailBox = detailBox;
        this.scrollBar = new LayerScrollBar(parent, true, this);
        setSmoothScrollEnabled(true);
        addUIElements();
    }

    @Override
    public void refresh() {
        setScissorEnabled(false);
        setPosAndSize(0, 0, 100, (int) (ClientUtils.screenHeight()*0.8F));

        recalcContentSize();
        for (UIWidget element : elements) {
            element.refresh();
        }
        alignWidgets();
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, int w, int h) {
        int border = 8;
        graphics.fill(x-border, y-border, x+w+border, y+h+border, -2, 0xFF444651);
        graphics.fill(x-border+1, y-border+1, x+w+border+1, y-border, -2, 0xFF585966);
        graphics.fill(x-border+1, y-border, x-border, y+h+border, -2, 0xFF585966);
        graphics.fill(x+w+border, y-border, x+w+border+1, y+h+border, -2, 0xFF585966);
        graphics.fill(x-border+1, y+h+border, x+w+border+1, y+h+border+1, -2, 0xFF585966);

        super.render(graphics, x, y, w, h);
    }

    public void addCategories() {
        Category tipCategory = new Category(this, Component.literal("Tips"));
        List<TipEntry> tipEntries = new ArrayList<>();
        Set<String> childTipIds = new HashSet<>();
        for (Tip tip : TipManager.INSTANCE.state().getAllUnlockedTips()) {
            childTipIds.addAll(tip.getChildren());
        }
        for (Tip tip : TipManager.INSTANCE.state().getAllUnlockedTips()) {
            if (!tip.isHide() && !childTipIds.contains(tip.getId())) {
                TipEntry tipEntry = new TipEntry(tip, tipCategory);
                tipEntries.add(tipEntry);
            }
        }
        tipCategory.addAll(tipEntries);
        add(tipCategory);
    }

    @Override
    public void addUIElements() {
        addCategories();
    }

    @Override
    public void alignWidgets() {
        align(false);
    }

    public class Category extends Layer {
        boolean opened = false;
        public final byte depth;
        public Component title;

        public Category(UIWidget parent, Component title) {
            super(parent);
            this.title = title;
            if (parent instanceof Category p) {
                if (p.depth >= 2) {
                    this.parent = CategoryBox.this;
                    this.depth = 1;
                    this.title = Component.literal("TOO COMPLEX!").withStyle(ChatFormatting.RED);
                } else {
                    this.depth = (byte)(p.depth + 1);
                }
                p.add(this);
            } else {
                this.depth = 1;
            }
        }

        @Override
        public void render(GuiGraphics graphics, int x, int y, int w, int h) {
            graphics.fill(x, y, x+w, y+h, 0x80000000);
            graphics.drawString(getFont(), title, x+2, y+4, ColorHelper.WHITE);
            if (opened) {
                super.render(graphics, x, y, w, h);
            }
        }

        @Override
        public void addUIElements() {

        }

        @Override
        public void alignWidgets() {
            if (opened) {
                align(false);
            } else {
                recalcContentSize();
            }
        }

        public void setOpened(boolean opened) {
            this.opened = opened;
            if (parent instanceof Category category) {
                category.refresh();
            } else {
                refresh();
            }
            CategoryBox.this.alignWidgets();
        }

        @Override
        public void refresh() {
            setWidth(parent.getWidth() - (depth <= 1 ? 0 : 8));
            setOffsetY(16);
            setOffsetX(8);

            recalcContentSize();
            if (opened) {
                for (UIWidget ele : elements) {
                    ele.refresh();
                }
                alignWidgets();
                setHeight(getContentHeight() + DEF_ITEM_HEIGHT);
            } else {
                setHeight(DEF_ITEM_HEIGHT);
            }
        }

        @Override
        public boolean onMousePressed(MouseButton button) {
            if (!isMouseOver()) {
                return false;
            }

            boolean consumed = false;
            for (int i = elements.size() - 1; i >= 0; i--) {
                UIWidget element = elements.get(i);

                if (element.isEnabled() && element.isVisible() && element.onMousePressed(button)) {
                    return true;
                }
            }

            if (!consumed) {
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
            return consumed;
        }

        public void addAll(Collection<? extends Entry> entries) {
            for (Entry entry : entries) {
                add(entry);
            }
        }

        @Override
        public Component getTitle() {
            return this.title;
        }
    }

    public class TipEntry extends Entry {
        final Tip tip;
        final List<Tip> children;

        public TipEntry(Tip tip, UIWidget parent) {
            super(parent);
            this.tip = tip;
            this.children = tip.getChildren().stream().filter(TipManager.INSTANCE.state()::isUnlocked).map(TipManager.INSTANCE::getTip).toList();
            this.title = tip.getContents().get(0);
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
            TipManager.INSTANCE.state().setViewState(tip, true);
            for (Tip tip : children) {
                TipManager.INSTANCE.state().setViewState(tip, true);
            }
            return true;
        }

        @Override
        public List<DetailBox.Line> getContents() {
            List<DetailBox.Line> lines = new ArrayList<>();
            List<Tip> tips = new ArrayList<>();
            tips.add(tip);
            tips.addAll(children);

            for (int j = 0; j < tips.size(); j++) {
                Tip tip = tips.get(j);
                if (tip.isHide()) continue;
                var tipContents = tip.getContents();
                if (j == 0) {
                    lines.add(box.text(tipContents.get(0)).setQuote(tip.getFontColor()));
                    lines.add(box.br());
                } else if (!TipManager.INSTANCE.state().isViewed(tip)) {
                    lines.add(box.br());
                    lines.add(box.text(Component.translatable("gui.frostedheart.archive.new_tip")).setTitle(tip.getFontColor(), 1).setBaseColor(ColorHelper.getTextColor(tip.getFontColor())));
                } else {
                    lines.add(box.br());
                }
                for (int i = 1; i < tipContents.size(); i++) {
                    Component line = tipContents.get(i);
                    if (!line.getString().isBlank()) {
                        lines.add(box.text(line));
                    }
                }
                if (tip.getImage() != null) {
                    var img = box.image(tip.getImage());
                    if (img.imgSize.width < 64) {
                        img.setBackgroundColor(ColorHelper.L_BG_GRAY);
                    }
                    lines.add(img);
                }
            }
            return lines;
        }
    }

    public abstract class Entry extends UIWidget {
        public final DetailBox box = CategoryBox.this.detailBox;
        public Component title = Component.empty();
        protected Component cachedTitle = Component.empty();
        public int baseColor = ColorHelper.WHITE;
        protected boolean read;

        public Entry(UIWidget parent) {
            super(parent);
        }

        @Override
        public void render(GuiGraphics graphics, int x, int y, int w, int h) {
            graphics.fill(x, y, x+w, y+h, 0x40000000);

            if (getFont().width(cachedTitle) > w-4) {
                var t = FormattedText.composite(getFont().substrByWidth(cachedTitle, w-4), CommonComponents.ELLIPSIS);
                graphics.drawString(getFont(), Language.getInstance().getVisualOrder(t), x+2, y+4, baseColor);
            } else {
                graphics.drawString(getFont(), cachedTitle, x+2, y+4, baseColor);
            }

            if (!read) {
                // TODO 优化小红点
                graphics.fill(x+w-6, y+2, x+w-4, y+4, ColorHelper.RED);
            }
        }

        public abstract boolean isRead();

        public abstract boolean read();

        public abstract Collection<DetailBox.Line> getContents();

        @Override
        public void refresh() {
            cachedTitle = StringTextComponentParser.parse(title.getString());
            setWidth(parent.getWidth()-8);
            setHeight(DEF_ITEM_HEIGHT);
            read = isRead();
        }

        @Override
        public boolean onMousePressed(MouseButton button) {
            if (isEnabled() && isVisible() && isMouseOver) {
                if (button == MouseButton.LEFT) {
                    CategoryBox.this.detailBox.fillContent(getContents());
                    read = read();
                }
                return true;
            }
            return false;
        }

        @Override
        public Component getTitle() {
            return this.title;
        }

        @Override
        public boolean isEnabled() {
            return !(parent instanceof Category p) || p.opened;
        }
    }
}
