package com.teammoeg.frostedheart.content.town.tabs;

import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.UIElement;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.function.Supplier;

public class BuildingInfoElement extends UIElement {

    private final Supplier<List<Component>> lineSource;
    private static final int LINE_HEIGHT = 12;
    private static final int PADDING_X = 5;
    private static final int PADDING_Y = 5;

    public BuildingInfoElement(UIElement parent, int x, int y, int width, int height,
                               Supplier<List<Component>> lineSource) {
        super(parent);
        this.lineSource = lineSource;
        this.setPos(x, y);
        this.setSize(width, height);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int x, int y, int w, int h, RenderingHint hint) {
        Minecraft mc = Minecraft.getInstance();
        List<Component> lines = lineSource.get();
        if (lines == null || lines.isEmpty()) return;

        guiGraphics.fill(x, y, x + w, y + h, 0xC0101010);


        guiGraphics.fill(x, y, x + w, y + 1, 0xFF373737);
        guiGraphics.fill(x, y, x + 1, y + h, 0xFF373737);
        guiGraphics.fill(x, y + h - 1, x + w, y + h, 0xFF8B8B8B);
        guiGraphics.fill(x + w - 1, y, x + w, y + h, 0xFF8B8B8B);


        for (int i = 0; i < lines.size(); i++) {
            int textY = y + PADDING_Y + i * LINE_HEIGHT;
            if (textY + mc.font.lineHeight > y + h - PADDING_Y) break;
            guiGraphics.drawString(mc.font, lines.get(i),
                    x + PADDING_X, textY, 0xFFFFFFFF, true);
        }
    }

    public static Component title(String text) {
        return Component.literal(text).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
    }

    public static Component separator() {
        return Component.literal("────────────────").withStyle(ChatFormatting.DARK_GRAY);
    }

    public static Component keyValue(String label, Object value) {
        return Component.literal(label + ": ")
                .withStyle(ChatFormatting.WHITE)
                .append(String.valueOf(value));
    }


    public static Component status(String label, boolean value) {
        ChatFormatting color = value ? ChatFormatting.GREEN : ChatFormatting.RED;
        return Component.literal(label + ": ")
                .withStyle(ChatFormatting.WHITE)
                .append(Component.literal(String.valueOf(value)).withStyle(color));
    }

    public static Component number(String label, double value) {
        String formatted = BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue() + "";
        return keyValue(label, formatted);
    }
}