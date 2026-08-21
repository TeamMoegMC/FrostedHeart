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

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
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
	private final Map<AbstractWidget, WidgetState> hiddenExternalWidgets = new IdentityHashMap<>();
	private final List<Object> hiddenFtbSidebarGroups = new ArrayList<>();
	@Nullable
	private List<Object> ftbSidebarGroups;
	private boolean ftbSidebarResolved;
	private boolean ftbSidebarHidden;
	private int lastArchiveWidth = -1;
	private int lastArchiveHeight = -1;

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
		if (lastArchiveWidth != width || lastArchiveHeight != height) {
			archive.resizeArchive(width, height);
			lastArchiveWidth = width;
			lastArchiveHeight = height;
		}
	}

	@Override
	public void tick() {
		if (workspaceState.surface() == ResearchWorkspaceState.Surface.RESEARCH_ARCHIVE) {
			resizeArchiveToWindow();
		}
		super.tick();
		if (workspaceState.surface() == ResearchWorkspaceState.Surface.RESEARCH_ARCHIVE) {
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
		hideFtbSidebar();
	}

	private void restoreExternalWidgets() {
		hiddenExternalWidgets.forEach((widget, original) -> {
			widget.visible = original.visible();
			widget.active = original.active();
		});
		hiddenExternalWidgets.clear();
		restoreFtbSidebar();
	}

	private void hideFtbSidebar() {
		List<Object> groups = ftbSidebarGroups();
		if (groups == null) {
			return;
		}
		if (!groups.isEmpty()) {
			hiddenFtbSidebarGroups.clear();
			hiddenFtbSidebarGroups.addAll(groups);
			groups.clear();
		}
		ftbSidebarHidden = true;
	}

	private void restoreFtbSidebar() {
		if (!ftbSidebarHidden) {
			return;
		}
		List<Object> groups = ftbSidebarGroups();
		if (groups == null) {
			return;
		}
		if (groups.isEmpty()) {
			groups.addAll(hiddenFtbSidebarGroups);
		}
		hiddenFtbSidebarGroups.clear();
		ftbSidebarHidden = false;
	}

	@Nullable
	@SuppressWarnings("unchecked")
	private List<Object> ftbSidebarGroups() {
		if (ftbSidebarResolved) {
			return ftbSidebarGroups;
		}
		ftbSidebarResolved = true;
		try {
			Class<?> managerClass = Class.forName(
					"dev.ftb.mods.ftblibrary.sidebar.SidebarButtonManager",
					false,
					DrawDeskScreen.class.getClassLoader());
			Object manager = managerClass.getField("INSTANCE").get(null);
			Object groups = managerClass.getMethod("getGroups").invoke(manager);
			if (groups instanceof List<?> list) {
				ftbSidebarGroups = (List<Object>) list;
			}
		} catch (ReflectiveOperationException | LinkageError ignored) {
			// FTB Library is optional; an absent or incompatible sidebar is safe to ignore.
		}
		return ftbSidebarGroups;
	}

	@Override
	public void back() {
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
