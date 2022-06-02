package com.teammoeg.frostedheart.research.gui;

import java.util.List;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import com.teammoeg.frostedheart.research.Research;
import com.teammoeg.frostedheart.research.ResearchData;
import com.teammoeg.frostedheart.research.gui.FHIcons.FHIcon;

import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.ItemIcon;
import dev.ftb.mods.ftblibrary.ui.*;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;

public class ResearchDetailPanel extends Panel {
	Research research;
	FHIcon icon;
	ResearchDashboardPanel dashboardPanel;
	ResearchInfoPanel infoPanel;
	DescPanel descPanel;
	ResearchScreen researchScreen;

	public PanelScrollBar scrollInfo;

	public ResearchDetailPanel(ResearchScreen panel) {
		super(panel);
		this.setOnlyInteractWithWidgetsInside(true);
		this.setOnlyRenderWidgetsInside(true);
		descPanel = new DescPanel(this);
		infoPanel = new ResearchInfoPanel(this);
		dashboardPanel = new ResearchDashboardPanel(this);
		researchScreen = panel;
	}

	@Override
	public void addWidgets() {
		if (research == null)
			return;
		icon = research.getIcon();

		add(dashboardPanel);
		dashboardPanel.setPosAndSize(4, 11, 140, 51);

		add(descPanel);
		descPanel.setPosAndSize(8, 64, 132, 100);

		add(infoPanel);
		infoPanel.setPosAndSize(150, 15, 135, 151);
		Button closePanel = new Button(this) {
			@Override
			public void onClicked(MouseButton mouseButton) {
				close();
			}

			@Override
			public void drawBackground(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
			}
		};
		closePanel.setPosAndSize(284, 7, 9, 8);
		add(closePanel);
		scrollInfo = new TechScrollBar(this, infoPanel);
		scrollInfo.setPosAndSize(285, 18, 8, 146);
		add(scrollInfo);
		// already committed items
		ResearchData rd = research.getData();
		TextField status = new TextField(this);
		status.setMaxWidth(135);
		if (research.getData().isInProgress()) {
			status.setText(GuiUtils.translateGui("research.in_progress").mergeStyle(TextFormatting.BOLD)
					.mergeStyle(TextFormatting.BLUE));
		} else if (rd.canResearch()) {
			status.setText(GuiUtils.translateGui("research.can_research").mergeStyle(TextFormatting.BOLD)
					.mergeStyle(TextFormatting.GREEN));

		}
		status.setPos(0, 6);
		add(status);
	}

	@Override
	public void alignWidgets() {
	}

	@Override
	public void draw(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
		if (research == null) {
			return;
		}
		matrixStack.push();
		matrixStack.translate(0, 0, 500);
		super.draw(matrixStack, theme, x, y, w, h);
		matrixStack.pop();
	}

	public void open(Research r) {
		this.research = r;
		this.refreshWidgets();
		researchScreen.setModal(this);
		;
		researchScreen.refreshWidgets();

	}

	public void close() {
		this.research = null;
		this.refreshWidgets();
		researchScreen.closeModal(this);
		researchScreen.refreshWidgets();
	}

	@Override
	public void addMouseOverText(TooltipList list) {
		list.zOffset = 950;
		list.zOffsetItemTooltip = 500;
		super.addMouseOverText(list);
	}

	@Override
	public void drawBackground(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
		// drawBackground(matrixStack, theme, x, y, w, h);
		// theme.drawGui(matrixStack, x, y, w, h,WidgetType.NORMAL);
		DrawDeskIcons.DIALOG.draw(matrixStack, x, y, w, h);
	}

	public static class DescPanel extends Panel {
		ResearchDetailPanel detailPanel;

		public DescPanel(ResearchDetailPanel panel) {
			super(panel);
			this.setOnlyInteractWithWidgetsInside(true);
			this.setOnlyRenderWidgetsInside(true);
			detailPanel = panel;
		}

		@Override
		public void addWidgets() {
			List<ITextComponent> itxs=detailPanel.research.getDesc();
			int offset=0;
			for(ITextComponent itx:itxs) {
				TextField desc = new TextField(this);
				add(desc);
				desc.setMaxWidth(width);
				desc.setPosAndSize(0,offset, width, height);
				desc.setText(itx);
				desc.setColor(DrawDeskIcons.text);
				offset+=desc.height;
			}
			this.setHeight(offset);
		}

		@Override
		public void alignWidgets() {

		}
	}

	@Override
	public boolean isEnabled() {
		return researchScreen.canEnable(this) && research != null;
	}
}
