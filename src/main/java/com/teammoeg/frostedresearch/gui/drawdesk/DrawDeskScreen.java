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

package com.teammoeg.frostedresearch.gui.drawdesk;

import com.teammoeg.chorda.client.CInputHelper;
import com.teammoeg.chorda.client.cui.base.MenuPrimaryLayer;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.cui.screenadapter.CUIScreen;
import com.teammoeg.frostedresearch.blocks.DrawingDeskTileEntity;
import com.teammoeg.frostedresearch.data.ClientResearchData;
import com.teammoeg.frostedresearch.gui.DrawDeskTheme;
import com.teammoeg.frostedresearch.gui.ResearchGui;
import com.teammoeg.frostedresearch.gui.archive.ResearchArchiveLayer;
import com.teammoeg.frostedresearch.gui.archive.ResearchNavigationController;
import com.teammoeg.frostedresearch.gui.archive.ResearchOpenContext;
import com.teammoeg.frostedresearch.gui.archive.ResearchWorkspaceState;
import com.teammoeg.frostedresearch.gui.archive.StatefulResearchNavigationController;
import com.teammoeg.frostedresearch.gui.knowledge.KnowledgeLabLayer;

import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;

import java.util.IdentityHashMap;
import java.util.Map;

public class DrawDeskScreen extends MenuPrimaryLayer<DrawDeskContainer> implements ResearchGui {
	private static final int DRAWING_DESK_WIDTH = 387;
	private static final int DRAWING_DESK_HEIGHT = 203;
	private static final int ARCHIVE_MARGIN = 12;

	DrawDeskLayer p;
	private final ResearchOpenContext openContext;
	private final ResearchWorkspaceState workspaceState;
	private final ResearchNavigationController navigation;
	@Nullable
	private ResearchArchiveLayer archive;
	@Nullable
	private KnowledgeLabLayer knowledgeLab;
	private final Map<AbstractWidget, WidgetState> hiddenExternalWidgets = new IdentityHashMap<>();

	public DrawDeskScreen(DrawDeskContainer cx) {
		super(cx);
		this.setTheme(DrawDeskTheme.INSTANCE);
		p = new DrawDeskLayer(this);
		p.setVisible(true);
		openContext = ResearchOpenContext.drawingDesk(null);
		workspaceState = new ResearchWorkspaceState(openContext);
		workspaceState.selectResearch(ClientResearchData.last);
		navigation = new StatefulResearchNavigationController(
			openContext, workspaceState, () -> closeGui(true));
	}

	@Override
	public void addChildUIElements() {
		add(p);
		if (archive != null) {
			add(archive);
		}
		if (knowledgeLab != null) add(knowledgeLab);
	}

	@Override
	public void alignWidgets() {
	}

	public DrawingDeskTileEntity getTile() {
		return menu.getBlock();
	}

	public void hideTechTree() {
		workspaceState.setSurface(ResearchWorkspaceState.Surface.DRAWING_DESK);
		applyWorkspaceSurface();
	}

	@Override
	public boolean onInit() {
		this.setSize(DRAWING_DESK_WIDTH, DRAWING_DESK_HEIGHT);
		return super.onInit();
	}

	public void closeDialog(boolean refresh) {
		if (archive != null) {
			archive.setVisible(workspaceState.surface() == ResearchWorkspaceState.Surface.RESEARCH_ARCHIVE);
		}
		super.closeDialog(refresh);
	}

	public void openDialog(UIElement dialog, boolean refresh) {
		if (archive != null) {
			archive.setVisible(false);
		}
		super.openDialog(dialog, refresh);
	}

	public void showTechTree() {
		workspaceState.setSurface(ResearchWorkspaceState.Surface.RESEARCH_ARCHIVE);
		ensureArchive();
		applyWorkspaceSurface();
	}

	public void showKnowledgeLab() {
		workspaceState.setSurface(ResearchWorkspaceState.Surface.KNOWLEDGE_LAB);
		if (knowledgeLab == null) {
			knowledgeLab = new KnowledgeLabLayer(this, this, this::hideTechTree);
			refreshElements();
		}
		applyWorkspaceSurface();
	}

	/** Reveals a newly synchronized V2 card game and rebuilds its derived widgets. */
	public void showInspirationGame() {
		p.mgp.refresh();
		hideTechTree();
	}

	private void ensureArchive() {
		if (archive != null) {
			return;
		}
		archive = new ResearchArchiveLayer(
			this, openContext, workspaceState, navigation, this::applyWorkspaceSurface);
		refreshElements();
	}

