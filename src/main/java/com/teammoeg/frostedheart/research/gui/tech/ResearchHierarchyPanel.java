package com.teammoeg.frostedheart.research.gui.tech;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import com.teammoeg.frostedheart.research.FHResearch;
import com.teammoeg.frostedheart.research.Research;
import com.teammoeg.frostedheart.research.ResearchEditorDialog;
import com.teammoeg.frostedheart.research.gui.TechIcons;
import com.teammoeg.frostedheart.research.gui.TechTextButton;
import com.teammoeg.frostedheart.research.gui.ThickLine;
import com.teammoeg.frostedheart.research.gui.editor.EditUtils;

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

	public static class MoreResearchHierarchyLine extends ThickLine {
		List<Research> r;

		public MoreResearchHierarchyLine(List<Research> r) {
			this.r = r;
		}

		public boolean doShow() {
			return r.stream().allMatch(Research::isCompleted);
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

	public ResearchPanel researchPanel;

	public ResearchHierarchyPanel(ResearchPanel panel) {
		super(panel);
		this.setOnlyInteractWithWidgetsInside(true);
		this.setOnlyRenderWidgetsInside(true);
		researchPanel = panel;
	}

	private static int[] ButtonPos = new int[] { 76, 44, 108, 12, 140 };
	List<ThickLine> lines = new ArrayList<>();

	@Override
	public void addWidgets() {
		if (FHResearch.editor) {
			int offset = 5;
			if (researchPanel.selectedResearch != null) {
				Button par = new TechTextButton(this, GuiUtils.str("parents"), Icon.EMPTY) {
					@Override
					public void onClicked(MouseButton mouseButton) {
						// TODO Add parent
						Research r = researchPanel.selectedResearch;
						ResearchEditorDialog.RESEARCH_LIST.open(this, "Edit parents", r.getParents(), s -> {
							r.setParents(s.stream().map(Research::getSupplier).collect(Collectors.toList()));
							r.doIndex();
							EditUtils.saveResearch(r);
						});
					}
				};
				par.setPos(offset, 130);
				add(par);
				offset += par.width + 3;
				Button chd = new TechTextButton(this, GuiUtils.str("children"), Icon.EMPTY) {
					@Override
					public void onClicked(MouseButton mouseButton) {
						// TODO Add children
						Research r = researchPanel.selectedResearch;
						ResearchEditorDialog.RESEARCH_LIST.open(this, "Edit children", r.getChildren(), s -> {
							r.getChildren().forEach(e ->{ e.removeParent(r);EditUtils.saveResearch(e);});
							s.forEach(e -> {
								e.addParent(r.getSupplier());
								e.doIndex();
							});
							r.doIndex();
							EditUtils.saveResearch(r);
						});
					}
				};
				chd.setPos(offset, 130);
				add(chd);
				offset += chd.width + 3;
			}
			{
				Button create = new TechTextButton(this, GuiUtils.str("new"), Icon.EMPTY) {
					@Override
					public void onClicked(MouseButton mouseButton) {
						// TODO Add research
						new ResearchEditorDialog(this, null, researchPanel.selectedCategory).open();
					}
				};
				create.setPos(offset, 130);
				add(create);
				offset += create.width + 3;
			}
			if (researchPanel.selectedResearch != null) {
				Button create = new TechTextButton(this, GuiUtils.str("edit"), Icon.EMPTY) {
					@Override
					public void onClicked(MouseButton mouseButton) {
						EditUtils.editResearch(this, researchPanel.selectedResearch);
					}
				};
				create.setPos(offset, 130);
				add(create);
				offset += create.width + 3;
				Button rem = new TechTextButton(this, GuiUtils.str("delete"), Icon.EMPTY) {
					@Override
					public void onClicked(MouseButton mouseButton) {
						researchPanel.selectedResearch.delete();
					}
				};
				rem.setPos(offset, 130);
				add(rem);
				offset += rem.width + 3;
			}
		}
		if (researchPanel.selectedResearch == null)
			return;
		ResearchDetailButton button = new ResearchDetailButton(this, researchPanel.selectedResearch);
		add(button);
		button.setPos(70, 48);
		int k = 0;
		int trmost=0;
		Set<Research> parents = researchPanel.selectedResearch.getParents();
		for (Research parent : parents) {
			int x;
			if (k >= 4) {
				x = ButtonPos[4] + (k - 4) * 32;
			} else
				x = ButtonPos[k];
			ResearchSimpleButton parentButton = new ResearchSimpleButton(this, parent);
			add(parentButton);
			parentButton.setPos(x, 16);
			ThickLine l = new ResearchHierarchyLine(parent);
			lines.add(l);

			l.setPosAndDelta(x + 12, 34, 0, 8);
			k++;
		}

		if (k > 1) {
			int lmost = 0;
			int rmost = 0;
			
			ThickLine lu = new ResearchCombinatorLine(researchPanel.selectedResearch);
			lines.add(lu);
			if (k > 4) {
				rmost = ButtonPos[4] + (k - 5) * 32 + 12;
			} else if (k >= 3)
				rmost = ButtonPos[2] + 12;
			else
				rmost = ButtonPos[0] + 12;
			if (k >= 4)
				lmost = ButtonPos[3] + 12;
			else
				lmost = ButtonPos[1] + 12;
			trmost=rmost;
			lu.setPoints(lmost, 42, rmost, 42);

		}
		if (k > 0) {
			ThickLine lux = new ResearchCombinatorLine(researchPanel.selectedResearch);
			lines.add(lux);
			lux.setPosAndDelta(ButtonPos[0] + 12, 42, 0, 6);
		}
		k = 0;

		if (FHResearch.editor || researchPanel.selectedResearch.isUnlocked()) {

			Set<Research> children = researchPanel.selectedResearch.getChildren();
			for (Research child : children) {
				int x;
				if (k >= 4) {
					x = ButtonPos[4] + (k - 4) * 32;
				} else
					x = ButtonPos[k];
				ResearchSimpleButton childButton = new ResearchSimpleButton(this, child);
				childButton.setChildren(researchPanel.selectedResearch);
				add(childButton);
				childButton.setPos(x, 92);
				ThickLine l = new ResearchHierarchyLine(researchPanel.selectedResearch);
				lines.add(l);
				l.setPosAndDelta(x + 12, 90, 0, 24);
				k++;
			}
			if (k > 1) {
				int lmost = 0;
				int rmost = 0;
				ThickLine lu = new ResearchHierarchyLine(researchPanel.selectedResearch);
				lines.add(lu);
				if (k > 4) {
					rmost = ButtonPos[4] + (k - 5) * 32 + 12;
				} else if (k >= 3)
					rmost = ButtonPos[2] + 12;
				else
					rmost = ButtonPos[0] + 12;
				if (k >= 4)
					lmost = ButtonPos[3] + 12;
				else
					lmost = ButtonPos[1] + 12;
				trmost=Math.max(rmost, trmost);
				lu.setPoints(lmost, 90, rmost, 90);
			}
			if (k > 0) {
				ThickLine lux2 = new ResearchHierarchyLine(researchPanel.selectedResearch);
				lines.add(lux2);
				lux2.setPosAndDelta(ButtonPos[0] + 12, 66, 0, 24);
			}
		}
		researchPanel.hierarchyBar.setMaxValue(trmost+24);

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
			this.researchScreen = panel.researchPanel;
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
			this.researchScreen = panel.researchPanel;
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
		return researchPanel.canEnable(this);
	}

	@Override
	public void clearWidgets() {
		super.clearWidgets();
		lines.clear();
	}
}
