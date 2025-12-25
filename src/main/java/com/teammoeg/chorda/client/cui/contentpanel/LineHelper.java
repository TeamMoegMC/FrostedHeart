package com.teammoeg.chorda.client.cui.contentpanel;

import com.teammoeg.chorda.client.cui.MouseButton;
import com.teammoeg.chorda.client.cui.UIWidget;
import com.teammoeg.chorda.client.ui.Colors;
import com.teammoeg.chorda.lang.Components;
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
    public static TextLine text(UIWidget parent, String text) {
        return text(parent, Component.literal(text));
    }

    public static TextLine text(UIWidget parent, Component text) {
        return new TextLine(parent, text, Alignment.LEFT);
    }

    public static ImageLine img(UIWidget parent, String imageLocation) {
        return img(parent, ResourceLocation.tryParse(imageLocation));
    }

    public static ImageLine img(UIWidget parent, ResourceLocation imageLocation) {
        return new ImageLine(parent, imageLocation, Alignment.CENTER);
    }

    public static ItemRow items(UIWidget parent, ItemStack... items) {
        return items(parent, List.of(items));
    }

    public static ItemRow items(UIWidget parent, Collection<ItemStack> items) {
        return new ItemRow(parent, items, Alignment.CENTER);
    }

    public static EmptyLine space(UIWidget parent) {
        return space(parent, 8);
    }

    public static EmptyLine space(UIWidget parent, int height) {
        return new EmptyLine(parent, height);
    }

    public static BreakLine br(UIWidget parent) {
        return new BreakLine(parent);
    }

    public static BreakLine br(UIWidget parent, int color) {
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
            Consumer<MouseButton> clickAction = null;
            Component btnDesc = null;
            if (t1.hasClickAction()) {
                clickAction = b -> t1.runClickAction();
                btnDesc = ClickActions.getDesc(t1.getClickAction(), t1.getClickActionContent());
            }

            // title
            if (j == 0) {
                lines.add(text(parent, tipContents.get(0)).quote(t1.getFontColor()).button(btnDesc, clickAction));
                lines.add(br(parent));
            // new tip notification
            } else if (!TipManager.INSTANCE.state().isViewed(t1)) {
                lines.add(br(parent));
                lines.add(text(parent, Component.translatable("gui.frostedheart.archive.new_tip")).title(t1.getFontColor(), 1).color(Colors.readableColor(t1.getFontColor())));
            } else {
                lines.add(space(parent));
            }
            // if child tip has different title or click action
            if (j != 0 && (!tipContents.get(0).equals(tip.getContents().get(0)) || tip.hasClickAction())) {
                lines.add(br(parent));
                lines.add(text(parent, tipContents.get(0)).quote(t1.getFontColor()).button(btnDesc, clickAction));
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
