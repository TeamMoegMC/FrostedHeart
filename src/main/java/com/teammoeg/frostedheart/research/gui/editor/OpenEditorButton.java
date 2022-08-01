package com.teammoeg.frostedheart.research.gui.editor;

import com.teammoeg.frostedheart.client.util.GuiUtils;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.SimpleTextButton;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;

import java.util.function.Consumer;

public class OpenEditorButton<T> extends SimpleTextButton {
    private final String lbl;
    private final Editor<T> edi;
    private final T val;
    private final Consumer<T> cb;

    public OpenEditorButton(Panel panel, String label, Editor<T> e, T val, Icon ic, Consumer<T> cb) {
        super(panel, GuiUtils.str(label), ic);
        lbl = label;
        edi = e;
        this.val = val;
        this.cb = cb;
    }

    public OpenEditorButton(Panel panel, String label, Editor<T> e, T val, Consumer<T> cb) {
        this(panel, label, e, val, Icon.EMPTY, cb);
    }

    @Override
    public void onClicked(MouseButton arg0) {
        edi.open(this, lbl, val, cb);
    }


}
