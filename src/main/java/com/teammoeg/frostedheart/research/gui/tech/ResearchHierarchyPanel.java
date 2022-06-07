package com.teammoeg.frostedheart.research.gui.tech;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import com.teammoeg.frostedheart.research.FHResearch;
import com.teammoeg.frostedheart.research.Research;
import com.teammoeg.frostedheart.research.gui.TechIcons;
import com.teammoeg.frostedheart.research.gui.TechTextButton;
import com.teammoeg.frostedheart.research.gui.ThickLine;

import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.ui.Button;
import dev.ftb.mods.ftblibrary.ui.GuiHelper;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import net.minecraft.util.text.TextFormatting;

public class ResearchHierarchyPanel extends Panel {
	public static class ResearchHierarchyLine extends ThickLine {
		Research r;

		public ResearchHierarchyLine(Research r) {
			this.r = r;
		}

		public boolean doShow() {
			return r.isCompleted();
		}

		@Override
		public void draw(MatrixStack matrixStack, int x, int y) {
			if (doShow())
				color = TechIcons.text;
			else
				color = Color4I.rgb(0xADA691);
			super.draw(matrixStack, x, y);
		}
	}

	public static class ResearchCombinatorLine extends ResearchHierarchyLine {

		public ResearchCombinatorLine(Research r) {
			super(r);
		}

		public boolean doShow() {
			return r.isUnlocked();
		}

	}

	public ResearchPanel researchScreen;

	public ResearchHierarchyPanel(ResearchPanel panel) {
		super(panel);
		this.setOnlyInteractWithWidgetsInside(true);
		this.setOnlyRenderWidgetsInside(true);
		researchScreen = panel;
	}

	private static int[] ButtonPos = new int[] { 76, 44, 108, 12, 140 };
	List<ThickLine> lines = new ArrayList<>();

	@Override
	public void addWidgets() {
		if (researchScreen.selectedResearch == null)
			return;
		ResearchDetailButton button = new ResearchDetailButton(this, researchScreen.selectedResearch);
		add(button);
		button.setPos(70, 48);
		int k = 0;
		Set<Research> parents = researchScreen.selectedResearch.getParents();
		for (Research parent : parents) {
			if (k >= 4)
				break;
			ResearchSimpleButton parentButton = new ResearchSimpleButton(this, parent);
			add(parentButton);
			parentButton.setPos(ButtonPos[k], 16);
			ThickLine l = new ResearchHierarchyLine(parent);
			lines.add(l);

			l.setPosAndDelta(ButtonPos[k] + 12, 34, 0, 8);
			k++;
		}

		if (k > 1) {
			int lmost = 0;
			int rmost = 0;
			ThickLine lu = new ResearchCombinatorLine(researchScreen.selectedResearch);
			lines.add(lu);
			if (k == 5)
				rmost = ButtonPos[5] + 12;
			else if (k >= 3)
				rmost = ButtonPos[2] + 12;
			else
				rmost = ButtonPos[0] + 12;
			if (k >= 4)
				lmost = ButtonPos[3] + 12;
			else
				lmost = ButtonPos[1] + 12;
			lu.setPoints(lmost, 42, rmost, 42);

		}
		if (k > 0) {
			ThickLine lux = new ResearchCombinatorLine(researchScreen.selectedResearch);
			lines.add(lux);
			lux.setPosAndDelta(ButtonPos[0] + 12, 42, 0, 6);
		}
		k = 0;

		if (FHResearch.editor || researchScreen.selectedResearch.isUnlocked()) {

			Set<Research> children = researchScreen.selectedResearch.getChildren();
			for (Research child : children) {
				if (k >= 4)
					break;
				ResearchSimpleButton childButton = new ResearchSimpleButton(this, child);
				childButton.setChildren(researchScreen.selectedResearch);
				add(childButton);
				childButton.setPos(ButtonPos[k], 92);
				ThickLine l = new ResearchHierarchyLine(researchScreen.selectedResearch);
				lines.add(l);
				l.setPosAndDelta(ButtonPos[k] + 12, 90, 0, 24);
				k++;
			}
			if (k > 1) {
				int lmost = 0;
				int rmost = 0;
				ThickLine lu = new ResearchHierarchyLine(researchScreen.selectedResearch);
				lines.add(lu);
				if (k == 5)
					rmost = ButtonPos[5] + 12;
				else if (k >= 3)
					rmost = ButtonPos[2] + 12;
				else
					rmost = ButtonPos[0] + 12;
				if (k >= 4)
					lmost = ButtonPos[3] + 12;
				else
					lmost = ButtonPos[1] + 12;
				lu.setPoints(lmost, 90, rmost, 90);
			}
			if (k > 0) {
				ThickLine lux2 = new ResearchHierarchyLine(researchScreen.selectedResearch);
				lines.add(lux2);
				lux2.setPosAndDelta(ButtonPos[0] + 12, 66, 0, 24);
			}
		}
		if (FHResearch.editor) {
			int offset=5;
			Button par = new TechTextButton(this, GuiUtils.str("add parent"), Icon.EMPTY) {
				@Override
				public void onClicked(MouseButton mouseButton) {
					// TODO Add parent
				}
			};
			par.setPos(offset,130);
			add(par);
			offset += par.width + 3;
			Button chd = new TechTextButton(this, GuiUtils.str("add children"), Icon.EMPTY) {
				@Override
				public void onClicked(MouseButton mouseButton) {
					// TODO Add children
				}
			};
			chd.setPos(offset,130);
			add(chd);
			offset += chd.width + 3;

			Button create = new TechTextButton(this, GuiUtils.str("new"), Icon.EMPTY) {
				@Override
				public void onClicked(MouseButton mouseButton) {
					// TODO Add research
				}
			};
			create.setPos(offset, 130);
			add(create);
			offset += create.width + 3;

		}
	}

