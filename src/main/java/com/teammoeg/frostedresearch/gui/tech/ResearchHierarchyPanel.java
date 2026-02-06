/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedresearch.gui.tech;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.teammoeg.chorda.client.cui.Button;
import com.teammoeg.chorda.client.cui.MouseButton;
import com.teammoeg.chorda.client.cui.TooltipBuilder;
import com.teammoeg.chorda.client.cui.UILayer;
import com.teammoeg.chorda.client.cui.editor.EditUtils;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.chorda.client.ui.CGuiHelper;
import com.teammoeg.chorda.text.Components;
import com.teammoeg.frostedresearch.FHResearch;
import com.teammoeg.frostedresearch.Lang;
import com.teammoeg.frostedresearch.gui.DrawDeskTheme;
import com.teammoeg.frostedresearch.gui.ResearchEditUtils;
import com.teammoeg.frostedresearch.gui.TechIcons;
import com.teammoeg.frostedresearch.gui.TechTextButton;
import com.teammoeg.frostedresearch.gui.ThickLine;
import com.teammoeg.frostedresearch.research.Research;
import com.teammoeg.frostedresearch.research.ResearchEditors;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;

public class ResearchHierarchyPanel extends UILayer {
	private static int[] ButtonPos = new int[] { 76, 44, 108, 12, 140 };
	public ResearchLayer researchPanel;
	List<ThickLine> lines = new ArrayList<>();

	public ResearchHierarchyPanel(ResearchLayer panel) {
		super(panel);
		researchPanel = panel;
	}

	@Override
	public void addUIElements() {
		if (FHResearch.editor) {
			int offset = 5;
			if (researchPanel.selectedResearch != null) {
				TechTextButton par = new TechTextButton(this, Components.str("parents"), CIcons.nop()) {
					@Override
					public void onClicked(MouseButton mouseButton) {
						// TODO Add parent
						Research r = researchPanel.selectedResearch;
						ResearchEditors.RESEARCH_LIST.open(EditUtils.openEditorScreen(), Components.str("Edit parents"), r.getParents(), s -> {
							try {
								// System.out.println(s);
								r.setParents(s.stream().map(Research::getId).collect(Collectors.toList()));
								FHResearch.reindex();
								ResearchEditUtils.saveResearch(r);
							} catch (Throwable t) {
								t.printStackTrace();
							}
						});
					}
				};
				par.setPos(offset, 130);
				add(par);
				offset += par.getWidth() + 3;
				TechTextButton chd = new TechTextButton(this, Components.str("children"), CIcons.nop()) {
					@Override
					public void onClicked(MouseButton mouseButton) {
						// TODO Add children
						Research r = researchPanel.selectedResearch;
						ResearchEditors.RESEARCH_LIST.open(EditUtils.openEditorScreen(), Components.str("Edit children"), r.getChildren(), s -> {
							r.getChildren().forEach(e -> {
								if(!s.remove(e)) {
									e.removeParent(r);
									ResearchEditUtils.saveResearch(e);
								}
							});
							s.forEach(e ->{
								e.addParent(r);
								ResearchEditUtils.saveResearch(e);
							});
							FHResearch.reindex();
							ResearchEditUtils.saveResearch(r);
						});
					}
				};
				chd.setPos(offset, 130);
				add(chd);
				offset += chd.getWidth() + 3;
			}
			{
				TechTextButton create = new TechTextButton(this, Components.str("new"), CIcons.nop()) {
					@Override
					public void onClicked(MouseButton mouseButton) {
						ResearchEditUtils.editResearch(this, null, researchPanel.selectedCategory);
					}
				};
				create.setPos(offset, 130);
				add(create);
				offset += create.getWidth() + 3;
			}
			if (researchPanel.selectedResearch != null) {
				TechTextButton create = new TechTextButton(this, Components.str("edit"), CIcons.nop()) {
					@Override
					public void onClicked(MouseButton mouseButton) {
						ResearchEditUtils.editResearch(this, researchPanel.selectedResearch);
					}
				};
				create.setPos(offset, 130);
				add(create);
				offset += create.getWidth() + 3;
				TechTextButton rem = new TechTextButton(this, Components.str("delete"), CIcons.nop()) {
					@Override
					public void onClicked(MouseButton mouseButton) {
						researchPanel.selectedResearch.delete();
						researchPanel.refresh();
					}
				};
				rem.setPos(offset, 130);
				add(rem);
				offset += rem.getWidth() + 3;
			}
		}
		if (researchPanel.selectedResearch == null)
			return;
		ResearchDetailButton button = new ResearchDetailButton(this, researchPanel.selectedResearch);
		add(button);
		button.setPos(70, 48);
		int k = 0;
		int trmost = 0;
		boolean haveHScroll = false;
		Set<Research> parents = researchPanel.selectedResearch.getParents();
		for (Research parent : parents) {
			int x;
			if (k >= 4) {
				x = ButtonPos[4] + (k - 4) * 32;
			} else
				x = ButtonPos[k];
			// System.out.println(parent);
			ResearchSimpleButton parentButton = new ResearchSimpleButton(this, parent);
			add(parentButton);
			parentButton.setPos(x, 16);
			ThickLine l = new ResearchHierarchyLine(parent);
			lines.add(l);

			l.setPosAndDelta(x + 12, 30, 0, 12);
			k++;
		}
		if (k > 6)
			haveHScroll = true;
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
			trmost = rmost;
			lu.setPoints(lmost, 45, rmost, 45);

		}
		if (k > 0) {
			ThickLine lux = new ResearchCombinatorLine(researchPanel.selectedResearch);
			lines.add(lux);
			lux.setPosAndDelta(ButtonPos[0] + 12, 42, 0, 24);
		}
		k = 0;

