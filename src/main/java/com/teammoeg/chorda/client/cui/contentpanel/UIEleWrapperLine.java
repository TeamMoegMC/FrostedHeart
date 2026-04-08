package com.teammoeg.chorda.client.cui.contentpanel;

import com.teammoeg.chorda.client.cui.base.UIElement;
import lombok.Getter;

public class UIEleWrapperLine extends Line<UIEleWrapperLine> {
    @Getter
    private final UIElement wrappedEle;

    public UIEleWrapperLine(UIElement parent, UIElement wrappedElement) {
        super(parent);
        this.wrappedEle = wrappedElement;
        elements.add(wrappedElement);
    }

    @Override
    public void refresh() {
        super.refresh();
        wrappedEle.setWidth(getWidth());
        wrappedEle.refresh();
        if (wrappedEle.getHeight() == 0) {
            wrappedEle.setHeight(16);
        }
        setHeight(wrappedEle.getHeight());
    }
}
