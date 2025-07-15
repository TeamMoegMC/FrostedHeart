package com.teammoeg.frostedheart.content.archive;

import com.teammoeg.chorda.client.AnimationUtil;
import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.StringTextComponentParser;
import com.teammoeg.chorda.client.cui.Layer;
import com.teammoeg.chorda.client.cui.LayerScrollBar;
import com.teammoeg.chorda.client.cui.MouseButton;
import com.teammoeg.chorda.client.cui.TextField;
import com.teammoeg.chorda.client.cui.UIWidget;
import com.teammoeg.chorda.client.ui.CGuiHelper;
import com.teammoeg.chorda.client.ui.ColorHelper;
import com.teammoeg.chorda.client.widget.IconButton;
import com.teammoeg.chorda.client.cui.ItemWidget;
import com.teammoeg.frostedheart.FrostedHud;
import com.teammoeg.frostedheart.content.tips.Tip;
import com.teammoeg.frostedheart.content.tips.TipManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.item.Items;

import java.util.*;

public class CategoryBox extends Layer {
    public static final int DEF_ITEM_HEIGHT = 16;
    public final LayerScrollBar scrollBar;
    protected final DetailBox detailBox;
    public Entry selected;

    protected CategoryBox(UIWidget panel, DetailBox detailBox) {
        super(panel);
        this.detailBox = detailBox;
        this.scrollBar = new LayerScrollBar(parent, true, this) {
            @Override
            public boolean isVisible() {
                return false;
            }
        };
        scrollBar.setScrollStep((DEF_ITEM_HEIGHT+2)*2);
        setSmoothScrollEnabled(true);
        addUIElements();
    }

    @Override
    public void refresh() {
        setPosAndSize(0, 0, 100, (int) (ClientUtils.screenHeight()*0.8F));
        scrollBar.setValue(0);

        recalcContentSize();
        for (UIWidget element : elements) {
            element.refresh();
        }
        alignWidgets();
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, int w, int h) {
        int border = 8;
        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(0, 0, -1);
        graphics.fill(x-border, y-border, x+w+border, y+h+border, -2, 0xFF444651);
        CGuiHelper.drawBox(graphics, x-border, y-border, w+border*2, h+border*2, ColorHelper.L_BG_GRAY, true);
        pose.popPose();

        super.render(graphics, x, y, w, h);
    }

    public void addCategories() {
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
                    category.add(new TipEntry(tip, category));
                } else {
                    // 在主提示分类添加所有非子提示且无分类的提示
                    TipEntry tipEntry = new TipEntry(tip, tipCategory);
                    tipEntries.add(tipEntry);
                }
            }
        }
        // 在主提示分类里添加所有子分类和无分类提示
        tipCategory.addAll(tipEntries);
        add(tipCategory);
    }

    @Override
    public void addUIElements() {
        addCategories();
        add(new ItemWidget(this, Items.STICK.getDefaultInstance()));
    }

    @Override
    public void alignWidgets() {
        align(2, 2, false);
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
            drawBackground(graphics, x, y, w, h);
            graphics.drawString(getFont(), title, x+12, y+4, ColorHelper.WHITE);
            if (opened) {
                super.render(graphics, x, y, w, h);
            }
        }

        @Override
        public void drawBackground(GuiGraphics graphics, int x, int y, int w, int h) {
            graphics.fill(x, y, x+w, y+DEF_ITEM_HEIGHT, ColorHelper.L_BG_GRAY);
            if (opened) {
                IconButton.Icon.DOWN.render(graphics.pose(), x+1, y+3, ColorHelper.L_TEXT_GRAY);
            } else {
                IconButton.Icon.RIGHT.render(graphics.pose(), x, y+4, ColorHelper.L_TEXT_GRAY);
            }
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
        public void alignWidgets() {
            if (opened) {
                align(2, 2, false);
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
            var grandpa = CategoryBox.this;
            grandpa.alignWidgets();
            if (!grandpa.scrollBar.isEnabled() && grandpa.getOffsetY() != 0) {
                grandpa.scrollBar.setValue(0);
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

        public void addAll(Collection<? extends UIWidget> widgets) {
            for (UIWidget widget : widgets) {
                add(widget);
            }
        }

        @Override
        public Component getTitle() {
            return this.title;
        }

        @Override
        public void addUIElements() {

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
            TipManager.INSTANCE.state().view(tip, true);
            for (Tip tip : children) {
                TipManager.INSTANCE.state().view(tip, true);
            }
            return true;
        }

        @Override
        public List<DetailBox.Line<?>> getContents() {
            List<DetailBox.Line<?>> lines = new ArrayList<>();
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
                    lines.add(box.text(Component.translatable("gui.frostedheart.archive.new_tip")).setTitle(tip.getFontColor(), 1).setBaseColor(ColorHelper.readableColor(tip.getFontColor())));
                } else {
                    lines.add(box.br());
                }
                if (j != 0 && !tipContents.get(0).equals(this.tip.getContents().get(0))) {
                    lines.add(box.text(tipContents.get(0)).setQuote(tip.getFontColor()));
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
                if (FrostedHud.renderDebugOverlay) {
                    lines.add(box.text("ID: " + tip.getId()).setBaseColor(ColorHelper.L_BG_GRAY).setAlignment(Alignment.RIGHT));
                }
            }
            return lines;
        }
    }

    public abstract class Entry extends Layer {
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
            super.render(graphics, x, y, w, h);
            if (getFont().width(cachedTitle) > w-4) {
                var t = FormattedText.composite(getFont().substrByWidth(cachedTitle, w-4), CommonComponents.ELLIPSIS);
                graphics.drawString(getFont(), Language.getInstance().getVisualOrder(t), x+2, y+4, baseColor);
            } else {
                graphics.drawString(getFont(), cachedTitle, x+2, y+4, baseColor);
            }
            if (!read) {
                float anim = AnimationUtil.progress(3000, "archive_unread", true);
                anim = ((float)Math.sin(anim*Math.PI*2)*0.5F+0.5F)*0.3F;
                graphics.fill(x, y, x+w, y+h, ColorHelper.setAlpha(ColorHelper.L_BG_GREEN, anim));
            }
        }

        @Override
        public void drawBackground(GuiGraphics graphics, int x, int y, int w, int h) {
            if (CategoryBox.this.selected == this) {
                graphics.fill(x-4, y, x-2, y+h, ColorHelper.L_BG_GREEN);
            }
            graphics.fill(x, y, x+w, y+h, ColorHelper.L_BG_GRAY);
        }

        public abstract boolean isRead();

        public abstract boolean read();

        public abstract Collection<DetailBox.Line<?>> getContents();

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
                    CategoryBox.this.selected = this;
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

        @Override
        public void alignWidgets() {}

        @Override
        public void addUIElements() {}
    }
}