	@Override
	public void alignWidgets() {

	}

	@Override
	public void drawBackground(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
		// theme.drawPanelBackground(matrixStack, x, y, w, h);
		for (ThickLine l : lines)
			l.draw(matrixStack, x, y);
	}

	@Override
	public void draw(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
		super.draw(matrixStack, theme, x, y, w, h);
		theme.drawString(matrixStack, GuiUtils.translateGui("research_hierarchy"), x + 3, y + 3, TechIcons.text, 0);
		TechIcons.HLINE_L.draw(matrixStack, x + 1, y + 13, 80, 3);
	}

	public static class ResearchDetailButton extends Button {

		ResearchPanel researchScreen;
		Research research;

		public ResearchDetailButton(ResearchHierarchyPanel panel, Research research) {
			super(panel, research.getName(), research.getIcon());
			this.research = research;
			this.researchScreen = panel.researchScreen;
			setSize(36, 36);
		}

		@Override
		public void onClicked(MouseButton mouseButton) {
			this.researchScreen.detailframe.open(research);
			// this.researchScreen.refreshWidgets();
		}

		@Override
		public void addMouseOverText(TooltipList list) {
			list.add(research.getName().mergeStyle(TextFormatting.BOLD));
		}

		@Override
		public void draw(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
			// this.drawBackground(matrixStack, theme, x, y, w, h);
			GuiHelper.setupDrawing();
			TechIcons.LSLOT.draw(matrixStack, x, y, w, h);
			this.drawIcon(matrixStack, theme, x + 2, y + 2, 32, 32);
			if (research.isCompleted()) {
				matrixStack.push();
				matrixStack.translate(0, 0, 300);
				GuiHelper.setupDrawing();
				TechIcons.FIN.draw(matrixStack, x + 2, y + 2, 32, 32);
				matrixStack.pop();
			}
		}
	}

	public static class ResearchSimpleButton extends Button {

		ResearchPanel researchScreen;
		Research research;
		Research parent;

		public ResearchSimpleButton(ResearchHierarchyPanel panel, Research research) {
			super(panel, research.getName(), research.getIcon());
			this.research = research;
			this.researchScreen = panel.researchScreen;
			setSize(24, 24);
		}

		public void setChildren(Research p) {
			parent = p;
		}

		@Override
		public void onClicked(MouseButton mouseButton) {
			researchScreen.selectResearch(research);
		}

		@Override
		public void addMouseOverText(TooltipList list) {
			list.add(research.getName().mergeStyle(TextFormatting.BOLD));
			if ((parent == null && !research.isUnlocked()) || (parent != null && !parent.isUnlocked())) {
				list.add(GuiUtils.translateTooltip("research_is_locked").mergeStyle(TextFormatting.RED));
				for (Research parent : research.getParents()) {
					if (!parent.isCompleted()) {
						list.add(parent.getName().mergeStyle(TextFormatting.GRAY));
					}
				}
			}
		}

		@Override
		public void draw(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
			GuiHelper.setupDrawing();
			TechIcons.SLOT.draw(matrixStack, x, y, w, h);
			if (FHResearch.editor || (parent == null && research.isUnlocked())
					|| (parent != null && parent.isUnlocked())) {
				this.drawIcon(matrixStack, theme, x + 4, y + 4, 16, 16);
				if (research.isCompleted()) {
					matrixStack.push();
					matrixStack.translate(0, 0, 300);
					GuiHelper.setupDrawing();
					TechIcons.FIN.draw(matrixStack, x + 4, y + 4, 16, 16);
					matrixStack.pop();
				}
			} else
				TechIcons.Question.draw(matrixStack, x + 4, y + 4, 16, 16);
		}
	}

	@Override
	public boolean isEnabled() {
		return researchScreen.canEnable(this);
	}

	@Override
	public void clearWidgets() {
		super.clearWidgets();
		lines.clear();
	}
}
