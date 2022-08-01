package com.teammoeg.frostedheart.research.gui.editor;

import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.TextField;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.ui.WidgetLayout;

public class LabeledPane<T extends Widget> extends Panel {

    protected TextField label;
    protected T obj;

    public LabeledPane(Panel panel, String lab) {
        super(panel);
        label = new TextField(this).setMaxWidth(200).setTrim().setText(lab);

    }

    @Override
    public void addWidgets() {
        add(label);
        if (obj != null) ;
        add(obj);
    }

    @Override
    public void alignWidgets() {
        setSize(super.align(WidgetLayout.HORIZONTAL), 20);
    }
}
