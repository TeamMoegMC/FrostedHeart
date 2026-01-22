package com.teammoeg.chorda.client.cui;

import com.teammoeg.chorda.client.icon.CIcons;
import net.minecraft.client.gui.GuiGraphics;

import java.util.function.Supplier;

public abstract class TabImageButtonElement extends Button{
    final int tab;
    Supplier<Integer> currentTab;
    private final CIcons.CIcon inactiveIcon;
    public TabImageButtonElement(UIElement parent, int xIn, int yIn, int widthIn, int heightIn, int tab,
                          CIcons.CIcon icon1, CIcons.CIcon icon2) {
        super(parent);
        setPos(xIn, yIn);
        this.tab=tab;
        super.setIcon(icon1);
        this.inactiveIcon = icon2;
        this.setWidth(widthIn);
        this.setHeight(heightIn);
    }

    protected void fitSize() {

    }

    public TabImageButtonElement bind(Supplier<Integer> supp) {
        this.currentTab=supp;
        return this;
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, int w, int h) {
        int current=0;
        if(currentTab!=null)
            current=currentTab.get();

        if (tab == current) {
            drawIcon(graphics, x, y, w, h);
        }
        else inactiveIcon.draw(graphics, x, y, w, h);

    }
}
