/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.research.gui.editor;

import com.mojang.blaze3d.systems.RenderSystem;
import com.teammoeg.chorda.client.CInputHelper;
import com.teammoeg.chorda.client.cui.Button;
import com.teammoeg.chorda.client.cui.Layer;
import com.teammoeg.chorda.client.cui.MouseButton;
import com.teammoeg.chorda.client.cui.LayerScrollBar;
import com.teammoeg.chorda.client.cui.TextButton;
import com.teammoeg.chorda.client.cui.UIWidget;
import com.teammoeg.chorda.client.cui.UIWidget;
import com.teammoeg.chorda.client.icon.CIcons.CIcon;
import com.teammoeg.chorda.client.ui.CGuiHelper;
import com.teammoeg.chorda.client.widget.IconButton;
import com.teammoeg.chorda.lang.Components;
import com.teammoeg.frostedheart.util.client.Lang;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author  khjxiaogu
 */
public class EditListDialog<T> extends EditDialog {
    public static final Editor<Collection<String>> STRING_LIST = (p, l, v, c) -> new EditListDialog<>(p, l, v, "", EditPrompt.TEXT_EDITOR, e -> e, c).open();
    private final Consumer<Collection<T>> callback;
    private final Component title;
    private final Layer configPanel;
    private final Button buttonAccept, buttonCancel;
    private final List<T> list;
    private final Editor<T> editor;
    private final LayerScrollBar scroll;
    private final T def;
    private final Function<T, String> read;
    private final Function<T, CIcon> toicon;
    boolean modified;
    public EditListDialog(UIWidget p, Component label, Collection<T> vx, Editor<T> editor, Function<T, String> toread, Consumer<Collection<T>> li) {
        this(p, label, vx, null, editor, toread, null, li);
    }
    public EditListDialog(UIWidget p, Component label, Collection<T> vx, T def, Editor<T> editor, Function<T, String> toread, Consumer<Collection<T>> li) {
        this(p, label, vx, def, editor, toread, null, li);
    }

    public EditListDialog(UIWidget p, Component label, Collection<T> vx, T def, Editor<T> editor, Function<T, String> toread, Function<T, CIcon> icon, Consumer<Collection<T>> li) {
        super(p);
        callback = li;
        if (vx != null)
            list = new ArrayList<>(vx);
        else
            list = new ArrayList<>();
        title = (label).copy().withStyle(ChatFormatting.BOLD);
        this.editor = editor;
        this.def = def;
        this.read = toread;
        this.toicon = icon;
        int sw = 387;
        int sh = 203;
        this.setSize(sw, sh);
        configPanel = new Layer(this) {
            @Override
            public void addUIElements() {
                for (int i = 0; i < list.size(); i++) {
                    add(new ButtonConfigValue(this, i));
                }
                add(new ButtonAddValue(this));
            }

            @Override
            public void alignWidgets() {
                for (UIWidget w : super.elements) {
                    w.setWidth(super.getWidth() - 16);
                }
                align(false);
                //scroll.setMaxValue();
            }
        };

        scroll = new LayerScrollBar(this, configPanel);
        buttonAccept =TextButton.create(this, Components.empty(), IconButton.Icon.CHECK.toCIcon(), (button) -> {
            callback.accept(list);
            modified = false;
            close();
        });
        buttonCancel = TextButton.create(this, Components.empty(), IconButton.Icon.CROSS.toCIcon(), (button) -> close());
    }

    @Override
    public void addUIElements() {
        add(buttonAccept);
        add(buttonCancel);
        add(configPanel);
        add(scroll);
    }

    @Override
    public void alignWidgets() {
        configPanel.setPosAndSize(5, 25, width - 10, height - 30);
        configPanel.alignWidgets();
        scroll.setPosAndSize(width - 16, 25, 8, height - 30);

        buttonAccept.setPos(width - 26, 2);
        buttonCancel.setPos(width - 47, 2);
    }

    @Override
    public void drawBackground(GuiGraphics matrixStack, int x, int y, int w, int h) {
        CGuiHelper.drawUIBackground(matrixStack, x, y, w, h);
        matrixStack.drawString(getFont(), getTitle(), x, y-10, getLayerHolder().getFontColor());
    }

    @Override
    public Component getTitle() {
        return title;
    }

    @Override
    public void onClose() {
    }

    @Override
    public void onClosed() {
        if (modified) {
            ConfirmDialog.EDITOR.open(this, Components.str("Unsaved changes, discard?"), true, e -> {
                if (!e) open();
            });
        }
    }

    public class ButtonAddValue extends Button {
        public ButtonAddValue(Layer panel) {
            super(panel);
            setHeight(12);
            setTitle(Components.str("+ ").append(Lang.translateKey("gui.add")));
        }



        @Override
        public void render(GuiGraphics matrixStack, int x, int y, int w, int h) {
            boolean mouseOver = isMouseOver();

            if (mouseOver) {
               matrixStack.fill(x, y, x+w, y+h, 0x20FFFFFF);
            }
            matrixStack.drawString(getFont(), getTitle(), x+4, y+2, 0xFFFFFFFF);
        }

        @Override
        public void onClicked(MouseButton button) {
            CInputHelper.playClickSound();
            editor.open(this,Components.str( "New"), def, s -> {
                if (s != null) {
                    modified = true;
                    list.add(s);
                    ((Layer)parent).refresh();
                }
            });

        }
    }

    public class ButtonConfigValue extends Button {
        public final int index;

        public ButtonConfigValue(Layer panel, int i) {
            super(panel);
            index = i;
            setHeight(12);
        }

        @Override
        public void getTooltip(Consumer<Component> l) {
            if (getMouseX() >=  width - 19) {
                l.accept(Components.translatable("selectServer.delete"));
            } else {
                l.accept(Components.str(read.apply(list.get(index))));
            }
        }

        @Override
        public void render(GuiGraphics matrixStack, int x, int y, int w, int h) {
            boolean mouseOver = isMouseOver();
            int ioffset = 0;
            if (toicon != null) {
                toicon.apply(list.get(index)).draw(matrixStack, x + ioffset, y, 12, 12);
                ioffset += 13;
            }
            if (mouseOver) {

            	matrixStack.fill(x, y, x+w, y+h, 0x21FFFFFF);

                if (getMouseX() >= w - 19) {
                	matrixStack.fill(x + w - 19, y, x+w, y+h, 0x21FFFFFF);
                }
            }
            matrixStack.drawString(getFont(), read.apply(list.get(index)), x+4+ ioffset, y+2, 0xFFFFFFFF);

            if (mouseOver) {
            	matrixStack.drawString(getFont(), "[-]", x+w-16, y+2, 0xFFFFFFFF);
            }
        }

        @Override
        public void onClicked(MouseButton button) {
            CInputHelper.playClickSound();

            if (getMouseX() >=width - 19) {
                list.remove(index);
                modified = true;
                ((Layer)parent).refresh();

            } else {
                editor.open(this, Components.str("Edit"), list.get(index), s -> {
                    modified = true;
                    list.set(index, s);
                    ((Layer)parent).refresh();
                });
            }
        }
    }
}