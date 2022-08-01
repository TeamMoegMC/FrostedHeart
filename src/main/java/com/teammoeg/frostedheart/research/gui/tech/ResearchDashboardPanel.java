package com.teammoeg.frostedheart.research.gui.tech;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import com.teammoeg.frostedheart.research.FHResearch;
import com.teammoeg.frostedheart.research.ResearchData;
import com.teammoeg.frostedheart.research.gui.RTextField;
import com.teammoeg.frostedheart.research.gui.TechIcons;
import com.teammoeg.frostedheart.research.gui.TechTextButton;
import com.teammoeg.frostedheart.research.gui.editor.EditUtils;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.ui.Button;
import dev.ftb.mods.ftblibrary.ui.GuiHelper;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;

import java.text.DecimalFormat;

public class ResearchDashboardPanel extends Panel {

    ResearchDetailPanel detailPanel;
    RTextField techpoint;

    public ResearchDashboardPanel(ResearchDetailPanel panel) {
        super(panel);
        this.setOnlyInteractWithWidgetsInside(true);
        this.setOnlyRenderWidgetsInside(true);
        detailPanel = panel;
        techpoint = new RTextField(this).setMaxWidth(100).setMaxLine(1).setColor(TechIcons.text);
        techpoint.setPos(40, 28);
    }

    @Override
    public void addWidgets() {
        // close panel button
       /* Button closePanel = new SimpleTextButton(this, new StringTextComponent("Close"), Icon.EMPTY) {
            @Override
            public void onClicked(MouseButton mouseButton) {
                detailPanel.close();
                //closeGui();
            }
        };
        closePanel.setPosAndSize(width-PADDING, 0, PADDING, PADDING);
        add(closePanel);*/
        RTextField tf = new RTextField(this);
        tf.setPos(0, 0);
        add(tf);
        tf.setMaxWidth(140).setMinWidth(140).setMaxLine(2).setColor(TechIcons.text).addFlags(4);
        tf.setText(detailPanel.research.getName());
        if (FHResearch.editor) {
            Button create = new TechTextButton(this, GuiUtils.str("edit"),
                    Icon.EMPTY) {
                @Override
                public void onClicked(MouseButton mouseButton) {
                    EditUtils.editResearch(this, detailPanel.research);
                }
            };
            create.setPos(40, 30);
            add(create);
        }
        techpoint.setText(toReadable(detailPanel.research.getRequiredPoints()) + "IOPS");
        add(techpoint);
    }

    final static String read = "kmgtpezyh";
    final static DecimalFormat df1 = new DecimalFormat("#.#");
    final static DecimalFormat df2 = new DecimalFormat("#.##");
    final static DecimalFormat df3 = new DecimalFormat("#.##");

    public static synchronized String toReadable(long num) {
        int unit = -1;
        double lnum = num;
        while (lnum > 1999) {
            unit++;
            lnum /= 1000;
        }
        if (unit < 0)
            return String.valueOf(num);
        if (lnum >= 1000) {
            return "" + ((long) lnum) + read.charAt(unit);
        } else if (lnum >= 100) {
            return df1.format(lnum) + read.charAt(unit);
        } else if (lnum >= 10) {
            return df2.format(lnum) + read.charAt(unit);
        } else {
            return df3.format(lnum) + read.charAt(unit);
        }
    }

    @Override
    public void alignWidgets() {

    }

    @Override
    public void draw(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
        ResearchData rd = detailPanel.research.getData();
        if (rd.canResearch()) {
            techpoint.setText(toReadable(rd.getTotalCommitted()) + "/" + toReadable(detailPanel.research.getRequiredPoints()) + "IOPS");
        }
        techpoint.setX(140 - techpoint.width);
        super.draw(matrixStack, theme, x, y, w, h);

        // name
        //theme.drawString(matrixStack, detailPanel.research.getName(), x+7, y+8);
        // icon
        TechIcons.SHADOW.draw(matrixStack, x + 1, y + 36, 36, 9);
        detailPanel.icon.draw(matrixStack, x + 3, y + 10, 32, 32);
        theme.drawString(matrixStack, GuiUtils.translateGui("reasearch.points"), x + 40, y + 19, TechIcons.text, 0);
        GuiHelper.setupDrawing();
        TechIcons.HLINE_L.draw(matrixStack, x, y + 49, 140, 3);

        // TODO: research progress
        // ResearchData data = ResearchDataAPI.getData((ServerPlayerEntity) detailPanel.researchScreen.player).getData(detailPanel.research);
        // theme.drawString(matrixStack, data.getProgress()*100 + "%", x+theme.getStringWidth(detailPanel.research.getName())+5, y);
    }
}
