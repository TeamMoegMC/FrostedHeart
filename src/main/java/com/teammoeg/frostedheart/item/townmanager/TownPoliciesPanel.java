/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.item.townmanager;

import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.MouseButton;
import com.teammoeg.chorda.client.cui.base.TooltipBuilder;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.TownCareLaw;
import com.teammoeg.frostedheart.content.town.TownPolicyState;
import com.teammoeg.frostedheart.content.town.network.TownPolicyEditRequestPacket;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import com.teammoeg.frostedheart.content.town.tabs.TownTextLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

/** First tree-like policy domain: one of three mutually-exclusive care laws. */
public final class TownPoliciesPanel extends UIElement {
    private static final int ROW_TOP = 48;
    private static final int ROW_HEIGHT = 44;
    private final Supplier<TeamTown> townSource;

    public TownPoliciesPanel(UIElement parent, int x, int y, Supplier<TeamTown> townSource) {
        super(parent);
        this.townSource = townSource;
        setPos(x, y);
        setSize(TownManagerScreen.CONTENT_WIDTH, TownManagerScreen.CONTENT_HEIGHT);
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, int width, int height,
                       RenderingHint hint) {
        drawPanel(graphics, x, y, width, height);
        Font font = Minecraft.getInstance().font;
        graphics.drawString(font,
                Component.translatable("gui.frostedheart.town_manager.policies"),
                x + 4, y + 4, 0xFFFFAA00, true);
        TeamTown town = townSource.get();
        if (town == null) return;
        TownPolicyState state = town.getPolicyState();
        long townDay = town.getTownData().map(data -> data.getTownDay()).orElse(0L);
        long cooldown = state.remainingCooldown(townDay,
                FHConfig.SERVER.TOWN.RESIDENT_RULES.townPolicyCooldownDays.get());
        Component status = state.hasPendingChanges()
                ? Component.translatable("gui.frostedheart.town_manager.policy_pending")
                : cooldown > 0
                ? Component.translatable("gui.frostedheart.town_manager.policy_cooldown", cooldown)
                : Component.translatable("gui.frostedheart.town_manager.policy_ready");
        graphics.drawString(font,
                TownTextLayout.ellipsize(font, status.getString(), width - 8),
                x + 4, y + 18, cooldown > 0 ? 0xFFFFAA00 : 0xFFAAAAAA, false);
        graphics.drawString(font,
                Component.translatable("gui.frostedheart.town_manager.policy_residential_care"),
                x + 4, y + 34, 0xFFFFFFFF, true);

        for (TownCareLaw law : TownCareLaw.values()) {
            int rowY = y + ROW_TOP + law.ordinal() * ROW_HEIGHT;
            boolean active = state.careLaw() == law;
            boolean pending = state.hasPendingChanges() && state.displayedCareLaw() == law;
            boolean hovered = getMouseY() >= ROW_TOP + law.ordinal() * ROW_HEIGHT
                    && getMouseY() < ROW_TOP + (law.ordinal() + 1) * ROW_HEIGHT;
            int color = pending ? 0xA0665522 : active ? 0xA0246633
                    : hovered && cooldown == 0 ? 0x90444444 : 0x90202020;
            graphics.fill(x + 2, rowY, x + width - 3, rowY + ROW_HEIGHT - 3, color);
            String marker = pending ? "→ " : active ? "✓ " : "  ";
            graphics.drawString(font, marker + Component.translatable(lawKey(law)).getString(),
                    x + 7, rowY + 5, pending ? 0xFFFFCC55 : 0xFFFFFFFF, true);
            Component description = Component.translatable(lawKey(law) + ".description");
            graphics.drawString(font,
                    TownTextLayout.ellipsize(font, description.getString(), width - 18),
                    x + 9, rowY + 20, 0xFFAAAAAA, false);
        }
    }

    @Override
    public boolean onMousePressed(MouseButton button) {
        if (button != MouseButton.LEFT || !isMouseOver()) return false;
        if (getMouseY() < ROW_TOP) return false;
        int index = (int) ((getMouseY() - ROW_TOP) / ROW_HEIGHT);
        if (index < 0 || index >= TownCareLaw.values().length) return false;
        TeamTown town = townSource.get();
        if (town == null) return false;
        long day = town.getTownData().map(data -> data.getTownDay()).orElse(0L);
        TownPolicyState state = town.getPolicyState();
        TownCareLaw selected = TownCareLaw.values()[index];
        if (state.remainingCooldown(day,
                FHConfig.SERVER.TOWN.RESIDENT_RULES.townPolicyCooldownDays.get()) > 0
                || state.displayedCareLaw() == selected) return true;
        FHNetwork.INSTANCE.sendToServer(new TownPolicyEditRequestPacket(selected));
        return true;
    }

    @Override
    public void getTooltip(TooltipBuilder tooltip) {
        if (!isMouseOver()) return;
        tooltip.accept(Component.translatable(
                "gui.frostedheart.town_manager.policy_global_cooldown_help"));
        tooltip.accept(Component.translatable(
                "gui.frostedheart.town_manager.policy_next_settlement_help"));
    }

    private static String lawKey(TownCareLaw law) {
        return "gui.frostedheart.town_manager.policy." + law.id();
    }

    private static void drawPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, 0xE0101010);
        graphics.fill(x, y, x + width, y + 1, 0xFF373737);
        graphics.fill(x, y, x + 1, y + height, 0xFF373737);
        graphics.fill(x, y + height - 1, x + width, y + height, 0xFF8B8B8B);
        graphics.fill(x + width - 1, y, x + width, y + height, 0xFF8B8B8B);
    }
}
