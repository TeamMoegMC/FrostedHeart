package com.teammoeg.frostedheart.research.gui.drawdesk;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.research.gui.ResearchGui;
import com.teammoeg.frostedheart.research.gui.editor.EditDialog;
import com.teammoeg.frostedheart.research.gui.tech.ResearchPanel;
import com.teammoeg.frostedheart.research.machines.DrawingDeskTileEntity;
import dev.ftb.mods.ftblibrary.ui.BaseScreen;
import dev.ftb.mods.ftblibrary.ui.Theme;

public class DrawDeskScreen extends BaseScreen implements ResearchGui {
    DrawDeskContainer cx;
    DrawDeskPanel p;
    ResearchPanel r;
    EditDialog dialog;

    public DrawDeskScreen(DrawDeskContainer cx) {
        super();
        this.cx = cx;
        p = new DrawDeskPanel(this);
        p.setEnabled(true);

    }

    public DrawingDeskTileEntity getTile() {
        return cx.tile;
    }

    public void showTechTree() {
        if (r == null) {
            r = new ResearchPanel(this) {
                @Override
                public void onDisabled() {
                    hideTechTree();
                }
            };
            r.setPos(0, 0);
        }
        r.setEnabled(true);
        p.setEnabled(false);
        cx.setEnabled(false);
        this.refreshWidgets();

    }

    public void hideTechTree() {
        p.setEnabled(true);
        r.setEnabled(false);
        cx.setEnabled(true);
        this.refreshWidgets();
    }

    public void openDialog(EditDialog dialog, boolean refresh) {
        this.dialog = dialog;
        r.setEnabled(false);
        if (refresh)
            this.refreshWidgets();
    }

    public void closeDialog(boolean refresh) {
        this.dialog = null;
        r.setEnabled(true);
        if (refresh)
            this.refreshWidgets();
    }

    @Override
    public void addWidgets() {
        if (p != null && p.isEnabled())
            add(p);
        if (r != null && r.isEnabled())
            add(r);
        if (getDialog() != null)
            add(getDialog());
    }

    @Override
    public boolean onInit() {
        int sw = 387;
        int sh = 203;
        this.setSize(sw, sh);
        return super.onInit();
    }

    @Override
    public void drawBackground(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
    }

    public EditDialog getDialog() {
        return dialog;
    }

}
