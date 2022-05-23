package com.teammoeg.frostedheart.research.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.research.Research;

import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.ItemIcon;
import dev.ftb.mods.ftblibrary.ui.*;
import dev.ftb.mods.ftblibrary.util.TooltipList;

public class ResearchDetailPanel extends Panel {
	Research research;
	Icon icon;
	ResearchDashboardPanel dashboardPanel;
	ResearchInfoPanel infoPanel;
	DescPanel descPanel;
	ResearchScreen researchScreen;

	public PanelScrollBar scrollInfo;

	public static final int PADDING = 10;

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
		if(research ==null)return;
		icon =ItemIcon.getItemIcon(research.getIcon());

		add(dashboardPanel);
		dashboardPanel.setPosAndSize(PADDING, PADDING, width/2-PADDING*2, PADDING+32);

		add(descPanel);
		descPanel.setPosAndSize(PADDING, PADDING*3+32, width/2-PADDING*2, height-PADDING*4-32);

		add(infoPanel);
		infoPanel.setPosAndSize(width/2, PADDING, width/2-PADDING, height-PADDING*2);

		scrollInfo = new PanelScrollBar(this, infoPanel);
		scrollInfo.setPosAndSize(width-PADDING-5, PADDING, PADDING, height-PADDING*2);
		add(scrollInfo);
	}

	@Override
	public void alignWidgets() {
	}

	@Override
	public void draw(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
		if(research ==null) {
			return;
		}
		matrixStack.push();
		matrixStack.translate(0, 0, 500);
		super.draw(matrixStack,theme, x, y, w, h);
		matrixStack.pop();
	}

	public void open(Research r) {
		this.research=r;
		this.refreshWidgets();
		researchScreen.setModal(this);;
		researchScreen.refreshWidgets();
		
	}
	public void close() {
		this.research=null;
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
		super.drawBackground(matrixStack, theme, x, y, w, h);
		theme.drawGui(matrixStack, x, y, w, h,WidgetType.NORMAL);
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
			TextField desc = new TextField(this);
			add(desc);
			desc.setMaxWidth(width-PADDING);
			desc.setPosAndSize(0, 0, width, height);
			desc.setText(detailPanel.research.getDesc());
		}

		@Override
		public void alignWidgets() {

		}
	}
	@Override
	public boolean isEnabled() {
		return researchScreen.canEnable(this)&&research!=null;
	}
}
