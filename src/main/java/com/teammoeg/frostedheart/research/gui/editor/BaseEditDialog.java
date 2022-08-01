package com.teammoeg.frostedheart.research.gui.editor;

import com.mojang.blaze3d.matrix.MatrixStack;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.ui.WidgetLayout;
import dev.ftb.mods.ftblibrary.ui.WidgetType;

public abstract class BaseEditDialog extends EditDialog {

    public BaseEditDialog(Widget panel) {
        super(panel);
        setWidth(400);
    }

    @Override
    public void drawBackground(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
        theme.drawGui(matrixStack, x - 5, y - 5, w + 10, h + 10, WidgetType.NORMAL);
    }

    @Override
    public void alignWidgets() {
        this.setHeight(super.align(WidgetLayout.VERTICAL));
    }
}
