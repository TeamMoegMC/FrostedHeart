/*
 * Copyright (c) 2022-2024 TeamMoeg
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

package com.teammoeg.frostedheart.research.gui.editor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;

import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.ui.Button;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.PanelScrollBar;
import dev.ftb.mods.ftblibrary.ui.SimpleButton;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.ui.WidgetLayout;
import dev.ftb.mods.ftblibrary.ui.WidgetType;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;

/**
 * @author LatvianModder, khjxiaogu
 */
public class EditListDialog<T> extends EditDialog {
    public static final Editor<Collection<String>> STRING_LIST = (p, l, v, c) -> {
        new EditListDialog<>(p, l, v, "", EditPrompt.TEXT_EDITOR, e -> e, c).open();
    };

    public class ButtonConfigValue extends Button {
        public final int index;

        public ButtonConfigValue(Panel panel, int i) {
            super(panel);
            index = i;
            setHeight(12);
        }

        @Override
        public void draw(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
            boolean mouseOver = getMouseY() >= 20 && isMouseOver();
            int ioffset = 0;
            if (toicon != null) {
                toicon.apply(list.get(index)).draw(matrixStack, x + ioffset, y, 12, 12);
                ioffset += 13;
            }
            if (mouseOver) {

                Color4I.WHITE.withAlpha(33).draw(matrixStack, x, y, w, h);

                if (getMouseX() >= x + w - 19) {
                    Color4I.WHITE.withAlpha(33).draw(matrixStack, x + w - 19, y, 19, h);
                }
            }

            theme.drawString(matrixStack, read.apply(list.get(index)), x + 4 + ioffset, y + 2, Color4I.BLACK, 0);

            if (mouseOver) {
                theme.drawString(matrixStack, "[-]", x + w - 16, y + 2, Color4I.BLACK, 0);
            }

            RenderSystem.color4f(1F, 1F, 1F, 1F);
        }

        @Override
        public void onClicked(MouseButton button) {
            playClickSound();

            if (getMouseX() >= getX() + width - 19) {
                list.remove(index);
                modified = true;
                parent.refreshWidgets();

            } else {
                editor.open(this, "Edit", list.get(index), s -> {
                    modified = true;
                    list.set(index, s);
                    parent.refreshWidgets();
                });
            }
        }

        @Override
        public void addMouseOverText(TooltipList l) {
            if (getMouseX() >= getX() + width - 19) {
                l.translate("selectServer.delete");
            } else {
                l.add(new StringTextComponent(read.apply(list.get(index))));
            }
        }
    }

    public class ButtonAddValue extends Button {
        public ButtonAddValue(Panel panel) {
            super(panel);
            setHeight(12);
            setTitle(new StringTextComponent("+ ").appendSibling(new TranslationTextComponent("gui.add")));
        }

        @Override
        public void draw(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
            boolean mouseOver = getMouseY() >= 20 && isMouseOver();

            if (mouseOver) {
                Color4I.WHITE.withAlpha(33).draw(matrixStack, x, y, w, h);
            }

            theme.drawString(matrixStack, getTitle(), x + 4, y + 2, Color4I.BLACK, 0);
            RenderSystem.color4f(1F, 1F, 1F, 1F);
        }

        @Override
        public void onClicked(MouseButton button) {
            playClickSound();
            editor.open(this, "New", def, s -> {
                if (s != null) {
                    modified = true;
                    list.add(s);
                    parent.refreshWidgets();
                }
            });

        }

        @Override
        public void addMouseOverText(TooltipList list) {
        }
    }

    private final Consumer<Collection<T>> callback;

    private final ITextComponent title;
    private final Panel configPanel;
    private final Button buttonAccept, buttonCancel;
    private final List<T> list;
    private final Editor<T> editor;
    private final PanelScrollBar scroll;
    private final T def;
    private final Function<T, String> read;
    private final Function<T, Icon> toicon;
    boolean modified;

    public EditListDialog(Widget p, String label, Collection<T> vx, T def, Editor<T> editor, Function<T, String> toread, Function<T, Icon> icon, Consumer<Collection<T>> li) {
        super(p);
        callback = li;
        if (vx != null)
            list = new ArrayList<>(vx);
        else
            list = new ArrayList<>();
        title = new StringTextComponent(label).mergeStyle(TextFormatting.BOLD);
        this.editor = editor;
        this.def = def;
        this.read = toread;
        this.toicon = icon;
        int sw = 387;
        int sh = 203;
        this.setSize(sw, sh);
        configPanel = new Panel(this) {
            @Override
            public void addWidgets() {
                for (int i = 0; i < list.size(); i++) {
                    add(new ButtonConfigValue(this, i));
                }
                add(new ButtonAddValue(this));
            }

            @Override
            public void alignWidgets() {
                for (Widget w : widgets) {
                    w.setWidth(width - 16);
                }

                scroll.setMaxValue(align(WidgetLayout.VERTICAL));
            }
        };

        scroll = new PanelScrollBar(this, configPanel);
        buttonAccept = new SimpleButton(this, new TranslationTextComponent("gui.accept"), Icons.ACCEPT, (widget, button) -> {
            callback.accept(list);
            modified = false;
            close();
        });
        buttonCancel = new SimpleButton(this, new TranslationTextComponent("gui.cancel"), Icons.CANCEL, (widget, button) -> close());
    }

    public EditListDialog(Widget p, String label, Collection<T> vx, T def, Editor<T> editor, Function<T, String> toread, Consumer<Collection<T>> li) {
        this(p, label, vx, def, editor, toread, null, li);
    }

    public EditListDialog(Widget p, String label, Collection<T> vx, Editor<T> editor, Function<T, String> toread, Consumer<Collection<T>> li) {
        this(p, label, vx, null, editor, toread, null, li);
    }

    @Override
    public void addWidgets() {
        add(buttonAccept);
        add(buttonCancel);
        add(configPanel);
        add(scroll);
    }

    @Override
    public void alignWidgets() {
        configPanel.setPosAndSize(5, 20, width - 10, height - 20);
        configPanel.alignWidgets();
        scroll.setPosAndSize(width - 16, 20, 16, height - 20);

        buttonAccept.setPos(width - 18, 2);
        buttonCancel.setPos(width - 38, 2);
    }

    @Override
    public void drawBackground(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
        theme.drawGui(matrixStack, x, y, w, h, WidgetType.NORMAL);

        theme.drawString(matrixStack, getTitle(), x, y - 10);
    }

    @Override
    public ITextComponent getTitle() {
        return title;
    }

    @Override
    public void onClosed() {
        if (modified) {
            ConfirmDialog.EDITOR.open(this, "Unsaved changes, discard?", true, e -> {
                if (!e) open();
            });
        }
    }

    @Override
    public void onClose() {
    }
}