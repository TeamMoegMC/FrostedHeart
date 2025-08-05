package com.teammoeg.chorda.client.cui.contentpanel;

import com.teammoeg.chorda.client.cui.ItemWidget;
import com.teammoeg.chorda.client.cui.UIWidget;
import com.teammoeg.chorda.client.ui.Colors;
import com.teammoeg.frostedheart.content.archive.Alignment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ItemRow extends Line<ItemRow> {
    protected final List<ItemStack> items = new ArrayList<>();
    protected int rowSize = 1;
    protected int backgroundColor = Colors.L_BG_GRAY;

    public ItemRow(UIWidget parent, Collection<ItemStack> items, Alignment alignment) {
        super(parent, alignment);
        this.items.addAll(items);
        addUIElements();
    }

    public void addItem(ItemStack item) {
        items.add(item);
        elements.add(new ItemWidget(this, item));
    }

    @Override
    public void addUIElements() {
        this.elements.addAll(items.stream().map(item -> new ItemWidget(this, item)).toList());
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, int w, int h) {
        graphics.fill(x, y, x+w, y+h, backgroundColor);
        super.render(graphics, x, y, w, h);
    }

    @Override
    public void refresh() {
        super.refresh();
        rowSize = Math.max(getWidth() / (ItemWidget.ITEM_WIDTH+4), 1);
        setHeight(Math.max((int)Math.ceil((double)items.size() / rowSize), 1) * (ItemWidget.ITEM_HEIGHT+4));
        alignWidgets();
    }

    @Override
    public void alignWidgets() {
        List<List<UIWidget>> rows = new ArrayList<>();
        List<UIWidget> row = new ArrayList<>();
        for (int i = 0; i < elements.size(); i++) {
            if (i > 0 && i % rowSize == 0) {
                rows.add(row);
                row = new ArrayList<>();
            }
            row.add(elements.get(i));
        }
        rows.add(row);

        for (int y = 0; y < rows.size(); y++) {
            List<UIWidget> widgets = rows.get(y);
            for (int x = 0; x < widgets.size(); x++) {
                UIWidget widget = widgets.get(x);
                switch (alignment) {
                    case LEFT -> widget.setPos(x*(ItemWidget.ITEM_WIDTH +4)+2, y*(ItemWidget.ITEM_HEIGHT+4)+2);
                    case CENTER -> widget.setPos((getWidth()/2 - widgets.size()*10) + x*(ItemWidget.ITEM_WIDTH+4) + 2, y*(ItemWidget.ITEM_HEIGHT+4)+2);
                    case RIGHT -> widget.setPos(getWidth()-16-2 - x*(ItemWidget.ITEM_WIDTH +4), y*(ItemWidget.ITEM_HEIGHT+4)+2);
                }
            }
        }
    }

    public ItemRow bgColor(int color) {
        this.backgroundColor = color;
        return this;
    }

    @Override
    public Component getTitle() {
        var title = Component.empty();
        int max = Math.min(items.size(), 4);
        for (int i = 0; i < max; i++) {
            ItemStack item = items.get(i);
            title.append(item.getDisplayName());
            if (i < max-1) {
                title.append(", ");
            }
        }
        if (items.size() > 4) {
            title.append("...");
        }
        return title;
    }
}
