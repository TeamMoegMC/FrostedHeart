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

package com.teammoeg.frostedheart.content.research.gui.editor;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.teammoeg.frostedheart.content.research.FHResearch;
import com.teammoeg.frostedheart.content.research.gui.FHIcons;
import com.teammoeg.frostedheart.content.research.gui.TechScrollBar;
import com.teammoeg.frostedheart.content.research.research.Research;
import com.teammoeg.frostedheart.util.RegistryUtils;
import com.teammoeg.frostedheart.util.Lang;
import com.teammoeg.frostedheart.util.client.ClientUtils;

import blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler;
import blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler.IMultiblock;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.ui.Button;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.PanelScrollBar;
import dev.ftb.mods.ftblibrary.ui.TextBox;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.ui.WidgetType;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import net.minecraft.advancements.Advancement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;

public class SelectDialog<T> extends EditDialog {
    public class SelectorButton extends Button {
        T obj;
        SelectorList listPanel;
        Component t;

        public SelectorButton(SelectorList panel, T obj) {
            super(panel, Component.empty(), toicon.apply(obj));
            this.obj = obj;
            this.listPanel = panel;
            t = tostr.apply(obj);

        }

        @Override
        public void addMouseOverText(TooltipList list) {
            list.add(tostr.apply(obj));
        }

        @Override
        public void draw(GuiGraphics matrixStack, Theme theme, int x, int y, int w, int h) {
            //GuiHelper.setupDrawing();
            if (val == this.obj)
                theme.drawButton(matrixStack, x, y, w, h, WidgetType.DISABLED);
            else
                theme.drawButton(matrixStack, x, y, w, h, WidgetType.mouseOver(isMouseOver()));
            this.drawIcon(matrixStack, theme, x + 1, y + 1, 16, 16);
            theme.drawString(matrixStack, t, x + 18, y + 6);


        }

        @Override
        public void onClicked(MouseButton mouseButton) {
            cb.accept(obj);
            close();
        }
    }

    public class SelectorList extends Panel {
        public SelectorList(Panel panel) {
            super(panel);
            this.setWidth(200);

        }

        @Override
        public void addWidgets() {
            int offset = 0;
            String stext = searchBox.getText();
            for (T r : fetcher.get()) {
                if (!stext.isEmpty()) {
                    boolean flag = false;
                    for (String s : tosearch.apply(r)) {
                        if (s.contains(stext)) {
                            flag = true;
                            break;
                        }
                    }
                    if (!flag) continue;
                }
                SelectorButton button = new SelectorButton(this, r);
                add(button);
                button.setPosAndSize(4, offset, width, 16);
                offset += 18;
            }
            //scroll.setMaxValue(offset);
        }

        @Override
        public void alignWidgets() {

        }

    }
    public static final Editor<Research> EDITOR_RESEARCH = (p, l, v, c) -> new SelectDialog<>(p, l, v, c, FHResearch::getAllResearch,
            Research::getName, e -> new String[]{e.getId(), e.getName().getString()},
            Research::getIcon
    ).open();
    public static final Editor<IMultiblock> EDITOR_MULTIBLOCK = (p, l, v, c) -> new SelectDialog<>(p, l, v, c, MultiblockHandler::getMultiblocks,
            wrap(IMultiblock::getUniqueName)
    ).open();
    public static final Editor<ResourceLocation> EDITOR_ADVANCEMENT = (p, l, v, c) -> {
        ClientAdvancements cam = ClientUtils.mc().player.connection.getAdvancements();
        Advancement adv = cam.getAdvancements().get(v);

        new SelectDialog<>(p, l, adv, e -> c.accept(e.getId()), () -> cam.getAdvancements().getAllAdvancements(),
                Advancement::getChatComponent, advx -> new String[]{advx.getChatComponent().getString(), advx.getId().toString()},
                advx -> FHIcons.getIcon(advx.getDisplay().getIcon())
        ).open();
        
    };
    public static final Editor<EntityType<?>> EDITOR_ENTITY = (p, l, v, c) -> new SelectDialog<>(p, l, v, c, RegistryUtils::getEntities, EntityType::getDescription, e -> new String[]{e.getDescription().getString(), RegistryUtils.getRegistryName(e).toString()}
    ).open();
    public static final Editor<String> EDITOR_ITEM_TAGS = (p, l, v, c) -> new SelectDialog<>(p, l, v, c, () -> ForgeRegistries.ITEMS.tags().getTagNames().map(t->t.location()).map(ResourceLocation::toString).collect(Collectors.toSet())).open();
    String lbl;
    T val;
    Consumer<T> cb;
    Supplier<Collection<T>> fetcher;
    Function<T, Component> tostr;
    Function<T, String[]> tosearch;

    Function<T, Icon> toicon;

    public PanelScrollBar scroll;

    public SelectorList rl;

    public TextBox searchBox;

    public static <R> Function<R, Component> wrap(Function<R, Object> str) {
        return e -> Lang.str(String.valueOf(str.apply(e)));
    }
    public SelectDialog(Widget panel, String lbl, T val, Consumer<T> cb, Supplier<Collection<T>> fetcher) {
        this(panel, lbl, val, cb, fetcher, e -> Lang.str(e.toString()), e -> new String[]{e.toString()}, e -> Icon.empty());
    }
    public SelectDialog(Widget panel, String lbl, T val, Consumer<T> cb, Supplier<Collection<T>> fetcher,
                        Function<T, Component> tostr) {
        this(panel, lbl, val, cb, fetcher, tostr, e -> new String[]{tostr.apply(val).getString()}, e -> Icon.empty());
    }

    public SelectDialog(Widget panel, String lbl, T val, Consumer<T> cb, Supplier<Collection<T>> fetcher,
                        Function<T, Component> tostr, Function<T, String[]> tosearch) {
        this(panel, lbl, val, cb, fetcher, tostr, tosearch, e -> Icon.empty());
    }

    public SelectDialog(Widget panel, String lbl, T val, Consumer<T> cb, Supplier<Collection<T>> fetcher,
                        Function<T, Component> tostr, Function<T, String[]> tosearch, Function<T, Icon> toicon) {
        super(panel);
        this.lbl = lbl;
        this.val = val;
        this.cb = cb;
        this.fetcher = fetcher;
        this.tostr = tostr;
        this.tosearch = tosearch;
        this.toicon = toicon;
        setSize(400, 300);
    }

    @Override
    public void addWidgets() {

        rl = new SelectorList(this);
        searchBox = new TextBox(this) {
            @Override
            public void onTextChanged() {
                rl.refreshWidgets();
            }
        };
        searchBox.ghostText = "Search...";
        searchBox.setFocused(true);
        rl.setPosAndSize(5, 25, 360, 270);
        scroll = new TechScrollBar(this, rl);
        add(rl);
        add(scroll);
        add(searchBox);
        searchBox.setPosAndSize(0, 0, width, 20);
        scroll.setPos(370, 20);
        scroll.setSize(8, height);

    }

    @Override
    public void alignWidgets() {

    }

    @Override
    public void draw(GuiGraphics matrixStack, Theme theme, int x, int y, int w, int h) {
        super.draw(matrixStack, theme, x, y, w, h);
        theme.drawString(matrixStack, lbl, x, y - 10);
    }

    @Override
    public void drawBackground(GuiGraphics matrixStack, Theme theme, int x, int y, int w, int h) {
        theme.drawGui(matrixStack, x, y, w, h, WidgetType.NORMAL);
    }

    @Override
    public void onClose() {
    }
}
