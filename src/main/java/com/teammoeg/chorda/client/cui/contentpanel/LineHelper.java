package com.teammoeg.chorda.client.cui.contentpanel;

import com.teammoeg.chorda.client.cui.UIWidget;
import com.teammoeg.chorda.client.ui.Colors;
import com.teammoeg.frostedheart.FrostedHud;
import com.teammoeg.frostedheart.content.archive.Alignment;
import com.teammoeg.frostedheart.content.tips.Tip;
import com.teammoeg.frostedheart.content.tips.TipManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
        return new EmptyLine(parent);
    }

    public static BreakLine br(UIWidget parent) {
        return new BreakLine(parent);
    }

    public static BreakLine br(UIWidget parent, int color) {
        return br(parent).color(color);
    }

    public static List<Line<?>> fromTip(Tip tip, ContentPanel affectedPanel) {
        List<Line<?>> lines = new ArrayList<>();
        List<Tip> tips = new ArrayList<>();
        tips.add(tip);
        tips.addAll(TipManager.INSTANCE.state().getChildren(tip));

        for (int j = 0; j < tips.size(); j++) {
            Tip t1 = tips.get(j);
            if (t1.isHide()) continue;
            var tipContents = t1.getContents();
            if (j == 0) {
                lines.add(LineHelper.text(affectedPanel, tipContents.get(0)).quote(t1.getFontColor()));
                lines.add(LineHelper.br(affectedPanel));
            } else if (!TipManager.INSTANCE.state().isViewed(t1)) {
                lines.add(LineHelper.br(affectedPanel));
                lines.add(LineHelper.text(affectedPanel, Component.translatable("gui.frostedheart.archive.new_tip")).title(t1.getFontColor(), 1).color(Colors.readableColor(t1.getFontColor())));
            } else {
                lines.add(LineHelper.br(affectedPanel));
            }
            if (j != 0 && !tipContents.get(0).equals(tip.getContents().get(0))) {
                lines.add(LineHelper.text(affectedPanel, tipContents.get(0)).quote(t1.getFontColor()));
            }
            for (int i = 1; i < tipContents.size(); i++) {
                Component line = tipContents.get(i);
                if (!line.getString().isBlank()) {
                    lines.add(LineHelper.text(affectedPanel, line));
                }
            }
            if (t1.getImage() != null) {
                var img = LineHelper.img(affectedPanel, t1.getImage());
                if (img.getImgSize() != null && img.getImgSize().width < 64) {
                    img.bgColor(Colors.L_BG_GRAY);
                }
                lines.add(img);
            }
            if (FrostedHud.renderDebugOverlay) {
                lines.add(LineHelper.text(affectedPanel, "ID: " + t1.getId()).color(Colors.L_BG_GRAY).alignment(Alignment.RIGHT));
            }
        }
        return lines;
    }
}
