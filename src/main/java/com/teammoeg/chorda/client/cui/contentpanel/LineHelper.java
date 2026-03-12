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

import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.icon.FlatIcon;
import com.teammoeg.chorda.math.Colors;
import com.teammoeg.frostedheart.content.archive.Alignment;
import com.teammoeg.frostedheart.content.tips.Tip;
import com.teammoeg.frostedheart.content.tips.TipHelper;
import com.teammoeg.frostedheart.content.tips.TipManager;
import com.teammoeg.frostedheart.infrastructure.command.TipCommand;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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

    public static List<Line<?>> fromTip(Tip tipInput, ContentPanel parent) {
        List<Line<?>> lines = new ArrayList<>();
        List<Tip> tips = new ArrayList<>();
        tips.add(tipInput);
        tips.addAll(TipManager.state().getChildren(tipInput));

        for (int j = 0; j < tips.size(); j++) {
            final Tip tip = tips.get(j);
            if (tip.display().hide()) continue;
            var tipContents = tip.contents();
            int color = Colors.cyanToTheme(tip.display().fontColor());

            // title
            if (j == 0) {
                lines.add(text(parent, Component.translatable(tipContents.get(0))).color(parent.theme().UIAltTextColor()).scale(2).button(tip.clickAction()));
            // new tip notification
            } else if (!TipManager.state().isViewed(tip)) {
                lines.add(br(parent));
                lines.add(text(parent, Component.translatable("gui.frostedheart.archive.new_tip")).title(color, 1).color(Colors.readableColor(color)));
            }
            // if child tip has different title or has click action
            if (j != 0 && (TipHelper.hasClickAction(tip) || !tipContents.get(0).equals(tipInput.contents().get(0)))) {
                lines.add(space(parent));
                lines.add(text(parent, Component.translatable(tipContents.get(0))).quote(color).button(tip.clickAction()));
            }
            // lines
            for (int i = 1; i < tipContents.size(); i++) {
                String text = tipContents.get(i);
                if (!text.isBlank()) {
                    lines.add(text(parent, Component.translatable(text)));
                }
            }
            // items
            if (!tip.display().displayItems().isEmpty()) {
                lines.add(items(parent, tip.display().displayItems()));
            }
            // image
            if (TipHelper.hasImage(tip)) {
                var img = img(parent, tip.image());
                lines.add(img);
            }
            // debug
            if (TipCommand.editMode) {
                lines.add(text(parent, "ID: " + tip.id())
                        .color(parent.theme().UIBGBorderColor())
                        .alignment(Alignment.RIGHT)
                        .button(Component.translatable("controls.reset"), FlatIcon.HISTORY, b -> TipManager.state().reset(tip))
                        .button(Component.translatable("selectServer.edit"), FlatIcon.WRENCH, b -> TipHelper.edit(tip.id(), parent.theme()))
                );
                lines.add(space(parent));
            }
        }
        return lines;
    }
}
