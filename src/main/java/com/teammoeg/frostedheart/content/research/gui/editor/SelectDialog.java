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

import blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler;
import blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler.IMultiblock;

import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.CIconFTBWrapper;
import com.teammoeg.chorda.client.cui.Button;
import com.teammoeg.chorda.client.cui.Layer;
import com.teammoeg.chorda.client.cui.MouseButton;
import com.teammoeg.chorda.client.cui.LayerScrollBar;
import com.teammoeg.chorda.client.cui.TextBox;
import com.teammoeg.chorda.client.cui.UIWidget;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.chorda.client.icon.CIcons.CIcon;
import com.teammoeg.chorda.client.ui.CGuiHelper;
import com.teammoeg.chorda.lang.Components;
import com.teammoeg.chorda.util.CRegistryHelper;
import com.teammoeg.frostedheart.content.research.FHResearch;
import com.teammoeg.frostedheart.content.research.gui.TechScrollBar;
import com.teammoeg.frostedheart.content.research.research.Research;
import net.minecraft.advancements.Advancement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class SelectDialog<T> extends EditDialog {
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
                advx -> CIcons.getIcon(advx.getDisplay().getIcon())
        ).open();

    };
    public static final Editor<EntityType<?>> EDITOR_ENTITY = (p, l, v, c) -> new SelectDialog<>(p, l, v, c, CRegistryHelper::getEntities, EntityType::getDescription, e -> new String[]{e.getDescription().getString(), CRegistryHelper.getRegistryName(e).toString()}
    ).open();
    public static final Editor<String> EDITOR_ITEM_TAGS = (p, l, v, c) -> new SelectDialog<>(p, l, v, c, () -> ForgeRegistries.ITEMS.tags().getTagNames().map(t -> t.location()).map(ResourceLocation::toString).collect(Collectors.toSet())).open();
    public LayerScrollBar scroll;
    public SelectorList rl;
    public TextBox searchBox;
    Component lbl;
    T val;
    Consumer<T> cb;
    Supplier<Collection<T>> fetcher;
    Function<T, Component> tostr;
    Function<T, String[]> tosearch;
    Function<T, CIcon> toicon;

    public SelectDialog(UIWidget panel, Component lbl, T val, Consumer<T> cb, Supplier<Collection<T>> fetcher) {
        this(panel, lbl, val, cb, fetcher, e -> Components.str(e.toString()), e -> new String[]{e.toString()}, e -> CIcons.nop());
    }

    public SelectDialog(UIWidget panel, Component lbl, T val, Consumer<T> cb, Supplier<Collection<T>> fetcher,
                        Function<T, Component> tostr) {
        this(panel, lbl, val, cb, fetcher, tostr, e -> new String[]{tostr.apply(val).getString()}, e -> CIcons.nop());
    }

    public SelectDialog(UIWidget panel, Component lbl, T val, Consumer<T> cb, Supplier<Collection<T>> fetcher,
                        Function<T, Component> tostr, Function<T, String[]> tosearch) {
        this(panel, lbl, val, cb, fetcher, tostr, tosearch, e -> CIcons.nop());
    }

    public SelectDialog(UIWidget panel, Component lbl, T val, Consumer<T> cb, Supplier<Collection<T>> fetcher,
                        Function<T, Component> tostr, Function<T, String[]> tosearch, Function<T, CIcon> toicon) {
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


	public static <R> Function<R, Component> wrap(Function<R, Object> str) {
        return e -> Components.str(String.valueOf(str.apply(e)));
    }

    @Override
    public void addUIElements() {

        rl = new SelectorList(this);
        searchBox = new TextBox(this) {
            @Override
            public void onTextChanged() {
                rl.refresh();
            }
        };
        searchBox.ghostText = "Search...";
        searchBox.setFocused(true);
        rl.setPosAndSize(5, 25, width-18, height-30);
        scroll = new LayerScrollBar(this, rl);
        add(rl);
        add(scroll);
        add(searchBox);
        searchBox.setPosAndSize(5, 5, width-12, 18);
        scroll.setPosAndSize(width-12, 25,8, height-25);

    }

    @Override
    public void alignWidgets() {

    }

    @Override
    public void render(GuiGraphics matrixStack, int x, int y, int w, int h) {
        super.render(matrixStack, x, y, w, h);
        matrixStack.drawString(getFont(), lbl, x, y-10, getLayerHolder().getFontColor());
    }

    @Override
    public void drawBackground(GuiGraphics matrixStack, int x, int y, int w, int h) {
       CGuiHelper.drawUIBackground(matrixStack, x, y, w, h);
    }

    @Override
    public void onClose() {
    }

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
        public void getTooltip(Consumer<Component> list) {
            list.accept(tostr.apply(obj));
        }

        @Override
        public void render(GuiGraphics matrixStack, int x, int y, int w, int h) {
            //CGuis.setupDrawing();
            
            matrixStack.blitNineSliced(AbstractWidget.WIDGETS_LOCATION, x, y, w, h, 20, 4, 200, 20, 0, this.getTextureY());
            this.drawIcon(matrixStack, x + 1, y + 1, 16, 16);
            matrixStack.drawString(getFont(), t, x + 18, y + 6,getLayerHolder().getFontColor());


        }
    	private int getTextureY() {
    		int i = 1;
    		if (val == this.obj) {
    			i = 0;
    		} else if (this.isMouseOver()) {
    			i = 2;
    		}

    		return 46 + i * 20;
    	}
        @Override
        public void onClicked(MouseButton mouseButton) {
            cb.accept(obj);
            close();
        }
    }

    public class SelectorList extends Layer {
        public SelectorList(UIWidget panel) {
            super(panel);
            this.setWidth(200);

        }

        @Override
        public void addUIElements() {
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
                button.setPosAndSize(2, offset, width-4, 16);
                offset += 18;
            }
            //scroll.setMaxValue(offset);
        }

        @Override
        public void alignWidgets() {

        }

    }
}
