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

package com.teammoeg.chorda.client.cui.contentpanel;

import com.teammoeg.chorda.client.cui.base.MouseButton;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.ui.Colors;
import com.teammoeg.chorda.text.Components;
import com.teammoeg.frostedheart.FrostedHud;
import com.teammoeg.frostedheart.bootstrap.common.FHItems;
import com.teammoeg.frostedheart.content.archive.Alignment;
import com.teammoeg.frostedheart.content.tips.ClickActions;
import com.teammoeg.frostedheart.content.tips.Tip;
import com.teammoeg.frostedheart.content.tips.TipManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public class LineHelper {
    public static TextLine text(UIElement parent, String text) {
        return text(parent, Component.literal(text));
    }

    public static TextLine text(UIElement parent, Component text) {
        return new TextLine(parent, text, Alignment.LEFT);
    }

    public static ImageLine img(UIElement parent, String imageLocation) {
        return img(parent, ResourceLocation.tryParse(imageLocation));
    }

    public static ImageLine img(UIElement parent, ResourceLocation imageLocation) {
        return new ImageLine(parent, imageLocation, Alignment.CENTER);
    }

    public static ItemRow items(UIElement parent, ItemStack... items) {
        return items(parent, List.of(items));
    }

    public static ItemRow items(UIElement parent, Collection<ItemStack> items) {
        return new ItemRow(parent, items, Alignment.CENTER);
    }

    public static EmptyLine space(UIElement parent) {
        return space(parent, 8);
    }

    public static EmptyLine space(UIElement parent, int height) {
        return new EmptyLine(parent, height);
    }

    public static BreakLine br(UIElement parent) {
        return new BreakLine(parent);
    }

    public static BreakLine br(UIElement parent, int color) {
        return br(parent).color(color);
    }

    public static List<Line<?>> fromTipWithoutChildren(Tip tip, ContentPanel parent) {
        List<Line<?>> lines = new ArrayList<>();

        List<Component> contents = tip.getContents();
        // title
        var title = contents.get(0);
        if (FrostedHud.renderDebugOverlay) {
            var id = Component.literal(" | ID: " + tip.getId()).withStyle(Components.color(Colors.L_BG_GRAY));
            title = Component.empty().append(title).append(id);
        }
        // button
        Consumer<MouseButton> clickAction = null;
        Component btnDesc = null;
        if (tip.hasClickAction()) {
            clickAction = b -> tip.runClickAction();
            btnDesc = ClickActions.getDesc(tip.getClickAction(), tip.getClickActionContent());
        }
        lines.add(text(parent, title).button(btnDesc, clickAction));
        // contents
        for (int i = 1; i < contents.size(); i++) {
            lines.add(text(parent, contents.get(i)));
        }
        // image
        if (tip.getImage() != null) {
            lines.add(space(parent));
            var img = img(parent, tip.getImage());
            if (img.getImgSize() != null && img.getImgSize().width < 64) {
                img.bgColor(Colors.L_BG_GRAY);
            }
            lines.add(img);
        }
        lines.add(items(parent, FHItems.ICE_SKATES.asStack(), FHItems.SNOWSHOES.asStack()));
        return lines;
    }

    public static List<Line<?>> fromTip(Tip tip, ContentPanel parent) {
        List<Line<?>> lines = new ArrayList<>();
        List<Tip> tips = new ArrayList<>();
        tips.add(tip);
        tips.addAll(TipManager.INSTANCE.state().getChildren(tip));

        for (int j = 0; j < tips.size(); j++) {
            Tip t1 = tips.get(j);
            if (t1.isHide()) continue;
            var tipContents = t1.getContents();
            int color = Colors.cyanToTheme(t1.getFontColor());
            Consumer<MouseButton> clickAction = null;
            Component btnDesc = null;
            if (t1.hasClickAction()) {
                clickAction = b -> t1.runClickAction();
                btnDesc = ClickActions.getDesc(t1.getClickAction(), t1.getClickActionContent());
            }

            // title
            if (j == 0) {
                lines.add(text(parent, tipContents.get(0)).quote(color).button(btnDesc, clickAction));
                lines.add(br(parent));
            // new tip notification
            } else if (!TipManager.INSTANCE.state().isViewed(t1)) {
                lines.add(br(parent));
                lines.add(text(parent, Component.translatable("gui.frostedheart.archive.new_tip")).title(color, 1).color(Colors.readableColor(color)));
            } else {
                lines.add(space(parent));
            }
            // if child tip has different title or click action
            if (j != 0 && (!tipContents.get(0).equals(tip.getContents().get(0)) || tip.hasClickAction())) {
                lines.add(br(parent));
                lines.add(text(parent, tipContents.get(0)).quote(color).button(btnDesc, clickAction));
            }
            // lines
            for (int i = 1; i < tipContents.size(); i++) {
                Component line = tipContents.get(i);
                if (!line.getString().isBlank()) {
                    lines.add(text(parent, line));
                }
            }
            // image
            if (t1.getImage() != null) {
                var img = img(parent, t1.getImage());
                if (img.getImgSize() != null && img.getImgSize().width < 64) {
                    img.bgColor(Colors.L_BG_GRAY);
                }
                lines.add(img);
            }
            // debug
            if (FrostedHud.renderDebugOverlay) {
                lines.add(text(parent, "ID: " + t1.getId()).color(Colors.L_BG_GRAY).alignment(Alignment.RIGHT));
            }
        }
        return lines;
    }
}
