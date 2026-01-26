package com.teammoeg.chorda.client.cui.contentpanel;

import com.teammoeg.chorda.client.cui.LayerScrollBar;
import com.teammoeg.chorda.client.cui.ScrollBar;
import com.teammoeg.chorda.client.cui.UIElement;
import com.teammoeg.chorda.client.cui.UILayer;
import com.teammoeg.chorda.client.ui.CGuiHelper;
import com.teammoeg.chorda.client.ui.Colors;
import com.teammoeg.frostedheart.content.archive.Alignment;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public class ContentPanel extends UILayer {
    public ScrollBar scrollBar;
    protected List<Line<?>> lines = new ArrayList<>();
    public static class Builder{
    	private ContentPanel parent;
    	public Builder(ContentPanel parent) {
			super();
			this.parent = parent;
		}
		public <T extends Line<?>>Builder add(T line,Consumer<T> config){
    		config.accept(line);
    		parent.lines.add(line);
    		parent.add(line);
    		return this;
    	}
        public  Builder text(String text) {
        	return text(text,a->{});
        }

        public  Builder text(Component text) {
        	return text(text,a->{});
        }
        public  Builder text(String text,Consumer<TextLine> config) {
        	return text(Component.literal(text),config);
        }

        public  Builder text(Component text,Consumer<TextLine> config) {
        	return add(new TextLine(parent, text, Alignment.LEFT),config);
        }

        public  Builder img(ResourceLocation imageLocation) {
        	 return img(imageLocation, a->{});
        }
        public  Builder img(String imageLocation) {
            return img(imageLocation, a->{});
        }

        public  Builder img(String imageLocation,Consumer<ImageLine> config) {
            return img(ResourceLocation.tryParse(imageLocation), config);
        }

        public  Builder img(ResourceLocation imageLocation,Consumer<ImageLine> config) {
        	return add(new ImageLine(parent, imageLocation, Alignment.CENTER),config);
        }
        public  Builder items(ItemStack... items) {
            return items(a->{},items);
        }

        public  Builder items(Collection<ItemStack> items) {
        	return items(items, a->{});
        }
        public  Builder items(Consumer<ItemRow> config,ItemStack... items) {
            return items(List.of(items),config);
        }

        public  Builder items(Collection<ItemStack> items,Consumer<ItemRow> config) {
        	return add(new ItemRow(parent, items, Alignment.CENTER),config);
        }
        public  Builder space() {
            return space(a->{});
        }

        public  Builder space(int height) {
            return space(height,a->{});
        }
        public  Builder space(Consumer<EmptyLine> config) {
            return space(8,config);
        }

        public  Builder space(int height,Consumer<EmptyLine> config) {
        	return add(new EmptyLine(parent, height),config);
        }
        public  Builder br() {
        	return br(a->{});
        }

        public  Builder br(int color) {
            return br(color,a->{});
        }
        public  Builder br(Consumer<BreakLine> config) {
        	return add(new BreakLine(parent),config);
        }

        public  Builder br(int color,Consumer<BreakLine> config) {
            return br(c->{c.color(color);config.accept(c);});
        }
    	public ContentPanel build() {
    		parent.refresh();
    		return parent;
    	}
    }
    public ContentPanel(UIElement parent) {
        super(parent);
        this.scrollBar = new LayerScrollBar(parent, true, this);
        resize();
    }
    public Builder builder() {
    	return new Builder(this);
    }
    public static Builder builder(UIElement parent) {
    	return new Builder(new ContentPanel(parent));
    }
    @Override
    public void drawBackground(GuiGraphics graphics, int x, int y, int w, int h) {
        int border = 8;
        graphics.fill(x-border, y-border, x+w+border*2, y+h+border, 0xFF444651);
        CGuiHelper.drawBox(graphics, x-border, y-border, w+border*3, h+border*2, Colors.L_BG_GRAY, true);
    }

    public void fillContent(Collection<? extends UIElement> widgets) {
        clearElement();
        for (UIElement widget : widgets) {
            if (widget instanceof Line<?> line) {
                this.lines.add(line);
            }
            add(widget);
        }
        refresh();
    }

    public void addLine(Line<?> line) {
        this.lines.add(line);
        add(line);
        refresh();
    }

    public void addLines(Collection<? extends Line<?>> lines) {
        this.lines.addAll(lines);
        lines.forEach(this::add);
        refresh();
    }

    @Override
    public void refresh() {
        resize();
        recalcContentSize();
        for (UIElement element : elements) {
            element.refresh();
        }
        alignWidgets();
        scrollBar.setValue(0);
    }

    public void resize() {
        scrollBar.setPosAndSize(getX() + getWidth()+9, -7, 6, getHeight()+14);
    }

    @Override
    public void alignWidgets() {
        align(4, false);
    }

    @Override
    public void addUIElements() {}
}
