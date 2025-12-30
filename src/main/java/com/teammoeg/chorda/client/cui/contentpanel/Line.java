package com.teammoeg.chorda.client.cui.contentpanel;

import com.teammoeg.chorda.client.cui.Layer;
import com.teammoeg.chorda.client.cui.UIWidget;
import com.teammoeg.chorda.client.ui.Colors;
import com.teammoeg.frostedheart.content.archive.Alignment;
import lombok.Getter;
import net.minecraft.network.chat.Style;

@Getter
public abstract class Line<T extends Line<T>> extends Layer {
    public static final int DEF_LINE_HEIGHT = 12;
    protected static Style hoveredStyle;
    protected Alignment alignment;
    protected int color;

    public Line(UIWidget parent) {
        this(parent, Alignment.LEFT);
    }

    public Line(UIWidget parent, Alignment alignment) {
        this(parent,alignment, Colors.WHITE);
    }

    public Line(UIWidget parent, Alignment alignment, int color) {
        super(parent);
        this.alignment = alignment;
        this.color = color;
    }

    @SuppressWarnings("unchecked")
    public T alignment(Alignment alignment) {
        this.alignment = alignment;
        refresh();
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T color(int color) {
        this.color = color;
        refresh();
        return (T) this;
    }

    @Override
    public void refresh() {
        setWidth(parent.getWidth());
    }

    @Override
    public void alignWidgets() {}

    @Override
    public void addUIElements() {}
}