	private void applyWorkspaceSurface() {
		boolean showArchive = workspaceState.surface() == ResearchWorkspaceState.Surface.RESEARCH_ARCHIVE;
		boolean showKnowledge = workspaceState.surface() == ResearchWorkspaceState.Surface.KNOWLEDGE_LAB;
		if (archive != null) {
			archive.setVisible(showArchive);
			archive.setEnabled(showArchive);
		}
		if (knowledgeLab != null) {
			knowledgeLab.setVisible(showKnowledge);
			knowledgeLab.setEnabled(showKnowledge);
		}
		p.setVisible(!showArchive && !showKnowledge);
		p.setEnabled(!showArchive && !showKnowledge);
		menu.setSlotVisible(!showArchive && !showKnowledge);
		if (showArchive || showKnowledge) {
			resizeArchiveToWindow();
			hideExternalWidgets();
		} else {
			restoreExternalWidgets();
			setSize(DRAWING_DESK_WIDTH, DRAWING_DESK_HEIGHT);
			p.setPosAndSize(0, 0, DRAWING_DESK_WIDTH, DRAWING_DESK_HEIGHT);
			p.focusTarget(workspaceState.consumeDrawDeskFocusTarget());
		}
	}

	private void resizeArchiveToWindow() {
		if (archive == null && knowledgeLab == null) {
			return;
		}
		int width = Math.max(280, Minecraft.getInstance().getWindow().getGuiScaledWidth() - ARCHIVE_MARGIN * 2);
		int height = Math.max(188, Minecraft.getInstance().getWindow().getGuiScaledHeight() - ARCHIVE_MARGIN * 2);
		if (getWidth() != width || getHeight() != height) {
			setSize(width, height);
		}
		if (archive != null && (archive.getX() != 0 || archive.getY() != 0)) {
			archive.setPos(0, 0);
		}
		if (archive != null) archive.resizeArchive(width, height);
		if (knowledgeLab != null) {
			knowledgeLab.setPos(0, 0);
			knowledgeLab.resizeLab(width, height);
		}
	}

	@Override
	public void tick() {
		if (workspaceState.surface() != ResearchWorkspaceState.Surface.DRAWING_DESK) {
			resizeArchiveToWindow();
		}
		super.tick();
		if (workspaceState.surface() != ResearchWorkspaceState.Surface.DRAWING_DESK) {
			hideExternalWidgets();
		}
	}

	@Override
	public void onClosed() {
		restoreExternalWidgets();
		super.onClosed();
	}

	private void hideExternalWidgets() {
		Screen screen = getScreen() == null ? null : getScreen().getScreen();
		if (screen == null) {
			return;
		}
		for (Object child : screen.children()) {
			if (child instanceof AbstractWidget widget) {
				hiddenExternalWidgets.computeIfAbsent(
						widget, ignored -> new WidgetState(widget.visible, widget.active));
				if (widget.visible) {
					widget.visible = false;
				}
				if (widget.active) {
					widget.active = false;
				}
			}
		}
	}

	private void restoreExternalWidgets() {
		hiddenExternalWidgets.forEach((widget, original) -> {
			widget.visible = original.visible();
			widget.active = original.active();
		});
		hiddenExternalWidgets.clear();
	}

	/** Safe query used by the optional FTB sidebar render hook. */
	public static boolean isResearchArchiveOpen() {
		Screen current = Minecraft.getInstance().screen;
		return current instanceof CUIScreen cui
				&& cui.getPrimaryLayer() instanceof DrawDeskScreen desk
				&& desk.workspaceState.surface() != ResearchWorkspaceState.Surface.DRAWING_DESK;
	}

	@Override
	public void back() {
		if (workspaceState.surface() == ResearchWorkspaceState.Surface.KNOWLEDGE_LAB) {
			hideTechTree();
			return;
		}
		if (navigation.back()) {
			applyWorkspaceSurface();
			return;
		}
		super.back();
	}

	@Override
	public boolean onKeyPressed(int keyCode, int scanCode, int modifier) {
		if (super.onKeyPressed(keyCode, scanCode, modifier)) {
			return true;
		}
		if (CInputHelper.isEsc(keyCode)
				&& workspaceState.surface() != ResearchWorkspaceState.Surface.DRAWING_DESK) {
			if (workspaceState.surface() == ResearchWorkspaceState.Surface.KNOWLEDGE_LAB) {
				hideTechTree();
				return true;
			}
			if (navigation.back()) {
				applyWorkspaceSurface();
				return true;
			}
		}
		return false;
	}

	@Override
	public void onResearchDefinitionsChanged() {
		if (archive != null) {
			archive.onResearchDefinitionsChanged();
		}
	}

	@Override
	public void onResearchDataReplaced() {
		if (archive != null) {
			archive.onResearchDataReplaced();
		}
	}

	@Override
	public void onResearchProgressChanged(String researchId) {
		if (archive != null) {
			archive.onResearchProgressChanged(researchId);
		}
	}

	@Override
	public void onActiveResearchChanged(@Nullable String researchId) {
		if (archive != null) {
			archive.onActiveResearchChanged(researchId);
		}
	}

	@Override
	public void onClueProgressChanged(String researchId, String clueNonce) {
		if (archive != null) {
			archive.onClueProgressChanged(researchId, clueNonce);
		}
	}

	private record WidgetState(boolean visible, boolean active) {
	}

}