		// if (FHResearch.editor || researchPanel.selectedResearch.isUnlocked()) {
		boolean crunlocked = researchPanel.selectedResearch.isUnlocked();
		Collection<Research> children = researchPanel.selectedResearch.getChildren();
		for (Research child : children) {
			if (!crunlocked && !child.isShowable())
				continue;
			int x;
			if (k >= 4) {
				x = ButtonPos[4] + (k - 4) * 32;
			} else
				x = ButtonPos[k];
			ResearchSimpleButton childButton = new ResearchSimpleButton(this, child);
			childButton.setChildren(researchPanel.selectedResearch);
			add(childButton);
			childButton.setPos(x, 95);
			ThickLine l = new ResearchHierarchyLine(researchPanel.selectedResearch);
			lines.add(l);
			l.setPosAndDelta(x + 12, 89, 0, 22);
			k++;
		}
		if (k > 6)
			haveHScroll = true;
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
			trmost = Math.max(rmost, trmost);
			lu.setPoints(lmost, 89, rmost, 89);
		}
		if (k > 0) {
			ThickLine lux2 = new ResearchHierarchyLine(researchPanel.selectedResearch);
			lines.add(lux2);
			lux2.setPosAndDelta(ButtonPos[0] + 12, 66, 0, 24);
		}
		// }
		if (haveHScroll) {
			researchPanel.hierarchyBar.unhide();
			// researchPanel.hierarchyBar.setMaxValue(trmost + 24);
		} else
			researchPanel.hierarchyBar.hide();

	}

	@Override
	public void alignWidgets() {

	}

	@Override
	public void clearElement() {
		super.clearElement();
		lines.clear();
	}

	@Override
	public void render(GuiGraphics matrixStack, int x, int y, int w, int h) {
		super.render(matrixStack, x, y, w, h);
		matrixStack.drawString(getFont(), Lang.translateGui("research_hierarchy"), x + 3, y + 3, DrawDeskTheme.getTextColor(), false);
		TechIcons.HLINE_L.draw(matrixStack, x + 1, y + 13, 80, 3);
	}

	@Override
	public void drawBackground(GuiGraphics matrixStack, int x, int y, int w, int h) {
		CGuiHelper.resetGuiDrawing();
		for (ThickLine l : lines)
			l.draw(matrixStack, x, y);
	}

	@Override
	public boolean isEnabled() {
		return researchPanel.canEnable(this);
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
		public void draw(GuiGraphics matrixStack, int x, int y) {
			if (doShow())
				color = DrawDeskTheme.getTextColor();
			else
				color = DrawDeskTheme.getWeakColor();
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

	public static class ResearchDetailButton extends Button {

		ResearchLayer researchScreen;
		Research research;

		public ResearchDetailButton(ResearchHierarchyPanel panel, Research research) {
			super(panel, research.getName(), research.getIcon());
			this.research = research;
			this.researchScreen = panel.researchPanel;
			setSize(36, 36);
		}

		@Override
		public void getTooltip(TooltipBuilder list) {
			list.add(research.getName().copy().withStyle(ChatFormatting.BOLD));
			if (!research.isUnlocked()) {
				list.add(Lang.translateTooltip("research_is_locked").withStyle(ChatFormatting.RED));
				for (Research parent : research.getParents()) {
					if (!parent.isCompleted()) {
						list.add(parent.getName().copy().withStyle(ChatFormatting.GRAY));
					}
				}
			}
		}

		@Override
		public void render(GuiGraphics matrixStack, int x, int y, int w, int h) {
			// this.drawBackground(matrixStack, theme, x, y, w, h);
			CGuiHelper.resetGuiDrawing();
			TechIcons.LSLOT.draw(matrixStack, x, y, w, h);
			if (FHResearch.editor || research.isShowable()) {
				this.drawIcon(matrixStack, x + 2, y + 2, 32, 32);
				if (research.isCompleted()) {
					matrixStack.pose().pushPose();
					matrixStack.pose().translate(0, 0, 300);
					CGuiHelper.resetGuiDrawing();
					TechIcons.FIN.draw(matrixStack, x + 2, y + 2, 32, 32);
					matrixStack.pose().popPose();
				}
			} else
				TechIcons.Question.draw(matrixStack, x + 2, y + 2, 32, 32);
		}

		@Override
		public void onClicked(MouseButton mouseButton) {
			if ((research.isShowable() && !research.isHidden()) || FHResearch.isEditor())
				this.researchScreen.detailframe.open(research);
			// this.researchScreen.refreshWidgets();
		}
	}

	public static class ResearchHierarchyLine extends ThickLine {
		Research r;

		public ResearchHierarchyLine(Research r) {
			this.r = r;
		}

		public boolean doShow() {
			return r.isCompleted();
		}

		@Override
		public void draw(GuiGraphics matrixStack, int x, int y) {
			if (doShow())
				color = DrawDeskTheme.getTextColor();
			else
				color = DrawDeskTheme.getWeakColor();
			super.draw(matrixStack, x, y);
		}
	}

	public static class ResearchSimpleButton extends Button {

		ResearchLayer researchScreen;
		Research research;
		Research parent;

		public ResearchSimpleButton(ResearchHierarchyPanel panel, Research research) {
			super(panel, research.getName(), research.getIcon());
			this.research = research;
			this.researchScreen = panel.researchPanel;
			setSize(24, 24);

		}

		@Override
		public void getTooltip(TooltipBuilder list) {
			list.add(research.getName().copy().withStyle(ChatFormatting.BOLD));
			if ((parent == null && !research.isUnlocked()) || (parent != null && !parent.isUnlocked())) {
				list.add(Lang.translateTooltip("research_is_locked").withStyle(ChatFormatting.RED));
				for (Research parent : research.getParents()) {
					if (!parent.isCompleted()) {
						list.add(parent.getName().copy().withStyle(ChatFormatting.GRAY));
					}
				}
			}
		}

		@Override
		public void render(GuiGraphics matrixStack, int x, int y, int w, int h) {
			CGuiHelper.resetGuiDrawing();
			DrawDeskTheme.drawSlot(matrixStack, x, y, w, h);
			if (FHResearch.editor || research.isShowable()) {
				this.drawIcon(matrixStack, x + 4, y + 4, 16, 16);
				if (research.isCompleted()) {
					matrixStack.pose().pushPose();
					matrixStack.pose().translate(0, 0, 300);
					CGuiHelper.resetGuiDrawing();
					TechIcons.FIN.draw(matrixStack, x + 4, y + 4, 16, 16);
					matrixStack.pose().popPose();
				}
			} else
				TechIcons.Question.draw(matrixStack, x + 4, y + 4, 16, 16);
		}

		@Override
		public void onClicked(MouseButton mouseButton) {
			researchScreen.selectResearch(research);
		}

		public void setChildren(Research p) {
			parent = p;
		}
	}
}
