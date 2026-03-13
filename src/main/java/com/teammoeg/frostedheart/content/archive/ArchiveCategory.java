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

package com.teammoeg.frostedheart.content.archive;

import com.teammoeg.chorda.client.AnimationUtil;
import com.teammoeg.chorda.client.CInputHelper;
import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.cui.base.MouseButton;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.cui.base.UILayer;
import com.teammoeg.chorda.client.cui.category.Category;
import com.teammoeg.chorda.client.cui.category.CategoryHelper;
import com.teammoeg.chorda.client.cui.category.Entry;
import com.teammoeg.chorda.client.cui.contentpanel.ContentPanel;
import com.teammoeg.chorda.client.cui.contentpanel.LineHelper;
import com.teammoeg.chorda.client.cui.widgets.Button;
import com.teammoeg.chorda.client.cui.widgets.LayerScrollBar;
import com.teammoeg.chorda.client.icon.FlatIcon;
import com.teammoeg.chorda.math.Colors;
import com.teammoeg.frostedheart.content.tips.Tip;
import com.teammoeg.frostedheart.content.tips.TipHelper;
import com.teammoeg.frostedheart.content.tips.TipManager;
import com.teammoeg.frostedheart.infrastructure.command.TipClientCommand;
import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ArchiveCategory extends UILayer {
	public final LayerScrollBar scrollBar;
	protected final ArchiveScreen panel;
	private final Category root = new Category(this, Component.literal("root"));

	public static String currentPath = "";

	protected ArchiveCategory(ArchiveScreen panel) {
		super(panel);
		this.panel = panel;
		this.scrollBar = new LayerScrollBar(parent, true, this) {
			@Override
			public void render(GuiGraphics graphics, int x, int y, int width, int height) {
			}
		};
		this.scrollBar.setScrollStep((Entry.DEF_HEIGHT + 2) * 2);
		this.root.clearElement();
		addUIElements();
		// 路径为空时打开第一个分类
		if (currentPath.isBlank())
			for (UIElement ele : getElements())
				if (ele instanceof Category c) {
					c.setOpened(true);
					return;
				}
		scrollTo(open(currentPath));
	}

	@Override
	public void drawBackground(GuiGraphics graphics, int x, int y, int w, int h) {
		theme().drawUIBackground(graphics, x - 8, y - 8, w + 16, h + 16);
	}

	public UIElement open(String path) {
		var entry = root.open(path);
		if (entry != null) {
			currentPath = path;
			if (entry instanceof ArchiveEntry ae) {
				// 在内容面板显示内容

				panel.select(ae);
				// 选中条目
				ae.getParent().select(ae);
				// 已读
				ae.read = ae.read();
			}
		}
		return entry;
	}

	public void scrollTo(UIElement widget) {
		if (widget != null) {
			scrollBar.setValue(widget.getScreenY());
		}
	}

	public UIElement find(String path) {
		return root.find(path);
	}

	@Override
	public void refresh() {
		setPosAndSize(0, 0, 100, (int) (ClientUtils.screenHeight() * 0.8F));

		recalcContentSize();
		for (UIElement element : elements) {
			element.refresh();
		}
		alignWidgets();

		if (getY() + getContentHeight() < getY() + getHeight() || -getOffsetY() > scrollBar.getMax()) {
			scrollBar.setValue(scrollBar.getMax());
		}
	}

	public void addCategory() {
		var tipCategory = TipHelper.getCategory(new Category(this, Component.translatable("gui.frostedheart.archive.category.tips")));
		root.getElements().add(tipCategory);
	}

	@Override
	public boolean onKeyPressed(int keyCode, int scanCode, int modifier) {
		if (CInputHelper.isNextKey(keyCode, scanCode, modifier) || CInputHelper.isPrevKey(keyCode, scanCode, modifier)) {
			Entry current = root.getSelected();
			if (current == null) {
				List<Entry> list = new ArrayList<>();
				CategoryHelper.collectAllEntries(root, list);
				if (!list.isEmpty()) {
					scrollTo(open(currentPath = CategoryHelper.path(list.get(0))));
					return true;
				}
				return super.onKeyPressed(keyCode, scanCode, modifier);
			}

			if (CInputHelper.isNextKey(keyCode, scanCode, modifier)) {
				current = CategoryHelper.next(current);
			} else {
				current = CategoryHelper.prev(current);
			}
			scrollTo(open(currentPath = CategoryHelper.path(current)));
			return true;
		}
		return super.onKeyPressed(keyCode, scanCode, modifier);
	}

	@Override
	public void addUIElements() {
		clearElement();
		if (TipClientCommand.editMode) {
			getElements().add(new Button(this, Component.literal("Add New Tip"), FlatIcon.WRENCH.toCIcon()) {
				@Override
				public void drawBackground(GuiGraphics graphics, int x, int y, int w, int h) {
					graphics.fill(x, y, x + w, y + h, theme().UIBGBorderColor());
					if (isMouseOver() && isEnabled()) {
						graphics.fill(x - 4, y, x - 2, y + h, theme().UIAltTextColor());
					}
				}

				@Override
				public void onClicked(MouseButton button) {
					if (button.is(MouseButton.LEFT)) {
						TipHelper.edit((String) null, theme());
					}
				}

				@Override
				public void refresh() {
					setSize(parent.getWidth(), Entry.DEF_HEIGHT);
				}
			});
			getElements().add(new Button(this, Component.literal("Edit Tips"), FlatIcon.CONFIG.toCIcon()) {
				@Override
				public void drawBackground(GuiGraphics graphics, int x, int y, int w, int h) {
					graphics.fill(x, y, x + w, y + h, theme().UIBGBorderColor());
					if (isMouseOver() && isEnabled()) {
						graphics.fill(x - 4, y, x - 2, y + h, theme().UIAltTextColor());
					}
				}

				@Override
				public void onClicked(MouseButton button) {
					if (button.is(MouseButton.LEFT)) {
						panel.setContent(panel -> {
							var list = new ArrayList<UIElement>();
							var lock = Component.literal("{@font F211 chorda:default -44224}");
							var unlock = Component.literal("{@font F241 chorda:default -4070097}");
							for (Tip tip : TipManager.INSTANCE.getSortedTips()) {
								list.add(LineHelper.text(panel, (TipManager.state().isUnlocked(tip) ? unlock : lock).copy().append(" ID: " + tip.id()))
									.button(Component.translatable("controls.reset"), FlatIcon.HISTORY, b -> TipManager.state().reset(tip))
									.button(Component.translatable("gui.frostedheart.unlock"), FlatIcon.UNLOCK, b -> TipManager.state().unlock(tip))
									.button(Component.translatable("selectServer.edit"), FlatIcon.CONFIG, b -> TipHelper.edit(tip.id(), theme()))
									.button(Component.translatable("gui.open"), FlatIcon.FILE, b -> ArchiveCategory.this.panel.setContent(t -> LineHelper.fromTip(tip, t))));
								list.add(LineHelper.text(panel, Component.translatable(tip.contents().get(0))).color(theme().UIAltTextColor()));
								list.add(LineHelper.br(panel));
							}
							return list;
						});
					}
				}

				@Override
				public void refresh() {
					setSize(parent.getWidth(), Entry.DEF_HEIGHT);
				}
			});
		}
		addCategory();
	}

	@Override
	public void alignWidgets() {
		align(0, 2, false);
	}

	public static class TipEntry extends ArchiveEntry {
		@Getter
		final String tip;

		public TipEntry(Category parent, String tip) {
			super(parent, Component.translatable(TipManager.INSTANCE.getTip(tip).contents().get(0)));
			this.tip = tip;
			read = isRead();
			setIcon(FlatIcon.LIST);
		}

		@Override
		public boolean isRead() {
			boolean hasUnread = !TipManager.state().isViewed(tip);
			if (hasUnread) {
				return false;
			}
			for (Tip child : TipManager.state().getChildren(tip())) {
				hasUnread = !TipManager.state().isViewed(child);
				if (hasUnread) {
					return false;
				}
			}
			return true;
		}

		@Override
		public boolean read() {
			TipManager.state().view(tip());
			for (Tip tip : TipManager.state().getChildren(tip())) {
				TipManager.state().view(tip, true);
			}
			return true;
		}

		public Tip tip() {
			return TipManager.INSTANCE.getTip(tip);
		}

		@Override
		public Collection<? extends UIElement> getContents(UIElement parent) {
			return LineHelper.fromTip(tip(), parent);
		}
	}

	public abstract static class ArchiveEntry extends Entry {
		protected boolean read;

		public ArchiveEntry(Category parent, Component title) {
			super(parent, title);
			read = isRead();
		}

		@Override
		public void render(GuiGraphics graphics, int x, int y, int w, int h) {
			super.render(graphics, x, y, w, h);
			if (!read) {
				float anim = AnimationUtil.progress(3000, "archive_unread", true);
				anim = ((float) Math.sin(anim * Math.PI * 2) * 0.5F + 0.5F) * 0.3F;
				graphics.fill(x, y, x + w, y + h, Colors.setAlpha(Colors.themeColor(), anim));
			}
		}

		public abstract boolean read();

		public abstract boolean isRead();

		public abstract Collection<? extends UIElement> getContents(UIElement parent);

		public Collection<UIElement> getExtraElements(UIElement parent) {
			return Collections.emptyList();
		}

		@Override
		public boolean onMousePressed(MouseButton button) {
			if (!isMouseOver()) return false;

			if (isEnabled() && isVisible() && button == MouseButton.LEFT) {
				if (getParent().getRoot().getParent() instanceof ArchiveCategory category) {
					category.open(CategoryHelper.path(this));
					return true;
				}
			}

			for (int i = elements.size() - 1; i >= 0; i--) {
				UIElement element = elements.get(i);
				if (element.isEnabled() && element.isVisible() && element.onMousePressed(button)) {
					return true;
				}
			}
			return false;
		}
	}
}
