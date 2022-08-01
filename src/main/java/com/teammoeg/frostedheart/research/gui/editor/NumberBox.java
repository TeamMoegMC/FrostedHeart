package com.teammoeg.frostedheart.research.gui.editor;

import dev.ftb.mods.ftblibrary.ui.Panel;

public class NumberBox extends LabeledTextBox {

    public NumberBox(Panel panel, String lab, long val) {
        super(panel, lab, String.valueOf(val));
    }

    public void setNum(long number) {
        super.setText(String.valueOf(number));
    }

    ;

    public long getNum() {
        try {
            return Long.parseLong(getText());
        } catch (NumberFormatException ex) {

        }
        return Long.parseLong(orig);
    }

    ;
}
