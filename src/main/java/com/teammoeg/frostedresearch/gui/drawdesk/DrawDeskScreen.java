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
    private com.teammoeg.frostedresearch.knowledge.client.KnowledgeLayer knowledge;
    private boolean knowledgeOpen;
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
        if (knowledge != null) add(knowledge);
		if (archive != null) {
			add(archive);
		}
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
        knowledgeOpen = false;
		workspaceState.setSurface(ResearchWorkspaceState.Surface.RESEARCH_ARCHIVE);
		ensureArchive();
		applyWorkspaceSurface();
	}

	private void ensureArchive() {
		if (archive != null) {
			return;
		}
		archive = new ResearchArchiveLayer(
			this, openContext, workspaceState, navigation, this::applyWorkspaceSurface);
		refreshElements();
	}

    public void showKnowledge() {
        if (knowledge == null) {
            knowledge = new com.teammoeg.frostedresearch.knowledge.client.KnowledgeLayer(this, menu, () -> {
                knowledgeOpen = false;
                applyWorkspaceSurface();
            }, this::applyWorkspaceSurface);
            refreshElements();
        }
        knowledgeOpen = true;
        applyWorkspaceSurface();
        knowledge.opened();
    }

    public com.teammoeg.frostedresearch.knowledge.client.KnowledgeLayer getKnowledgeLayer() {
        return knowledgeOpen ? knowledge : null;
    }

    private void resizeKnowledgeToWindow() {
        int width = Math.min(660, Math.max(296, Minecraft.getInstance().getWindow().getGuiScaledWidth() - 24));
        int height = Math.min(420, Math.max(210, Minecraft.getInstance().getWindow().getGuiScaledHeight() - 24));
        if (knowledge.toolsOpen()) {
            width = Math.min(440, width);
            height = 210;
        }
        if (getWidth() != width || getHeight() != height) setSize(width, height);
        knowledge.setPos(0, 0);
        knowledge.resize(width, height);
    }
	private void applyWorkspaceSurface() {
        if (knowledge != null) {
            knowledge.setVisible(knowledgeOpen);
            knowledge.setEnabled(knowledgeOpen);
        }
        if (knowledgeOpen) {
            p.setVisible(false);
            p.setEnabled(false);
            if (archive != null) {
                archive.setVisible(false);
                archive.setEnabled(false);
            }
            menu.setSlotVisible(knowledge.toolsOpen());
            // Knowledge copying needs paper and the examine slot; ink remains a legacy research tool.
            if (knowledge.toolsOpen() && menu.slots.get(1) instanceof com.teammoeg.chorda.client.cui.menu.DeactivatableSlot slot)
                slot.setActived(false);
            resizeKnowledgeToWindow();
            hideExternalWidgets();
            return;
        }
		boolean showArchive = workspaceState.surface() == ResearchWorkspaceState.Surface.RESEARCH_ARCHIVE;
		if (archive != null) {
			archive.setVisible(showArchive);
			archive.setEnabled(showArchive);
		}
		p.setVisible(!showArchive);
		p.setEnabled(!showArchive);
		menu.setSlotVisible(!showArchive);
		if (showArchive) {
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
		if (archive == null) {
			return;
		}
		int width = Math.max(280, Minecraft.getInstance().getWindow().getGuiScaledWidth() - ARCHIVE_MARGIN * 2);
		int height = Math.max(188, Minecraft.getInstance().getWindow().getGuiScaledHeight() - ARCHIVE_MARGIN * 2);
		if (getWidth() != width || getHeight() != height) {
			setSize(width, height);
		}
		if (archive.getX() != 0 || archive.getY() != 0) {
			archive.setPos(0, 0);
		}
		archive.resizeArchive(width, height);
	}

	@Override
	public void tick() {
        if (knowledgeOpen) resizeKnowledgeToWindow();
        else if (workspaceState.surface() == ResearchWorkspaceState.Surface.RESEARCH_ARCHIVE) {
			resizeArchiveToWindow();
		}
		super.tick();
        if (knowledgeOpen || workspaceState.surface() == ResearchWorkspaceState.Surface.RESEARCH_ARCHIVE) {
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

    public static boolean isKnowledgeOpen() {
        return Minecraft.getInstance().screen instanceof CUIScreen cui && cui.getPrimaryLayer() instanceof DrawDeskScreen desk && desk.knowledgeOpen;
    }
	/** Safe query used by the optional FTB sidebar render hook. */
	public static boolean isResearchArchiveOpen() {
		Screen current = Minecraft.getInstance().screen;
		return current instanceof CUIScreen cui
				&& cui.getPrimaryLayer() instanceof DrawDeskScreen desk
				&& desk.workspaceState.surface() == ResearchWorkspaceState.Surface.RESEARCH_ARCHIVE;
	}

	@Override
	public void back() {
        if (knowledgeOpen) {
            if (!knowledge.goBack()) {
                knowledgeOpen = false;
                applyWorkspaceSurface();
            }
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
        if (knowledgeOpen && CInputHelper.isEsc(keyCode)) {
            back();
            return true;
        }
		if (CInputHelper.isEsc(keyCode)
				&& workspaceState.surface() == ResearchWorkspaceState.Surface.RESEARCH_ARCHIVE) {
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
